package com.lowbudgetlcs

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi

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
//    val result: Result= Json.decodeFromString(test)

//    StatsHandler(result).receiveCallback() // Explicitly call function for testing- assume RabbitMQ works
    launch {
        RabbitMQBridge().listen(arrayOf("callback"))
    }
//    launch {
//        RabbitMQBridge().listen(arrayOf("series"))
//    }
}