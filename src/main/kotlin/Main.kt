package io.bharatpatel.ezpassreport

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import kotlin.Exception

fun main(args: Array<String>) {

    val options = setupOptions()
    val commandLineParser = DefaultParser()
    val parseResult = try {
        commandLineParser.parse(options, args)
    } catch (e: Exception) {
        println("${e.message}\n")
        printHelp(options)
        return
    }

    val loginCredentials = try {
        createLoginCredential(parseResult)
    } catch (e: RuntimeException) {
        println("${e.message}\n")
        printHelp(options)
        return
    }


    val cookies = getSession(loginCredentials)

}

private fun setupOptions(): Options {

    val options = Options()

    options.addOption("u", true, "Account username")
    options.addOption("p", true, "Account password")

    return options
}

private fun createLoginCredential(commandLine: CommandLine): LoginCredentials {

    val username = commandLine.getOptionValue("u") ?: throw IllegalStateException("Username is missing")
    val password = commandLine.getOptionValue("p") ?: throw IllegalStateException("Password is missing")

    return LoginCredentials(username, password)
}

private fun printHelp(options: Options) {
    val formatter = HelpFormatter()
    formatter.printHelp("tollreport", options)
}