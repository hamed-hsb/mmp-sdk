package com.motrack.sdk.network.parser

import org.json.JSONObject
import java.io.UnsupportedEncodingException

class JsonObjectEncoder constructor(val parameters: Map<String, String>, val sendingParameters: Map<String, String>? = null){

    /**
     * The Class convert HashMap to Json
     */


    /**
     * This Func Convert Map to JsonObject
     */
    @Throws(UnsupportedEncodingException::class)
    fun generatePOSTBodyJsonString(): String? {

        if (parameters.isEmpty()) {
            return null
        }

        val json = encodingJson(JSONObject(parameters).toString())


        return json
    }


    /**
     * This Func Convert Map to JsonObject
     */
    @Throws(UnsupportedEncodingException::class)
    private fun encodingJson(
        input: String
    ): String? = input.replace("\\","").replace("\"{","{").replace("}\"","}")


}