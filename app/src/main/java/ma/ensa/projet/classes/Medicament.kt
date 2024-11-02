package ma.ensa.projet.classes

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.util.Log

class Medicament(
    var nom: String,
    var dosage: String,
    var frequence: String,
    var heurePris: String // Format "HH:mm"
) {


    // Attribution d'un ID unique
    var id: Int = ++comp
    companion object {
        private var comp: Int = 0
    }
}