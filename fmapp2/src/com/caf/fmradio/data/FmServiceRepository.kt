/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.caf.fmradio.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.caf.fmradio.FMRadioService
import com.caf.fmradio.IFMRadioService
import com.caf.fmradio.IFMRadioServiceCallbacks

/**
 * Repository that encapsulates IPC communication with [FMRadioService].
 */
class FmServiceRepository {

    companion object {
        private const val TAG = "FmServiceRepo"
    }

    interface Listener {
        fun onServiceConnected()
        fun onServiceDisconnected()
        fun onEnabled()
        fun onDisabled()
        fun onRadioReset()
        fun onTuneStatusChanged()
        fun onProgramServiceChanged()
        fun onRadioTextChanged()
        fun onSignalStrengthChanged()
        fun onSearchComplete(scannedFrequencies: List<Int>)
        fun onMute(isMuted: Boolean)
        fun onAudioUpdate(isStereo: Boolean)
        fun onStationRDSSupported(isRdsSupported: Boolean)
        fun onRecordingStarted()
        fun onRecordingStopped()
        fun onFmAudioPathStarted()
        fun onFmAudioPathStopped()
    }

    var listener: Listener? = null
    private var service: IFMRadioService? = null
    private var isBound = false

    private val callbacks = object : IFMRadioServiceCallbacks.Stub() {
        override fun onEnabled() {
            Log.d(TAG, "onEnabled callback")
            listener?.onEnabled()
        }

        override fun onDisabled() {
            Log.d(TAG, "onDisabled callback")
            listener?.onDisabled()
        }

        override fun onRadioReset() {
            Log.d(TAG, "onRadioReset callback")
            listener?.onRadioReset()
        }

        override fun onTuneStatusChanged() {
            Log.d(TAG, "onTuneStatusChanged callback")
            listener?.onTuneStatusChanged()
        }

        override fun onProgramServiceChanged() {
            Log.d(TAG, "onProgramServiceChanged callback")
            listener?.onProgramServiceChanged()
        }

        override fun onRadioTextChanged() {
            Log.d(TAG, "onRadioTextChanged callback")
            listener?.onRadioTextChanged()
        }

        override fun onAlternateFrequencyChanged() {
            Log.d(TAG, "onAlternateFrequencyChanged callback")
        }

        override fun onSignalStrengthChanged() {
            Log.d(TAG, "onSignalStrengthChanged callback")
            listener?.onSignalStrengthChanged()
        }

        override fun onSearchComplete() {
            Log.d(TAG, "onSearchComplete callback")
            val freqs = getScannedFrequencies()
            listener?.onSearchComplete(freqs)
        }

        override fun onSearchListComplete() {
            Log.d(TAG, "onSearchListComplete callback")
        }

        override fun onMute(bMuted: Boolean) {
            Log.d(TAG, "onMute: $bMuted")
            listener?.onMute(bMuted)
        }

        override fun onAudioUpdate(bStereo: Boolean) {
            Log.d(TAG, "onAudioUpdate: isStereo=$bStereo")
            listener?.onAudioUpdate(bStereo)
        }

        override fun onStationRDSSupported(bRDSSupported: Boolean) {
            Log.d(TAG, "onStationRDSSupported: $bRDSSupported")
            listener?.onStationRDSSupported(bRDSSupported)
        }

        override fun onRecordingStopped() {
            Log.d(TAG, "onRecordingStopped")
            listener?.onRecordingStopped()
        }

        override fun onRecordingStarted() {
            Log.d(TAG, "onRecordingStarted")
            listener?.onRecordingStarted()
        }

        override fun onExtenRadioTextChanged() {}
        override fun onExtenCountryCodeChanged() {}
        override fun onSeekNextStation() {}
        override fun onA2DPConnectionstateChanged(state: Boolean) {}

        override fun onFmAudioPathStarted() {
            Log.d(TAG, "onFmAudioPathStarted")
            listener?.onFmAudioPathStarted()
        }

        override fun onFmAudioPathStopped() {
            Log.d(TAG, "onFmAudioPathStopped")
            listener?.onFmAudioPathStopped()
        }

        override fun getSigThCb(valArg: Int, status: Int) {}
        override fun getChDetThCb(valArg: Int, status: Int) {}
        override fun DefDataRdCb(valArg: Int, status: Int) {}
        override fun getBlendCb(valArg: Int, status: Int) {}
        override fun setChDetThCb(status: Int) {}
        override fun DefDataWrtCb(status: Int) {}
        override fun setBlendCb(status: Int) {}
        override fun getStationParamCb(valArg: Int, status: Int) {}
        override fun getStationDbgParamCb(valArg: Int, status: Int) {}
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            Log.d(TAG, "ServiceConnection connected")
            service = IFMRadioService.Stub.asInterface(binder)
            try {
                service?.registerCallbacks(callbacks)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to registerCallbacks", e)
            }
            listener?.onServiceConnected()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(TAG, "ServiceConnection disconnected")
            service = null
            listener?.onServiceDisconnected()
        }
    }

    fun bind(context: Context) {
        if (!isBound) {
            val intent = Intent(context, FMRadioService::class.java)
            context.startService(intent)
            isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "Binding to FMRadioService: success=$isBound")
        }
    }

    fun unbind(context: Context) {
        if (isBound) {
            try {
                service?.unregisterCallbacks()
            } catch (e: RemoteException) {
                Log.e(TAG, "Error unregistering callbacks", e)
            }
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Service not registered", e)
            }
            isBound = false
            service = null
        }
    }

    val isConnected: Boolean
        get() = service != null

    fun isFmOn(): Boolean = try {
        service?.isFmOn ?: false
    } catch (e: RemoteException) {
        false
    }

    fun fmOn(): Boolean = try {
        service?.fmOn() ?: false
    } catch (e: RemoteException) {
        false
    }

    fun fmOff(): Boolean = try {
        service?.fmOff() ?: false
    } catch (e: RemoteException) {
        false
    }

    fun tune(frequencyKHz: Int): Boolean = try {
        service?.tune(frequencyKHz) ?: false
    } catch (e: RemoteException) {
        false
    }

    fun seek(up: Boolean): Boolean = try {
        service?.seek(up) ?: false
    } catch (e: RemoteException) {
        false
    }

    fun scan(pty: Int = 0): Boolean = try {
        service?.scan(pty) ?: false
    } catch (e: RemoteException) {
        false
    }

    fun cancelSearch(): Boolean = try {
        service?.cancelSearch() ?: false
    } catch (e: RemoteException) {
        false
    }

    fun fmReconfigure(): Boolean = try {
        service?.fmReconfigure() ?: false
    } catch (e: RemoteException) {
        false
    }

    fun enableStereo(isStereo: Boolean): Boolean = try {
        service?.enableStereo(isStereo) ?: false
    } catch (e: RemoteException) {
        false
    }

    fun enableAutoAF(enable: Boolean): Boolean = try {
        service?.enableAutoAF(enable) ?: false
    } catch (e: RemoteException) {
        false
    }

    fun mute(): Boolean = try {
        service?.mute() ?: false
    } catch (e: RemoteException) {
        false
    }

    fun unMute(): Boolean = try {
        service?.unMute() ?: false
    } catch (e: RemoteException) {
        false
    }

    fun isMuted(): Boolean = try {
        service?.isMuted ?: false
    } catch (e: RemoteException) {
        false
    }

    fun enableSpeaker(speakerOn: Boolean) = try {
        service?.enableSpeaker(speakerOn)
    } catch (e: RemoteException) {
        Log.e(TAG, "enableSpeaker error", e)
    }

    fun isSpeakerEnabled(): Boolean = try {
        service?.isSpeakerEnabled ?: false
    } catch (e: RemoteException) {
        false
    }

    fun startRecording(): Boolean = try {
        service?.startRecording() ?: false
    } catch (e: RemoteException) {
        false
    }

    fun stopRecording() = try {
        service?.stopRecording()
    } catch (e: RemoteException) {
        Log.e(TAG, "stopRecording error", e)
    }

    fun isRecording(): Boolean = try {
        service?.isFmRecordingOn ?: false
    } catch (e: RemoteException) {
        false
    }

    fun isAntennaAvailable(): Boolean = try {
        service?.isAntennaAvailable ?: false
    } catch (e: RemoteException) {
        false
    }

    fun isWiredHeadsetAvailable(): Boolean = try {
        service?.isWiredHeadsetAvailable ?: false
    } catch (e: RemoteException) {
        false
    }

    fun getProgramService(): String = try {
        service?.programService ?: ""
    } catch (e: RemoteException) {
        ""
    }

    fun getRadioText(): String = try {
        service?.radioText ?: ""
    } catch (e: RemoteException) {
        ""
    }

    fun getRssi(): Int = try {
        service?.rssi ?: 0
    } catch (e: RemoteException) {
        0
    }

    @Suppress("UNCHECKED_CAST")
    fun getScannedFrequencies(): List<Int> = try {
        (service?.scannedFrequencies as? List<Int>) ?: emptyList()
    } catch (e: RemoteException) {
        emptyList()
    }
}
