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

class ChecklistFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ChecklistAdapter
    private lateinit var uid: String
    private lateinit var eventName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checklist, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.checklistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ChecklistAdapter { item -> saveChecklistItem(item) }
        recyclerView.adapter = adapter

        db = FirebaseFirestore.getInstance()
        uid = arguments?.getString("uid") ?: ""
        eventName = arguments?.getString("eventName") ?: ""
        if (eventName.isNotEmpty() && uid.isNotEmpty()) {
            loadChecklist(uid, eventName)
        }

        return view
    }

    private fun loadChecklist(uid: String, eventName: String) {
        val sanitizedName = sanitizeEventName(eventName)
        db.collection("users").document(uid)
            .collection("events").document(sanitizedName)
            .collection("checklist").get()
            .addOnSuccessListener { querySnapshot ->
                val checklist = mutableListOf<ChecklistItem>()
                for (doc in querySnapshot.documents) {
                    val item = doc.toObject(ChecklistItem::class.java)
                    if (item != null) {
                        val itemWithId = item.copy(id = doc.id)
                        checklist.add(itemWithId)
                    }
                }
                adapter.submitList(checklist)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load checklist: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

    private fun saveChecklistItem(item: ChecklistItem) {
        val sanitizedName = sanitizeEventName(eventName)
        db.collection("users").document(uid)
            .collection("events").document(sanitizedName)
            .collection("checklist").document(item.id)
            .update("completed", item.completed)
            .addOnSuccessListener {
                // Optionally show success message
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}
