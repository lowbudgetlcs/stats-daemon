package com.lowbudgetlcs
import com.lowbudgetlcs.data.MetaData
import com.lowbudgetlcs.data.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant
import org.slf4j.LoggerFactory
import queries.FindDivisionId


class StatsHandler(private val result: Result){
    private val logger = LoggerFactory.getLogger("com.lowbudgetlcs.StatsHandler")
    private val db = Db.db
    @OptIn(ExperimentalSerializationApi::class)
    private val metaData: MetaData = Json.decodeFromString<MetaData>(result.metaData)
    private val seriesId = metaData.seriesId
    private val code = result.shortCode

    fun receiveCallback() = runBlocking {

        launch {
            val match: LOLMatch = RiotAPIBridge.getMatchData(result.gameId)
            saveStatus(match)
            //val something: something = writeToSheets()
        }
    }



    private fun saveStatus(match: LOLMatch) {
        logger.info("Updating player stats")
        val gameQueries = Db.db.gameQueries
        val id = gameQueries.selectIdByCode(code).executeAsOneOrNull()
        when (id) {
            null -> {
                logger.warn("No game found for series '{}' game code '{}'", seriesId, code)
                return
            }
            else -> {
                logger.debug("Inserting game id '{}' performances...", id)
                val winningPlayers = match.participants.filter { participant -> participant.didWin() }
                val losingPlayers = match.participants.filter { participant -> !participant.didWin() }
                val (winnerId: Int, loserId: Int) = Pair(
                    getTeamId(winningPlayers),
                    getTeamId(losingPlayers),
                )
                var teamKills: Int
                for (player in winningPlayers)
                {
                    val teamQueries = Db.db.teamQueries
                    val divisionId = teamQueries.findDivisionId(winnerId).executeAsOneOrNull()?.division_id
                    if(match.teams.get(0).didWin())
                        teamKills = match.teams.get(0).objectives.get("champion")?.kills ?: 0
                    else
                        teamKills = match.teams.get(1).objectives.get("champion")?.kills ?: 0

                    val playerDataId = savePlayerData(player, match.gameDuration, teamKills)
                    val playerId = getPlayerId(player)
                    if(divisionId==null || playerId == -1) {
                        logger.warn("Team or Player not valid")
                        return
                    }
                    savePerformances(playerId, winnerId, divisionId, playerDataId)

                }
                for (player in losingPlayers)
                {
                    val teamQueries = Db.db.teamQueries
                    val divisionId = teamQueries.findDivisionId(loserId).executeAsOneOrNull()?.division_id
                    if(!match.teams.get(0).didWin())
                        teamKills = match.teams.get(0).objectives.get("champion")?.kills ?: 0
                    else
                        teamKills = match.teams.get(1).objectives.get("champion")?.kills ?: 0

                    val playerDataId = savePlayerData(player, match.gameDuration, teamKills)
                    val playerId = getPlayerId(player)
                    if(divisionId==null || playerId == -1) {
                        logger.warn("Team or Player not valid")
                        return
                    }
                    savePerformances(playerId, winnerId, divisionId, playerDataId)
                }
                logger.debug("Succsesfully inserted performances for series id '{}' game id '{}'!", seriesId, id)
            }
        }
    }

    private fun getTeamId(players: List<MatchParticipant>): Int {
        val playerQueries = db.playerQueries
        for (player in players) {
            val teamId = playerQueries.selectTeamId(player.puuid).executeAsOneOrNull()
            teamId?.let {
                it.team_id?.let { team_id ->
                    logger.debug("Fetched team id: {}", team_id)
                    return team_id
                }
            }
        }
        logger.warn("No valid player's provided.")
        return -1
    }

    private fun getPlayerId(player: MatchParticipant): Int {
        val playerQueries = db.playerQueries
        val playerId = playerQueries.selectPlayerId(player.puuid).executeAsOneOrNull()
        if(playerId==null) {
            logger.warn("Not a valid player")
            return -1
        }
        return playerId
    }

    private fun savePlayerData(player: MatchParticipant, gameLength: Int, teamkills: Int): Int {
        val playerDataqueriesQueries = db.player_DataQueries
        val id = playerDataqueriesQueries.insertData(
            kills = player.kills,
            deaths = player.deaths,
            assists = player.assists,
            level = player.championLevel,
            gold = player.goldEarned.toLong(),
            vision_score = player.visionScore.toLong(),
            damage = player.totalDamageDealt.toLong(),
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
            cs = player.totalMinionsKilled,
            champion_name = player.championName,
            team_kills = teamkills
        ).executeAsOne()
        return id;
    }

    private fun savePerformances(playerId: Int, teamId: Int, division_id: Int, player_data_id: Int) : Int {
        val performancesQueries = db.performancesQueries
        val id = performancesQueries.insertPerformance(
            player_id = playerId,
            team_id = teamId,
            division_id = division_id,
            player_data_id = player_data_id,
            game_id = result.gameId.toInt()
        ).executeAsOne()
        return id
    }
}