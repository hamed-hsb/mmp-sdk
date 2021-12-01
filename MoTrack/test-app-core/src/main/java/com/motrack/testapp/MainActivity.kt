package com.motrack.testapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.motrack.test.TestLibrary

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var testLibrary: TestLibrary
        private const val baseIp = "127.0.0.1"
        var baseUrl = "http://$baseIp:8080"
        const val gdprUrl = "https://$baseIp:8443"
        const val controlUrl = "ws://$baseIp:1987"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        baseUrl = (application as ApplicationLoader).getBaseUrl()
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
        findViewById<Button>(R.id.textview).setOnClickListener{
            startTestSession()
        }
    }

    private fun startTestSession() {
        // testLibrary.addTestDirectory("current/gdpr");
        testLibrary.startTestSession(Motrack.getSdkVersion())
    }
}