package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object AppBreadcrumbs {
    private val breadcrumbs = ConcurrentLinkedQueue<String>()
    private const val MAX_BREADCRUMBS = 30

    fun add(crumb: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        breadcrumbs.add("[$timestamp] $crumb")
        if (breadcrumbs.size > MAX_BREADCRUMBS) {
            breadcrumbs.poll()
        }
    }

    fun getList(): List<String> = breadcrumbs.toList()
}
