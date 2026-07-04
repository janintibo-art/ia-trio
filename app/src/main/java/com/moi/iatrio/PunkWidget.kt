package com.moi.iatrio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * WIDGET 🤘 : le punk sur ton écran d'accueil, avec une pensée qui se
 * renouvelle toutes les ~30 minutes (tirée du cerveau code du profil actif).
 * Toucher le widget ouvre l'app.
 */
class PunkWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val thought = makeThought(context)
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_punk)
            views.setTextViewText(R.id.widget_punk_text, thought)
            val pi = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_punk_img, pi)
            views.setOnClickPendingIntent(R.id.widget_punk_text, pi)
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun makeThought(context: Context): String {
            return try {
                val profiles = ProfileManager(context.filesDir)
                val code = CodeBrain(profiles.dir(profiles.currentName()))
                if (code.size() > 0) {
                    val starters = listOf("Hé, ", "Tu sais quoi, ", "Je pense que ", "La vie c'est ")
                    code.complete(starters.random(), 60, 85).take(110)
                } else fallback()
            } catch (e: Exception) { fallback() }
        }

        private fun fallback(): String = listOf(
            "Apprends-moi des trucs et je te raconterai ma vie ! \uD83E\uDD18",
            "Punk un jour, punk toujours.",
            "J'attends que tu m'entraînes, moi...",
            "Ouvre l'app, on va faire des étincelles !"
        ).random()
    }
}
