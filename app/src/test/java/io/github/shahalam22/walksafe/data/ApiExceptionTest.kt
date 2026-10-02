package io.github.shahalam22.walksafe.data

import io.github.shahalam22.walksafe.data.server.ApiException
import io.github.shahalam22.walksafe.data.server.cleanServerUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiExceptionTest {

    @Test
    fun messagesMatchTheWebApp() {
        assertEquals("The server could not be reached.", ApiException(0, ApiException.NETWORK).userMessage)
        assertEquals("The server took too long to answer.", ApiException(0, ApiException.TIMEOUT).userMessage)
        assertEquals(
            "No server address is saved. The admin sets it under Server.",
            ApiException(0, ApiException.NO_ADDRESS).userMessage,
        )
        assertEquals("The server is still loading. Try again in a minute.", ApiException(503, "models_loading").userMessage)
        assertEquals("This account has been turned off by the admin.", ApiException(403, "account_disabled").userMessage)
        assertEquals("The sign-in has expired. Sign in again.", ApiException(401, "login_expired").userMessage)
        assertEquals("A user with this email address has already been registered",
            ApiException(400, "A user with this email address has already been registered").userMessage)
    }

    @Test
    fun cleansAddresses() {
        assertEquals("https://abc.trycloudflare.com", cleanServerUrl(" abc.trycloudflare.com/ "))
        assertEquals("http://127.0.0.1:8000", cleanServerUrl("http://127.0.0.1:8000//"))
        assertEquals("", cleanServerUrl("  "))
    }
}
