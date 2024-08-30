package com.lowbudgetlcs

import com.lowbudgetlcs.RiotAPIBridge.logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.system.exitProcess

@OptIn(ExperimentalSerializationApi::class)
fun main(): Unit = runBlocking {
    println("Hello World!")
//    val test = """
//        {
//        "startTime": 1722990493519,
//        "shortCode": "NA04de6-8dff127e-c0e9-4b3e-949b-5bafb4a3facc",
//        "metaData": "{\"gameNum\":1,\"seriesId\":61}",
//        "gameId": 5077899330,
//        "gameName": "841cc303-3af1-4328-82e4-577ad29a527d",
//        "gameType": "CUSTOM",
//        "gameMap": 11,
//        "gameMode": "CLASSIC",
//        "region": "NA1"
//    }""".trimIndent()
//    val result = Json.decodeFromString<Result>(test)

//    StatsHandler(result).receiveCallback() // Explicitly call function for testing- assume RabbitMQ works
    when(RiotAPIBridge.healthCheck()) {
        0 -> logger.info("Riot status healthy!")
        else -> exitProcess(1)
    }
    launch {
        val bridge = RabbitMQBridge()
        bridge.listen(arrayOf("callback"))
    }
//    launch {
//      val bridge = RabbitMQBridge()
//      bridge.listen(arrayOf("series"))
//    }
}