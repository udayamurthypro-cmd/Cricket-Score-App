package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hostTeam: String,
    val visitorTeam: String,
    val totalOvers: Int,
    val matchFormat: String,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Innings scoring state
    val inning1Runs: Int = 0,
    val inning1Wickets: Int = 0,
    val inning1Balls: Int = 0,
    val inning1Completed: Boolean = false,
    val inning2Runs: Int = 0,
    val inning2Wickets: Int = 0,
    val inning2Balls: Int = 0,
    
    val currentInnings: Int = 1,
    
    // Live state persistence
    val strikerName: String = "",
    val strikerRuns: Int = 0,
    val strikerBalls: Int = 0,
    val strikerFours: Int = 0,
    val strikerSixes: Int = 0,
    val strikerSequence: Int = 1,
    
    val nonStrikerName: String = "",
    val nonStrikerRuns: Int = 0,
    val nonStrikerBalls: Int = 0,
    val nonStrikerFours: Int = 0,
    val nonStrikerSixes: Int = 0,
    val nonStrikerSequence: Int = 2,
    
    val activeStrikerIsFirst: Boolean = true,
    
    val bowlerName: String = "",
    val bowlerRuns: Int = 0,
    val bowlerBalls: Int = 0,
    val bowlerWickets: Int = 0,
    val bowlerMaidens: Int = 0,

    val ballHistoryJson: String = "",
    val currentOverBallsJson: String = "",
    
    // Scorecard data as JSON strings
    val inning1BatsmenJson: String = "",
    val inning2BatsmenJson: String = "",
    val inning1BowlersJson: String = "",
    val inning2BowlersJson: String = ""
)
