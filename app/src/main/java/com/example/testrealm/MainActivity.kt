package com.example.testrealm

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    lateinit var context:Context

    //adapter 會用來控制 RecyclerView 畫面更新，所以需全域使用較方便
    lateinit var adapter: MyAdapter

    lateinit var uiThreadRealm: Realm
    lateinit var config: RealmConfiguration
//    lateinit var memos : RealmResults<Memo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this

        //設定 realm 參數
        val realmName = "My Project"
        config = RealmConfiguration.Builder()
            .name(realmName)
//            .allowWritesOnUiThread(true)    //可跑在 UI thread
            .build()

        //初始化 RecyclerView
        initRecyclerView()

        //用參數建立 Realm 物件
        uiThreadRealm = Realm.getInstance(config)

        //添加變化監聽器
        var memos : RealmResults<Memo> = uiThreadRealm.where<Memo>().findAllAsync()
        memos.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<Memo>> { collection, changeSet ->
            Log.d("TAG", "資料有改變")

            //取得刪除範圍
            val deletions = changeSet.deletionRanges
            Log.d("TAG", "deletions：$deletions")
            Log.d("TAG", "deletions size：${deletions.size}")
            // process deletions in reverse order if maintaining parallel data structures so indices don't change as you iterate
            for (i in deletions.indices.reversed()) {
                //「刪除」可能會有多個範圍
                val range = deletions[i]
                Log.d("TAG", "Deleted range: ${range.startIndex} to ${range.startIndex + range.length - 1}")
            }

            //取得新增範圍
            val insertions = changeSet.insertionRanges
            Log.d("TAG", "insertions：$insertions")
            Log.d("TAG", "insertions size：${insertions.size}")
            for (range in insertions) {
                Log.d("TAG", "Inserted range: ${range.startIndex} to ${range.startIndex + range.length - 1}")
            }

            //取得修改範圍
            val modifications = changeSet.changeRanges
            Log.d("TAG", "modifications：$modifications")
            Log.d("TAG", "modifications size：${modifications.size}")
            for (range in modifications) {
                Log.d("TAG", "Updated range: ${range.startIndex} to ${range.startIndex + range.length - 1}")
            }
        })

        //在非主執行緒進行資料庫操作
        val threadTodo = Thread{
            //用參數建立 Realm 物件
            val backgroundThreadRealm : Realm = Realm.getInstance(config)

            //「查詢」
            // 取出所有 realm 中的 memo
            memos = backgroundThreadRealm.where<Memo>().findAll()
            Log.d("TAG", "查詢 memos: $memos")
            //查找 name 中包含 "N" 的元素
            val memosThatBeginWithN : List<Memo> = memos.where().beginsWith("memoContent", "i").findAll()
            //查找 status 爲 MemoStatus.Open.name 的元素
            val openMemos : List<Memo> = memos.where().equalTo("status", MemoStatus.Important.name).findAll()

//            //「新增」
//            val memo: Memo = Memo()
//            //遞增流水號
//            memo.id = (memos.max("id") as Long? ?: 0) + 1
//            Log.d("TAG", "memo.id: ${memo.id}")
//            memo.memoContent = "init"
//            // 所有對 realm 的修改都必須在 write block 內
//            backgroundThreadRealm.executeTransaction { transactionRealm ->
//                transactionRealm.insert(memo)
//            }

//            //「修改」
//            // 取出 memo
//            var otherMemo: Memo = memos[0]!!
//            Log.d("TAG", "修改 otherMemo 前: $otherMemo")
//            // 所有對 realm 的修改都必須在 write block 內
//            backgroundThreadRealm.executeTransaction { transactionRealm ->
//                val innerOtherMemo : Memo = transactionRealm.where<Memo>().equalTo("id", otherMemo.id).findFirst()!!
//                innerOtherMemo.status = MemoStatus.Important.name
//            }
//            Log.d("TAG", "修改 otherMemo 後（此時 otherMemo 值會自動更新）: $otherMemo")
//            Log.d("TAG", "查詢 memos（此時 memos 值會自動更新）: $memos")

//            //「刪除」
//            val yetAnotherMemo: Memo = memos.last()!!
//            Log.d("TAG", "刪除 yetAnotherMemo: $yetAnotherMemo")
//            val yetAnotherMemoId: Long = yetAnotherMemo.id
//            Log.d("TAG", "刪除的 memo 名稱 yetAnotherMemoId: $yetAnotherMemoId")
//            // 所有對 realm 的修改都必須在 write block 內
//            backgroundThreadRealm.executeTransaction { transactionRealm ->
//                val innerYetAnotherMemo : Memo = transactionRealm.where<Memo>().equalTo("id", yetAnotherMemoId).findFirst()!!
//                innerYetAnotherMemo.deleteFromRealm()
//            }

            //最後要釋放 Realm 物件
            backgroundThreadRealm.close()
        }

        threadTodo.start()
    }

    private fun initRecyclerView() {
        //取得 recyclerView
        val rc: RecyclerView = findViewById(R.id.rc)

        //建立LayoutManager
        val layoutManager = LinearLayoutManager(context)
        //設置LayoutManager
        rc.layoutManager = layoutManager

        //建立自定義適配器
        adapter = MyAdapter(context, getItemList())
        //連接適配器
        rc.adapter = adapter
    }

    //創建項目清單
    //從 realm 資料庫取得 recyclerView 所需資料
    //應使用 RealmBaseAdapter，之後有空再研究：
    //https://github.com/realm/realm-android-adapters/blob/master/adapters/src/main/java/io/realm/RealmBaseAdapter.java
    private fun getItemList(): ArrayList<MyData>{
        val itemList = arrayListOf<MyData>()
        lifecycleScope.launch(Dispatchers.IO) {
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config)

            //「查詢」
            // 取出所有 realm 中的 memo
            val memos = backgroundThreadRealm.where<Memo>().findAll()
            Log.d("TAG", "[查詢所有 memos]: $memos")

            //從資料庫取出所有資料
            memos.forEachIndexed { index, memo ->
                val data = MyData(
                    memo.id,
                    (memo.status == MemoStatus.Important.name),
                    memo.memoContent
                )
                itemList.add(data)
            }

            //最後要釋放 Realm 物件
            backgroundThreadRealm.close()
        }

        return itemList
    }

    override fun onDestroy() {
        super.onDestroy()
        //釋放 Realm 物件
        uiThreadRealm.close()
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
                            addMemo(memoStr)
                        }
                    }
                    .show()

            }
            R.id.item_edit -> {
                //修改記事
                editMemo()
            }
            R.id.item_delete -> {
                //刪除記事
                deleteMemo()
                adapter.deleteItem()
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
        val yetAnotherMemoId = adapter.itemDBId
        //若有選擇項目則做
        if (yetAnotherMemoId > 0) {
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
                        lifecycleScope.launch(Dispatchers.IO){
                            //用參數建立 Realm 物件
                            val backgroundThreadRealm: Realm = Realm.getInstance(config)
                            //修改記事
                            Log.d("TAG", "要修改的 memo Id: $yetAnotherMemoId")
                            // 所有對 realm 的修改都必須在 write block 內
                            backgroundThreadRealm.executeTransaction { transactionRealm ->
                                val innerYetAnotherMemo: Memo =
                                    transactionRealm.where<Memo>().equalTo("id", yetAnotherMemoId).findFirst()!!
                                innerYetAnotherMemo.memoContent = memoStr
                            }

                            //最後要釋放 Realm 物件
                            backgroundThreadRealm.close()
                            //重置點擊位置
                            adapter.itemDBId = -1
                        }

                        //刷新 recyclerView 畫面
                        adapter.editItem(memoStr)
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
        //刪除資料庫資料
        lifecycleScope.launch(Dispatchers.IO) {
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config)

            //「刪除」
            val yetAnotherMemoId = adapter.itemDBId
            if(yetAnotherMemoId > 0){
                Log.d("TAG", "要刪除的 memo Id: $yetAnotherMemoId")
                // 所有對 realm 的修改都必須在 write block 內
                backgroundThreadRealm.executeTransaction { transactionRealm ->
                    val innerYetAnotherMemo : Memo = transactionRealm.where<Memo>().equalTo("id", yetAnotherMemoId).findFirst()!!
                    innerYetAnotherMemo.deleteFromRealm()
                }

                //最後要釋放 Realm 物件
                backgroundThreadRealm.close()
                //重置點擊位置
                adapter.itemDBId = -1
            }else{
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "請選擇要刪除的項目", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 刪除資料庫與 recyclerView 資料
     */
    private fun deleteAllMemos() {
        //刪除資料庫資料
        lifecycleScope.launch(Dispatchers.IO) {
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config)

            //「刪除」
            // 所有對 realm 的修改都必須在 write block 內
            Log.d("TAG", "[全部刪除]")
            backgroundThreadRealm.executeTransaction { transactionRealm ->
                transactionRealm.deleteAll()
            }

            //最後要釋放 Realm 物件
            backgroundThreadRealm.close()
        }

        //刪除 recyclerView 資料
        adapter.deleteAll()
    }


    /**
     * 新增資料庫與 recyclerView 資料
     */
    private fun addMemo(memoStr: String) {
        //新增資料庫資料
        lifecycleScope.launch(Dispatchers.IO) {
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config)
            //「查詢」
            // 取出所有 realm 中的 memo
            val memos = backgroundThreadRealm.where<Memo>().findAll()

            //「新增」
            val memo = Memo()
            //遞增流水號
            memo.id = (memos.max("id") as Long? ?: 0) + 1
            memo.status = MemoStatus.Normal.name
            memo.memoContent = memoStr
            Log.d("TAG", "新增前的 memos: $memos")
            // 所有對 realm 的修改都必須在 write block 內
            backgroundThreadRealm.executeTransaction { transactionRealm ->
                transactionRealm.insert(memo)
            }
            Log.d("TAG", "[新增 memo 到資料庫] id: ${memo.id} memoContent: ${memo.memoContent}")
            Log.d("TAG", "新增後的 memos: $memos")

            //資料庫讀出來的資料只會在該執行緒存在，所以這邊如果寫在協程區塊外是抓不到資料的
            withContext(Dispatchers.Main) {
                //新增 recyclerView 資料
                adapter.addItem(memo.id, memo.status, memo.memoContent)
//                Log.d("TAG", "[新增 memo 到畫面] id: ${memo.id} memoContent: ${memo.memoContent}")
                Toast.makeText(applicationContext, "新增成功！", Toast.LENGTH_SHORT).show()
            }

            //最後要釋放 Realm 物件
            backgroundThreadRealm.close()
        }
    }
}

enum class MemoStatus(val displayName: String) {
    Important("Important"),
    Normal("Normal"),
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




