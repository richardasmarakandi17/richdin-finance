package com.richdin.finance

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.richdin.finance.core.security.PinSecurityManager
import com.richdin.finance.core.security.SessionLockManager
import com.richdin.finance.core.ui.theme.RichdinFinanceTheme
import com.richdin.finance.core.worker.WorkScheduler
import com.richdin.finance.feature.auth.AuthLockScreen
import com.richdin.finance.feature.dashboard.DashboardScreen
import com.richdin.finance.feature.expense.QuickExpenseScreen
import com.richdin.finance.feature.income.IncomeAllocationScreen
import com.richdin.finance.feature.loan.EmergencyLoanScreen
import com.richdin.finance.feature.pockets.PocketsScreen
import com.richdin.finance.feature.report.ReportScreen
import com.richdin.finance.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object QuickExpense : Screen("quick_expense")
    data object IncomeAllocation : Screen("income_allocation")
    data object EmergencyLoan : Screen("emergency_loan")
    data object PocketsAndGoals : Screen("pockets_and_goals")
    data object Report : Screen("report")
    data object Settings : Screen("settings")
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var sessionLockManager: SessionLockManager

    @Inject
    lateinit var pinSecurityManager: PinSecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lifecycle observer for background auto-lock and debounced/background backup
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                sessionLockManager.onAppMovedToForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                sessionLockManager.onAppMovedToBackground()
                WorkScheduler.scheduleBackgroundBackup(this@MainActivity)
            }
        })

        setContent {
            RichdinFinanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isUnlocked by sessionLockManager.isUnlocked.collectAsState()

                    if (!isUnlocked) {
                        AuthLockScreen(
                            onUnlocked = {
                                sessionLockManager.unlock()
                            }
                        )
                    } else {
                        MainNavigationGraph()
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToIncome = { navController.navigate(Screen.IncomeAllocation.route) },
                onNavigateToQuickExpense = { navController.navigate(Screen.QuickExpense.route) },
                onNavigateToLoan = { navController.navigate(Screen.EmergencyLoan.route) },
                onNavigateToPockets = { navController.navigate(Screen.PocketsAndGoals.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.QuickExpense.route) {
            QuickExpenseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.IncomeAllocation.route) {
            IncomeAllocationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EmergencyLoan.route) {
            EmergencyLoanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PocketsAndGoals.route) {
            PocketsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Report.route) {
            ReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
