package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable

interface AuthenticationHandler {
    fun login()
}

@Composable
expect fun rememberAuthenticationHandler(): AuthenticationHandler

