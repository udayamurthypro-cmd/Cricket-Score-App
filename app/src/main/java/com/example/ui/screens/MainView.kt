package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.mvi.CricketIntent
import com.example.ui.mvi.CricketViewModel
import com.example.ui.mvi.NavigationTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    viewModel: CricketViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.messageToast) {
        state.messageToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.processIntent(CricketIntent.ClearToast)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = "sports cricket icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "CRICKET SCORER",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* Settings action */ },
                        modifier = Modifier.testTag("settings_top_bar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "settings icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = state.currentTab == NavigationTab.SCORECARD,
                    onClick = { viewModel.processIntent(CricketIntent.ChangeTab(NavigationTab.SCORECARD)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Scoreboard,
                            contentDescription = "Scorecard tab"
                        )
                    },
                    label = { Text("Scorecard", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_scorecard_tab")
                )

                NavigationBarItem(
                    selected = state.currentTab == NavigationTab.ANALYSIS,
                    onClick = { viewModel.processIntent(CricketIntent.ChangeTab(NavigationTab.ANALYSIS)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analysis tab"
                        )
                    },
                    label = { Text("Analysis", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_analysis_tab")
                )

                NavigationBarItem(
                    selected = state.currentTab == NavigationTab.HISTORY,
                    onClick = { viewModel.processIntent(CricketIntent.ChangeTab(NavigationTab.HISTORY)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History tab"
                        )
                    },
                    label = { Text("History", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_history_tab")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = state.currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "Main tab contents"
            ) { targetTab ->
                when (targetTab) {
                    NavigationTab.SCORECARD -> {
                        if (state.activeMatchId != null) {
                            ScoringScreen(
                                state = state,
                                onIntent = { viewModel.processIntent(it) }
                            )
                        } else {
                            SetupScreen(
                                state = state,
                                onIntent = { viewModel.processIntent(it) }
                            )
                        }
                    }
                    NavigationTab.ANALYSIS -> {
                        AnalysisScreen(state = state)
                    }
                    NavigationTab.HISTORY -> {
                        HistoryScreen(
                            state = state,
                            onIntent = { viewModel.processIntent(it) }
                        )
                    }
                }
            }
        }
    }
}
