package com.lowbudgetlcs

import com.lowbudgetlcs.data.Result
import com.rabbitmq.client.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class RabbitMQBridge(private val exchangeName: String = "RIOT_CALLBACKS") {
    private val logger = LoggerFactory.getLogger("com.lowbudgetlcs.RabbitMQBridge")
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
            exchangeDeclare(exchangeName, "topic")
        }.also {
            logger.debug("Created new messageq channel.")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun listen(topics: Array<String>) {
        channel.run {
            val queue = queueDeclare().queue
            for (t in topics) queueBind(queue, exchangeName, t)
            val callback = DeliverCallback { _, delivery: Delivery ->
                val topic = delivery.envelope.routingKey
                logger.info("[x] Recieved data on $topic topic.")
                when (topic) {
                    "callback" -> {
                        val message = String(delivery.body, charset("UTF-8"))
                        val result = Json.decodeFromString<Result>(message)
                        logger.debug("Callback: {}", result.toString())
                        // Actually do something with the callback
                        StatsHandler(result).receiveCallback()
                    }

                    "series" -> TODO()
                    else -> {
                        logger.info("Recieved unkown topic: '{}'.", topic)
                    }
                }
            }
            basicConsume(queue, true, callback) { _ -> }
        }
    }
}
