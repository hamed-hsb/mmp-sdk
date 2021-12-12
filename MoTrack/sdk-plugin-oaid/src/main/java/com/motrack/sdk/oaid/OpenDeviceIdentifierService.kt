package com.motrack.sdk.oaid

import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.RemoteException

/**
 * @author yaya (@yahyalmh)
 * @since 12th December 2021
 */

/**
 * Important: Please do not revise the order of the method in this AIDL file
 */
interface OpenDeviceIdentifierService : IInterface {
    /**
     * Local-side IPC implementation stub class.
     */
    companion object {
        public abstract class Stub() : android.os.Binder(), OpenDeviceIdentifierService {
            companion object {
                const val DESCRIPTOR = "com.uodis.opendevice.aidl.OpenDeviceIdentifierService"
                const val TRANSACTION_getOaid = IBinder.FIRST_CALL_TRANSACTION + 0
                const val TRANSACTION_isOaidTrackLimited = IBinder.FIRST_CALL_TRANSACTION + 1

                /**
                 * Cast an IBinder object into an com.uodis.opendevice.aidl.OpenDeviceIdentifierService interface,
                 * generating a proxy if needed.
                 */
                fun asInterface(obj: IBinder?): OpenDeviceIdentifierService? {
                    if (obj == null) {
                        return null
                    }
                    val iin = obj.queryLocalInterface(DESCRIPTOR)
                    return if (iin != null && iin is OpenDeviceIdentifierService) {
                        iin
                    } else Stub.Proxy(obj)
                }
            }

            init {
                this.attachInterface(this, DESCRIPTOR)
            }

            override fun asBinder(): IBinder {
                return this
            }

            @Throws(RemoteException::class)
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                return when (code) {
                    IBinder.INTERFACE_TRANSACTION -> {
                        reply?.writeString(DESCRIPTOR)
                        true
                    }
                    Stub.TRANSACTION_getOaid -> {
                        data.enforceInterface(DESCRIPTOR)
                        val _result: String = this.getOaid()
                        reply?.writeNoException()
                        reply?.writeString(_result)
                        true
                    }
                    Stub.TRANSACTION_isOaidTrackLimited -> {
                        data.enforceInterface(DESCRIPTOR)
                        val _result: Boolean = this.isOaidTrackLimited()
                        reply?.writeNoException()
                        reply?.writeInt(if (_result) 1 else 0)
                        true
                    }
                    else -> {
                        super.onTransact(code, data, reply, flags)
                    }
                }
            }

            class Proxy internal constructor(private val mRemote: IBinder) :
                OpenDeviceIdentifierService {

                override fun asBinder(): IBinder {
                    return mRemote
                }

                val interfaceDescriptor: String
                    get() = Stub.DESCRIPTOR

                override fun getOaid(): String {
                    val _data = Parcel.obtain()
                    val _reply = Parcel.obtain()
                    val _result: String?
                    _result = try {
                        _data.writeInterfaceToken(Stub.DESCRIPTOR)
                        mRemote.transact(
                            Stub.TRANSACTION_getOaid,
                            _data,
                            _reply,
                            0
                        )
                        _reply.readException()
                        _reply.readString()
                    } finally {
                        _reply.recycle()
                        _data.recycle()
                    }
                    return _result!!
                }

                /**
                 * Obtain limit ad tracking parameter, true: limit tracking; false: do not limit tracking
                 */
                override fun isOaidTrackLimited(): Boolean {
                    val _data = Parcel.obtain()
                    val _reply = Parcel.obtain()
                    val _result: Boolean = try {
                        _data.writeInterfaceToken(Stub.DESCRIPTOR)
                        mRemote.transact(
                            Stub.TRANSACTION_isOaidTrackLimited,
                            _data,
                            _reply,
                            0
                        )
                        _reply.readException()
                        0 != _reply.readInt()
                    } finally {
                        _reply.recycle()
                        _data.recycle()
                    }
                    return _result
                }
            }
        }
    }

    @Throws(RemoteException::class)
    fun getOaid(): String

    /**
     * Obtain limit ad tracking parameter, true: limit tracking; false: do not limit tracking
     */
    @Throws(RemoteException::class)
    fun isOaidTrackLimited(): Boolean

}