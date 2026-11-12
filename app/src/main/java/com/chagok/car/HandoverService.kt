package com.chagok.car

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.chagok.car.ui.ChagokViewModel

/**
 * 화면에 차곡차곡이 떠 있지 않아도(내비나 음악 화면이어도) 주차와 탑승을 알아채도록 앱을 살려 둔다.
 * 이때는 화면 없이 음성으로만 묻고 답을 받는다.
 */
class HandoverService : Service() {

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "주차·탑승 감지", NotificationManager.IMPORTANCE_LOW),
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("차곡차곡")
            .setContentText("주차하면 차가 먼저 물어봐요")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
        ChagokViewModel.get(application)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "handover"

        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, HandoverService::class.java)) }
                .onFailure { Log.w("ChagokService", "감지 서비스를 시작하지 못했어요: ${it.message}") }
        }
    }
}

/** 차 전원이 들어와 시스템이 켜지면 감지를 시작한다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) HandoverService.start(context)
    }
}
