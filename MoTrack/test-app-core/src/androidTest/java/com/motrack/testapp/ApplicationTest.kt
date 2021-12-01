package com.motrack.testapp

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.jakewharton.espresso.OkHttp3IdlingResource
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ApplicationTest {
    // Use [ActivityScenario] to create and launch the activity under test.
//    val scenario = launchActivity<MainActivity>()
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, false)

    private val mockWebServer = MockWebServer()

    @Before
    fun setup() {
        mockWebServer.start(8080)
        IdlingRegistry.getInstance().register(
            OkHttp3IdlingResource.create(
                "okhttp",
                OkHttpProvider.getOkHttpClient()
            )
        )
    }

    @Test
    fun whenActivityStarted_showTextView() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                println(request.toString())
                return MockResponse()
                    .setResponseCode(200)
                    .setBody("success_response.json")
//                when (request.path) {
//                    "/init_session" -> return MockResponse().setResponseCode(200)
//                        .addHeader("Test-Session-Id", "54131546541")
//                    "/v1/check/version/" -> return MockResponse().setResponseCode(200)
//                        .setBody("version=9")
//                    "/v1/profile/info" -> return MockResponse().setResponseCode(200)
//                        .setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}")
//                }
//                return MockResponse().setResponseCode(404)
            }
        }
        activityRule.launchActivity(null)
        onView(withText(R.string.hello_world)).perform(click());
        onView(withId(R.id.textview))
            .check(matches(withText(R.string.hello_world)))

    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.motrack.testapp", appContext.packageName)
    }
}