package com.motrack.testapp

class TestApplicationLoader : ApplicationLoader() {

  var url = "https://127.0.0.1:8080"
 
  override fun getBaseUrl(): String {  
    return url  
  }  
}