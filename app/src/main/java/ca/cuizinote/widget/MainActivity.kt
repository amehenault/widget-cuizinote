package ca.cuizinote.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Écran de configuration : coller la clé créée dans Cuizinote (Paramètres, Widget Android). */
class MainActivity : Activity() {

    private val formatCle = Regex("^czw_[A-Za-z0-9_-]{40,60}$")
    private val minuterie = Handler(Looper.getMainLooper())
    private lateinit var statut: TextView

    // Relit l'état toutes les 2 secondes pendant que l'écran est ouvert, pour afficher le vrai résultat.
    private val actualiser = object : Runnable {
        override fun run() {
            afficherStatut()
            minuterie.postDelayed(this, 2000)
        }
    }

    override fun onCreate(etatSauvegarde: Bundle?) {
        super.onCreate(etatSauvegarde)
        setContentView(R.layout.activity_main)

        val champCle = findViewById<EditText>(R.id.champ_cle)
        statut = findViewById(R.id.statut)

        findViewById<Button>(R.id.bouton_enregistrer).setOnClickListener {
            val cle = champCle.text.toString().trim().removePrefix("Bearer ").trim()
            if (!formatCle.matches(cle)) {
                statut.text = getString(R.string.cle_invalide)
                return@setOnClickListener
            }
            Reglages.enregistrerCle(this, cle)
            champCle.setText("")
            Rafraichissement.planifier(this)
            Rafraichissement.maintenant(this)
            statut.text = getString(R.string.cle_enregistree)
        }

        // Samsung met les applications en veille : la page de l'application permet de choisir « Sans restriction ».
        findViewById<Button>(R.id.bouton_batterie).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }

        findViewById<Button>(R.id.bouton_ouvrir).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Reglages.ADRESSE)))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Reglages.cle(this).isNullOrBlank()) Rafraichissement.maintenant(this)
        minuterie.post(actualiser)
    }

    override fun onPause() {
        super.onPause()
        minuterie.removeCallbacks(actualiser)
    }

    private fun afficherStatut() {
        if (Reglages.cle(this).isNullOrBlank()) {
            statut.text = getString(R.string.aucune_cle)
            return
        }
        val etat = Reglages.etat(this)
        if (etat.erreur != null) {
            statut.text = etat.erreur
            return
        }
        if (etat.misAJour == 0L) {
            statut.text = getString(R.string.cle_enregistree)
            return
        }
        val nombres = NumberFormat.getIntegerInstance(Locale.CANADA_FRENCH)
        val heure = SimpleDateFormat("HH 'h' mm", Locale.CANADA_FRENCH).format(Date(etat.misAJour))
        val total = if (etat.objectif > 0) {
            "${nombres.format(etat.kcal)} / ${nombres.format(etat.objectif)} Cal"
        } else {
            "${nombres.format(etat.kcal)} Cal"
        }
        statut.text = getString(R.string.statut_ok, total, heure)
    }
}
