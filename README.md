# 💳 SmartLedger — Modern AI-Powered Personal Finance Manager for Android

<div align="center">

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.x-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-blue.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material Design 3](https://img.shields.io/badge/Material-3%20Expressive-orange.svg?style=flat-square&logo=materialdesign)](https://m3.material.io)
[![Room Database](https://img.shields.io/badge/Room-Database-green.svg?style=flat-square&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

*A production-grade, offline-first personal finance and budgeting Android application built with Jetpack Compose, Material Design 3 Frosted Glassmorphism, and On-Device Android AI (Gemini Nano / LiteRT-LM).*

</div>

---

## 🌟 Overview

**SmartLedger** is a feature-rich, privacy-focused expense manager designed to replicate the classic multi-tab ledger workflow while integrating cutting-edge Android architecture, immersive frosted glass animations, multi-currency conversion, bulk Excel/CSV importing, and 100% offline local AI capabilities.

---

## 🚀 Key Features & Architecture

### 🎨 Material 3 Expressive & Frosted Glass UI
- **Glassmorphism Design System:** Built over a deep ambient mesh gradient with dynamic backdrop blur (`Modifier.frostedGlass()`), translucent surface fills (`alpha = 0.68f`), and subtle specular borders.
- **Dynamic Semantic Colors:** Clean Ocean Blue for expenses/Saturdays, vibrant Coral-Red for income/Sundays/FABs, and crisp white typography.

### 📚 Ledger & Multi-Tab Hub (`Trans.` & `Stats`)
- **5 Transaction Sub-Tabs:** **Daily** (grouped by date with sticky day headers and daily subtotals), **Calendar** (full month grid with cell totals), **Monthly**, **Total**, and **Note**.
- **Visual Analytics Canvas:** Custom Jetpack Compose Canvas Donut & Pie Charts with smooth animations, external callout leader lines, and ranked percentage breakdown badges.
- **Custom Docked Picker:** Fast 3-column grid input system replacing rigid keyboards for accounts (`CASH`, `DebitCard`, `AMEX`) and categories (`Grocery`, `Transportation`, `Apparel`, etc.).

### 🤖 On-Device Android AI Hub (Dual-Engine)
- **Zero-Cloud Privacy:** Automatically routes requests to **Google AICore (Gemini Nano)** on supported devices, or downloads an open-source model (**Gemma-3-1B / Gemma-3n** via **Google AI Edge LiteRT-LM**) with GPU acceleration (`libOpenCL.so`) on non-AICore devices.
- **Natural Language & Voice Parsing:** Instantly parses phrases like *"Spent £31.51 on petrol with AMEX"* into structured transaction drafts using native Kotlin function calling (`@Tool`).
- **Multimodal Receipt OCR:** Captures and scans physical receipts via ML Kit Document Scanner to auto-fill merchant notes, dates, and amounts.

### 📂 Bulk Import, Export & 24-Hour Rollback
- **Multi-Format Spreadsheet Engine:** Supports `.csv`, `.xls`, and `.xlsx` files using Apache POI.
- **Interactive Import Preview Staging Screen:** Review, filter, batch-edit, or inline-edit spreadsheet rows before committing them to Room.
- **24-Hour Safety Rollback:** 1-tap **"Revert Transactions Added in Last 24 Hours"** safely deletes recent imports and restores account balances in an atomic `@Transaction`.

### 💱 Non-Destructive Multi-Currency Converter (`GBP` ↔ `INR` ↔ `USD`)
- **Base-Currency Protection:** All raw database values remain safely stored in British Pounds (`GBP`) to prevent floating-point rounding drift.
- **Manual Exchange Rate Input:** Easily switch between `GBP (£)`, `INR (₹)`, and `USD ($)` with custom manual multipliers via DataStore and global Compose `CompositionLocal`.

### 🔒 Backup, Migration & Telemetry Diagnostics
- **WAL-Safe Room Backups:** Flushes SQLite Write-Ahead Logs (`PRAGMA wal_checkpoint(TRUNCATE)`) to export pristine `.db` backups for smooth phone migrations.
- **AI Studio Error Exporter:** Captures navigation breadcrumbs, caught exceptions, and PII-redacted `logcat` tails into an `ai_studio_debug_report.txt` for instant root-cause analysis in Google AI Studio.

---

## 📱 Screenshots & UI Preview

| Transactions Ledger (Daily) | Visual Stats (Leader-Line Pie Chart) | Transaction Entry Form |
| :---: | :---: | :---: |
| *(Tab headers, sticky summary bar, grouped date headers, and frosted bottom navigation)* | *(Canvas donut chart with external leader lines and ranked breakdown badges)* | *(3-button mode selector, dynamic underline accents, and docked account grid)* |

---

## 🛠️ Tech Stack & Libraries

- **Language:** 100% Kotlin (2.x)
- **UI Toolkit:** Jetpack Compose & Material 3 (Material You) + Adaptive Navigation suite.
- **Architecture:** Clean Architecture (UI, Domain, Data layers) + MVVM + Unidirectional Data Flow (UDF).
- **Local Storage:** Room Database (with KSP, SQLCipher encryption support, and WAL mode).
- **Preferences:** Jetpack DataStore (Proto & Preferences).
- **Async & Background:** Kotlin Coroutines & Flow + WorkManager.
- **AI & ML:** Google AI Edge LiteRT-LM + ML Kit Document Scanner & Prompt APIs.
- **Spreadsheets:** Apache POI (`poi-ooxml`) for native `.xlsx` & `.xls` parsing on Android.
- **Diagnostics:** Timber (PII-redacted ring-buffer logger) + Jetpack Tracing.

---

## ⚙️ Getting Started in Android Studio

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/your-username/smart-ledger-android.git](https://github.com/your-username/smart-ledger-android.git)
