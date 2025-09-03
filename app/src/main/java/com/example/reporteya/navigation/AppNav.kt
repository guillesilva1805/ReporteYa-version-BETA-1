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
import com.example.reporteya.ui.screens.CheckEmailView
import com.example.reporteya.ui.screens.RegistrationEmpleadoView
import com.example.reporteya.ui.screens.RecuperarContrasenaView
import com.example.reporteya.services.AuthService
import com.example.reporteya.services.SecureStorage

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
        val refresh = SecureStorage.getRefresh(ctx)
        val jwt = SecureStorage.getJwt(ctx)
        // Intentar refresh si existe
        if (!refresh.isNullOrBlank()) {
            val res = AuthService.refresh(ctx, refresh)
            res.getOrNull()?.let { new ->
                SecureStorage.setSession(ctx, SecureStorage.getDni(ctx) ?: "", new.accessToken, new.refreshToken)
                return@run AppRoute.EmployeeFlow.route // navegación fina se decidirá tras login/rol
            }
        }
        if (!jwt.isNullOrBlank()) AppRoute.EmployeeFlow.route else AppRoute.Login.route
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
            RegistrationEmpleadoView(onRegisteredPendingId = { _ ->
                navController.navigate("checkEmail")
            })
        }
        composable("recuperarContrasena") { RecuperarContrasenaView(onSuccessBackToLogin = { navController.navigate(AppRoute.Login.route) { popUpTo(AppRoute.Login.route) { inclusive = true } } }) }
        composable("checkEmail") { CheckEmailView(onBackToLogin = { navController.navigate(AppRoute.Login.route) { popUpTo(AppRoute.Login.route) { inclusive = true } } }) }
    }
}


