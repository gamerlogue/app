package it.maicol07.gamerlogue

import at.released.igdbclient.error.IgdbException
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.unwrapError
import it.maicol07.gamerlogue.ui.components.layout.AppUi

suspend fun <T> safeRequest(request: suspend () -> T) = runCatching {
    request()
}.apply {
    if (isErr) {
        val error = unwrapError()
        when (error) {
            is IgdbException -> {
                AppUi.reportNetworkException(error)
            }
        }
        error.printStackTrace()
    }
}
