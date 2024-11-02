// MedicationAdapter.kt
package ma.ensa.projet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ma.ensa.projet.R
import ma.ensa.projet.classes.Medicament

class MedicationAdapter(
    private var medications: MutableList<Medicament>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>(), Filterable {

    // Liste pour garder la liste complète originale
    private var medicationsFull: List<Medicament> = ArrayList(medications)

    // Liste filtrée pour l'affichage
    private var medicationsFiltered: MutableList<Medicament> = ArrayList(medications)

    fun updateList(newList: List<Medicament>) {
        medications = ArrayList(newList)
        medicationsFull = ArrayList(newList)
        medicationsFiltered = ArrayList(newList)
        notifyDataSetChanged()
    }

    class MedicationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMedName: TextView = view.findViewById(R.id.textMedName)
        val textMedTime: TextView = view.findViewById(R.id.textMedTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication, parent, false)
        return MedicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medicament = medicationsFiltered[position]
        holder.apply {
            textMedName.text = medicament.nom
            textMedTime.text = medicament.heurePris

            holder.itemView.setOnLongClickListener {
                onDelete(medicationsFiltered.indexOf(medicament))
                true
            }
        }
    }

    override fun getItemCount(): Int = medicationsFiltered.size

    fun removeAt(position: Int) {
        val medicament = medicationsFiltered[position]
        medicationsFiltered.removeAt(position)
        medications.remove(medicament)
        medicationsFull = ArrayList(medications)
        notifyItemRemoved(position)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchString = constraint?.toString() ?: ""

                val filteredList = if (searchString.isEmpty()) {
                    medicationsFull
                } else {
                    medicationsFull.filter { medicament ->
                        medicament.nom.lowercase().contains(searchString.lowercase()) ||
                                medicament.heurePris.lowercase().contains(searchString.lowercase())
                    }
                }

                return FilterResults().apply {
                    values = filteredList
                    count = filteredList.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                medicationsFiltered = (results.values as? List<Medicament>)?.toMutableList()
                    ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }
}
