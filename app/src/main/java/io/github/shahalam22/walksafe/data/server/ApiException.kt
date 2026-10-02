package io.github.shahalam22.walksafe.data.server

/**
 * A failed call to the WalkSafe server. [status] is 0 when the server could not
 * be reached; [detail] is the server's reason (e.g. "models_loading").
 */
class ApiException(val status: Int, val detail: String) : Exception(detail) {

    /** Plain-language reason, spoken to the user or shown to the admin. */
    val userMessage: String
        get() = when {
            detail == NO_ADDRESS -> "No server address is saved. The admin sets it under Server."
            status == 0 && detail == TIMEOUT -> "The server took too long to answer."
            status == 0 -> "The server could not be reached."
            detail == "not_walksafe" -> "That address is not a WalkSafe server."
            detail == "models_loading" -> "The server is still loading. Try again in a minute."
            detail == "account_disabled" -> "This account has been turned off by the admin."
            status == 401 -> "The sign-in has expired. Sign in again."
            status == 403 -> "This account is not allowed to do that."
            else -> detail.ifBlank { "Something went wrong." }
        }

    companion object {
        const val NO_ADDRESS = "no_address"
        const val TIMEOUT = "timeout"
        const val NETWORK = "network"
    }
}

/** Adds https:// when missing and drops trailing slashes. */
fun cleanServerUrl(url: String): String {
    var u = url.trim().trimEnd('/')
    if (u.isNotEmpty() && !u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
    return u
}
