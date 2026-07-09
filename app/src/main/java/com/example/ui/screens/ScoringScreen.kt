package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.mvi.CricketIntent
import com.example.ui.mvi.CricketUiState

@Composable
fun ScoringScreen(
    state: CricketUiState,
    onIntent: (CricketIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExtraRunsDialog by remember { mutableStateOf<String?>(null) }

    if (state.showNextBowlerDialog) {
        NextBowlerSelectionDialog(onSelected = { onIntent(CricketIntent.SetNextBowler(it)) })
    }

    if (state.showWicketDialog) {
        WicketSelectionDialog(
            onConfirm = { name, isLegal -> onIntent(CricketIntent.ConfirmWicket(name, isLegal)) },
            onDismiss = { onIntent(CricketIntent.DismissWicketDialog) }
        )
    }

    if (state.showNewInningsDialog) {
        NewInningsSetupDialog(
            hostTeam = state.hostTeam,
            visitorTeam = state.visitorTeam,
            onConfirm = { s, ns, b -> onIntent(CricketIntent.SetupSecondInnings(s, ns, b)) },
            onDismiss = { onIntent(CricketIntent.UndoLastBall) } // Allow going back to undo the ball that ended innings
        )
    }
    
    if (showExtraRunsDialog != null) {
        ExtraRunsDialog(
            type = showExtraRunsDialog!!,
            onConfirm = { runs -> 
                onIntent(CricketIntent.RecordExtra(showExtraRunsDialog!!, runs))
                showExtraRunsDialog = null
            },
            onDismiss = { showExtraRunsDialog = null }
        )
    }

    val verticalScroll = rememberScrollState()
    val isMatchActive = state.isMatchActive

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Match Result / Status Banner
        if (!isMatchActive) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MATCH COMPLETED",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.innings == 2 && state.targetScore != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "TARGET: ${state.targetScore}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp
                        )
                        val remainingBalls = (state.overs * 6) - state.inning2Balls
                        val runsNeededText = if (state.runsNeeded != null) "NEED ${state.runsNeeded} OFF $remainingBalls" else "MATCH OVER"
                        Text(
                            text = if (isMatchActive) runsNeededText else "MATCH OVER",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            IconButton(
                enabled = isMatchActive || state.showNewInningsDialog,
                onClick = { onIntent(CricketIntent.UndoLastBall) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo last ball")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = if (state.innings == 1) "INNINGS 1" else "INNINGS 2",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.currentInningsRuns}/${state.currentInningsWickets}",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("current_score_text")
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Overs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = state.currentOversString,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/${state.overs}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "CRR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = state.formatCRR(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "REQ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = state.formatRRR(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isStriker1 = state.activeStrikerIsFirst
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = isMatchActive) { onIntent(CricketIntent.RotateStrike) }
                    .border(
                        width = if (isStriker1) 1.5.dp else 0.dp,
                        color = if (isStriker1) MaterialTheme.colorScheme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isStriker1) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${state.striker.name}${if (isStriker1) "*" else ""}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "${state.striker.runs} (${state.striker.balls})", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = isMatchActive) { onIntent(CricketIntent.RotateStrike) }
                    .border(
                        width = if (!isStriker1) 1.5.dp else 0.dp,
                        color = if (!isStriker1) MaterialTheme.colorScheme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isStriker1) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${state.nonStriker.name}${if (!isStriker1) "*" else ""}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "${state.nonStriker.runs} (${state.nonStriker.balls})", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Bowling: ${state.bowler.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${state.bowler.wickets}/${state.bowler.runs} (${state.bowler.oversString})", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Scoring Controls
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreActionButton(label = "0", onClick = { onIntent(CricketIntent.RecordRuns(0)) }, modifier = Modifier.weight(1f), enabled = isMatchActive)
                ScoreActionButton(label = "1", onClick = { onIntent(CricketIntent.RecordRuns(1)) }, modifier = Modifier.weight(1f), enabled = isMatchActive)
                ScoreActionButton(label = "2", onClick = { onIntent(CricketIntent.RecordRuns(2)) }, modifier = Modifier.weight(1f), enabled = isMatchActive)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreActionButton(label = "3", onClick = { onIntent(CricketIntent.RecordRuns(3)) }, modifier = Modifier.weight(1f), enabled = isMatchActive)
                ScoreActionButton(label = "4", onClick = { onIntent(CricketIntent.RecordRuns(4)) }, modifier = Modifier.weight(1f), isHighlight = true, enabled = isMatchActive)
                ScoreActionButton(label = "6", onClick = { onIntent(CricketIntent.RecordRuns(6)) }, modifier = Modifier.weight(1f), isHighlight = true, enabled = isMatchActive)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtraButton(label = "WD", onClick = { showExtraRunsDialog = "WD" }, modifier = Modifier.weight(1f), enabled = isMatchActive)
                ExtraButton(label = "NB", onClick = { showExtraRunsDialog = "NB" }, modifier = Modifier.weight(1f), enabled = isMatchActive)
                ExtraButton(label = "B", onClick = { showExtraRunsDialog = "B" }, modifier = Modifier.weight(1f), enabled = isMatchActive)
                ExtraButton(label = "LB", onClick = { showExtraRunsDialog = "LB" }, modifier = Modifier.weight(1f), enabled = isMatchActive)
            }

            Button(
                onClick = { onIntent(CricketIntent.RequestWicket) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isMatchActive
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    Text(text = "WICKET", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Column {
            Text(text = "THIS OVER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.currentOverBalls.forEach { TimelineBallCircle(outcome = it) }
                repeat(maxOf(0, 6 - state.currentOverBalls.count { !it.contains("WD") && !it.contains("NB") })) { TimelineBallCirclePlaceholder() }
            }
        }

        OutlinedButton(
            onClick = { onIntent(CricketIntent.NewMatchSetup) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reset & Start New Match", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun NewInningsSetupDialog(hostTeam: String, visitorTeam: String, onConfirm: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var striker by remember { mutableStateOf("") }
    var nonStriker by remember { mutableStateOf("") }
    var bowler by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup 2nd Innings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Innings 1 finished. Enter 2nd Innings details:")
                OutlinedTextField(value = striker, onValueChange = { striker = it }, label = { Text("Striker Name") })
                OutlinedTextField(value = nonStriker, onValueChange = { nonStriker = it }, label = { Text("Non-Striker Name") })
                OutlinedTextField(value = bowler, onValueChange = { bowler = it }, label = { Text("Bowler Name") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(striker, nonStriker, bowler) }) { Text("Start 2nd Innings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Undo Last Ball") }
        }
    )
}

@Composable
fun ExtraRunsDialog(type: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var runs by remember { mutableStateOf("0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extra: $type") },
        text = {
            Column {
                Text("Enter additional runs (wides/no-balls already include 1 penalty run):")
                OutlinedTextField(
                    value = runs,
                    onValueChange = { if (it.all { char -> char.isDigit() }) runs = it },
                    label = { Text("Additional Runs") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(runs.toIntOrNull() ?: 0) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ScoreActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, isHighlight: Boolean = false, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isHighlight) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(56.dp)
    ) {
        Text(text = label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExtraButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(44.dp)
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimelineBallCircle(outcome: String) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(text = outcome, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimelineBallCirclePlaceholder() {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = ".", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}

@Composable
fun NextBowlerSelectionDialog(onSelected: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Next Over Bowler") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Enter Bowler Name") })
        },
        confirmButton = {
            Button(onClick = { onSelected(name.ifEmpty { "Bowler" }) }) { Text("Confirm") }
        }
    )
}

@Composable
fun WicketSelectionDialog(onConfirm: (String, Boolean) -> Unit, onDismiss: () -> Unit) {
    var nextBatsmanName by remember { mutableStateOf("") }
    var isLegalBall by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wicket Fallen!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the name of the new batsman:")
                OutlinedTextField(
                    value = nextBatsmanName,
                    onValueChange = { nextBatsmanName = it },
                    placeholder = { Text("e.g. MS Dhoni") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isLegalBall, onCheckedChange = { isLegalBall = it })
                    Text("Legal Ball (Count toward over)")
                }
                Text(
                    text = "Uncheck for Run Out on Wide/No Ball",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nextBatsmanName.ifEmpty { "New Batsman" }, isLegalBall) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
