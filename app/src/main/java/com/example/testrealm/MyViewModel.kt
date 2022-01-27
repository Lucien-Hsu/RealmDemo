package com.example.testrealm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.where
import kotlinx.coroutines.*

class MyViewModel: ViewModel() {
    var config: RealmConfiguration

    init{
        //設定 realm 參數
        val realmName = "My Project"
        config = RealmConfiguration.Builder()
            .name(realmName)
//            .allowWritesOnUiThread(true)    //可跑在 UI thread
            .deleteRealmIfMigrationNeeded()   //在需要資料庫遷移時直接刪掉資料庫
            .build()
    }

    /**
     * 創建項目清單
     * 從 realm 資料庫取得 recyclerView 所需資料
     * 應使用 RealmBaseAdapter，之後有空再研究：
     * https://github.com/realm/realm-android-adapters/blob/master/adapters/src/main/java/io/realm/RealmBaseAdapter.java
     */
    fun getItemList(): ArrayList<MyData>{
        val itemList = arrayListOf<MyData>()
        viewModelScope.launch(Dispatchers.IO) {
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

    /**
     * 新增一個項目
     */
    fun addDBData(dataStr: String): Pair<Boolean, MyData> = runBlocking {

            val results: Deferred<Pair<Boolean, MyData>> = viewModelScope.async(Dispatchers.IO) {
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
                memo.memoContent = dataStr
                Log.d("TAG", "新增前的 memos: $memos")
                // 所有對 realm 的修改都必須在 write block 內
                backgroundThreadRealm.executeTransaction { transactionRealm ->
                    transactionRealm.insert(memo)
                }
                Log.d("TAG", "[新增 memo 到資料庫] id: ${memo.id} memoContent: ${memo.memoContent}")
                Log.d("TAG", "新增後的 memos: $memos")

                val result = true
                val myData = memoToMyData(memo)

                //最後要釋放 Realm 物件
                backgroundThreadRealm.close()

                Pair(result, myData)
            }

            results.await()
        }

    /**
     * 編輯一個項目
     */
    fun editDBData(dataId: Long, dataStr: String): Boolean = runBlocking {
        //修改資料庫資料
        val result = viewModelScope.async(Dispatchers.IO){
            var resultAsync = false
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config)

            if(dataId > 0) {
                //修改記事
                Log.d("TAG", "要修改的 Id: $dataId")
                // 所有對 realm 的修改都必須在 write block 內
                backgroundThreadRealm.executeTransaction { transactionRealm ->
                    val innerYetAnotherMemo: Memo =
                        transactionRealm.where<Memo>().equalTo("id", dataId).findFirst()!!
                    innerYetAnotherMemo.memoContent = dataStr
                }

                //最後要釋放 Realm 物件
                backgroundThreadRealm.close()

                resultAsync = true
            }

            resultAsync
        }

        result.await()
    }

    /**
     * 刪除一個項目
     */
    fun deleteDBData(dataId: Long): Boolean = runBlocking {
        //刪除資料庫資料
        val result: Deferred<Boolean> = viewModelScope.async(Dispatchers.IO) {
            var resultAsync = false
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config)

            if(dataId > 0){
                Log.d("TAG", "要刪除的 Id: $dataId")
                // 所有對 realm 的修改都必須在 write block 內
                backgroundThreadRealm.executeTransaction { transactionRealm ->
                    val innerYetAnotherMemo : Memo = transactionRealm.where<Memo>().equalTo("id", dataId).findFirst()!!
                    innerYetAnotherMemo.deleteFromRealm()
                }

                //最後要釋放 Realm 物件
                backgroundThreadRealm.close()

                resultAsync = true
            }

            resultAsync
        }

        result.await()
    }

    /**
     * 刪除全部項目
     */
    fun deleteAllDBData(){
        //刪除資料庫資料
        viewModelScope.launch(Dispatchers.IO) {
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
    }

    /**
     * 將 Memo 轉換成 MyData
     */
    private fun memoToMyData(memo: Memo) = MyData(
        memo.id,
        (memo.status == MemoStatus.Important.name),
        memo.memoContent
    )

}