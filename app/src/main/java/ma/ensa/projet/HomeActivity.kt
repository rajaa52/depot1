package ma.ensa.projet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ShareCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import ma.ensa.projet.MedicationListActivity
import ma.ensa.projet.R
import org.json.JSONArray
import org.json.JSONObject
import ma.ensa.projet.adapter.MedicationAdapter
import ma.ensa.projet.classes.Medicament
import java.time.LocalTime

class HomeActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var shareIcon: ImageView
    private lateinit var recyclerViewMorning: RecyclerView
    private lateinit var recyclerViewEvening: RecyclerView
    private lateinit var morningMedicationAdapter: MedicationAdapter
    private lateinit var eveningMedicationAdapter: MedicationAdapter
    private val morningMedications = mutableListOf<Medicament>()
    private val eveningMedications = mutableListOf<Medicament>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        button = findViewById(R.id.button)
        shareIcon = findViewById(R.id.shareIcon)
        recyclerViewMorning = findViewById(R.id.recyclerViewMorningMeds)
        recyclerViewEvening = findViewById(R.id.recyclerViewEveningMeds)

        recyclerViewMorning.layoutManager = LinearLayoutManager(this)
        recyclerViewEvening.layoutManager = LinearLayoutManager(this)

        // Initialiser les adaptateurs
        morningMedicationAdapter = MedicationAdapter(morningMedications) { id -> }
        eveningMedicationAdapter = MedicationAdapter(eveningMedications) { id -> }

        recyclerViewMorning.adapter = morningMedicationAdapter
        recyclerViewEvening.adapter = eveningMedicationAdapter

        // Charger les médicaments depuis l'API
        fetchMedications()

        // Action pour le bouton "Mes Médicaments"
        button.setOnClickListener {
            val intent = Intent(this, MedicationListActivity::class.java)
            startActivity(intent)
        }

        // Action pour l'icône de partage
        shareIcon.setOnClickListener {
            shareApp()
        }
    }

    private fun fetchMedications() {
        val url = "http://10.0.2.2/ControleMobile/controller/getMedicament.php" // Changez l'URL si nécessaire

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONArray -> parseMedications(response) },
            { error ->
                Toast.makeText(this, "Erreur de connexion: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("HomeActivity", "Erreur de connexion: ${error.message}")
            }
        )

        // Ajouter la requête à la file d'attente de Volley
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

    private fun parseMedications(response: JSONArray) {
        morningMedications.clear()
        eveningMedications.clear()

        for (i in 0 until response.length()) {
            val jsonObject: JSONObject = response.getJSONObject(i)
            val medicament = Medicament(
                nom = jsonObject.getString("nom"),
                dosage = jsonObject.getString("dosage"),
                frequence = jsonObject.getString("frequence"),
                heurePris = jsonObject.getString("heure_pris")
            )

            try {
                val heurePris = LocalTime.parse(medicament.heurePris)

                when {
                    heurePris.isBefore(LocalTime.of(14, 0)) -> {
                        morningMedications.add(medicament)
                    }
                    heurePris.isAfter(LocalTime.of(22, 0)) -> {
                        eveningMedications.add(medicament)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Erreur de parsing de l'heure: ${medicament.heurePris}")
            }
        }

        morningMedicationAdapter.updateList(morningMedications)
        eveningMedicationAdapter.updateList(eveningMedications)
        Log.d("HomeActivity", "Médicaments du matin: ${morningMedications.size}, Médicaments du soir: ${eveningMedications.size}")
    }

    private fun shareApp() {
        val shareText = "Découvrez cette application pour gérer vos médicaments!"
        ShareCompat.IntentBuilder
            .from(this)
            .setType("text/plain")
            .setChooserTitle("Partager l'application")
            .setText(shareText)
            .startChooser()
    }
}
