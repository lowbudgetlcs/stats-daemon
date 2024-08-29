package com.lowbudgetlcs

import com.rabbitmq.client.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import com.lowbudgetlcs.data.*
import kotlinx.coroutines.*

object RabbitMQBridge {
    private val logger = LoggerFactory.getLogger("com.lowbudgetlcs.RabbitMQBridge")
    private const val EXCHANGE_NAME = "RIOT_CALLBACKS"
    private val factory = ConnectionFactory().apply {
        host = System.getenv("MESSAGEQ_HOST") ?: "rabbitmq"
        isAutomaticRecoveryEnabled = true
        networkRecoveryInterval = 15000
    }

    private val connection: Connection by lazy {
        factory.newConnection().also {
            logger.debug("Created new messageq connection.")
        }
    }

    private val channel: Channel by lazy {
        connection.createChannel().apply {
            exchangeDeclare(EXCHANGE_NAME, "topic")
        }.also {
            logger.debug("Created new messageq channel.")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun listen() = runBlocking {
        launch {
            channel.run {
                val queue = queueDeclare().queue
                queueBind(queue, EXCHANGE_NAME, "callback")
                val callback = DeliverCallback { _, delivery: Delivery ->
                    logger.info("[x] Recieved data on [callback] topic.")
                    val message = String(delivery.body, charset("UTF-8"))
                    val result = Json.decodeFromString<Result>(message)
                    logger.debug("Callback: {}", result.toString())
                    // Actually do something with the callback
                    StatsHandler(result).receiveCallback()
                }
                basicConsume(queue, true, callback) { _ -> }
            }
        }
    }
}
