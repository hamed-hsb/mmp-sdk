package com.motrack.testapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.motrack.test.TestLibrary

class MainActivity : AppCompatActivity() {
    companion object{
        var testLibrary: TestLibrary? = null
        private const val baseIp = "10.0.2.2"
        const val baseUrl = "https://$baseIp:8443"
        const val gdprUrl = "https://$baseIp:8443"
        const val controlUrl = "ws://$baseIp:1987"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}