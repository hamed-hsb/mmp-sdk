package com.motrack.sdk

import android.content.Context

/**
 * @author yaya (@yahyalmh)
 * @since 11th October 2021
 */

class MotrackSigner {
    private constructor()

    companion object {
        // https://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        private var signerInstance: Any? = null

        fun enableSigning(logger: ILogger) {
            getSignerInstance()
            if (signerInstance == null) {
                return
            }
            try {
                Reflection.invokeInstanceMethod(signerInstance!!, "enableSigning", null)
            } catch (e: Exception) {
                logger.warn("Invoking Signer enableSigning() received an error [${e.message!!}]")
            }
        }

        fun disableSigning(logger: ILogger) {
            getSignerInstance()
            if (signerInstance == null) {
                return
            }
            try {
                Reflection.invokeInstanceMethod(signerInstance!!, "disableSigning", null)
            } catch (e: Exception) {
                logger.warn("Invoking Signer disableSigning() received an error [${e.message!!}]")
            }
        }

        fun onResume(logger: ILogger) {
            getSignerInstance()
            if (signerInstance == null) {
                return
            }
            try {
                Reflection.invokeInstanceMethod(signerInstance!!, "onResume", null)
            } catch (e: Exception) {
                logger.warn("Invoking Signer onResume() received an error [${e.message!!}]")
            }
        }

        fun sign(
            parameters: HashMap<String, String>?, activityKind: String?, clientSdk: String?,
            context: Context?, logger: ILogger
        ) {
            getSignerInstance()
            if (signerInstance == null) {
                return
            }
            try {
                Reflection.invokeInstanceMethod(
                    signerInstance!!, "sign", arrayOf(
                        Context::class.java,
                        MutableMap::class.java,
                        String::class.java,
                        String::class.java
                    ),
                    context, parameters, activityKind, clientSdk
                )
            } catch (e: Exception) {
                logger.warn("Invoking Signer sign() for ${activityKind!!}received an error [${e.message!!}]")
            }
        }

        private fun getSignerInstance() {
            if (signerInstance == null) {
                synchronized(MotrackSigner::class.java) {
                    if (signerInstance == null) {
                        signerInstance =
                            Reflection.createDefaultInstance("com.motrack.sdk.sig.Signer")
                    }
                }
            }
        }
    }
}