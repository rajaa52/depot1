package ma.ensa.projet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Récupérer les données de l'intention
        val medicamentName = intent.getStringExtra("medicament_name") ?: "Médicament"
        val notificationType = intent.getStringExtra("notification_type") ?: "default"

        // Appeler la méthode de notification
        showNotification(context, medicamentName, notificationType)
    }

    private fun showNotification(context: Context, medicamentName: String, type: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_channel"

        // Créer un canal de notification pour Android O et supérieur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rappels de Médicament",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Intent pour ouvrir l'activité lors du clic sur la notification
        val intent = Intent(context, MedicationListActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Construire la notification avec des variations selon le type
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.soleil)
            .setContentTitle("Notification de Médicament")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Personnaliser selon le type de notification
        when (type) {
            "reminder" -> {
                notificationBuilder.setContentText("Il est temps de prendre votre médicament: $medicamentName")
            }
            "delete" -> {
                notificationBuilder.setContentText("Le médicament $medicamentName a été supprimé.")
            }
            "add" -> {
                notificationBuilder.setContentText("Le médicament $medicamentName a été ajouté avec succès.")
            }
            // Vous pouvez ajouter d'autres types si nécessaire
            else -> {
                notificationBuilder.setContentText("Notification inconnue.")
            }
        }

        // Afficher la notification
        notificationManager.notify(medicamentName.hashCode(), notificationBuilder.build())
    }
}