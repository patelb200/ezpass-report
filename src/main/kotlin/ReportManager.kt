package io.bharatpatel.ezpassreport

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/112.0"
const val HOST = "https://www.ezpass.csc.paturnpike.com"
const val LOGIN_URL = "$HOST/ezpass/Maintenance/Login"
const val TRANSPONDER_URL = "$HOST/ezpass/Maintenance/Transponders"
const val VEHICLE_URL = "$HOST/ezpass/Maintenance/Vehicles"
const val TRANSACTIONS_URL = "$HOST/ezpass/Maintenance/Transactions"
const val TRANSACTIONS_PAGE_URL = "$HOST/ezpass/Maintenance/TransactionResultPagination"

fun getSession(loginCredentials: LoginCredentials): Session {

    val getLoginFormResponse = Jsoup.connect(LOGIN_URL)
        .userAgent(USER_AGENT)
        .method(Connection.Method.GET)
        .execute()

    val loginFormDocument = getLoginFormResponse.parse()

    val loginResponse = Jsoup.connect(LOGIN_URL)
        .userAgent(USER_AGENT)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .method(Connection.Method.POST)
        .cookies(getLoginFormResponse.cookies())
        .data(
            mapOf(
                Pair("UserName", loginCredentials.username),
                Pair("Password", loginCredentials.password),
                Pair(
                    "__RequestVerificationToken",
                    loginFormDocument.select("input[name=__RequestVerificationToken]").`val`()
                ),
            )
        ).execute()

    return Session(loginCredentials.username, loginResponse.cookies())
}

fun getTransponders(session: Session): List<Transponder> {

    val transponderTableRows = Jsoup.connect(TRANSPONDER_URL)
        .userAgent(USER_AGENT)
        .cookies(session.cookies)
        .get()
        .getElementById("transponderstatustable")!!
        .select("tr")


    return transponderTableRows.asSequence()
        .drop(1) // skip header row
        .map { tr ->
            val tds = tr.children()
            Transponder(tds[0].text(), tds[1].text(), tds[2].text(), tds[3].text())
        }
        .toList()
}

fun getVehicles(session: Session): List<Vehicle> {

    val vehicleTableRows = Jsoup.connect(VEHICLE_URL)
        .userAgent(USER_AGENT)
        .cookies(session.cookies)
        .get()
        .getElementById("divVehicleList")!!
        .select("table")[0]!!
        .select("tr")

    return vehicleTableRows.asSequence()
        .drop(1) // skip header row
        .map { tr ->
            val tds = tr.children()
            val startDate = sanitize(tds[7].text())?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MM/dd/yyy")) }
            val endDate = sanitize(tds[8].text())?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MM/dd/yyy")) }

            Vehicle(
                sanitize(tds[0].text())!!,
                sanitize(tds[1].text())!!,
                sanitize(tds[2].text())!!,
                sanitize(tds[3].text())!!,
                sanitize(tds[4].text())!!,
                sanitize(tds[5].text())!!,
                yesNoToBoolean(sanitize(tds[6].text())),
                startDate,
                endDate
            )
        }
        .toList()
}

fun getTransactions(session: Session, startDate: LocalDate, endDate: LocalDate): List<Transaction> {

    fun parseTransactionTableElement(table: Element): List<Transaction> {
        return table.select("tbody")[0]//the first tbody contains the transactions
            .select("tr")
            .asSequence()
            .map { tr -> tr.select("td") } // get the td elements
            .map { parseTransaction(it) }
            .toList()
    }

    val transactions = mutableListOf<Transaction>()

    val initTransactionPage = Jsoup.connect(TRANSACTIONS_URL)
        .userAgent(USER_AGENT)
        .cookies(session.cookies)
        .data(
            mapOf(
                Pair("TransSearchModel.PostingStartDate", startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                Pair("TransSearchModel.PostingEndDate", endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                Pair("TransponderSelected", ""),
                Pair("TBPFilterBySelection", "ALL"),
                Pair("SortBySelected", "Post+Date"),
                Pair("OrderBySelected", "ASC"),
                Pair("action:SearchTrans", "SearchTrans"),
            )
        )
        .post()
        .getElementById("tableTransactions")?.let { parseTransactionTableElement(it) } ?: return emptyList()

    transactions.addAll(initTransactionPage)

    var page = 2
    do {
        val resultTable = getTransactionPageResult(session, page++)?.let {
            parseTransactionTableElement(it)
        }?.forEach { transactions.add(it) }

    } while (resultTable != null && page < 100)

    return transactions
}

fun getTransactionPageResult(session: Session, page: Int): Element? {
    return Jsoup.connect(TRANSACTIONS_PAGE_URL)
        .userAgent(USER_AGENT)
        .referrer(TRANSACTIONS_URL)
        .cookies(session.cookies)
        .header("X-Requested-With", "XMLHttpRequest")
        .header("Content-Type", "application/json")
        .requestBody("{\"Page\": $page }")
        .post()
        .getElementById("tableTransactions")
}