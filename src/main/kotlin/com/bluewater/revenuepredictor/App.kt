package com.bluewater.revenuepredictor

import com.bluewater.revenuepredictor.api.predictionRoutes
import com.bluewater.revenuepredictor.config.LocalConfig
import com.bluewater.revenuepredictor.mongo.MockMongoDatabase
import com.bluewater.revenuepredictor.repository.SeedData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() {
    runBlocking {
        MockMongoDatabase.pingOrPretend()
        SeedData.seedIfEmpty()
    }

    embeddedServer(Netty, port = LocalConfig.port, host = "0.0.0.0", module = Application::blueWaterModule)
        .start(wait = true)
}

fun Application.blueWaterModule() {
    install(DefaultHeaders) {
        header(HttpHeaders.Server, "BlueWater-Agentic-Revenue-Predictor")
    }
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            encodeDefaults = true
            ignoreUnknownKeys = true
        })
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        get("/") {
            call.respond(
                mapOf(
                    "name" to "BlueWater Agentic AI Revenue Prediction Model",
                    "status" to "ok",
                    "mongoMode" to "mock",
                    "llmMode" to if (LocalConfig.liveLlmConfigured) "live-koog" else "mock-koog",
                ),
            )
        }
        route("/api") {
            predictionRoutes()
        }
    }
}

