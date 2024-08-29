package com.example.securekeep.intruderdetection

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.securekeep.R

class ImageCollectionAdapter(arrayList: ArrayList<String>?) :
    RecyclerView.Adapter<ImageCollectionAdapter.CollectionsHolder>() {
    private var arrayList: ArrayList<String> = ArrayList()
    private var context: Context? = null

    init {
        this.arrayList.addAll(arrayList!!)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): CollectionsHolder {
        context = viewGroup.context
        return CollectionsHolder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.intruder_selfie_item, viewGroup, false)
        )
    }

    override fun onBindViewHolder(
        collectionsHolder: CollectionsHolder,
        @SuppressLint("RecyclerView") i: Int
    ) {
        /*Glide.with(context!!).load(arrayList[i]).into(collectionsHolder.imageView)
        collectionsHolder.imageView.setOnClickListener {
            context!!.startActivity(
                Intent(context, FullPictureActivity::class.java).putExtra(
                    "single_photo_path",
                    arrayList[i]
                )
            )
        }*/
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    inner class CollectionsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById<ImageView>(R.id.imageView)
    }
}