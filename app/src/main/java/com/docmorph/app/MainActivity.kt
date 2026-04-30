package com.docmorph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.docmorph.app.presentation.navigation.DocMorphNavGraph
import com.docmorph.app.presentation.theme.DocMorphTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity that hosts the entire Compose navigation graph.
 * All routing/back-stack management is handled by NavGraph.kt.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the splash screen while the app initialises
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DocMorphTheme {
                DocMorphNavGraph(
                    // Pass any PDF URI received via an external VIEW intent
                    externalPdfUri = intent?.data
                )
            }
        }
    }
}
