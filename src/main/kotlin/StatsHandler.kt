package com.lowbudgetlcs

import com.lowbudgetlcs.data.MetaData
import com.lowbudgetlcs.data.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import migrations.Players
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant
import org.slf4j.LoggerFactory

class StatsHandler(private val result: Result) {
    private val logger = LoggerFactory.getLogger("com.lowbudgetlcs.StatsHandler")
    private val db = Db.db

    @OptIn(ExperimentalSerializationApi::class)
    private val metaData: MetaData = Json.decodeFromString<MetaData>(result.metaData)
    private val seriesId = metaData.seriesId
    private val code = result.shortCode

    fun receiveCallback() = runBlocking {
        val match: LOLMatch = RiotAPIBridge.getMatchData(result.gameId)
        saveStats(match)
        //val something: something = writeToSheets()
    }

    private fun saveStats(match: LOLMatch) = runBlocking {
        logger.info("Updating player stats")
        val gameQueries = db.gameQueries
        when (val id = gameQueries.selectIdByCode(code).executeAsOneOrNull()) {
            null -> {
                logger.warn("No game found for series '{}' game code '{}'", seriesId, code)
            }

            else -> {
                logger.debug("Inserting game id '{}' performances...", id)
                for (player in match.participants) {
                    launch {
                        db.transaction {
                            processPlayer(id, match, player)
                        }
                    }
                    logger.debug("Finished inserting performances for series id '{}' game id '{}'!", seriesId, id)
                }
            }
        }
    }

    private fun processPlayer(gameId: Int, match: LOLMatch, player: MatchParticipant) {
        logger.debug("Processing code '{}'::'{}'", result.shortCode, player.riotIdName)
        val teamKills: Int = when (player.didWin()) {
            match.teams[0].didWin() -> match.teams[0].objectives["champion"]?.kills ?: 0
            match.teams[1].didWin() -> match.teams[1].objectives["champion"]?.kills ?: 0
            else -> {
                logger.warn("Code: '{}' - Both teams won or lost???", result.shortCode)
                0
            }
        }
        val teamQueries = db.teamQueries
        getPlayerId(player)?.let { p ->
            p.team_id?.let { teamId ->
                teamQueries.findDivisionId(teamId).executeAsOneOrNull()?.let { divisionId ->
                    when (val perfId = savePerformance(p.id, teamId, divisionId, gameId)) {
                        -1 -> {}
                        else -> {
                            savePlayerData(perfId, player, match.gameDuration, teamKills)
                        }
                    }
                }
            }
        }
        logger.debug("Finished processing '{}'::'{}'", result.shortCode, player.riotIdName)
    }

    private fun getPlayerId(player: MatchParticipant): Players? {
        val playerQueries = db.playerQueries
        playerQueries.selectByPuuid(player.puuid).executeAsOneOrNull()?.let {
            return it
        }
        logger.warn("Player {} not in database. ({})", player.riotIdName, result.shortCode)
        return null
    }

    private fun savePlayerData(performanceId: Int, player: MatchParticipant, gameLength: Int, teamkills: Int): Int {
        val playerDataqueriesQueries = db.player_DataQueries
        try {
            playerDataqueriesQueries.insertData(
                kills = player.kills,
                deaths = player.deaths,
                assists = player.assists,
                level = player.championLevel,
                gold = player.goldEarned.toLong(),
                vision_score = player.visionScore.toLong(),
                damage = player.totalDamageDealtToChampions.toLong(),
                healing = player.totalHeal.toLong(),
                shielding = player.totalDamageShieldedOnTeammates.toLong(),
                damage_taken = player.totalDamageTaken.toLong(),
                self_mitigated_damage = player.damageSelfMitigated.toLong(),
                damage_to_turrets = player.damageDealtToTurrets.toLong(),
                longest_life = player.longestTimeSpentLiving.toLong(),
                double_kills = player.doubleKills.toShort(),
                triple_kills = player.tripleKills.toShort(),
                quadra_kills = player.quadraKills.toShort(),
                penta_kills = player.pentaKills.toShort(),
                game_length = gameLength.toLong(),
                win = player.didWin(),
                cs = player.totalMinionsKilled + player.neutralMinionsKilled,
                champion_name = player.championName,
                team_kills = teamkills,
                short_code = result.shortCode,
                performance_id = performanceId
            ).executeAsOneOrNull()?.let {
                return it
            }
        } catch (e: Throwable) {
            logger.error(e.message)
        }
        logger.warn("Inserting player data failed for code {} player {}", result.shortCode, player.riotIdName)
        return -1
    }

    private fun savePerformance(playerId: Int, teamId: Int, divisionId: Int, gameId: Int): Int {
        val performancesQueries = db.performancesQueries
        try {
            performancesQueries.insertPerformance(
                player_id = playerId, team_id = teamId, division_id = divisionId, game_id = gameId
            ).executeAsOneOrNull()?.let {
                return it
            }
        } catch (e: Throwable) {
            logger.error(e.message)
        }
        logger.warn("Error inserting performance for player #'{}'.", playerId)
        return -1
    }
}