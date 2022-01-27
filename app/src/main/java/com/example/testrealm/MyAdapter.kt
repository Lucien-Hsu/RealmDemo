package com.example.testrealm

import android.content.Context
import android.util.Log
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
class MyAdapter(private val context: Context, private val dataList: ArrayList<MutableMap<String, Any>>): Adapter<MyAdapter.ViewHolder>() {

    var clickPosition = -1
    var itemDBId: Long = -1

    //以 context 建立 LayoutInflater
    val myLayoutInflater: LayoutInflater = LayoutInflater.from(context)

    //回傳資料筆數
    override fun getItemCount(): Int {
        return dataList.size
    }

    //建立ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //充氣
        val view = myLayoutInflater.inflate(R.layout.item_layout, null)
        return ViewHolder(view)
    }

    //將資料連接到 ViewHolder，主要的畫面呈現邏輯寫在這
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemTvId.text = (dataList[position]["id"] as Long?).toString()

        if(dataList[position]["star"] as Boolean){
            holder.itemImgStar.setImageResource(R.drawable.ic_baseline_star_24)
        }else{
            holder.itemImgStar.setImageResource(R.drawable.ic_baseline_star_outline_24)
        }
        holder.itemTvContent.text = dataList[position]["memo"] as String
    }

    //定義 ViewHolder 內部類別，必須繼承 RecyclerView.ViewHolder
    //點擊監聽寫在這
    //只能在 ViewHolder 中取得當前項目索引
    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val itemTvId = itemView.findViewById<TextView>(R.id.tv_id)
        val itemImgStar = itemView.findViewById<ImageView>(R.id.img)
        val itemTvContent = itemView.findViewById<TextView>(R.id.tv_content)

        init {
            //項目點擊監聽器
            itemView.setOnClickListener {
                Toast.makeText(context, "[Click] itemId: ${itemTvId.text} + memo: ${itemTvContent.text}", Toast.LENGTH_SHORT).show()
                //記錄點擊項目的索引值
                clickPosition = adapterPosition
                //記錄點擊的項目之資料庫索引
                itemDBId = itemTvId.text.toString().toLong()
            }
        }
    }

    /**
     * 新增一個項目
     */
    fun addItem(id: Long, status: String = MemoStatus.Normal.name, memoContent: String){
        val star = (status == MemoStatus.Important.name)
        //新增資料
        dataList.add(mutableMapOf(
            Pair("id", id),
            Pair("star", star),
            Pair("memo", memoContent)
        ))
        //更新畫面上插入的項目
        notifyItemInserted(itemCount)
    }

    /**
     * 編輯一個項目
     */
    fun editItem(newStr: String){
        //若有選擇一個項目
        if(clickPosition >= 0){
            //找出點擊位置的項目，將其資料內容更改
            dataList.forEach{ map ->
                if(map["id"] == itemDBId ){
                    map["memo"] = newStr
                    Log.d("TAG", "memo Id: ${map["id"]} 已修改爲： ${map["memo"]}")
                }
            }

            //更新畫面上修改的項目
            notifyItemChanged(clickPosition)
            //重置點擊位置
            clickPosition = -1
        }
    }

    /**
     * 刪除一個項目
     */
    fun deleteItem(){
        //若有選擇一個項目
        if(clickPosition >= 0){
            //找出點擊位置的項目，將其資料內容刪除
            dataList.removeAt(clickPosition)
            //更新畫面上移除的項目
            notifyItemRemoved(clickPosition)
            //重置點擊位置
            clickPosition = -1
        }
    }

    /**
     * 刪除全部項目
     */
    fun deleteAll(){
        //清除資料
        dataList.clear()
        //更新全畫面
        notifyDataSetChanged()
    }
}