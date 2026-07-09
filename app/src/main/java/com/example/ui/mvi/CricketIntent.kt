package com.example.ui.mvi

sealed interface CricketIntent {
    // Setup actions
    data class UpdateHostTeam(val name: String) : CricketIntent
    data class UpdateVisitorTeam(val name: String) : CricketIntent
    data class UpdateStrikerName(val name: String) : CricketIntent
    data class UpdateNonStrikerName(val name: String) : CricketIntent
    data class UpdateOpeningBowlerName(val name: String) : CricketIntent
    data class UpdateOvers(val overs: Int) : CricketIntent
    data class UpdateMaxWickets(val wickets: Int) : CricketIntent
    data class UpdateMatchFormat(val format: String) : CricketIntent
    data object StartMatch : CricketIntent
    
    // Scoring actions
    data class RecordRuns(val runs: Int) : CricketIntent
    data class RecordExtra(val extraType: String, val additionalRuns: Int = 0) : CricketIntent // "WD", "NB", "B", "LB"
    data object RequestWicket : CricketIntent
    data class ConfirmWicket(val newBatsmanName: String, val isLegalBall: Boolean = true) : CricketIntent
    data object DismissWicketDialog : CricketIntent
    data object RotateStrike : CricketIntent
    data class SetNextBowler(val name: String) : CricketIntent
    data object UndoLastBall : CricketIntent
    data object ClearToast : CricketIntent
    data object NewMatchSetup : CricketIntent 
    
    // Innings transition
    data class SetupSecondInnings(val striker: String, val nonStriker: String, val bowler: String) : CricketIntent
    
    // Navigation / Persistence
    data class ChangeTab(val tab: NavigationTab) : CricketIntent
    data class DeleteMatch(val matchId: Int) : CricketIntent
    data class LoadPastMatch(val matchId: Int) : CricketIntent
}
