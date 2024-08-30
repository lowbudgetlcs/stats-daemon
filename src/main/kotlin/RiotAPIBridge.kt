package com.lowbudgetlcs

import com.lowbudgetlcs.Db.db
import no.stelar7.api.r4j.basic.APICredentials
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard
import no.stelar7.api.r4j.basic.constants.api.regions.RegionShard
import no.stelar7.api.r4j.impl.R4J
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch
import org.slf4j.LoggerFactory
import javax.swing.plaf.synth.Region

object RiotAPIBridge {
    val logger = LoggerFactory.getLogger("com.lowbudgetlcs.RiotAPIBridge")

    private val client by lazy {
        val creds = APICredentials(System.getenv("RIOT_API_TOKEN"))
        logger.info("Key: ${creds.loLAPIKey}")
        R4J(creds)
    }

    fun healthCheck() {
        logger.info("Starting healthcheck!")
        val ruuffian = client.accountAPI.getAccountByTag(RegionShard.AMERICAS, "ruuffian", "FUNZ")
        val summoner = client.loLAPI.summonerAPI.getSummonerByPUUID(LeagueShard.NA1, ruuffian.puuid)
        logger.info("Welcome, ${ruuffian.name}. Congrats on hitting level ${summoner.summonerLevel}!")
    }

    fun getMatchData(matchId: Long): LOLMatch {
        // Fetch match
        return client.loLAPI.matchAPI.getMatch(RegionShard.AMERICAS, "NA1_$matchId")
    }
}