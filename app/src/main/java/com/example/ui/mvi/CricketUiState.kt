package com.example.ui.mvi

import com.example.data.MatchEntity
import java.util.Locale

enum class NavigationTab {
    SCORECARD,
    ANALYSIS,
    HISTORY
}

data class BatsmanState(
    val name: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val outDetail: String = "not out",
    val sequenceNumber: Int = 0 // Used to keep track of batting order
)

data class BowlerState(
    val name: String,
    val balls: Int = 0, // Total legal balls bowled
    val runs: Int = 0,
    val wickets: Int = 0,
    val maidens: Int = 0
) {
    val oversString: String
        get() {
            val completedOvers = balls / 6
            val remainingBalls = balls % 6
            return "$completedOvers.$remainingBalls"
        }
    
    val economyRate: Double
        get() {
            return if (balls == 0) 0.0 else (runs.toDouble() / (balls.toDouble() / 6.0))
        }
}

data class CricketUiState(
    val currentTab: NavigationTab = NavigationTab.SCORECARD,
    
    // SETUP TAB / STATE
    val hostTeam: String = "",
    val visitorTeam: String = "",
    val strikerName: String = "Batsman 1",
    val nonStrikerName: String = "Batsman 2",
    val openingBowlerName: String = "Bowler 1",
    val overs: Int = 20,
    val matchFormat: String = "Friendly",
    val maxWickets: Int = 10,
    
    // SCORING TAB / ACTIVE MATCH STATE
    val activeMatchId: Int? = null,
    val isMatchActive: Boolean = false,
    val innings: Int = 1, // 1 or 2
    
    // Scores for both teams
    val inning1Runs: Int = 0,
    val inning1Wickets: Int = 0,
    val inning1Balls: Int = 0,
    val inning1Completed: Boolean = false,
    val inning1Batsmen: List<BatsmanState> = emptyList(),
    val inning1Bowlers: List<BowlerState> = emptyList(),
    
    val inning2Runs: Int = 0,
    val inning2Wickets: Int = 0,
    val inning2Balls: Int = 0,
    val inning2Batsmen: List<BatsmanState> = emptyList(),
    val inning2Bowlers: List<BowlerState> = emptyList(),
    
    // Current Over outcomes
    val currentOverBalls: List<String> = emptyList(), 
    val ballHistoryList: List<String> = emptyList(), 
    
    // Player Details
    val striker: BatsmanState = BatsmanState(name = ""),
    val nonStriker: BatsmanState = BatsmanState(name = ""),
    val activeStrikerIsFirst: Boolean = true, 
    
    val bowler: BowlerState = BowlerState(name = ""),
    
    // Historic matches list loaded from DB
    val savedMatches: List<MatchEntity> = emptyList(),
    
    // Dialog / Secondary views state
    val showNextBowlerDialog: Boolean = false,
    val showWicketDialog: Boolean = false,
    val showNewInningsDialog: Boolean = false,
    val messageToast: String? = null
) {
    val currentInningsRuns: Int
        get() = if (innings == 1) inning1Runs else inning2Runs

    val currentInningsWickets: Int
        get() = if (innings == 1) inning1Wickets else inning2Wickets

    val currentInningsBalls: Int
        get() = if (innings == 1) inning1Balls else inning2Balls

    val currentOversString: String
        get() {
            val completedOvers = currentInningsBalls / 6
            val remainingBalls = currentInningsBalls % 6
            return "$completedOvers.$remainingBalls"
        }
        
    val currentRunRate: Double
        get() {
            if (currentInningsBalls == 0) return 0.0
            val oversFloat = currentInningsBalls.toDouble() / 6.0
            return currentInningsRuns.toDouble() / oversFloat
        }

    val requiredRunRate: Double
        get() {
            if (innings == 1) return 0.0
            val target = inning1Runs + 1
            val remainingBalls = (overs * 6) - inning2Balls
            if (remainingBalls <= 0) return 0.0
            val runsNeeded = target - inning2Runs
            if (runsNeeded <= 0) return 0.0
            val remainingOversFloat = remainingBalls.toDouble() / 6.0
            return runsNeeded.toDouble() / remainingOversFloat
        }
        
    val targetScore: Int?
        get() = if (innings == 2) inning1Runs + 1 else null
        
    val runsNeeded: Int?
        get() = if (innings == 2) (inning1Runs + 1) - inning2Runs else null

    fun formatCRR(): String = String.format(Locale.US, "%.2f", currentRunRate)
    fun formatRRR(): String = if (innings == 1) "-" else String.format(Locale.US, "%.2f", requiredRunRate)
}
