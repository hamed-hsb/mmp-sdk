package com.motrack.testapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.jakewharton.espresso.OkHttp3IdlingResource
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.util.concurrent.TimeUnit


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

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        initMockWebServer()

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                    "/init_session" -> MockResponse().setResponseCode(200)
                        .setBody(FileReader.readStringFromFile("commands.json"))
                        .setHeader("Test-Session-Id", "54131546541")
                    else -> {
                        MockResponse().setResponseCode(404)
                    }
                }
            }
        }

        mockWebServer.start(8080)
    }

    private fun initMockWebServer() {
        mockWebServer = MockWebServer()

        val localhost = InetAddress.getByName("localhost").canonicalHostName
        val localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName(localhost)
            .build()

        // Set ssl
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()
        mockWebServer.useHttps(serverCertificates.sslSocketFactory(), false)

        initOkHttpClient(localhostCertificate)
    }

    private fun initOkHttpClient(localhostCertificate: HeldCertificate) {
        val REQUEST_TIMEOUT = 3L

        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(localhostCertificate.certificate)
            .build()

        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .sslSocketFactory(
                clientCertificates.sslSocketFactory(),
                clientCertificates.trustManager
            ).build()

        IdlingRegistry.getInstance().register(
            OkHttp3IdlingResource.create(
                "okhttp",
                okHttpClient
            )
        )
    }

    @Test
    fun whenActivityStarted_showTextView() {
        activityRule.launchActivity(null)

        onView(withId(R.id.textview))
            .check(matches(withText(R.string.hello_world)))

        onView(withText(R.string.hello_world)).perform(click())

//        mockWebServer.enqueue(MockResponse().setBody("unused"))

//        onView(ViewMatchers.isRoot()).perform(TestUtil.waitFor(30000))

//        Thread.sleep(50000)
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