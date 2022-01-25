package com.example.testrealm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

//建立自定義 Adapter
//需要繼承 RecyclerView.Adapter，並且此類別會有一個泛型參數，此參數需要是一個繼承 RecyclerView.ViewHolder 的類別
//我們通常會把這個繼承 RecyclerView.ViewHolder 的類別作爲自定義 Adapter 的內部類別
class MyAdapter(private val context: Context, private val dataList: ArrayList<Map<String, Any>>): Adapter<MyAdapter.ViewHolder>() {

    //以 context 建立 LayoutInflater
    private val myLayoutInflater: LayoutInflater = LayoutInflater.from(context)

    //建立 ViewHolder 內部類別，必須繼承 RecyclerView.ViewHolder
    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val itemImg = itemView.findViewById<ImageView>(R.id.img)
        val itemText = itemView.findViewById<TextView>(R.id.tv)

        init {
            //項目點擊監聽器
            itemView.setOnClickListener {
                Toast.makeText(context, "${itemText.text}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //建立ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = myLayoutInflater.inflate(R.layout.item_layout, null)
        return ViewHolder(view)
    }

    //將資料連接到 ViewHolder，主要的畫面呈現邏輯寫在這
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(dataList[position]["star"] as Boolean){
            holder.itemImg.setImageResource(R.drawable.ic_baseline_star_24)
        }else{
            holder.itemImg.setImageResource(R.drawable.ic_baseline_star_outline_24)
        }

        holder.itemText.text = dataList[position]["memo"] as String
    }

    override fun getItemCount(): Int {
        //回傳資料數
        return dataList.size
    }
}