package ca.cuizinote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** Widget d'écran d'accueil : anneau des calories mangées aujourd'hui sur l'objectif. Toucher = mettre à jour. */
class CaloriesWidget : AppWidgetProvider() {

    override fun onUpdate(contexte: Context, gestionnaire: AppWidgetManager, ids: IntArray) {
        dessiner(contexte, gestionnaire, ids, enCours = false)
        Rafraichissement.planifier(contexte)
        Rafraichissement.maintenant(contexte)
    }

    override fun onReceive(contexte: Context, intention: Intent) {
        super.onReceive(contexte, intention)
        if (intention.action == ACTION_ACTUALISER) {
            val gestionnaire = AppWidgetManager.getInstance(contexte)
            val ids = gestionnaire.getAppWidgetIds(ComponentName(contexte, CaloriesWidget::class.java))
            dessiner(contexte, gestionnaire, ids, enCours = true)
            Rafraichissement.planifier(contexte)
            // Lecture immédiate, sans attendre la file de tâches d'Android (souvent retardée par Samsung).
            val enAttente = goAsync()
            Thread {
                try {
                    Rafraichissement.recuperer(contexte.applicationContext)
                } finally {
                    enAttente.finish()
                }
            }.start()
        }
    }

    override fun onEnabled(contexte: Context) {
        Rafraichissement.planifier(contexte)
    }

    override fun onDisabled(contexte: Context) {
        Rafraichissement.arreter(contexte)
    }

    companion object {
        const val ACTION_ACTUALISER = "ca.cuizinote.widget.ACTUALISER"

        // Couleurs de la palette Cuizinote
        private val COULEUR_PISTE = Color.parseColor("#6E3E70")
        private val COULEUR_PROGRES = Color.parseColor("#FDC79C")
        private val COULEUR_DEPASSE = Color.parseColor("#F2735C")

        fun mettreAJourTous(contexte: Context) {
            val gestionnaire = AppWidgetManager.getInstance(contexte)
            val ids = gestionnaire.getAppWidgetIds(ComponentName(contexte, CaloriesWidget::class.java))
            if (ids.isNotEmpty()) dessiner(contexte, gestionnaire, ids, enCours = false)
        }

        private fun dessiner(contexte: Context, gestionnaire: AppWidgetManager, ids: IntArray, enCours: Boolean) {
            if (ids.isEmpty()) return
            val etat = Reglages.etat(contexte)
            val sansCle = Reglages.cle(contexte).isNullOrBlank()
            val aujourdhui = LocalDate.now().toString()
            // Si le dernier total date d'hier, on repart de zéro en attendant la mise à jour.
            val kcal = if (etat.jour == aujourdhui) etat.kcal else 0
            val objectif = etat.objectif
            val format = NumberFormat.getIntegerInstance(Locale.CANADA_FRENCH)

            val vues = RemoteViews(contexte.packageName, R.layout.widget_calories)
            vues.setTextViewText(R.id.kcal, format.format(kcal))
            vues.setTextViewText(
                R.id.objectif,
                when {
                    sansCle -> "Ouvrir pour configurer"
                    etat.erreur != null -> etat.erreur
                    objectif > 0 -> "/${format.format(objectif)} Cal"
                    else -> "Cal mangées"
                }
            )
            vues.setTextViewText(
                R.id.heure,
                when {
                    sansCle -> ""
                    enCours -> "Mise à jour…"
                    etat.misAJour > 0 -> SimpleDateFormat("HH 'h' mm", Locale.CANADA_FRENCH).format(Date(etat.misAJour))
                    else -> ""
                }
            )
            vues.setImageViewBitmap(R.id.anneau, anneau(kcal, objectif))

            // Toucher : mise à jour immédiate ; sans clé, ouvre la configuration.
            val attente = if (sansCle) {
                PendingIntent.getActivity(
                    contexte, 0, Intent(contexte, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    contexte, 1,
                    Intent(contexte, CaloriesWidget::class.java).setAction(ACTION_ACTUALISER),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            vues.setOnClickPendingIntent(R.id.racine, attente)

            ids.forEach { gestionnaire.updateAppWidget(it, vues) }
        }

        private fun anneau(kcal: Int, objectif: Int): Bitmap {
            val taille = 360
            val epaisseur = 30f
            val image = Bitmap.createBitmap(taille, taille, Bitmap.Config.ARGB_8888)
            val toile = Canvas(image)
            val zone = RectF(epaisseur, epaisseur, taille - epaisseur, taille - epaisseur)
            val pinceau = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = epaisseur
                strokeCap = Paint.Cap.ROUND
            }
            pinceau.color = COULEUR_PISTE
            toile.drawArc(zone, 0f, 360f, false, pinceau)

            if (objectif > 0 && kcal > 0) {
                val fraction = (kcal.toFloat() / objectif).coerceAtMost(1f)
                pinceau.color = if (kcal > objectif) COULEUR_DEPASSE else COULEUR_PROGRES
                toile.drawArc(zone, -90f, 360f * fraction, false, pinceau)
            }
            return image
        }
    }
}
