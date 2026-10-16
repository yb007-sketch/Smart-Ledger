package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_ledger.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.let { db ->
                    if (db.isOpen) {
                        db.close()
                    }
                }
                INSTANCE = null
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()
            val transactionDao = database.transactionDao()
            val budgetDao = database.budgetDao()

            // 1. Initial Accounts
            val cash = AccountEntity(id = 1, name = "CASH", type = AccountType.CASH, balance = 250.00, colorHex = "#10B981")
            val debit = AccountEntity(id = 2, name = "DebitCard", type = AccountType.DEBIT_CARD, balance = 3850.00, colorHex = "#3B82F6")
            val amex = AccountEntity(id = 3, name = "AMEX", type = AccountType.CREDIT_CARD, balance = -587.59, creditLimit = 5000.00, billingCycleDay = 15, colorHex = "#F59E0B")
            accountDao.insertAccounts(listOf(cash, debit, amex))

            // 2. Initial Categories
            val expenseCategories = listOf(
                CategoryEntity(name = "Food", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#10B981", subcategories = "Dining Out,Cafe,Fast Food", sortOrder = 1),
                CategoryEntity(name = "Grocery", type = TransactionType.EXPENSE, iconName = "shopping_cart", colorHex = "#EF4444", subcategories = "Supermarket,Produce,Pantry", sortOrder = 2),
                CategoryEntity(name = "Transportation", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = "#F59E0B", subcategories = "Fuel,Public Transit,Parking,Maintenance", sortOrder = 3),
                CategoryEntity(name = "House Rent &etc", type = TransactionType.EXPENSE, iconName = "home", colorHex = "#EC4899", subcategories = "Rent,Mortgage,Utilities,Repairs", sortOrder = 4),
                CategoryEntity(name = "Apparel", type = TransactionType.EXPENSE, iconName = "checkroom", colorHex = "#8B5CF6", subcategories = "Clothes,Shoes,Accessories", sortOrder = 5),
                CategoryEntity(name = "Health", type = TransactionType.EXPENSE, iconName = "medical_services", colorHex = "#06B6D4", subcategories = "Pharmacy,Doctor,Dentist,Fitness", sortOrder = 6),
                CategoryEntity(name = "Education", type = TransactionType.EXPENSE, iconName = "school", colorHex = "#6366F1", subcategories = "Courses,Books,Tuition", sortOrder = 7),
                CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE, iconName = "movie", colorHex = "#EAB308", subcategories = "Movies,Streaming,Games,Concerts", sortOrder = 8),
                CategoryEntity(name = "Travel", type = TransactionType.EXPENSE, iconName = "flight", colorHex = "#14B8A6", subcategories = "Flights,Hotels,Car Rental", sortOrder = 9),
                CategoryEntity(name = "charity", type = TransactionType.EXPENSE, iconName = "volunteer_activism", colorHex = "#F97316", subcategories = "Donations,Gifts", sortOrder = 10),
                CategoryEntity(name = "Electronics", type = TransactionType.EXPENSE, iconName = "devices", colorHex = "#38BDF8", subcategories = "Gadgets,Software,Hardware", sortOrder = 11),
                CategoryEntity(name = "Visa", type = TransactionType.EXPENSE, iconName = "credit_card", colorHex = "#A855F7", subcategories = "Card Payments", sortOrder = 12),
                CategoryEntity(name = "India TransactionDetail", type = TransactionType.EXPENSE, iconName = "public", colorHex = "#64748B", subcategories = "Forex,Transfers", sortOrder = 13)
            )
            categoryDao.insertCategories(expenseCategories)

            val incomeCategories = listOf(
                CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconName = "payments", colorHex = "#10B981", subcategories = "Base Pay,Overtime", sortOrder = 1),
                CategoryEntity(name = "Bonus", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#F59E0B", subcategories = "Quarterly,Performance", sortOrder = 2),
                CategoryEntity(name = "borrowing", type = TransactionType.INCOME, iconName = "account_balance", colorHex = "#3B82F6", subcategories = "Loan,Refund", sortOrder = 3)
            )
            categoryDao.insertCategories(incomeCategories)

            // 3. Initial Transactions matching screenshot data
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) // 0-indexed

            fun makeTime(day: Int, hour: Int, minute: Int): Long {
                val c = Calendar.getInstance()
                c.set(year, month, day, hour, minute, 0)
                return c.timeInMillis
            }

            val sampleTxs = listOf(
                TransactionEntity(amount = 23.00, type = TransactionType.EXPENSE, accountId = 3, categoryId = 3, merchantName = "Dvla Test", transactionDateMillis = makeTime(9, 14, 20), notes = "AMEX"),
                TransactionEntity(amount = 4.60, type = TransactionType.EXPENSE, accountId = 3, categoryId = 2, merchantName = "Asda", transactionDateMillis = makeTime(9, 11, 45), notes = "AMEX"),
                TransactionEntity(amount = 12.97, type = TransactionType.EXPENSE, accountId = 3, categoryId = 5, merchantName = "Amazon prime", transactionDateMillis = makeTime(8, 18, 30), notes = "AMEX"),
                TransactionEntity(amount = 9.39, type = TransactionType.EXPENSE, accountId = 3, categoryId = 2, merchantName = "Aldi", transactionDateMillis = makeTime(8, 16, 10), notes = "AMEX"),
                TransactionEntity(amount = 3.25, type = TransactionType.EXPENSE, accountId = 3, categoryId = 3, merchantName = "Car Parking", transactionDateMillis = makeTime(8, 9, 15), notes = "AMEX"),
                TransactionEntity(amount = 31.51, type = TransactionType.EXPENSE, accountId = 3, categoryId = 3, merchantName = "Petrol", transactionDateMillis = makeTime(7, 19, 0), notes = "AMEX"),
                TransactionEntity(amount = 16.40, type = TransactionType.EXPENSE, accountId = 3, categoryId = 2, merchantName = "Asda", transactionDateMillis = makeTime(7, 17, 30), notes = "AMEX"),
                TransactionEntity(amount = 4.48, type = TransactionType.EXPENSE, accountId = 3, categoryId = 1, merchantName = "Tesco", transactionDateMillis = makeTime(7, 13, 10), notes = "AMEX"),
                TransactionEntity(amount = 14.07, type = TransactionType.EXPENSE, accountId = 3, categoryId = 2, merchantName = "Tesco", transactionDateMillis = makeTime(7, 10, 5), notes = "AMEX"),
                TransactionEntity(amount = 254.67, type = TransactionType.EXPENSE, accountId = 2, categoryId = 11, merchantName = "Currys PC World", transactionDateMillis = makeTime(2, 15, 0), notes = "Electronics"),
                TransactionEntity(amount = 82.39, type = TransactionType.EXPENSE, accountId = 2, categoryId = 1, merchantName = "Restaurant Bill", transactionDateMillis = makeTime(1, 20, 0), notes = "Dining"),
                TransactionEntity(amount = 135.45, type = TransactionType.EXPENSE, accountId = 3, categoryId = 8, merchantName = "Cineworld & Dinner", transactionDateMillis = makeTime(3, 19, 30), notes = "Entertainment")
            )
            transactionDao.insertTransactions(sampleTxs)

            // Budget
            budgetDao.insertBudget(
                BudgetEntity(monthlyLimit = 1200.00, monthYear = String.format("%04d-%02d", year, month + 1), rolloverUnused = false)
            )
        }
    }
}
