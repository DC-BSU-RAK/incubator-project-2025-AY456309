package com.example.vowera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class GuestsFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: GuestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_guests, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.guestsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = GuestAdapter()
        recyclerView.adapter = adapter

        db = FirebaseFirestore.getInstance()
        val eventName = arguments?.getString("eventName") ?: ""
        val uid = arguments?.getString("uid") ?: ""
        if (eventName.isNotEmpty() && uid.isNotEmpty()) {
            loadGuests(uid, eventName)
        }

        return view
    }

    private fun loadGuests(uid: String, eventName: String) {
        val sanitizedName = sanitizeEventName(eventName)
        db.collection("users").document(uid)
            .collection("events").document(sanitizedName)
            .collection("guests").get()
            .addOnSuccessListener { querySnapshot ->
                val guests = mutableListOf<Guest>()
                for (doc in querySnapshot.documents) {
                    val guest = doc.toObject(Guest::class.java)
                    if (guest != null) {
                        guests.add(guest)
                    }
                }
                adapter.submitList(guests)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load guests: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sanitizeEventName(name: String): String {
        return name.replace("/", "_")
            .replace("\\", "_")
            .replace(" ", "_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("$", "_")
            .replace("?", "_")
            .replace("*", "_")
            .replace("|", "_")
    }
}
