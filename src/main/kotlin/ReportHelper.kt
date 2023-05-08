package io.bharatpatel.ezpassreport

import org.apache.commons.text.StringEscapeUtils
import org.jsoup.select.Elements
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

val DEBIT_PATTERN: Pattern = Pattern.compile("\\(\\$([0-9]+.[0-9]+)\\)")
val CREDIT_PATTERN: Pattern = Pattern.compile("\\$([0-9]+.[0-9]+)")

fun sanitize(value: String?): String? {
    return if (!value.isNullOrBlank()) StringEscapeUtils.unescapeHtml4(value).trim() else null
}

fun yesNoToBoolean(value: String?): Boolean {
    return when (value?.lowercase()) {
        "yes", "y" -> true
        else -> false
    }
}

fun extractMonetaryValue(value: String): Pair<ChargeType, Double> {
    val debitMatcher = DEBIT_PATTERN.matcher(value)
    val creditMatcher = CREDIT_PATTERN.matcher(value)
    if (debitMatcher.matches()) {
        return ChargeType.DEBIT to debitMatcher.group(1).toDouble()
    }
    return if (creditMatcher.matches()) {
        ChargeType.CREDIT to creditMatcher.group(1).toDouble()
    } else throw IllegalArgumentException("$value not a monetary value.")
}

fun parseTransaction(tds: Elements): Transaction {

    val transactionId = sanitize(tds[0].select("input[name=SelectedDispute]").`val`())
    val postDate = LocalDate.parse(sanitize(tds[2].text()), DateTimeFormatter.ofPattern("MM/dd/yyy"))
    val transactionType = sanitize(tds[3].text())!!
    val transponderNumber = sanitize(tds[4].text())
    val plateNumber = sanitize(tds[5].text())
    val entryDateTime =
        sanitize(tds[6].text())?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("MM/dd/yyyy h:m a")).atZone(
                ZoneId.systemDefault()
            ).toInstant()
        }
    val entryPlaza = sanitize(tds[7].text())
    val exitDateTime =
        sanitize(tds[8].text())?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("MM/dd/yyyy h:m a")).atZone(
                ZoneId.systemDefault()
            ).toInstant()
        }
    val exitPlaza = sanitize(tds[9].text())
    val vehicleClass = sanitize(tds[10].text())

    val (chargeType, amount) = extractMonetaryValue(sanitize(tds[11].text())!!)
    val charge = Charge(chargeType, amount, sanitize(tds[12].text()), sanitize(tds[13].text()))
    return Transaction(
        transactionId, postDate, transactionType,
        transponderNumber, plateNumber, entryDateTime,
        entryPlaza, exitDateTime, exitPlaza, vehicleClass,
        charge
    )
}