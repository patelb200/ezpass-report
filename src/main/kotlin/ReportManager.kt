package io.bharatpatel.ezpassreport

import org.jsoup.Connection
import org.jsoup.Jsoup

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/112.0"
const val HOST = "https://www.ezpass.csc.paturnpike.com"
const val LOGIN_URL = "$HOST/ezpass/Maintenance/Login"
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