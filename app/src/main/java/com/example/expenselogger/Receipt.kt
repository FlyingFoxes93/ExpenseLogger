package com.example.expenselogger

data class Receipt(
    val id: Int,
    val imageUri: String,
    val amount: Double,
    val timestamp: String,
    var activityId: Int
)