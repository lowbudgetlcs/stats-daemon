package com.lowbudgetlcs

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import kotlinx.serialization.ExperimentalSerializationApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RabbitMQBridge(private val exchangeName: String = "RIOT_CALLBACKS") {
    val logger: Logger = LoggerFactory.getLogger("com.lowbudgetlcs.RabbitMQBridge")

    companion object {
        private val factory = ConnectionFactory().apply {
            host = System.getenv("MESSAGEQ_HOST") ?: "rabbitmq"
            isAutomaticRecoveryEnabled = true
            networkRecoveryInterval = 15000
        }

        private val connection: Connection by lazy {
            factory.newConnection()
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
    fun listen(topics: Array<String>, callback: DeliverCallback) {
        channel.run {
            val queue = queueDeclare().queue
            for (t in topics) queueBind(queue, exchangeName, t)
            basicConsume(queue, true, callback) { _ -> }
        }
    }
}

