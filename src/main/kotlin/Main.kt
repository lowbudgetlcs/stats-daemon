package com.lowbudgetlcs

import kotlinx.serialization.json.Json
import com.lowbudgetlcs.data.Result
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalSerializationApi::class)
fun main(): Unit = runBlocking {
    println("Hello World!")
    val result: Result= Json.decodeFromString("{\n" +
            "  \"startTime\": 1234567890000,\n" +
            "  \"shortCode\": \"NA1234a-1a23b456-a1b2-1abc-ab12-1234567890ab\",\n" +
            "  \"metaData\": \"{\\\"title\\\":\\\"Game 42 - Finals\\\"}\",\n" +
            "  \"gameId\": 1234567890,\n" +
            "  \"gameName\": \"a123bc45-ab1c-1a23-ab12-12345a67b89c\",\n" +
            "  \"gameType\": \"Practice\",\n" +
            "  \"gameMap\": 11,\n" +
            "  \"gameMode\": \"CLASSIC\",\n" +
            "  \"region\": \"NA1\"\n" +
            "}") // Hardcode this

    println(result.toString());
    StatsHandler(result).receiveCallback() // Explicitly call function for testing- assume RabbitMQ works
//    launch {
//        RabbitMQBridge().listen(arrayOf("callback"))
//    }
//    launch {
//        RabbitMQBridge().listen(arrayOf("series"))
//    }
}