package com.example.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MatchEntity
import com.example.data.MatchRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Locale

class CricketViewModel(private val repository: MatchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CricketUiState())
    val uiState: StateFlow<CricketUiState> = _uiState.asStateFlow()

    private val stateHistory = ArrayDeque<CricketUiState>(51)
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val batsmanListAdapter = moshi.adapter<List<BatsmanState>>(
        Types.newParameterizedType(List::class.java, BatsmanState::class.java)
    )
    private val bowlerListAdapter = moshi.adapter<List<BowlerState>>(
        Types.newParameterizedType(List::class.java, BowlerState::class.java)
    )

    init {
        loadSavedMatches()
        resumeActiveMatch()
    }

    private fun loadSavedMatches() {
        viewModelScope.launch {
            repository.allMatches.collectLatest { matches ->
                _uiState.update { it.copy(savedMatches = matches) }
            }
        }
    }

    private fun resumeActiveMatch() {
        viewModelScope.launch {
            val activeMatch = repository.getActiveMatch()
            if (activeMatch != null) {
                _uiState.update { state ->
                    state.copy(
                        hostTeam = activeMatch.hostTeam,
                        visitorTeam = activeMatch.visitorTeam,
                        overs = activeMatch.totalOvers,
                        matchFormat = activeMatch.matchFormat,
                        activeMatchId = activeMatch.id,
                        isMatchActive = !activeMatch.isCompleted,
                        innings = activeMatch.currentInnings,
                        inning1Runs = activeMatch.inning1Runs,
                        inning1Wickets = activeMatch.inning1Wickets,
                        inning1Balls = activeMatch.inning1Balls,
                        inning1Completed = activeMatch.inning1Completed,
                        inning1Batsmen = parseBatsmenJson(activeMatch.inning1BatsmenJson),
                        inning1Bowlers = parseBowlersJson(activeMatch.inning1BowlersJson),
                        inning2Runs = activeMatch.inning2Runs,
                        inning2Wickets = activeMatch.inning2Wickets,
                        inning2Balls = activeMatch.inning2Balls,
                        inning2Batsmen = parseBatsmenJson(activeMatch.inning2BatsmenJson),
                        inning2Bowlers = parseBowlersJson(activeMatch.inning2BowlersJson),
                        currentOverBalls = parseBallsString(activeMatch.currentOverBallsJson),
                        ballHistoryList = parseBallsString(activeMatch.ballHistoryJson),
                        striker = BatsmanState(
                            name = activeMatch.strikerName.ifEmpty { "Striker" },
                            runs = activeMatch.strikerRuns,
                            balls = activeMatch.strikerBalls,
                            fours = activeMatch.strikerFours,
                            sixes = activeMatch.strikerSixes,
                            sequenceNumber = activeMatch.strikerSequence
                        ),
                        nonStriker = BatsmanState(
                            name = activeMatch.nonStrikerName.ifEmpty { "Non-Striker" },
                            runs = activeMatch.nonStrikerRuns,
                            balls = activeMatch.nonStrikerBalls,
                            fours = activeMatch.nonStrikerFours,
                            sixes = activeMatch.nonStrikerSixes,
                            sequenceNumber = activeMatch.nonStrikerSequence
                        ),
                        activeStrikerIsFirst = activeMatch.activeStrikerIsFirst,
                        bowler = BowlerState(
                            activeMatch.bowlerName.ifEmpty { "Bowler" },
                            activeMatch.bowlerBalls,
                            activeMatch.bowlerRuns,
                            activeMatch.bowlerWickets,
                            activeMatch.bowlerMaidens
                        )
                    )
                }
            }
        }
    }

    fun processIntent(intent: CricketIntent) {
        if (isScoringIntent(intent)) {
            val state = _uiState.value
            if (!state.isMatchActive || (state.innings == 1 && state.inning1Completed)) return
            saveStateToHistory(state)
        }

        when (intent) {
            is CricketIntent.UpdateHostTeam -> _uiState.update { it.copy(hostTeam = intent.name) }
            is CricketIntent.UpdateVisitorTeam -> _uiState.update { it.copy(visitorTeam = intent.name) }
            is CricketIntent.UpdateStrikerName -> _uiState.update { it.copy(strikerName = intent.name) }
            is CricketIntent.UpdateNonStrikerName -> _uiState.update { it.copy(nonStrikerName = intent.name) }
            is CricketIntent.UpdateOpeningBowlerName -> _uiState.update { it.copy(openingBowlerName = intent.name) }
            is CricketIntent.UpdateOvers -> _uiState.update { it.copy(overs = intent.overs) }
            is CricketIntent.UpdateMaxWickets -> _uiState.update { it.copy(maxWickets = intent.wickets) }
            is CricketIntent.UpdateMatchFormat -> _uiState.update { it.copy(matchFormat = intent.format) }
            is CricketIntent.StartMatch -> handleStartMatch()
            is CricketIntent.RecordRuns -> handleRecordRuns(intent.runs)
            is CricketIntent.RecordExtra -> handleRecordExtra(intent.extraType, intent.additionalRuns)
            is CricketIntent.RequestWicket -> _uiState.update { it.copy(showWicketDialog = true) }
            is CricketIntent.ConfirmWicket -> handleConfirmWicket(intent.newBatsmanName, intent.isLegalBall)
            is CricketIntent.DismissWicketDialog -> _uiState.update { it.copy(showWicketDialog = false) }
            is CricketIntent.RotateStrike -> _uiState.update { it.copy(activeStrikerIsFirst = !it.activeStrikerIsFirst) }
            is CricketIntent.SetNextBowler -> handleSetNextBowler(intent.name)
            is CricketIntent.SetupSecondInnings -> handleSetupSecondInnings(intent.striker, intent.nonStriker, intent.bowler)
            is CricketIntent.UndoLastBall -> handleUndo()
            is CricketIntent.NewMatchSetup -> handleReset()
            is CricketIntent.ChangeTab -> _uiState.update { it.copy(currentTab = intent.tab) }
            is CricketIntent.DeleteMatch -> viewModelScope.launch { repository.deleteMatchById(intent.matchId) }
            is CricketIntent.LoadPastMatch -> handleLoadPastMatch(intent.matchId)
            is CricketIntent.ClearToast -> _uiState.update { it.copy(messageToast = null) }
        }
    }

    private fun saveStateToHistory(state: CricketUiState) {
        if (stateHistory.size >= 50) stateHistory.removeFirst()
        stateHistory.addLast(state.copy(savedMatches = emptyList()))
    }

    private fun isScoringIntent(intent: CricketIntent) =
        intent is CricketIntent.RecordRuns || intent is CricketIntent.RecordExtra || 
        intent is CricketIntent.ConfirmWicket || intent is CricketIntent.RotateStrike ||
        intent is CricketIntent.SetNextBowler

    private fun handleStartMatch() {
        val state = _uiState.value
        viewModelScope.launch {
            val match = MatchEntity(
                hostTeam = state.hostTeam.trim().ifEmpty { "Host" },
                visitorTeam = state.visitorTeam.trim().ifEmpty { "Visitor" },
                totalOvers = state.overs,
                matchFormat = state.matchFormat,
                strikerName = state.strikerName.trim().ifEmpty { "Batsman 1" },
                nonStrikerName = state.nonStrikerName.trim().ifEmpty { "Batsman 2" },
                bowlerName = state.openingBowlerName.trim().ifEmpty { "Bowler 1" },
                strikerSequence = 1,
                nonStrikerSequence = 2
            )
            val id = repository.insertMatch(match).toInt()
            _uiState.update { it.copy(
                activeMatchId = id, isMatchActive = true, innings = 1,
                striker = BatsmanState(match.strikerName, sequenceNumber = 1),
                nonStriker = BatsmanState(match.nonStrikerName, sequenceNumber = 2),
                bowler = BowlerState(match.bowlerName),
                inning1Runs = 0, inning1Wickets = 0, inning1Balls = 0,
                inning2Runs = 0, inning2Wickets = 0, inning2Balls = 0,
                inning1Batsmen = emptyList(), inning2Batsmen = emptyList(),
                inning1Bowlers = emptyList(), inning2Bowlers = emptyList(),
                currentOverBalls = emptyList(), ballHistoryList = emptyList(),
                inning1Completed = false, showNewInningsDialog = false
            ) }
        }
    }

    private fun handleRecordRuns(runs: Int) {
        _uiState.update { state ->
            if (!state.isMatchActive || (state.innings == 1 && state.inning1Completed)) return@update state
            
            val facingBatsman = if (state.activeStrikerIsFirst) state.striker else state.nonStriker
            val updatedFacing = facingBatsman.copy(
                runs = facingBatsman.runs + runs,
                balls = facingBatsman.balls + 1,
                fours = facingBatsman.fours + (if (runs == 4) 1 else 0),
                sixes = facingBatsman.sixes + (if (runs == 6) 1 else 0)
            )
            
            val updatedStriker = if (state.activeStrikerIsFirst) updatedFacing else state.striker
            val updatedNonStriker = if (!state.activeStrikerIsFirst) updatedFacing else state.nonStriker
            
            val updatedBowler = state.bowler.copy(runs = state.bowler.runs + runs, balls = state.bowler.balls + 1)
            val nextBalls = state.currentInningsBalls + 1
            val nextRuns = state.currentInningsRuns + runs

            val outcome = runs.toString()
            val intermediateState = state.copy(
                striker = updatedStriker, 
                nonStriker = updatedNonStriker,
                bowler = updatedBowler,
                currentOverBalls = state.currentOverBalls + outcome,
                ballHistoryList = state.ballHistoryList + outcome,
                activeStrikerIsFirst = if (runs % 2 != 0) !state.activeStrikerIsFirst else state.activeStrikerIsFirst,
                inning1Runs = if (state.innings == 1) nextRuns else state.inning1Runs,
                inning1Balls = if (state.innings == 1) nextBalls else state.inning1Balls,
                inning2Runs = if (state.innings == 2) nextRuns else state.inning2Runs,
                inning2Balls = if (state.innings == 2) nextBalls else state.inning2Balls
            )
            applyInningsLogic(intermediateState)
        }
    }

    private fun handleRecordExtra(type: String, additionalRuns: Int) {
        _uiState.update { state ->
            if (!state.isMatchActive || (state.innings == 1 && state.inning1Completed)) return@update state
            
            val penalty = if (type == "WD" || type == "NB") 1 else 0
            val totalBallRuns = penalty + additionalRuns
            val isLegal = type == "B" || type == "LB"
            
            val facingBatsman = if (state.activeStrikerIsFirst) state.striker else state.nonStriker
            val updatedFacing = if (isLegal) facingBatsman.copy(balls = facingBatsman.balls + 1) else facingBatsman
            
            val updatedStriker = if (state.activeStrikerIsFirst) updatedFacing else state.striker
            val updatedNonStriker = if (!state.activeStrikerIsFirst) updatedFacing else state.nonStriker

            val outcome = if (additionalRuns > 0) "$type+$additionalRuns" else type
            val intermediateState = state.copy(
                striker = updatedStriker,
                nonStriker = updatedNonStriker,
                currentOverBalls = state.currentOverBalls + outcome,
                ballHistoryList = state.ballHistoryList + outcome,
                inning1Runs = if (state.innings == 1) state.inning1Runs + totalBallRuns else state.inning1Runs,
                inning2Runs = if (state.innings == 2) state.inning2Runs + totalBallRuns else state.inning2Runs,
                bowler = state.bowler.copy(runs = state.bowler.runs + (if (type == "WD" || type == "NB") totalBallRuns else 0), balls = state.bowler.balls + (if (isLegal) 1 else 0)),
                inning1Balls = if (state.innings == 1 && isLegal) state.inning1Balls + 1 else state.inning1Balls,
                inning2Balls = if (state.innings == 2 && isLegal) state.inning2Balls + 1 else state.inning2Balls,
                activeStrikerIsFirst = if (additionalRuns % 2 != 0) !state.activeStrikerIsFirst else state.activeStrikerIsFirst
            )
            applyInningsLogic(intermediateState)
        }
    }

    private fun handleConfirmWicket(newBatsmanName: String, isLegalBall: Boolean) {
        _uiState.update { state ->
            if (!state.isMatchActive || (state.innings == 1 && state.inning1Completed)) return@update state
            
            val facingBatsman = if (state.activeStrikerIsFirst) state.striker else state.nonStriker
            val dismissedBatsman = facingBatsman.copy(
                balls = if (isLegalBall) facingBatsman.balls + 1 else facingBatsman.balls, 
                outDetail = "b ${state.bowler.name}"
            )
            
            val newInning1Batsmen = if (state.innings == 1) (state.inning1Batsmen + dismissedBatsman) else state.inning1Batsmen
            val newInning2Batsmen = if (state.innings == 2) (state.inning2Batsmen + dismissedBatsman) else state.inning2Batsmen

            val currentWickets = if (state.innings == 1) state.inning1Wickets else state.inning2Wickets
            val isAllOut = currentWickets + 1 >= state.maxWickets
            
            val nextSequence = (state.inning1Batsmen.size + state.inning2Batsmen.size) + 3
            val placeholderBatsman = if (isAllOut) BatsmanState("") else BatsmanState(newBatsmanName.trim().ifEmpty { "New Batsman" }, sequenceNumber = nextSequence)
            val updatedStriker = if (state.activeStrikerIsFirst) placeholderBatsman else state.striker
            val updatedNonStriker = if (!state.activeStrikerIsFirst) placeholderBatsman else state.nonStriker

            val intermediateState = state.copy(
                striker = updatedStriker,
                nonStriker = updatedNonStriker,
                inning1Batsmen = newInning1Batsmen,
                inning2Batsmen = newInning2Batsmen,
                bowler = state.bowler.copy(wickets = state.bowler.wickets + 1, balls = state.bowler.balls + (if (isLegalBall) 1 else 0)),
                currentOverBalls = state.currentOverBalls + "W",
                ballHistoryList = state.ballHistoryList + "W",
                inning1Wickets = if (state.innings == 1) state.inning1Wickets + 1 else state.inning1Wickets,
                inning1Balls = if (state.innings == 1 && isLegalBall) state.inning1Balls + 1 else state.inning1Balls,
                inning2Wickets = if (state.innings == 2) state.inning2Wickets + 1 else state.inning2Wickets,
                inning2Balls = if (state.innings == 2 && isLegalBall) state.inning2Balls + 1 else state.inning2Balls,
                showWicketDialog = false
            )
            applyInningsLogic(intermediateState)
        }
    }

    private fun applyInningsLogic(state: CricketUiState): CricketUiState {
        if (!state.isMatchActive) return state
        if (state.innings == 1 && state.inning1Completed) return state

        var nextState = state
        val oversCompleted = state.currentInningsBalls >= state.overs * 6
        val allOut = state.currentInningsWickets >= state.maxWickets
        val targetReached = state.innings == 2 && state.inning2Runs > state.inning1Runs

        if (targetReached || (state.innings == 2 && (oversCompleted || allOut))) {
            val finalInning2Batsmen = prepareFinalScorecard(state.inning2Batsmen, state.striker, state.nonStriker)
            val finalInning2Bowlers = updateBowlerList(state.inning2Bowlers, state.bowler)
            
            val winner = when {
                state.inning2Runs > state.inning1Runs -> state.visitorTeam.ifEmpty { "Visitor Team" }
                state.inning1Runs > state.inning2Runs -> state.hostTeam.ifEmpty { "Host Team" }
                else -> "None (Tie)"
            }

            nextState = state.copy(
                isMatchActive = false,
                inning2Batsmen = finalInning2Batsmen,
                inning2Bowlers = finalInning2Bowlers,
                messageToast = "Match Completed! Winner: $winner"
            )
        } else if (state.innings == 1 && (oversCompleted || allOut)) {
            val finalInning1Batsmen = prepareFinalScorecard(state.inning1Batsmen, state.striker, state.nonStriker)
            val finalInning1Bowlers = updateBowlerList(state.inning1Bowlers, state.bowler)

            nextState = state.copy(
                inning1Completed = true,
                inning1Batsmen = finalInning1Batsmen,
                inning1Bowlers = finalInning1Bowlers,
                showNewInningsDialog = true,
                messageToast = "Innings 1 Completed. Target: ${state.inning1Runs + 1}"
            )
        } else {
            val legalBallsThisOver = state.currentOverBalls.count { !it.contains("WD") && !it.contains("NB") }
            if (legalBallsThisOver >= 6) {
                val runsInOver = state.currentOverBalls.sumOf { ball ->
                    when {
                        ball.contains("WD") || ball.contains("NB") -> {
                            val extraParts = ball.split("+")
                            val additional = if (extraParts.size > 1) extraParts[1].toIntOrNull() ?: 0 else 0
                            1 + additional
                        }
                        ball == "W" || ball == "B" || ball == "LB" || ball == "0" -> 0
                        ball.all { it.isDigit() } -> ball.toInt()
                        else -> 0
                    }
                }
                
                val updatedBowler = if (runsInOver == 0) {
                    state.bowler.copy(maidens = state.bowler.maidens + 1)
                } else {
                    state.bowler
                }

                val updatedBowlers = if (state.innings == 1) {
                    updateBowlerList(state.inning1Bowlers, updatedBowler)
                } else {
                    updateBowlerList(state.inning2Bowlers, updatedBowler)
                }

                nextState = state.copy(
                    bowler = updatedBowler,
                    inning1Bowlers = if (state.innings == 1) updatedBowlers else state.inning1Bowlers,
                    inning2Bowlers = if (state.innings == 2) updatedBowlers else state.inning2Bowlers,
                    showNextBowlerDialog = true,
                    activeStrikerIsFirst = !state.activeStrikerIsFirst
                )
            }
        }
        
        saveMatchToDb(nextState)
        return nextState
    }

    private fun prepareFinalScorecard(dismissed: List<BatsmanState>, s1: BatsmanState, s2: BatsmanState): List<BatsmanState> {
        val stillBatting = listOf(s1, s2).filter { it.name.isNotEmpty() }.map { it.copy(outDetail = "not out") }
        return (dismissed + stillBatting).sortedBy { it.sequenceNumber }
    }

    private fun handleSetupSecondInnings(striker: String, nonStriker: String, bowler: String) {
        _uiState.update { state ->
            state.copy(
                innings = 2,
                striker = BatsmanState(striker.trim().ifEmpty { "Batsman 1" }, sequenceNumber = 1),
                nonStriker = BatsmanState(nonStriker.trim().ifEmpty { "Batsman 2" }, sequenceNumber = 2),
                bowler = BowlerState(bowler.trim().ifEmpty { "Bowler 1" }),
                currentOverBalls = emptyList(),
                ballHistoryList = emptyList(),
                activeStrikerIsFirst = true,
                showNewInningsDialog = false,
                isMatchActive = true
            )
        }
        saveMatchToDb(_uiState.value)
    }

    private fun updateBowlerList(list: List<BowlerState>, current: BowlerState): List<BowlerState> {
        val index = list.indexOfFirst { it.name == current.name }
        return if (index != -1) {
            list.toMutableList().apply { this[index] = current }
        } else {
            list + current
        }
    }

    private fun handleSetNextBowler(name: String) {
        _uiState.update { state ->
            val existingBowler = (if (state.innings == 1) state.inning1Bowlers else state.inning2Bowlers)
                .find { it.name == name.trim() }
            
            state.copy(
                bowler = existingBowler ?: BowlerState(name.trim()),
                currentOverBalls = emptyList(),
                showNextBowlerDialog = false
            )
        }
        saveMatchToDb(_uiState.value)
    }

    private fun handleUndo() {
        if (stateHistory.isNotEmpty()) {
            val prevState = stateHistory.removeLast()
            _uiState.update { prevState.copy(savedMatches = it.savedMatches) }
            saveMatchToDb(_uiState.value)
        }
    }

    private fun handleReset() {
        stateHistory.clear()
        _uiState.update { CricketUiState(savedMatches = it.savedMatches) }
    }

    private fun saveMatchToDb(state: CricketUiState) {
        val id = state.activeMatchId ?: return
        viewModelScope.launch {
            repository.getMatchById(id)?.let { match ->
                repository.updateMatch(match.copy(
                    inning1Runs = state.inning1Runs, inning1Wickets = state.inning1Wickets, inning1Balls = state.inning1Balls,
                    inning1Completed = state.inning1Completed,
                    inning2Runs = state.inning2Runs, inning2Wickets = state.inning2Wickets, inning2Balls = state.inning2Balls,
                    currentInnings = state.innings, 
                    strikerName = state.striker.name, strikerRuns = state.striker.runs, strikerBalls = state.striker.balls,
                    strikerFours = state.striker.fours, strikerSixes = state.striker.sixes, strikerSequence = state.striker.sequenceNumber,
                    nonStrikerName = state.nonStriker.name, nonStrikerRuns = state.nonStriker.runs, nonStrikerBalls = state.nonStriker.balls,
                    nonStrikerFours = state.nonStriker.fours, nonStrikerSixes = state.nonStriker.sixes, nonStrikerSequence = state.nonStriker.sequenceNumber,
                    activeStrikerIsFirst = state.activeStrikerIsFirst,
                    bowlerName = state.bowler.name, bowlerRuns = state.bowler.runs, bowlerBalls = state.bowler.balls,
                    bowlerWickets = state.bowler.wickets, bowlerMaidens = state.bowler.maidens,
                    ballHistoryJson = state.ballHistoryList.joinToString(","),
                    currentOverBallsJson = state.currentOverBalls.joinToString(","),
                    inning1BatsmenJson = batsmanListAdapter.toJson(state.inning1Batsmen),
                    inning2BatsmenJson = batsmanListAdapter.toJson(state.inning2Batsmen),
                    inning1BowlersJson = bowlerListAdapter.toJson(state.inning1Bowlers),
                    inning2BowlersJson = bowlerListAdapter.toJson(state.inning2Bowlers),
                    isCompleted = !state.isMatchActive
                ))
            }
        }
    }

    private fun handleLoadPastMatch(id: Int) {
        viewModelScope.launch {
            repository.getMatchById(id)?.let { match ->
                _uiState.update { it.copy(
                    hostTeam = match.hostTeam, visitorTeam = match.visitorTeam, activeMatchId = match.id,
                    isMatchActive = !match.isCompleted, innings = match.currentInnings,
                    inning1Runs = match.inning1Runs, inning1Wickets = match.inning1Wickets, inning1Balls = match.inning1Balls,
                    inning1Completed = match.inning1Completed,
                    inning1Batsmen = parseBatsmenJson(match.inning1BatsmenJson),
                    inning1Bowlers = parseBowlersJson(match.inning1BowlersJson),
                    inning2Runs = match.inning2Runs, inning2Wickets = match.inning2Wickets, inning2Balls = match.inning2Balls,
                    inning2Batsmen = parseBatsmenJson(match.inning2BatsmenJson),
                    inning2Bowlers = parseBowlersJson(match.inning2BowlersJson),
                    striker = BatsmanState(match.strikerName, match.strikerRuns, match.strikerBalls, match.strikerFours, match.strikerSixes, sequenceNumber = match.strikerSequence),
                    nonStriker = BatsmanState(match.nonStrikerName, match.nonStrikerRuns, match.nonStrikerBalls, match.nonStrikerFours, match.nonStrikerSixes, sequenceNumber = match.nonStrikerSequence),
                    activeStrikerIsFirst = match.activeStrikerIsFirst,
                    bowler = BowlerState(match.bowlerName, match.bowlerBalls, match.bowlerRuns, match.bowlerWickets, match.bowlerMaidens),
                    currentOverBalls = parseBallsString(match.currentOverBallsJson),
                    ballHistoryList = parseBallsString(match.ballHistoryJson),
                    currentTab = NavigationTab.SCORECARD
                ) }
            }
        }
    }

    private fun parseBallsString(s: String) = if (s.isEmpty()) emptyList() else s.split(",")
    
    private fun parseBatsmenJson(json: String): List<BatsmanState> {
        return if (json.isEmpty()) emptyList() else try {
            batsmanListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseBowlersJson(json: String): List<BowlerState> {
        return if (json.isEmpty()) emptyList() else try {
            bowlerListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class CricketViewModelFactory(private val repository: MatchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CricketViewModel(repository) as T
}
