package ca.cuizinote.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Va chercher les calories du jour dans Cuizinote, puis redessine les widgets. */
class Rafraichissement(contexte: Context, parametres: WorkerParameters) : CoroutineWorker(contexte, parametres) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val contexte = applicationContext
        val cle = Reglages.cle(contexte)
        if (cle.isNullOrBlank()) {
            CaloriesWidget.mettreAJourTous(contexte)
            return@withContext Result.success()
        }
        val jour = LocalDate.now().toString()
        var connexion: HttpURLConnection? = null
        try {
            connexion = (URL("${Reglages.ADRESSE}/api/widget/calories?jour=$jour").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Authorization", "Bearer $cle")
                setRequestProperty("Accept", "application/json")
            }
            when (connexion.responseCode) {
                200 -> {
                    val json = JSONObject(connexion.inputStream.bufferedReader().use { it.readText() })
                    val objectif = if (json.isNull("objectif")) null else json.optInt("objectif")
                    Reglages.enregistrerTotal(contexte, jour, json.optInt("kcal", 0), objectif)
                }
                401 -> Reglages.enregistrerErreur(contexte, "Clé refusée : créez une nouvelle clé dans Cuizinote")
                else -> Reglages.enregistrerErreur(contexte, "Cuizinote indisponible")
            }
        } catch (e: Exception) {
            // Pas de réseau : on garde le dernier total connu.
        } finally {
            connexion?.disconnect()
        }
        CaloriesWidget.mettreAJourTous(contexte)
        Result.success()
    }

    companion object {
        private val reseau = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Mise à jour automatique toutes les 30 minutes. */
        fun planifier(contexte: Context) {
            val requete = PeriodicWorkRequestBuilder<Rafraichissement>(30, TimeUnit.MINUTES).setConstraints(reseau).build()
            WorkManager.getInstance(contexte)
                .enqueueUniquePeriodicWork("calories", ExistingPeriodicWorkPolicy.KEEP, requete)
        }

        /** Mise à jour immédiate (ouverture de l'application, ajout du widget, toucher le bouton). */
        fun maintenant(contexte: Context) {
            val requete = OneTimeWorkRequestBuilder<Rafraichissement>().setConstraints(reseau).build()
            WorkManager.getInstance(contexte)
                .enqueueUniqueWork("calories-maintenant", ExistingWorkPolicy.REPLACE, requete)
        }

        fun arreter(contexte: Context) {
            WorkManager.getInstance(contexte).cancelUniqueWork("calories")
        }
    }
}
