package io.bharatpatel.ezpassreport

import java.time.Instant
import java.time.LocalDate

data class LoginCredentials(
    val username: String,
    val password: String
)

data class Session(val username: String, val cookies: Map<String, String>)

data class Transponder(
    val tagNumber: String,
    val style: String,
    val color: String,
    val status: String
)

data class Vehicle(
    val plateNumber: String,
    val state: String,
    val make: String,
    val model: String,
    val year: String,
    val color: String,
    val temporary: Boolean,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)

enum class ChargeType {
    CREDIT, DEBIT
}

data class Charge(val type: ChargeType, val amount: Double, val method: String?, val account: String?)

data class Transaction(
    val transactionId: String?,
    val postDate: LocalDate,
    val transactionType: String,
    val transponderNumber: String?,
    val plateNumber: String?,
    val entryDateTime: Instant?,
    val entryPlaza: String?,
    val exitDateTime: Instant?,
    val exitPlaza: String?,
    val vehicleClass: String?,
    val charge: Charge
)
