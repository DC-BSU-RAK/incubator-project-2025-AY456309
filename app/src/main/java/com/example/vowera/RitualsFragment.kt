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

class RitualsFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: RitualAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rituals, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.ritualsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RitualAdapter()
        recyclerView.adapter = adapter

        db = FirebaseFirestore.getInstance()
        val eventName = arguments?.getString("eventName") ?: ""
        val uid = arguments?.getString("uid") ?: ""
        if (eventName.isNotEmpty() && uid.isNotEmpty()) {
            loadRituals(uid, eventName)
        }

        return view
    }

    private fun loadRituals(uid: String, eventName: String) {
        val sanitizedName = sanitizeEventName(eventName)
        db.collection("users").document(uid)
            .collection("events").document(sanitizedName)
            .collection("rituals").get()
            .addOnSuccessListener { querySnapshot ->
                val rituals = mutableListOf<Ritual>()
                for (doc in querySnapshot.documents) {
                    val ritual = doc.toObject(Ritual::class.java)
                    if (ritual != null) {
                        rituals.add(ritual)
                    }
                }
                adapter.submitList(rituals)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load rituals: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
