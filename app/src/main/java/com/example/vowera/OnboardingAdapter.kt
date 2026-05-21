package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val items: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageOnboarding)
        val titleView: TextView = itemView.findViewById(R.id.textTitle)
        val descriptionView: TextView = itemView.findViewById(R.id.textDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]

        holder.imageView.setImageResource(item.imageResId)
        holder.titleView.text = item.title
        holder.descriptionView.text = item.description

        holder.imageView.alpha = 0f
        holder.titleView.alpha = 0f
        holder.descriptionView.alpha = 0f

        holder.imageView.translationY = 40f
        holder.titleView.translationY = 40f
        holder.descriptionView.translationY = 40f

        holder.imageView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        holder.titleView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(120)
            .start()

        holder.descriptionView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(220)
            .start()
    }

    override fun getItemCount(): Int = items.size
}