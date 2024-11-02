package ma.ensa.projet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import ma.ensa.projet.adapter.MedicationAdapter
import ma.ensa.projet.classes.Medicament
import org.json.JSONArray
import org.json.JSONObject

class MedicationListActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var medicationAdapter: MedicationAdapter
    private val medications = mutableListOf<Medicament>()
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_list)

        initializeViews()
        setupRecyclerView()
        setupSearchView()
        fetchMedications()

        button.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun initializeViews() {
        button = findViewById(R.id.buttonAddMedication)
        recyclerView = findViewById(R.id.recyclerViewMeds)
        searchView = findViewById(R.id.search_button)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        medicationAdapter = MedicationAdapter(medications) { position ->
            confirmDelete(position)
        }
        recyclerView.adapter = medicationAdapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                medicationAdapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                medicationAdapter.filter.filter(newText)
                return true
            }
        })
    }

    private fun fetchMedications() {
        val url = "http://10.0.2.2/ControleMobile/controller/getMedicament.php"

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response -> parseMedications(response) },
            { error ->
                Toast.makeText(
                    this, "Erreur de connexion: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

    private fun parseMedications(response: JSONArray) {
        medications.clear()
        for (i in 0 until response.length()) {
            val jsonObject: JSONObject = response.getJSONObject(i)
            val medicament = Medicament(
                nom = jsonObject.getString("nom"),
                dosage = jsonObject.getString("dosage"),
                frequence = jsonObject.getString("frequence"),
                heurePris = jsonObject.getString("heure_pris")
            ).apply {
                id = jsonObject.getInt("id")
            }
            medications.add(medicament)
        }
        medicationAdapter.updateList(medications)
    }

    private fun sendDeleteRequest(medicament: Medicament) {
        val url = "http://10.0.2.2/ControleMobile/controller/delete.php"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            { response ->
                if (response.contains("success")) {
                    // Envoyer une intention au BroadcastReceiver pour afficher la notification
                    sendNotificationToReceiver(medicament)
                    Toast.makeText(this, "Médicament supprimé avec succès!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        this,
                        "Erreur lors de la suppression: $response",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            { error ->
                Toast.makeText(this, "Erreur de connexion: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        ) {
            override fun getParams(): Map<String, String> =
                mapOf("id" to medicament.id.toString())

            override fun getBodyContentType(): String =
                "application/x-www-form-urlencoded; charset=UTF-8"
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun confirmDelete(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirmation de suppression")
            .setMessage("Voulez-vous vraiment supprimer ce médicament ?")
            .setPositiveButton("Oui") { _, _ ->
                val medicamentToDelete = medications[position]
                sendDeleteRequest(medicamentToDelete)
                medicationAdapter.removeAt(position)
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun sendNotificationToReceiver(medicament: Medicament) {
        val intent = Intent(this, MedicationReminderReceiver::class.java).apply {
            putExtra("medicament_name", medicament.nom)
            putExtra("notification_type", "delete")
        }
        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            Toast.makeText(this, "Erreur lors de l'envoi de la notification", Toast.LENGTH_SHORT)
                .show()
        }
    }
}