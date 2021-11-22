package com.motrack.testapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.motrack.test.TestLibrary

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var testLibrary: TestLibrary
        private const val baseIp = "10.0.2.2"
        const val baseUrl = "https://$baseIp:8443"
        const val gdprUrl = "https://$baseIp:8443"
        const val controlUrl = "ws://$baseIp:1987"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if deferred deep link was received

        // Check if deferred deep link was received
        val intent = intent
        val deeplinkData = intent.data
        if (deeplinkData != null) {
            Motrack.appWillOpenUrl(deeplinkData, applicationContext)
            return
        }

        testLibrary = TestLibrary(
            baseUrl,
            controlUrl,
            CommandListener(this.applicationContext)
        )
        // testLibrary.doNotExitAfterEnd();

        // testLibrary.doNotExitAfterEnd();
        startTestSession()
    }

    private fun startTestSession() {
        // testLibrary.addTestDirectory("current/gdpr");
        testLibrary.startTestSession(Motrack.getSdkVersion())
    }
}