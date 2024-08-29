package com.example.securekeep.intruderdetection.IntruderAdapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.securekeep.R
import com.example.securekeep.databinding.IntruderSelfieItemBinding
import com.example.securekeep.intruderdetection.FullPictureActivity

class SelfieAdapter(val context: Context, private var selfieList: MutableList<SelfieModel>) : RecyclerView.Adapter<SelfieAdapter.CollectionsHolder>() {
    class CollectionsHolder(val binding: IntruderSelfieItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionsHolder {
        return CollectionsHolder(IntruderSelfieItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CollectionsHolder, position: Int) {
        holder.binding.dateTime.text = selfieList[position].dateTime
        Glide.with(context)
            .load(selfieList[position].imageUri)
            .apply(RequestOptions().placeholder(R.drawable.camera))  // Placeholder image
            .into(holder.binding.intruderSelfie)

        holder.binding.intruderSelfie.setOnClickListener {
            val intent = Intent(context, FullPictureActivity::class.java).apply {
                putExtra("SelfieUri", selfieList[position].imageUri.toString()) // Convert Uri to String
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return selfieList.size
    }


}