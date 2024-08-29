package com.lowbudgetlcs

import kotlinx.serialization.json.Json
import com.lowbudgetlcs.data.Result
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalSerializationApi::class)
fun main(): Unit = runBlocking {
    println("Hello World!")
    val result: Result= Json.decodeFromString("") // Hardcode this
    StatsHandler(result).receiveCallback() // Explicitly call function for testing- assume RabbitMQ works
//    launch {
//        RabbitMQBridge().listen(arrayOf("callback"))
//    }
//    launch {
//        RabbitMQBridge().listen(arrayOf("series"))
//    }
}