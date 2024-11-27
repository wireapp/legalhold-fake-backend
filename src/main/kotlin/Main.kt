package com.wire

import com.wire.com.wire.model.ApiVersion
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun main(args: Array<String>) {
    val parser = ArgParser("LegalHold fake backend")

    // Required parameters
    val email by parser.option(
        ArgType.String,
        shortName = "e",
        description = "Email of the user"
    ).required()
    val password by parser.option(
        ArgType.String,
        shortName = "p",
        description = "Password of the user"
    ).required()
    val teamId by parser.option(
        ArgType.String,
        shortName = "t",
        description = "Team of the user"
    ).required()
    val userId by parser.option(
        ArgType.String,
        shortName = "ui",
        description = "UserId of the user"
    ).required()

    // Optional parameters
    val userDomain by parser.option(
        ArgType.String,
        shortName = "ud",
        description = "UserDomain of the user"
    ).default("anta.wire.link")
    val env by parser.option(
        ArgType.String,
        description = "Backend environment host"
    ).default("https://nginz-https.anta.wire.link/v6")

    parser.parse(args)

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    val response: HttpResponse = client.get("http://localhost:8080/api-version")
    logger.info { response.status }
    val apiVersion : ApiVersion = response.body()
    logger.info { apiVersion }

    // Call /initiate with a real qualified_user_id and team_id
    // Use the response to call POST /client, adding the prekeys and default data
    // Get the clientId from the response
    // Call /login, get the cookie in the response header "set-cookie"

    // Call /initiate with a real qualified_user_id,  team_id, refresb_token(the cookie?) and clientId
    // Check the response is HTTP 200
    client.close()
}