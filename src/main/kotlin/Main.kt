package com.wire

import com.wire.com.wire.model.QualifiedId
import com.wire.com.wire.model.backend.ClientAddRequest
import com.wire.com.wire.model.backend.ClientAddResponse
import com.wire.com.wire.model.backend.LoginRequest
import com.wire.com.wire.model.backend.LoginResponse
import com.wire.com.wire.model.legalhold.ConfirmRequest
import com.wire.com.wire.model.legalhold.InitRequest
import com.wire.com.wire.model.legalhold.InitResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private const val LEGAL_HOLD_HOST = "http://localhost:8080"

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
                ignoreUnknownKeys = true
            })
        }
    }
    val qualifiedUser = QualifiedId(userId, userDomain)

    val initResponseDto: InitResponse = initLegalHold(client, qualifiedUser, teamId)
    val (accessToken, refreshToken) = loginUser(client, env, email, password)
    val clientId: String = registerNewClient(client, env, accessToken, password, initResponseDto)
    confirmLegalHold(client, qualifiedUser, teamId, refreshToken, clientId)

    client.close()
}

/**
 * Initiate client registration on LegalHold
 */
private suspend fun initLegalHold(
    client: HttpClient,
    qualifiedUser: QualifiedId,
    teamId: String
): InitResponse {
    val initResponse: HttpResponse = client.post("$LEGAL_HOLD_HOST/v1/initiate") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.Authorization, "secr3t")
        }
        setBody(InitRequest(qualifiedUser, teamId))
    }
    logger.info { initResponse.status }
    val initResponseDto: InitResponse = initResponse.body()
    logger.info { initResponseDto }
    return initResponseDto
}

/**
 * Login user in the backend, get access_token from response body and get the cookie in the response header "set-cookie"
 */
private suspend fun loginUser(
    client: HttpClient,
    env: String,
    email: String,
    password: String
): Pair<String, String> {
    val loginResponse: HttpResponse = client.post("$env/login") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
        }
        setBody(LoginRequest(email, password))
    }
    logger.info { loginResponse.status }
    val accessToken: String = loginResponse.body<LoginResponse>().accessToken
    val refreshToken = loginResponse.setCookie().first().value
    logger.info { accessToken }
    return Pair(accessToken, refreshToken)
}

/**
 * Use the Proteus data created by LegalHold to create a new client on the backend,
 * that will be used as a LegalHold device.
 */
private suspend fun registerNewClient(
    client: HttpClient,
    env: String,
    accessToken: String,
    password: String,
    initResponseDto: InitResponse
): String {
    val clientAddResponse: HttpResponse = client.post("$env/clients") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        setBody(
            ClientAddRequest(
                type = "permanent",
                password = password,
                lastkey = initResponseDto.lastPrekey,
                prekeys = initResponseDto.prekeys
            )
        )
    }
    logger.info { clientAddResponse.status }
    clientAddResponse.bodyAsText().let { logger.info { it } }
    val clientId: String = clientAddResponse.body<ClientAddResponse>().id
    logger.info { clientId }
    return clientId
}

/**
 * Confirm LegalHold device registration, using data from the logged in user and the new client.
 */
private suspend fun confirmLegalHold(
    client: HttpClient,
    qualifiedUser: QualifiedId,
    teamId: String,
    refreshToken: String,
    clientId: String
) {
    val confirmResponse: HttpResponse = client.post("$LEGAL_HOLD_HOST/v1/confirm") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.Authorization, "secr3t")
        }
        setBody(
            ConfirmRequest(
                userId = qualifiedUser,
                teamId = teamId,
                refreshToken = refreshToken,
                clientId = clientId
            )
        )
    }
    logger.info { confirmResponse.status }
}
