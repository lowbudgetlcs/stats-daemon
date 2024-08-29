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
        val id =
            gameQueries.selectIdByCode(code).executeAsOneOrNull()
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
                    val divisionId = db.teamQueries.findDivisionId(winnerId);
                    if(match.teams.get(0).didWin())
                        teamKills = match.teams.get(0).objectives.get("champion")?.kills ?: 0
                    else
                        teamKills = match.teams.get(1).objectives.get("champion")?.kills ?: 0

                    val playerDataId = savePlayerData(player, match.gameDuration, teamKills)

                    // save performances using new playerDataId, divisionID, teamId, etc
                }
                for (player in losingPlayers)
                {
                    val divisionId = db.teamQueries.findDivisionId(loserId)
                    if(!match.teams.get(0).didWin())
                        teamKills = match.teams.get(0).objectives.get("champion")?.kills ?: 0
                    else
                        teamKills = match.teams.get(1).objectives.get("champion")?.kills ?: 0

                    val playerDataId = savePlayerData(player, match.gameDuration, teamKills)
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
}