package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.MatchRepository
import com.example.ui.mvi.CricketViewModel
import com.example.ui.mvi.CricketViewModelFactory
import com.example.ui.screens.MainView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support configuration
        enableEdgeToEdge()

        // Room Database Initialization
        val database = AppDatabase.getDatabase(this)
        val repository = MatchRepository(database.matchDao())
        
        // Setup MVI ViewModel using the custom factory
        val viewModel: CricketViewModel by viewModels {
            CricketViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainView(viewModel = viewModel)
                }
            }
        }
    }
}
