package com.example.securekeep.RCV

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.securekeep.antipocket.AntiPocketActivity
import com.example.securekeep.chargingdetect.ChargeDetectActivity
import com.example.securekeep.databinding.CatagoryItemsBinding
import com.example.securekeep.earphonedetection.EarphonesActivity
import com.example.securekeep.intruderdetection.PermissionActivity
import com.example.securekeep.overchargedetection.OverChargeActivity
import com.example.securekeep.touchdetection.TouchPhoneActivity
import com.example.securekeep.wifidetection.WifiActivity

class RvAdapter(val context: Context, var catagoryList: ArrayList<RCVModel>): Adapter<RvAdapter.MyCatViewHolder>() {
    class MyCatViewHolder(val binding: CatagoryItemsBinding):RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCatViewHolder {
        return MyCatViewHolder(CatagoryItemsBinding.inflate(LayoutInflater.from(parent.context),parent, false))
    }

    override fun getItemCount(): Int {
        return catagoryList.size
    }

    override fun onBindViewHolder(holder: MyCatViewHolder, position: Int) {
        //Set Category
        val dataList = catagoryList[position]
        holder.binding.categoryPic.setImageResource(dataList.catImage)
        holder.binding.categoryText.text = dataList.catText
        holder.binding.categoryDescrip.text = dataList.catDescrip

        holder.binding.categoryBtn.setOnClickListener {
            val intent = when (dataList.catText) {
                "Intruder Alert" -> Intent(context, PermissionActivity::class.java)
                "Don't Touch My Phone" -> Intent(context, TouchPhoneActivity::class.java)
                "Anti Pocket Detection" -> Intent(context, AntiPocketActivity::class.java)
                "Charging Detection" -> Intent(context, ChargeDetectActivity::class.java)
                "Wifi Detection" -> Intent(context, WifiActivity::class.java)
                "Avoid Over Charging" -> Intent(context, OverChargeActivity::class.java)
                "Earphones Detection" -> Intent(context, EarphonesActivity::class.java)

                else -> Intent(context, TouchPhoneActivity::class.java) // A default activity in case no match is found
            }
            ContextCompat.startActivity(context, intent, null)
        }

    }
}

