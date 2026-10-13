package no.dte2503.volunteer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import no.dte2503.volunteer.ui.screens.LoginScreen
import no.dte2503.volunteer.ui.theme.VolunteerHubTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LoginScreenDeviceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun existingAccountSignInAndDemoRemainAvailable() {
        var loginRequest: Pair<String, String>? = null
        var demoRequested = false
        ui.setContent {
            VolunteerHubTheme {
                LoginScreen(AppUiState(hasBackend = true), { email, password -> loginRequest = email to password }, { demoRequested = true })
            }
        }
        ui.onNodeWithTag("sign_in_submit").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithTag("sign_in_email").performScrollTo().performTextInput("member@example.org")
        ui.onNodeWithTag("sign_in_password").performScrollTo().performTextInput("test-password")
        ui.onNodeWithTag("sign_in_submit").performScrollTo().assertIsEnabled().performClick()
        assertEquals("member@example.org" to "test-password", loginRequest)
        ui.onNodeWithTag("explore_demo").performScrollTo().performClick()
        assertTrue(demoRequested)
    }
}
