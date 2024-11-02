package ma.ensa.projet

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import ma.ensa.projet.adapter.MedicationAdapter
import ma.ensa.projet.classes.Medicament
import ma.ensa.projet.services.MedicamentService
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var editNom: EditText
    private lateinit var editDosage: EditText
    private lateinit var editFrequence: EditText
    private lateinit var editHeurePris: EditText
    private lateinit var buttonAdd: Button
    private val baseUrl = "http://10.0.2.2/ControleMobile/controller/create.php"
    private val medications = mutableListOf<Medicament>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialiser les champs
        editNom = findViewById(R.id.editNom)
        editDosage = findViewById(R.id.editDosage)
        editFrequence = findViewById(R.id.editFrequence)
        editHeurePris = findViewById(R.id.editHeurePris)
        buttonAdd = findViewById(R.id.buttonAdd)

        loadMedications()

        buttonAdd.setOnClickListener {
            val nom = editNom.text.toString()
            val dosage = editDosage.text.toString()
            val frequence = editFrequence.text.toString()
            val heurePris = editHeurePris.text.toString()

            val newMedicament = Medicament(nom, dosage, frequence, heurePris)

            if (MedicamentService.getInstance().create(newMedicament)) {
                Toast.makeText(this, "Médicament ajouté avec succès!", Toast.LENGTH_SHORT).show()
                sendToServer(newMedicament)
                loadMedications()
                // Envoyer notification d'ajout
                sendNotification("add", newMedicament.nom)
            } else {
                Toast.makeText(this, "Erreur d'ajout!", Toast.LENGTH_SHORT).show()
            }
        }

        val buttonViewMeds: Button = findViewById(R.id.buttonViewMeds)
        buttonViewMeds.setOnClickListener {
            val intent = Intent(this, MedicationListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun sendToServer(medicament: Medicament) {
        val stringRequest = object : StringRequest(
            Request.Method.POST, baseUrl,
            Response.Listener { response ->
                Log.d("MainActivity", "Réponse du serveur: $response")
                if (response.contains("success")) {
                    Toast.makeText(this, "Médicament ajouté sur le serveur!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erreur: $response", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e("MainActivity", "Erreur de connexion: ${error.message}", error)
                Toast.makeText(this, "Erreur de connexion: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "nom" to medicament.nom,
                    "dosage" to medicament.dosage,
                    "frequence" to medicament.frequence,
                    "heure_pris" to medicament.heurePris
                )
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=UTF-8"
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun loadMedications() {
        fetchMedications()
    }

    private fun fetchMedications() {
        val url = "http://10.0.2.2/ControleMobile/controller/getMedicament.php"

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONArray ->
                medications.clear()
                for (i in 0 until response.length()) {
                    val jsonObject: JSONObject = response.getJSONObject(i)
                    val medicament = Medicament(
                        nom = jsonObject.getString("nom"),
                        dosage = jsonObject.getString("dosage"),
                        frequence = jsonObject.getString("frequence"),
                        heurePris = jsonObject.getString("heure_pris")
                    )
                    medications.add(medicament)
                }
                scheduleMedicationReminders()
            },
            { error ->
                Toast.makeText(this, "Erreur de connexion: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

    private fun scheduleMedicationReminders() {
        medications.forEach { medicament ->
            if (medicament.heurePris.isNotEmpty()) {
                medicament.heurePris.split(",").forEach { time ->
                    val formattedTime = time.trim()
                    val parts = formattedTime.split(":")
                    if (parts.size == 3) {
                        try {
                            val hour = parts[0].toInt()
                            val minute = parts[1].toInt()

                            if (hour in 0..23 && minute in 0..59) {
                                scheduleMedicationReminder(hour, minute, medicament.nom)
                            }
                        } catch (e: NumberFormatException) {
                            Log.e("MainActivity", "Erreur de format pour l'heure: ${e.message}")
                        }
                    } else {
                        Log.e("MainActivity", "Format d'heure invalide pour ${medicament.nom}: $time")
                    }
                }
            }
        }
    }

    private fun scheduleMedicationReminder(hour: Int, minute: Int, medicamentName: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MedicationReminderReceiver::class.java).apply {
            putExtra("medicament_name", medicamentName)
            putExtra("notification_type", "reminder") // Type de notification
        }
        val pendingIntent = PendingIntent.getBroadcast(this, medicamentName.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("MainActivity", "Rappel programmé pour $medicamentName à ${calendar.time}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur lors de la programmation du rappel pour $medicamentName: ${e.message}", e)
        }
    }

    private fun sendNotification(type: String, medicamentName: String) {
        val intent = Intent(this, MedicationReminderReceiver::class.java).apply {
            putExtra("medicament_name", medicamentName)
            putExtra("notification_type", "add") // Type de notification
        }
        val pendingIntent = PendingIntent.getBroadcast(this, medicamentName.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
        pendingIntent.send() // Envoie l'intention pour afficher la notification
    }
}