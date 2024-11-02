package ma.ensa.projet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var textView: TextView // TextView pour afficher la phrase
    private lateinit var words: Array<String> // Tableau pour stocker les mots de la phrase
    private var index = 0 // Pour suivre l'index du mot actuel
    private val handler = Handler(Looper.getMainLooper()) // Pour gérer le timing de l'animation

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logo = findViewById(R.id.logo)
        textView = findViewById(R.id.textView) // Assurez-vous d'avoir ce TextView dans votre layout

        // Appliquer l'animation de disparition sur le logo
        logo.animate()
            .alpha(0f) // Faire disparaître l'image
            .setDuration(4000) // Durée de l'animation
            .withEndAction {
                // Passer à l'activité principale après la disparition
                startActivity(Intent(this, HomeActivity::class.java))
                finish() // Terminer l'activité de splash
            }

        // Récupérer la phrase et démarrer l'animation de texte
        val phrase = textView.text.toString() // Récupérer la phrase
        words = phrase.split(" ").toTypedArray() // Diviser la phrase en mots
        animateText() // Démarrer l'animation de texte
    }

    private fun animateText() {
        // Réinitialiser le texte à vide au début
        val displayedText = StringBuilder()

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (index < words.size) {
                    // Ajouter un espace si ce n'est pas le premier mot
                    if (displayedText.isNotEmpty()) {
                        displayedText.append(" ")
                    }

                    // Ajouter le mot actuel
                    displayedText.append(words[index])

                    // Mettre à jour le TextView avec le texte accumulé
                    textView.text = displayedText.toString()

                    index++ // Passer au mot suivant
                    handler.postDelayed(this, 400) // Répéter après 400 ms
                }
            }
        }, 400) // Commencer après 400 ms
    }
}