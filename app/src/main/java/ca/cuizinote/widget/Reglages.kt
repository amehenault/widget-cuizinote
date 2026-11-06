package ca.cuizinote.widget

import android.content.Context

/** Clé du widget et dernier total reçu, gardés dans les préférences privées de l'application. */
object Reglages {
    const val ADRESSE = "https://cuizinote.vercel.app"
    private const val FICHIER = "cuizinote_widget"

    private fun prefs(contexte: Context) = contexte.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)

    fun cle(contexte: Context): String? = prefs(contexte).getString("cle", null)

    fun enregistrerCle(contexte: Context, cle: String) {
        prefs(contexte).edit().putString("cle", cle).remove("erreur").remove("mis_a_jour").apply()
    }

    fun enregistrerTotal(contexte: Context, jour: String, kcal: Int, objectif: Int?) {
        prefs(contexte).edit()
            .putString("jour", jour)
            .putInt("kcal", kcal)
            .putInt("objectif", objectif ?: 0)
            .putLong("mis_a_jour", System.currentTimeMillis())
            .remove("erreur")
            .apply()
    }

    fun enregistrerErreur(contexte: Context, message: String) {
        prefs(contexte).edit().putString("erreur", message).apply()
    }

    data class Etat(val jour: String?, val kcal: Int, val objectif: Int, val erreur: String?, val misAJour: Long)

    fun etat(contexte: Context): Etat {
        val p = prefs(contexte)
        return Etat(
            p.getString("jour", null), p.getInt("kcal", 0), p.getInt("objectif", 0),
            p.getString("erreur", null), p.getLong("mis_a_jour", 0L)
        )
    }
}
