package com.example.testrealm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.realm.*
import io.realm.kotlin.where
import kotlinx.coroutines.*

class MyViewModel: ViewModel() {
    private var config: RealmConfiguration? = null

    //必須將要添加監聽器的 RealmResults 設置爲強引用，例如放在類別層級，否則放在方法中會被GC。
    //Registering a change listener will not prevent the underlying RealmResults from being garbage collected.
    //If the RealmResults is garbage collected, the change listener will stop being triggered. To avoid this, keep a
    //strong reference for as long as appropriate e.g. in a class variable.
    //參考：https://stackoverflow.com/questions/43174517/android-realm-changelistener-not-being-triggered
    private lateinit var memosListener: RealmResults<Memo>

    fun initRealm(key: ByteArray){
        //設定 realm 參數
        val realmName = "My Project"
        config = RealmConfiguration.Builder()
            .name(realmName)
            .encryptionKey(key)               //加密，當把資料庫從未加密變成加密時需要進行資料庫遷移，反之也一樣要遷移
//            .allowWritesOnUiThread(true)    //可在 UI thread 寫入
//            .allowQueriesOnUiThread(true)   //可在 UI thread 查詢
            .deleteRealmIfMigrationNeeded()   //在需要資料庫遷移時直接刪掉資料庫
            .build()
    }

    /**
     * 爲資料變化設定監聽器
     */
    fun addChangeListenerToRealm(deleteListener: (Int, Int) -> Unit,
                                 insertListener: (Int, Int) -> Unit,
                                 modifyListener: (Int, Int) -> Unit){
        val uiThreadRealm = Realm.getInstance(config!!)

        memosListener = uiThreadRealm.where<Memo>().findAllAsync()
        memosListener.addChangeListener{ collection, changeSet ->
            //刪除範圍
            val deletions = changeSet.deletionRanges
            for (range in deletions) {
                deleteListener(range.startIndex, range.length)
            }
            //想取指標來操作的話可以這樣寫
            // process deletions in reverse order if maintaining parallel data structures so indices don't change as you iterate
//            for (i in deletions.indices.reversed()) {
//                val range = deletions[i]
//                deleteListener(range.startIndex, range.length)
//            }

            //新增範圍
            val insertions = changeSet.insertionRanges
            for (range in insertions) {
                insertListener(range.startIndex, range.length)
            }

            //修改範圍
            val modifications = changeSet.changeRanges
            for (range in modifications) {
                modifyListener(range.startIndex, range.length)
            }
        }
    }

    /**
     * 創建項目清單
     * 從 realm 資料庫取得資料給 recyclerView
     * 應使用 RealmBaseAdapter，之後有空再研究：
     * https://github.com/realm/realm-android-adapters/blob/master/adapters/src/main/java/io/realm/RealmBaseAdapter.java
     */
    fun getItemList(): ArrayList<MyData>{
        val itemList = arrayListOf<MyData>()
        viewModelScope.launch(Dispatchers.IO) {
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config!!)

            //「查詢」
            // 取出所有 realm 中的 memo
            val memos = backgroundThreadRealm.where<Memo>().findAll()
//            val memos2 = backgroundThreadRealm.where(Memo::class.java)
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
                val backgroundThreadRealm: Realm = Realm.getInstance(config!!)
                //「查詢」
                // 取出所有 realm 中的 memo
                val memos = backgroundThreadRealm.where<Memo>().findAll()

                //「新增」
                val memo = Memo()
                //遞增流水號
                memo.id = (memos.max("id") as Long? ?: 0) + 1
                memo.status = MemoStatus.Normal.name
                memo.memoContent = dataStr
//                Log.d("TAG", "新增前的 memos: $memos")
                // 所有對 realm 的修改都必須在 write block 內
                backgroundThreadRealm.executeTransaction { transactionRealm ->
                    transactionRealm.insert(memo)
                }
//                Log.d("TAG", "[新增 memo 到資料庫] id: ${memo.id} memoContent: ${memo.memoContent}")
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
     * 修改一個項目
     */
    fun editDBData(dataId: Long, dataStr: String): Boolean = runBlocking {
        //修改資料庫資料
        val result = viewModelScope.async(Dispatchers.IO){
            var resultAsync = false
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config!!)

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
     * 修改星星
     */
    fun editDBDataStar(dataId: Long, star: Boolean): Boolean = runBlocking {
        //修改資料庫資料
        val result = viewModelScope.async(Dispatchers.IO){
            var resultAsync = false
            //用參數建立 Realm 物件
            val backgroundThreadRealm: Realm = Realm.getInstance(config!!)

            if(dataId > 0) {
                //修改記事
                Log.d("TAG", "要修改的 Id: $dataId")
                // 所有對 realm 的修改都必須在 write block 內
                backgroundThreadRealm.executeTransaction { transactionRealm ->
                    val innerYetAnotherMemo: Memo =
                        transactionRealm.where<Memo>().equalTo("id", dataId).findFirst()!!
                    innerYetAnotherMemo.status = if(star){
                        MemoStatus.Important.name
                    }else{
                        MemoStatus.Normal.name
                    }
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
            val backgroundThreadRealm: Realm = Realm.getInstance(config!!)

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
            val backgroundThreadRealm: Realm = Realm.getInstance(config!!)

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