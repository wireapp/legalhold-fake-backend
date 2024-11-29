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
    val qualifiedUser = QualifiedId(userId, userDomain)

    // Call /initiate with a real qualified_user_id and team_id
    val initResponse: HttpResponse = client.post("http://localhost:8080/v1/initiate") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.Authorization, "secr3t")
        }
        setBody(InitRequest(qualifiedUser, teamId))
    }
    val initResponseDto : InitResponse = initResponse.body()
    logger.info { initResponseDto }

    // Call POST /login, get access_token from response body and get the cookie in the response header "set-cookie"
    val loginResponse: HttpResponse = client.post("$env/login") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
        }
        setBody(LoginRequest(email, password))
    }
    val accessToken : String = loginResponse.body<LoginResponse>().accessToken
    val refreshToken = loginResponse.setCookie().first().value
    logger.info { accessToken }

    // Use the response to call POST /client, adding the prekeys and default data
    val clientAddResponse: HttpResponse = client.post("$env/clients") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        setBody(ClientAddRequest(lastkey = initResponseDto.lastPrekey, prekeys = initResponseDto.prekeys))
    }
    val clientId : String = clientAddResponse.body<ClientAddResponse>().id
    logger.info { clientId }

    // Call /confirm with a real qualified_user_id, team_id, refresb_token(the cookie?) and clientId, check 200
    val confirmResponse: HttpResponse = client.post("http://localhost:8080/v1/confirm") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
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
    client.close()
}