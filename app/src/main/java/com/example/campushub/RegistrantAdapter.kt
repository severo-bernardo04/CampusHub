package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.domain.RegistrationStatus
import com.example.campushub.domain.RegistrationSummary
import com.example.campushub.model.Registration

class RegistrantAdapter(
    private val registrations: List<Registration>,
    private val waitlistOrder: List<String>
) : RecyclerView.Adapter<RegistrantAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.regName)
        val email: TextView = view.findViewById(R.id.regEmail)
        val status: TextView = view.findViewById(R.id.regStatus)
        val checkIn: TextView = view.findViewById(R.id.regCheckIn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_registrant, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val registration = registrations[position]
        val context = holder.itemView.context

        holder.name.text = registration.userName
        holder.email.text = registration.userEmail

        if (registration.status == RegistrationStatus.CONFIRMED) {
            holder.status.text = RegistrationStatus.label(registration.status)
            holder.status.setBackgroundResource(R.drawable.bg_badge_public)
            holder.status.setTextColor(context.getColor(R.color.success))

            holder.checkIn.visibility = View.VISIBLE
            if (registration.checkedIn) {
                holder.checkIn.text = "✔ Presença em ${registration.checkedInAt.toDateTimeString()}"
                holder.checkIn.setTextColor(context.getColor(R.color.success))
            } else {
                holder.checkIn.text = "Aguardando check-in"
                holder.checkIn.setTextColor(context.getColor(R.color.text_secondary))
            }
        } else {
            val place = RegistrationSummary.positionOf(registration.userId, waitlistOrder)
            holder.status.text = if (place != null) "Fila: ${place}º" else "Lista de espera"
            holder.status.setBackgroundResource(R.drawable.bg_badge_closed)
            holder.status.setTextColor(context.getColor(R.color.warning))
            holder.checkIn.visibility = View.GONE
        }
    }

    override fun getItemCount() = registrations.size
}