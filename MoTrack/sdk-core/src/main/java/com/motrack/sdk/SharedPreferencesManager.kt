package com.motrack.sdk

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONException


/**
 * Class used for shared preferences manipulation.
 *
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 *
 * @author hamed (@hamed-hsb)
 * @since 09th Jul 2022
 */


class SharedPreferencesManager private constructor(context: Context) {
    /**
     * Save raw referrer string into shared preferences.
     *
     * @param rawReferrer Raw referrer string
     * @param clickTime   Click time
     */
    @Synchronized
    fun saveRawReferrer(rawReferrer: String, clickTime: Long) {
        try {
            if (getRawReferrer(rawReferrer, clickTime) != null) {
                return
            }
            val rawReferrerArray = getRawReferrerArray()

            // There are exactly REFERRERS_COUNT saved referrers, do nothing.
            if (rawReferrerArray.length() == REFERRERS_COUNT) {
                return
            }
            val newRawReferrer = JSONArray()
            newRawReferrer.put(INDEX_RAW_REFERRER, rawReferrer)
            newRawReferrer.put(INDEX_CLICK_TIME, clickTime)
            newRawReferrer.put(INDEX_IS_SENDING, 0)
            rawReferrerArray.put(newRawReferrer)
            saveRawReferrerArray(rawReferrerArray)
        } catch (e: JSONException) {
        }
    }

    /**
     * Save referrer array to shared preferences.
     *
     * @param rawReferrerArray Array of referrers to be saved
     */
    @Synchronized
    fun saveRawReferrerArray(rawReferrerArray: JSONArray) {
        try {
            saveString(PREFS_KEY_RAW_REFERRERS, rawReferrerArray.toString())
        } catch (t: Throwable) {
            remove(PREFS_KEY_RAW_REFERRERS)
        }
    }

    /**
     * Remove referrer information from shared preferences.
     *
     * @param clickTime   Click time
     * @param rawReferrer Raw referrer string
     */
    @Synchronized
    fun removeRawReferrer(rawReferrer: String?, clickTime: Long) {
        // Don't even try to remove null or empty referrers since they shouldn't exist in shared preferences.
        if (rawReferrer == null || rawReferrer.length == 0) {
            return
        }
        val rawReferrerIndex = getRawReferrerIndex(rawReferrer, clickTime)
        if (rawReferrerIndex < 0) {
            return
        }
        val rawReferrerArray = getRawReferrerArray()

        // Rebuild queue without referrer that should be removed.
        val updatedReferrers = JSONArray()
        var i = 0
        while (i < rawReferrerArray.length()) {
            if (i == rawReferrerIndex) {
                i += 1
                continue
            }
            try {
                updatedReferrers.put(rawReferrerArray.getJSONArray(i))
            } catch (e: JSONException) {
            }
            i += 1
        }

        // Save new referrer queue JSON array as string back to shared preferences.
        saveString(PREFS_KEY_RAW_REFERRERS, updatedReferrers.toString())
    }

    /**
     * Get saved referrer JSONArray object.
     *
     * @param rawReferrer Raw referrer string
     * @param clickTime   Click time
     * @return JSONArray object containing referrer information. Defaults to null if not found.
     */
    @Synchronized
    fun getRawReferrer(rawReferrer: String, clickTime: Long): JSONArray? {
        val rawReferrerIndex = getRawReferrerIndex(rawReferrer, clickTime)
        if (rawReferrerIndex >= 0) {
            try {
                return getRawReferrerArray().getJSONArray(rawReferrerIndex)
            } catch (e: JSONException) {
            }
        }
        return null
    }

    /**
     * Get array of saved referrer JSONArray objects.
     *
     * @return JSONArray of saved referrers. Defaults to empty JSONArray if none found.
     */
    @Synchronized
    fun getRawReferrerArray(): JSONArray {
        val referrerQueueString = getString(PREFS_KEY_RAW_REFERRERS)
        if (referrerQueueString != null) {
            try {
                val rawReferrerArray = JSONArray(referrerQueueString)

                // Initial move for those who have more than REFERRERS_COUNT stored already.
                // Cut the array and leave it with only REFERRERS_COUNT elements.
                if (rawReferrerArray.length() > REFERRERS_COUNT) {
                    val tempReferrerArray = JSONArray()
                    var i = 0
                    while (i < REFERRERS_COUNT) {
                        tempReferrerArray.put(rawReferrerArray[i])
                        i += 1
                    }
                    saveRawReferrerArray(tempReferrerArray)
                    return tempReferrerArray
                }
                return JSONArray(referrerQueueString)
            } catch (e: JSONException) {
            } catch (t: Throwable) {
            }
        }
        return JSONArray()
    }

    /**
     * Save preinstall referrer string into shared preferences.
     *
     * @param referrer Preinstall referrer string
     */
    @Synchronized
    fun savePreinstallReferrer(referrer: String) {
        saveString(PREFS_KEY_PREINSTALL_SYSTEM_INSTALLER_REFERRER, referrer)
    }

    /**
     * Get saved preinstall referrer string from shared preferences.
     *
     * @return referrer Preinstall referrer string
     */
    @Synchronized
    fun getPreinstallReferrer(): String? {
        return getString(PREFS_KEY_PREINSTALL_SYSTEM_INSTALLER_REFERRER)
    }

    /**
     * Remove saved preinstall referrer string from shared preferences.
     */
    @Synchronized
    fun removePreinstallReferrer() {
        remove(PREFS_KEY_PREINSTALL_SYSTEM_INSTALLER_REFERRER)
    }

    /**
     * Initially called upon ActivityHandler initialisation.
     * Used to check if any of the still existing referrers was unsuccessfully being sent before app got killed.
     * If such found - switch it's isBeingSent flag back to "false".
     */
    @Synchronized
    fun setSendingReferrersAsNotSent() {
        try {
            val rawReferrerArray = getRawReferrerArray()
            var hasRawReferrersBeenChanged = false
            for (i in 0 until rawReferrerArray.length()) {
                val rawReferrer = rawReferrerArray.getJSONArray(i)
                val sendingStatus = rawReferrer.optInt(INDEX_IS_SENDING, -1)
                if (sendingStatus == 1) {
                    rawReferrer.put(INDEX_IS_SENDING, 0)
                    hasRawReferrersBeenChanged = true
                }
            }
            if (hasRawReferrersBeenChanged) {
                saveRawReferrerArray(rawReferrerArray)
            }
        } catch (e: JSONException) {
        }
    }

    /**
     * Get index of saved raw referrer.
     *
     * @param rawReferrer Raw referrer string
     * @param clickTime   Click time
     * @return Index of saved referrer. Defaults to -1 if referrer not found.
     */
    @Synchronized
    private fun getRawReferrerIndex(rawReferrer: String, clickTime: Long): Int {
        try {
            val rawReferrers = getRawReferrerArray()
            for (i in 0 until rawReferrers.length()) {
                val savedRawReferrer = rawReferrers.getJSONArray(i)
                // Check if raw referrer is already saved.
                val savedRawReferrerString = savedRawReferrer.optString(INDEX_RAW_REFERRER, null)
                if (savedRawReferrerString == null || savedRawReferrerString != rawReferrer) {
                    continue
                }
                val savedClickTime = savedRawReferrer.optLong(INDEX_CLICK_TIME, -1)
                if (savedClickTime != clickTime) {
                    continue
                }
                // Install referrer found, skip adding it.
                return i
            }
        } catch (e: JSONException) {
        }
        return -1
    }

    /**
     * Save push token to shared preferences.
     *
     * @param pushToken Push notifications token
     */
    @Synchronized
    fun savePushToken(pushToken: String) {
        saveString(PREFS_KEY_PUSH_TOKEN, pushToken)
    }

    /**
     * Get push token from shared preferences.
     *
     * @return Push token value
     */
    @Synchronized
    fun getPushToken(): String? {
        return getString(PREFS_KEY_PUSH_TOKEN)
    }

    /**
     * Remove push token from shared preferences.
     */
    @Synchronized
    fun removePushToken() {
        remove(PREFS_KEY_PUSH_TOKEN)
    }

    /**
     * Save information that install has been tracked to shared preferences.
     */
    @Synchronized
    fun setInstallTracked() {
        saveBoolean(PREFS_KEY_INSTALL_TRACKED, true)
    }

    /**
     * Get information if install has been tracked from shared preferences. If no info, default to false.
     *
     * @return boolean indicating whether install has been tracked or not
     */
    @Synchronized
    fun getInstallTracked(): Boolean {
        return getBoolean(PREFS_KEY_INSTALL_TRACKED, false)
    }

    @Synchronized
    fun setGdprForgetMe() {
        saveBoolean(PREFS_KEY_GDPR_FORGET_ME, true)
    }

    @Synchronized
    fun getGdprForgetMe(): Boolean {
        return getBoolean(PREFS_KEY_GDPR_FORGET_ME, false)
    }

    @Synchronized
    fun removeGdprForgetMe() {
        remove(PREFS_KEY_GDPR_FORGET_ME)
    }

    @Synchronized
    fun setDisableThirdPartySharing() {
        saveBoolean(PREFS_KEY_DISABLE_THIRD_PARTY_SHARING, true)
    }

    @Synchronized
    fun getDisableThirdPartySharing(): Boolean {
        return getBoolean(PREFS_KEY_DISABLE_THIRD_PARTY_SHARING, false)
    }

    @Synchronized
    fun removeDisableThirdPartySharing() {
        remove(PREFS_KEY_DISABLE_THIRD_PARTY_SHARING)
    }

    @Synchronized
    fun saveDeeplink(deeplink: Uri?, clickTime: Long) {
        if (deeplink == null) {
            return
        }
        saveString(PREFS_KEY_DEEPLINK_URL, deeplink.toString())
        saveLong(PREFS_KEY_DEEPLINK_CLICK_TIME, clickTime)
    }

    @Synchronized
    fun getDeeplinkUrl(): String? {
        return getString(PREFS_KEY_DEEPLINK_URL)
    }

    @Synchronized
    fun getDeeplinkClickTime(): Long {
        return getLong(PREFS_KEY_DEEPLINK_CLICK_TIME, -1)
    }

    @Synchronized
    fun removeDeeplink() {
        remove(PREFS_KEY_DEEPLINK_URL)
        remove(PREFS_KEY_DEEPLINK_CLICK_TIME)
    }

    /**
     * Save information that preinstall tracker has been tracked to shared preferences.
     */
    @Synchronized
    fun setPreinstallPayloadReadStatus(status: Long) {
        saveLong(PREFS_KEY_PREINSTALL_PAYLOAD_READ_STATUS, status)
    }

    /**
     * Get information if preinstall tracker has been tracked from shared preferences. If no info, default to 0.
     *
     * @return long returning current read status of each Preinstall location.
     * Default value in binary is `00.....00000000` indicating none of the locations are yet read.
     */
    @Synchronized
    fun getPreinstallPayloadReadStatus(): Long {
        return getLong(PREFS_KEY_PREINSTALL_PAYLOAD_READ_STATUS, 0)
    }

    /**
     * Remove all key-value pairs from shared preferences.
     */
    @Synchronized
    fun clear() {
        if (sharedPreferencesEditor != null) sharedPreferencesEditor!!.clear().apply()
    }

    /**
     * Write a string value to shared preferences.
     *
     * @param key   Key to be written to shared preferences
     * @param value Value to be written to shared preferences
     */
    @Synchronized
    private fun saveString(key: String, value: String) {
        if (sharedPreferencesEditor != null) sharedPreferencesEditor!!.putString(key, value).apply()
    }

    /**
     * Write a boolean value to shared preferences.
     *
     * @param key   Key to be written to shared preferences
     * @param value Value to be written to shared preferences
     */
    @Synchronized
    private fun saveBoolean(key: String, value: Boolean) {
        if (sharedPreferencesEditor != null) sharedPreferencesEditor!!.putBoolean(key, value)
            .apply()
    }

    /**
     * Write a long value to shared preferences.
     *
     * @param key   Key to be written to shared preferences
     * @param value Value to be written to shared preferences
     */
    @Synchronized
    private fun saveLong(key: String, value: Long) {
        if (sharedPreferencesEditor != null) sharedPreferencesEditor!!.putLong(key, value).apply()
    }

    /**
     * Write a integer value to shared preferences.
     *
     * @param key   Key to be written to shared preferences
     * @param value Value to be written to shared preferences
     */
    @Synchronized
    private fun saveInteger(key: String, value: Int) {
        if (sharedPreferencesEditor != null) sharedPreferencesEditor!!.putInt(key, value).apply()
    }

    /**
     * Get a string value from shared preferences.
     *
     * @param key Key for which string value should be retrieved
     * @return String value for given key saved in shared preferences (null if not found)
     */
    @Synchronized
    private fun getString(key: String): String? {
        return if (sharedPreferences != null) {
            try {
                sharedPreferences!!.getString(key, null)
            } catch (e: ClassCastException) {
                null
            } catch (t: Throwable) {
                if (key == PREFS_KEY_RAW_REFERRERS) {
                    remove(PREFS_KEY_RAW_REFERRERS)
                }
                null
            }
        } else {
            null
        }
    }

    /**
     * Get a boolean value from shared preferences.
     *
     * @param key          Key for which boolean value should be retrieved
     * @param defaultValue Default value to be returned if nothing found in shared preferences
     * @return Boolean value for given key saved in shared preferences
     */
    @Synchronized
    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (sharedPreferences != null) {
            try {
                sharedPreferences!!.getBoolean(key, defaultValue)
            } catch (e: ClassCastException) {
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    /**
     * Get a long value from shared preferences.
     *
     * @param key          Key for which long value should be retrieved
     * @param defaultValue Default value to be returned if nothing found in shared preferences
     * @return Long value for given key saved in shared preferences
     */
    @Synchronized
    private fun getLong(key: String, defaultValue: Long): Long {
        return if (sharedPreferences != null) {
            try {
                sharedPreferences!!.getLong(key, defaultValue)
            } catch (e: ClassCastException) {
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    /**
     * Remove a value saved with given key from shared preferences.
     *
     * @param key Key to be removed
     */
    @Synchronized
    private fun remove(key: String) {
        if (sharedPreferencesEditor != null) sharedPreferencesEditor!!.remove(key).apply()
    }

    companion object {
        /**
         * Name of Adjust preferences.
         */
        private const val PREFS_NAME = "adjust_preferences"

        /**
         * Key name for referrers.
         */
        private const val PREFS_KEY_RAW_REFERRERS = "raw_referrers"

        /**
         * Key name for push token.
         */
        private const val PREFS_KEY_PUSH_TOKEN = "push_token"

        /**
         * Key name for info about whether install has been tracked or not.
         */
        private const val PREFS_KEY_INSTALL_TRACKED = "install_tracked"
        private const val PREFS_KEY_GDPR_FORGET_ME = "gdpr_forget_me"
        private const val PREFS_KEY_DISABLE_THIRD_PARTY_SHARING = "disable_third_party_sharing"
        private const val PREFS_KEY_DEEPLINK_URL = "deeplink_url"
        private const val PREFS_KEY_DEEPLINK_CLICK_TIME = "deeplink_click_time"
        private const val PREFS_KEY_PREINSTALL_PAYLOAD_READ_STATUS =
            "preinstall_payload_read_status"
        private const val PREFS_KEY_PREINSTALL_SYSTEM_INSTALLER_REFERRER =
            "preinstall_system_installer_referrer"

        /**
         * Index for raw referrer string content in saved JSONArray object.
         */
        private const val INDEX_RAW_REFERRER = 0

        /**
         * Index for click time in saved JSONArray object.
         */
        private const val INDEX_CLICK_TIME = 1

        /**
         * Index for information whether referrer is being sent in saved JSONArray object.
         */
        private const val INDEX_IS_SENDING = 2

        /**
         * Number of persisted referrers.
         */
        private const val REFERRERS_COUNT = 10

        /**
         * Shared preferences editor of the app.
         */
        private var sharedPreferences: SharedPreferences? = null
        private var sharedPreferencesEditor: SharedPreferences.Editor? = null
        private var defaultInstance: SharedPreferencesManager? = null
        fun getDefaultInstance(context: Context): SharedPreferencesManager? {
            if (defaultInstance == null) {
                defaultInstance = SharedPreferencesManager(context)
                return defaultInstance
            }
            return defaultInstance
        }
    }

    /**
     * Default constructor.
     *
     * @param context Application context
     */
    init {
        try {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferencesEditor = sharedPreferences!!.edit()
        } catch (illegalStateException: IllegalStateException) {
            sharedPreferences = null
            sharedPreferencesEditor = null
        }
    }
}