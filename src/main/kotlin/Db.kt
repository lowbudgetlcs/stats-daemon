package com.lowbudgetlcs

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class Db {
    companion object {
        private val driver: SqlDriver by lazy {
            HikariConfig().apply {
                jdbcUrl = System.getenv("POSTGRES_URL")
                username = System.getenv("POSTGRES_USER")
                password = System.getenv("POSTGRES_PW")
            }.let { cfg ->
                HikariDataSource(cfg).asJdbcDriver()
            }
        }
    }

    val db by lazy {
        Database(driver)
    }
}
