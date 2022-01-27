package com.example.testrealm

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

class MainActivity : AppCompatActivity() {

    lateinit var myViewModel: MyViewModel
    lateinit var context:Context

    lateinit var rc: RecyclerView
    //adapter 會用來控制 RecyclerView 畫面更新，所以需全域使用較方便
    private lateinit var adapter: MyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this

        //創建 ViewModel
        myViewModel = ViewModelProvider(this).get(MyViewModel::class.java)

        //初始化 RecyclerView
        initRecyclerView()
    }

    private fun initRecyclerView() {
        //取得 recyclerView
        rc = findViewById(R.id.rc)

        //建立LayoutManager
        val layoutManager = LinearLayoutManager(context)
        //設置LayoutManager
        rc.layoutManager = layoutManager

        //建立自定義適配器
        adapter = MyAdapter(context, myViewModel.getItemList())
        //連接適配器
        rc.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        rc.adapter = null
    }

    //此方法會在創造 menu 時呼叫
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //這邊是把menu實體與它的view連結起來
        menuInflater.inflate(R.menu.menu_layout, menu)
        return true
    }

    //當點擊選項時會呼叫此方法，並傳入被選中的 Menuitem
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //根據選項 id 做處理
        when(item.itemId){
            R.id.item_add -> {
                //新增記事
                addMemo()
            }
            R.id.item_edit -> {
                //修改記事
                editMemo()
            }
            R.id.item_delete -> {
                //刪除記事
                deleteMemo()
            }
            R.id.item_delete_all -> {
                //刪除全部記事
                deleteAllMemos()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * 修改資料庫與 recyclerView 資料
     */
    private fun editMemo() {
        //取得當前選項的編號
        val memoId = adapter.itemDBId
        //若有選擇項目則做
        if (memoId > 0) {
            //1.先設定View並取出
            val item = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_add_layout, null)

            //2.創建對話框
            AlertDialog.Builder(this@MainActivity)
                .setTitle("請輸入修改內容")
                .setView(item)
                .setPositiveButton("確認") { _, _ ->
                    //把輸入的內容取出
                    val editText = item.findViewById<EditText>(R.id.et)
                    val memoStr = editText.text.toString()

                    if (TextUtils.isEmpty(memoStr)) {
                        //若沒輸入內容則跳出提醒
                        Toast.makeText(applicationContext, "請輸入修改內容", Toast.LENGTH_SHORT).show()
                    } else {
                        //修改資料庫資料
                        if(myViewModel.editDBData(memoId, memoStr)){
                            //刷新 recyclerView 畫面
                            adapter.editItem(memoStr)
                        }
                    }
                }
                .show()
        } else {
            //未選擇項目則跳出提醒
            Toast.makeText(applicationContext, "請選擇要修改的項目", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 刪除單筆資料庫與 recyclerView 資料
     */
    private fun deleteMemo() {
        val memoId = adapter.itemDBId

        if(myViewModel.deleteDBData(memoId)){
            //重置點擊位置
            adapter.itemDBId = -1
            //刷新畫面
            adapter.deleteItem()
        }else{
            Toast.makeText(applicationContext, "請選擇要刪除的項目", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 刪除資料庫與 recyclerView 資料
     */
    private fun deleteAllMemos() {
        //刪除資料庫資料
        myViewModel.deleteAllDBData()

        //刪除 recyclerView 資料
        adapter.deleteAll()
    }


    /**
     * 新增資料庫與 recyclerView 資料
     */
    private fun addMemo() {
        //1.先設定View並取出
        val item = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_add_layout, null)

        //2.創建對話框
        AlertDialog.Builder(this@MainActivity)
            .setTitle("請輸入記事內容")
            .setView(item)
            .setPositiveButton("確認") { _, _ ->
                //把輸入的內容取出
                val editText = item.findViewById<EditText>(R.id.et)
                val memoStr = editText.text.toString()

                if (TextUtils.isEmpty(memoStr)) {
                    Toast.makeText(applicationContext, "請輸入記事內容", Toast.LENGTH_SHORT).show()
                } else {
                    //新增記事
                    val results = myViewModel.addDBData(memoStr)
                    val result = results.first
                    val resultData = results.second
                    if(result){
                        //新增 recyclerView 資料
                        adapter.addItem(resultData)
                        Toast.makeText(applicationContext, "新增成功！", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
}

enum class MemoStatus {
    Important,
    Normal,
}

open class Memo() : RealmObject() {
    //主 key
    //注意，資料庫會以 Long 儲存，用 Int 在這邊是不行的
    @PrimaryKey
    var id: Long = 1

    //重要性
    //一般欄位
    var status: String = MemoStatus.Normal.name

    //記事內容
    //必填欄位
    @Required
    var memoContent: String = ""
}




