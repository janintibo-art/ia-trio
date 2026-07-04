package com.moi.iatrio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * RAPPEL QUOTIDIEN 🔔 : l'enfant réclame de l'attention (ou le punk envoie
 * sa pensée du jour) via une notification.
 */
class DailyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel("iatrio", "IA Trio", NotificationManager.IMPORTANCE_DEFAULT)
            )
            // Message : l'enfant s'ennuie s'il y en a un, sinon la pensée du punk
            val childName = try {
                val cm = ChildManager(context.filesDir)
                cm.currentName()
            } catch (e: Exception) { null }
            val (title, text) = if (childName != null)
                Pair("$childName s'ennuie ! \uD83D\uDC76", "Viens lui parler, chaque message le fait grandir.")
            else
                Pair("Pensée du punk \uD83E\uDD18", PunkWidget.makeThought(context))

            val pi = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = android.app.Notification.Builder(context, "iatrio")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(1, notif)
        } catch (e: Exception) { }
    }
}
