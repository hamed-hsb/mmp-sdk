package com.motrack.testapp

import android.app.Application

open class ApplicationLoader : Application() {
    open fun getBaseUrl() = "https://www.potterapi.com"
}