package com.example.vowera

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class ShoppingListActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvShoppingList: RecyclerView
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var shoppingListAdapter: ShoppingListAdapter
    private val shoppingItems = mutableListOf<ShoppingListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        rvShoppingList = findViewById(R.id.rvShoppingList)
        fabAddItem = findViewById(R.id.fabAddItem)

        val btnMore = findViewById<ImageView>(R.id.btnMore)
        val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
        val navEvents = findViewById<LinearLayout>(R.id.navEvents)
        val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
        val navBudget = findViewById<LinearLayout>(R.id.navBudget)
        val navGuests = findViewById<LinearLayout>(R.id.navGuests)

        shoppingListAdapter = ShoppingListAdapter(
            shoppingItems,
            { item -> showEditItemDialog(item) },
            { item -> deleteItem(item) },
            { item, isChecked -> updateItemCompletion(item, isChecked) }
        )

        rvShoppingList.layoutManager = LinearLayoutManager(this)
        rvShoppingList.adapter = shoppingListAdapter

        fabAddItem.setOnClickListener {
            showAddItemDialog()
        }

        btnMore.setOnClickListener {
            showMoreMenu(it)
        }

        setupNavigation(navDashboard, navEvents, navTimeline, navBudget, navGuests)

        loadShoppingItems()
    }

    private fun setupNavigation(navDashboard: LinearLayout, navEvents: LinearLayout, navTimeline: LinearLayout, navBudget: LinearLayout, navGuests: LinearLayout) {
        navDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
        navEvents.setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
            finish()
        }
        navTimeline.setOnClickListener {
            startActivity(Intent(this, TimelineActivity::class.java))
            finish()
        }
        navBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
            finish()
        }
        navGuests.setOnClickListener {
            startActivity(Intent(this, GuestListActivity::class.java))
            finish()
        }
    }

    private fun showMoreMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "Settings")
        popup.menu.add(0, 2, 1, "Logout")

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                2 -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shopping_item, null)
        val etItemName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)

        AlertDialog.Builder(this)
            .setTitle("Add Shopping Item")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etItemName.text.toString().trim()
                val quantity = etQuantity.text.toString().trim()
                val category = etCategory.text.toString().trim()

                if (name.isNotEmpty()) {
                    addItem(name, quantity, category)
                } else {
                    Toast.makeText(this, "Item name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditItemDialog(item: ShoppingListItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shopping_item, null)
        val etItemName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)

        etItemName.setText(item.name)
        etQuantity.setText(item.quantity)
        etCategory.setText(item.category)

        AlertDialog.Builder(this)
            .setTitle("Edit Shopping Item")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = etItemName.text.toString().trim()
                val quantity = etQuantity.text.toString().trim()
                val category = etCategory.text.toString().trim()

                if (name.isNotEmpty()) {
                    updateItem(item, name, quantity, category)
                } else {
                    Toast.makeText(this, "Item name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addItem(name: String, quantity: String, category: String) {
        val userId = auth.currentUser?.uid ?: return
        val itemId = UUID.randomUUID().toString()
        val item = ShoppingListItem(itemId, name, quantity, category, false)

        db.collection("users").document(userId)
            .collection("shopping").document(itemId)
            .set(item)
            .addOnSuccessListener {
                shoppingItems.add(item)
                shoppingListAdapter.notifyItemInserted(shoppingItems.size - 1)
                Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateItem(item: ShoppingListItem, name: String, quantity: String, category: String) {
        val userId = auth.currentUser?.uid ?: return
        val updatedItem = item.copy(name = name, quantity = quantity, category = category)

        db.collection("users").document(userId)
            .collection("shopping").document(item.id)
            .set(updatedItem)
            .addOnSuccessListener {
                val index = shoppingItems.indexOf(item)
                if (index != -1) {
                    shoppingItems[index] = updatedItem
                    shoppingListAdapter.notifyItemChanged(index)
                }
                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateItemCompletion(item: ShoppingListItem, isCompleted: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val updatedItem = item.copy(isCompleted = isCompleted)

        db.collection("users").document(userId)
            .collection("shopping").document(item.id)
            .update("isCompleted", isCompleted)
            .addOnSuccessListener {
                val index = shoppingItems.indexOf(item)
                if (index != -1) {
                    shoppingItems[index] = updatedItem
                    shoppingListAdapter.notifyItemChanged(index)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteItem(item: ShoppingListItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton

                db.collection("users").document(userId)
                    .collection("shopping").document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        val index = shoppingItems.indexOf(item)
                        if (index != -1) {
                            shoppingItems.removeAt(index)
                            shoppingListAdapter.notifyItemRemoved(index)
                        }
                        Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadShoppingItems() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("shopping")
            .get()
            .addOnSuccessListener { documents ->
                shoppingItems.clear()
                for (document in documents) {
                    val item = document.toObject(ShoppingListItem::class.java)
                    shoppingItems.add(item)
                }
                shoppingListAdapter.notifyItemRangeInserted(0, shoppingItems.size)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load shopping list", Toast.LENGTH_SHORT).show()
            }
    }
}
