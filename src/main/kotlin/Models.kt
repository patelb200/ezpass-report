package io.bharatpatel.ezpassreport

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