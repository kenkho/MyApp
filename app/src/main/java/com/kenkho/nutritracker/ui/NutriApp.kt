package com.kenkho.nutritracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val DIARY = "diary"
private const val HISTORY = "history"
private const val EDIT = "edit?${EditMealViewModel.ARG_ID}={${EditMealViewModel.ARG_ID}}" +
    "&${EditMealViewModel.ARG_DATE}={${EditMealViewModel.ARG_DATE}}"
private const val SCAN = "scan"

@Composable
fun NutriApp() {
    val nav = rememberNavController()
    // Activity-scoped so the history screen can change the diary's selected day.
    val diaryVm: DiaryViewModel = viewModel()

    NavHost(navController = nav, startDestination = DIARY) {
        composable(DIARY) {
            DiaryScreen(
                vm = diaryVm,
                onAdd = { date -> nav.navigate("edit?${EditMealViewModel.ARG_DATE}=${date.toEpochDay()}") },
                onOpenMeal = { id -> nav.navigate("edit?${EditMealViewModel.ARG_ID}=$id") },
                onOpenHistory = { nav.navigate(HISTORY) },
            )
        }
        composable(HISTORY) {
            HistoryScreen(
                vm = diaryVm,
                onBack = { nav.popBackStack() },
                onOpenDay = { date ->
                    diaryVm.selectDate(date)
                    nav.popBackStack()
                },
            )
        }
        composable(
            EDIT,
            arguments = listOf(
                navArgument(EditMealViewModel.ARG_ID) {
                    type = NavType.LongType
                    defaultValue = EditMealViewModel.NO_ID
                },
                navArgument(EditMealViewModel.ARG_DATE) {
                    type = NavType.LongType
                    defaultValue = EditMealViewModel.NO_DATE
                },
            ),
        ) {
            val vm: EditMealViewModel = viewModel()
            EditMealScreen(
                vm = vm,
                onClose = { nav.popBackStack() },
                onScan = { nav.navigate(SCAN) },
            )
        }
        composable(SCAN) { backStackEntry ->
            // Scan results go straight into the form that opened the scanner.
            val editEntry = remember(backStackEntry) { nav.getBackStackEntry(EDIT) }
            val editVm: EditMealViewModel = viewModel(viewModelStoreOwner = editEntry)
            ScanScreen(
                onBack = { nav.popBackStack() },
                onResult = { label ->
                    editVm.applyScan(label)
                    nav.popBackStack()
                },
            )
        }
    }
}
