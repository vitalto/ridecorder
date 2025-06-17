package ru.ridecorder.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class TrackingServiceBinder() {

    var trackingService: TrackingService? = null
        private set

    var isBound: Boolean = false
        private set

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TrackingService.LocalBinder
            trackingService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            trackingService = null
        }
    }

    /**
     * Связывает сервис. Если сервис не запущен, он будет создан автоматически.
     */
    fun bind(context: Context) {
        val intent = Intent(context.applicationContext, TrackingService::class.java)
        context.applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Отвязывает сервис, если он был привязан.
     */
    fun unbind(context: Context) {
        if (isBound) {
            context.applicationContext.unbindService(serviceConnection)
            isBound = false
            trackingService = null
        }
    }

    fun isRunning() : Boolean {
        if(!isBound || trackingService == null) return false
        return trackingService?.isRunning() ?: false
    }
}
