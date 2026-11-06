package ca.cuizinote.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

/** Écran de configuration : coller la clé créée dans Cuizinote (Paramètres, Widget Android). */
class MainActivity : Activity() {

    private val formatCle = Regex("^czw_[A-Za-z0-9_-]{40,60}$")

    override fun onCreate(etatSauvegarde: Bundle?) {
        super.onCreate(etatSauvegarde)
        setContentView(R.layout.activity_main)

        val champCle = findViewById<EditText>(R.id.champ_cle)
        val statut = findViewById<TextView>(R.id.statut)

        afficherStatut(statut)

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

        findViewById<Button>(R.id.bouton_ouvrir).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Reglages.ADRESSE)))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Reglages.cle(this).isNullOrBlank()) Rafraichissement.maintenant(this)
    }

    private fun afficherStatut(statut: TextView) {
        statut.text = if (Reglages.cle(this).isNullOrBlank()) {
            getString(R.string.aucune_cle)
        } else {
            val etat = Reglages.etat(this)
            etat.erreur ?: getString(R.string.cle_active)
        }
    }
}
