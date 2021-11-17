package com.motrack.sdk

import android.net.Uri

class MockActivityHandler(val testLogger: MockLogger) : IActivityHandler {
    private val prefix = "ActivityHandler "
    private lateinit var config: MotrackConfig
    private var lastResponseData: ResponseData? = null

    override fun init(config: MotrackConfig) {
        testLogger.test(prefix + "init")
        this.config = config
    }

    override fun onResume() {
        testLogger.test(prefix + "onResume")
    }

    override fun onPause() {
        testLogger.test(prefix + "onPause")
    }

    override fun trackEvent(event: MotrackEvent) {
        testLogger.test(prefix + "trackEvent, " + event)
    }

    override fun finishedTrackingActivity(responseData: ResponseData) {
        testLogger.test(prefix + "finishedTrackingActivity, " + responseData)
        lastResponseData = responseData
    }

    override fun setEnabled(enabled: Boolean) {
        testLogger.test(prefix + "setEnabled, " + enabled)
    }

    override fun isEnabled(): Boolean {
        testLogger.test(prefix + "isEnabled")
        return false
    }

    override fun readOpenUrl(url: Uri, clickTime: Long) {
        testLogger.test(prefix + "readOpenUrl, " + url + ". ClickTime, " + clickTime)
    }

    override fun updateAttributionI(attribution: MotrackAttribution?): Boolean {
        testLogger.test(prefix + "updateAttributionI, " + attribution)
        return false
    }

    override fun launchEventResponseTasks(eventResponseData: EventResponseData) {
        testLogger.test(prefix + "launchEventResponseTasks, " + eventResponseData)
        lastResponseData = eventResponseData
    }

    override fun launchSessionResponseTasks(sessionResponseData: SessionResponseData) {
        testLogger.test(prefix + "launchSessionResponseTasks, " + sessionResponseData)
        lastResponseData = sessionResponseData
    }

    override fun launchSdkClickResponseTasks(sdkClickResponseData: SdkClickResponseData) {
        testLogger.test(prefix + "launchSdkClickResponseTasks, " + sdkClickResponseData)
        lastResponseData = sdkClickResponseData
    }

    override fun launchAttributionResponseTasks(attributionResponseData: AttributionResponseData) {
        testLogger.test(prefix + "launchAttributionResponseTasks, " + attributionResponseData)
        lastResponseData = attributionResponseData
    }

    fun sendReferrer(referrer: String, clickTime: Long) {
        testLogger.test(prefix + "sendReferrer, " + referrer + ". ClickTime, " + clickTime)
    }

    override fun setOfflineMode(enabled: Boolean) {
        testLogger.test(prefix + "setOfflineMode, " + enabled)
    }

    override fun setAskingAttribution(askingAttribution: Boolean) {
        testLogger.test(prefix + "setAskingAttribution, " + askingAttribution)
    }

    override fun sendFirstPackages() {
        testLogger.test(prefix + "sendFirstPackages")
    }

    override fun addSessionCallbackParameter(key: String?, value: String?) {
        testLogger.test(prefix + "addSessionCallbackParameter key, " + key + ", value, " + value)
    }

    override fun addSessionPartnerParameter(key: String?, value: String?) {
        testLogger.test(prefix + "addSessionPartnerParameter key, " + key + ", value, " + value)
    }

    override fun removeSessionCallbackParameter(key: String?) {
        testLogger.test(prefix + "removeSessionCallbackParameter, " + key)
    }

    override fun removeSessionPartnerParameter(key: String?) {
        testLogger.test(prefix + "removeSessionPartnerParameter, " + key)
    }

    override fun resetSessionCallbackParameters() {
        testLogger.test(prefix + "resetSessionCallbackParameters")
    }

    override fun resetSessionPartnerParameters() {
        testLogger.test(prefix + "resetSessionPartnerParameters")
    }

    override fun teardown() {
        testLogger.test(prefix + "teardown deleteState, ") // deleteState
    }

    override fun setPushToken(token: String, preSaved: Boolean) {
        testLogger.test(prefix + "setPushToken token, " + token)
    }

}