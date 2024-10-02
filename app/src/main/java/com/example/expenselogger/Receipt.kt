package com.example.expenselogger

data class Receipt(
    val imageUri: String,
    val amount: Double,
    val timestamp: String,
    val activityId: Int
)