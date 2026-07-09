package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.mvi.BatsmanState
import com.example.ui.mvi.BowlerState
import com.example.ui.mvi.CricketUiState
import java.util.Locale

@Composable
fun AnalysisScreen(
    state: CricketUiState,
    modifier: Modifier = Modifier
) {
    if (state.activeMatchId == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Live Match",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start a match from the setup screen to see live analysis here.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val scrollState = rememberScrollState()

    var selectedInningsTab by remember { mutableIntStateOf(state.innings) }
    
    LaunchedEffect(state.innings) {
        selectedInningsTab = state.innings
    }

    val team1Name = state.hostTeam.ifEmpty { "Host Team" }
    val team2Name = state.visitorTeam.ifEmpty { "Visitor Team" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${team1Name.uppercase(Locale.getDefault())} vs ${team2Name.uppercase(Locale.getDefault())}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val currentStatusText = when {
                    !state.isMatchActive -> {
                        val target = state.inning1Runs + 1
                        when {
                            state.inning2Runs >= target -> "$team2Name won the match!"
                            state.inning1Runs > state.inning2Runs -> "$team1Name won the match!"
                            else -> "Match ended as a Tie"
                        }
                    }
                    state.innings == 1 -> "$team1Name is batting first"
                    else -> {
                        val remainingBalls = (state.overs * 6) - state.inning2Balls
                        if (state.runsNeeded != null) "$team2Name need ${state.runsNeeded} runs from $remainingBalls balls"
                        else "Innings 2 in progress"
                    }
                }
                
                Text(
                    text = currentStatusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedInningsTab - 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedInningsTab == 1,
                onClick = { selectedInningsTab = 1 },
                text = { Text(team1Name, fontWeight = FontWeight.Bold, maxLines = 1) }
            )
            Tab(
                selected = selectedInningsTab == 2,
                onClick = {
                    if (state.innings >= 2 || state.inning1Completed) {
                        selectedInningsTab = 2
                    }
                },
                text = {
                    val isLocked = state.innings < 2 && !state.inning1Completed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(team2Name, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "locked",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            )
        }

        if (selectedInningsTab == 2 && state.innings < 2 && !state.inning1Completed) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Innings 2 Not Started", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        } else {
            val totalRuns = if (selectedInningsTab == 1) state.inning1Runs else state.inning2Runs
            val totalWickets = if (selectedInningsTab == 1) state.inning1Wickets else state.inning2Wickets
            val totalBalls = if (selectedInningsTab == 1) state.inning1Balls else state.inning2Balls
            val oversString = "${totalBalls / 6}.${totalBalls % 6}"
            
            // Use state's CRR logic if current innings is selected, otherwise calculate for historical innings
            val crrText = if (state.innings == selectedInningsTab) state.formatCRR() else {
                val crrValue = if (totalBalls == 0) 0.0 else (totalRuns.toDouble() / (totalBalls.toDouble() / 6.0))
                String.format(Locale.US, "%.2f", crrValue)
            }

            val batsmenList = if (selectedInningsTab == 1) {
                if (state.innings == 1 && state.isMatchActive && !state.inning1Completed) {
                    val live = mutableListOf<BatsmanState>()
                    if (state.striker.name.isNotEmpty()) live.add(state.striker)
                    if (state.nonStriker.name.isNotEmpty()) live.add(state.nonStriker)
                    (state.inning1Batsmen + live).sortedBy { it.sequenceNumber }
                } else state.inning1Batsmen.sortedBy { it.sequenceNumber }
            } else {
                if (state.innings == 2 && state.isMatchActive) {
                    val live = mutableListOf<BatsmanState>()
                    if (state.striker.name.isNotEmpty()) live.add(state.striker)
                    if (state.nonStriker.name.isNotEmpty()) live.add(state.nonStriker)
                    (state.inning2Batsmen + live).sortedBy { it.sequenceNumber }
                } else state.inning2Batsmen.sortedBy { it.sequenceNumber }
            }

            val currentBowlers = if (selectedInningsTab == 1) state.inning1Bowlers else state.inning2Bowlers
            val bowlersList = if (state.isMatchActive && state.innings == selectedInningsTab) {
                updateBowlerSummaryList(currentBowlers, state.bowler)
            } else {
                currentBowlers
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = if (selectedInningsTab == 1) "$team1Name Innings" else "$team2Name Innings", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(text = "CRR: $crrText", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(text = "$totalRuns/$totalWickets ($oversString)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "BATTING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(text = "Batter", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.5f))
                            Text(text = "R", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text(text = "B", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text(text = "4s", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                            Text(text = "6s", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                        }
                        HorizontalDivider()

                        batsmenList.filter { it.name.isNotEmpty() }.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(2.5f)) {
                                    Text(text = row.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(text = row.outDetail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(text = row.runs.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text(text = row.balls.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text(text = row.fours.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                                Text(text = row.sixes.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "BOWLING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(text = "Bowler", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.5f))
                            Text(text = "O", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text(text = "M", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                            Text(text = "R", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text(text = "W", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                        }
                        HorizontalDivider()

                        bowlersList.filter { it.name.isNotEmpty() }.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = row.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(2.5f))
                                Text(text = row.oversString, fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text(text = row.maidens.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                                Text(text = row.runs.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text(text = row.wickets.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun updateBowlerSummaryList(list: List<BowlerState>, current: BowlerState): List<BowlerState> {
    val index = list.indexOfFirst { it.name == current.name }
    return if (index != -1) {
        list.toMutableList().apply { this[index] = current }
    } else {
        list + current
    }
}
