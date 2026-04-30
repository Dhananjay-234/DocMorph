package com.docmorph.app.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docmorph.app.presentation.ui.editor.EditorScreen
import com.docmorph.app.presentation.ui.home.HomeScreen
import com.docmorph.app.presentation.ui.viewer.ViewerScreen

// ─── Route constants ─────────────────────────────────────────────────────────

sealed class Screen(val route: String) {

    /** Home screen — file picker + recent files list */
    object Home : Screen("home")

    /**
     * Viewer screen — full PDF view, search, zoom.
     * [encodedUri] is the URL-encoded content URI.
     */
    object Viewer : Screen("viewer/{encodedUri}") {
        fun createRoute(uri: Uri) =
            "viewer/${Uri.encode(uri.toString())}"
    }

    /**
     * Editor screen — overlay annotation canvas.
     * [encodedUri] same encoding; [startPage] is the page the user was on.
     */
    object Editor : Screen("editor/{encodedUri}/{startPage}") {
        fun createRoute(uri: Uri, startPage: Int = 0) =
            "editor/${Uri.encode(uri.toString())}/$startPage"
    }
}

// ─── Nav Graph ───────────────────────────────────────────────────────────────

@Composable
fun DocMorphNavGraph(
    navController: NavHostController = rememberNavController(),
    externalPdfUri: Uri? = null
) {
    // If the app was opened from an external VIEW intent, jump straight to viewer
    val startDestination = if (externalPdfUri != null)
        Screen.Viewer.createRoute(externalPdfUri)
    else
        Screen.Home.route

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Home ──────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenPdf = { uri ->
                    navController.navigate(Screen.Viewer.createRoute(uri))
                }
            )
        }

        // ── Viewer ────────────────────────────────────────────────────────
        composable(
            route = Screen.Viewer.route,
            arguments = listOf(
                navArgument("encodedUri") { type = NavType.StringType }
            )
        ) { backStack ->
            val encodedUri = backStack.arguments?.getString("encodedUri") ?: return@composable
            val uri        = Uri.parse(Uri.decode(encodedUri))

            ViewerScreen(
                pdfUri      = uri,
                onNavigateUp = { navController.popBackStack() },
                onEnterEdit  = { currentPage ->
                    navController.navigate(Screen.Editor.createRoute(uri, currentPage))
                }
            )
        }

        // ── Editor ────────────────────────────────────────────────────────
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("encodedUri") { type = NavType.StringType },
                navArgument("startPage")  { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStack ->
            val encodedUri = backStack.arguments?.getString("encodedUri") ?: return@composable
            val startPage  = backStack.arguments?.getInt("startPage") ?: 0
            val uri        = Uri.parse(Uri.decode(encodedUri))

            EditorScreen(
                pdfUri      = uri,
                startPage   = startPage,
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
