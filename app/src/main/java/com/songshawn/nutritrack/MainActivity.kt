package com.songshawn.nutritrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.songshawn.nutritrack.navigation.NutriTrackNavHost
import com.songshawn.nutritrack.theme.NutriTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NutriTrackTheme{
                NutriTrackNavHost()
            }
        }
    }
}
