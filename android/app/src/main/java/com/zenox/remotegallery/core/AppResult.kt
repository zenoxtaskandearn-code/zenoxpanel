package com.zenox.remotegallery.core

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val code: Int? = null, val cause: Throwable? = null) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()
}

inline fun <T> runCatchingResult(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    AppResult.Error(e.message ?: "Unexpected error", cause = e)
}
