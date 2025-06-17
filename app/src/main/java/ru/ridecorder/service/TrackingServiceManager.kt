package ru.ridecorder.service

import android.content.Context

object TrackingServiceManager {

    private var binder: TrackingServiceBinder? = null

    // Флаг, привязан ли сервис. Либо можно проверять binder?.isBound.
    val isBound: Boolean
        get() = binder?.isBound == true

    fun bind(context: Context) {
        if (binder == null) {
            // Храним только applicationContext
            binder = TrackingServiceBinder()
        }
        binder?.bind(context)
    }

    fun unbind(context: Context) {
        binder?.unbind(context)
        binder = null
    }

    fun stopService(context: Context) {
        if (binder?.isRunning() == false) return
        unbind(context)  // сначала отвязываем
        TrackingService.stop(context)
    }
    fun startService(context: Context, workoutId: Long) {
        if (binder?.isRunning() == true) return
        TrackingService.start(context, workoutId)
        bind(context)
    }
    fun resumeService(context: Context, workoutId: Long) {
        if (binder?.isRunning() == true) return
        TrackingService.resume(context, workoutId)
        bind(context)
    }
    fun isRunning(): Boolean {
        return binder?.isRunning() ?: false
    }
}
