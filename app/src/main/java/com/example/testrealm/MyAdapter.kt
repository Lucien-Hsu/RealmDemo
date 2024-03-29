package com.example.testrealm

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

//建立自定義 Adapter
//需要繼承 RecyclerView.Adapter，並且此類別會有一個泛型參數，此參數需要是一個繼承 RecyclerView.ViewHolder 的類別
//我們通常會把這個繼承 RecyclerView.ViewHolder 的類別作爲自定義 Adapter 的內部類別
class MyAdapter(private val context: Context, private val dataList: ArrayList<MyData>): Adapter<MyAdapter.ViewHolder>() {

    private val myViewModel: MyViewModel = ViewModelProvider(context as ViewModelStoreOwner).get(MyViewModel::class.java)

    var clickPosition = -1
    var itemDBId: Long = -1

    //以 context 建立 LayoutInflater
    private val myLayoutInflater: LayoutInflater = LayoutInflater.from(context)

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
    //注意，在 onBindViewHolder 中取得的項目索引是 ViewHolder 的遞增索引，不是 recyclerView 的當前索引
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemTvId.text = (dataList[position].id as Long?).toString()

        var isImportant = dataList[position].star

        if(isImportant){
            holder.itemImgStar.setImageResource(R.drawable.ic_baseline_star_24)
        }else{
            holder.itemImgStar.setImageResource(R.drawable.ic_baseline_star_outline_24)
        }
        holder.itemTvContent.text = dataList[position].memo
    }

    //定義 ViewHolder 內部類別，必須繼承 RecyclerView.ViewHolder
    //監聽器寫在此，因爲只能在 ViewHolder 中取得當前項目索引
    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val itemTvId: TextView = itemView.findViewById(R.id.tv_id)
        val itemImgStar: ImageView = itemView.findViewById(R.id.img)
        val itemTvContent: TextView = itemView.findViewById(R.id.tv_content)

        init{
            //項目點擊監聽器
            itemView.setOnClickListener {
                Toast.makeText(context, "[Click] itemId: ${itemTvId.text} + memo: ${itemTvContent.text}", Toast.LENGTH_SHORT).show()
                //記錄點擊項目的索引值
                clickPosition = adapterPosition
                //記錄點擊的項目之資料庫索引
                itemDBId = itemTvId.text.toString().toLong()

            }

            /**
             * 改變星星標記
             */
            itemImgStar.setOnClickListener {
                //修改畫面星星
                val itemStar = changeStar()

                //記錄點擊的項目之資料庫索引
                itemDBId = itemTvId.text.toString().toLong()
                //修改資料庫內容
                myViewModel.editDBDataStar(itemDBId, itemStar)
            }
        }

        private fun changeStar(): Boolean {
            //記錄點擊項目的索引值
            clickPosition = adapterPosition

            val itemStar = !dataList[adapterPosition].star
            dataList[adapterPosition].star = itemStar
            if (itemStar) {
                itemImgStar.setImageResource(R.drawable.ic_baseline_star_24)
            } else {
                itemImgStar.setImageResource(R.drawable.ic_baseline_star_outline_24)
            }

            notifyItemChanged(adapterPosition)
            return itemStar
        }
    }



    /**
     * 新增一個項目
     */
    fun addItem(data: MyData){
        //新增資料
        dataList.add(data)
        notifyItemInserted(itemCount)
    }

    /**
     * 編輯一個項目
     */
    fun editItem(newStr: String){
        //若有選擇一個項目
        if(clickPosition >= 0){
            Log.d("TAG", "修改資料庫資料")
            //找出點擊位置的項目，將其資料內容更改
            dataList.forEach{ data ->
                if(data.id == itemDBId ){
                    data.memo = newStr
                    Log.d("TAG", "memo Id: ${data.id} 已修改爲： ${data.memo}")
                }
            }

            //重置點擊位置
            itemDBId = -1
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
    }
}