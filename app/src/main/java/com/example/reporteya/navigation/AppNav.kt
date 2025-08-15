package com.example.reporteya.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.reporteya.ui.screens.AdminPanelScreen
import com.example.reporteya.ui.reporte.ReporteFlowScreen
import com.example.reporteya.ui.screens.LoginScreen
import com.example.reporteya.ui.screens.RoleSelectionScreen
import com.example.reporteya.ui.screens.OtpEmpleadoView
import com.example.reporteya.ui.screens.RegistrationEmpleadoView
import com.example.reporteya.ui.screens.RecuperarContrasenaView

enum class AppRoute(val route: String) {
    Login("login"),
    EmployeeFlow("employeeFlow"),
    AdminPanel("adminPanel")
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val startDestination = run {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val prefs = ctx.getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
        when {
            !prefs.getString("dniEmpleado", null).isNullOrBlank() -> AppRoute.EmployeeFlow.route
            !prefs.getString("dniGerente", null).isNullOrBlank() -> AppRoute.AdminPanel.route
            else -> AppRoute.Login.route
        }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(AppRoute.Login.route) {
            LoginScreen(
                onEmpleadoSuccess = { navController.navigate(AppRoute.EmployeeFlow.route) { popUpTo(AppRoute.Login.route) { inclusive = true } } },
                onGerenteSuccess = { navController.navigate(AppRoute.AdminPanel.route) { popUpTo(AppRoute.Login.route) { inclusive = true } } },
                onNavigateRegistroEmpleado = { navController.navigate("registrationEmpleado") },
                onNavigateRecuperarContrasena = { navController.navigate("recuperarContrasena") }
            )
        }
        composable(AppRoute.EmployeeFlow.route) {
            ReporteFlowScreen(
                onFinish = { /* volveremos atrás desde la pantalla previa */ },
                onLogout = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.EmployeeFlow.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.AdminPanel.route) {
            AdminPanelScreen()
        }
        composable("registrationEmpleado") {
            RegistrationEmpleadoView(onRegisteredPendingId = { pendingId ->
                navController.navigate("otp/$pendingId")
            })
        }
        composable("recuperarContrasena") { RecuperarContrasenaView(onSuccessBackToLogin = { navController.navigate(AppRoute.Login.route) { popUpTo(AppRoute.Login.route) { inclusive = true } } }) }
        composable(
            route = "otp/{pendingId}",
            arguments = listOf(navArgument("pendingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getString("pendingId").orEmpty()
            OtpEmpleadoView(pendingId = pid, onOtpSuccessGoLogin = {
                navController.navigate(AppRoute.Login.route) { popUpTo(AppRoute.Login.route) { inclusive = true } }
            })
        }
    }
}


