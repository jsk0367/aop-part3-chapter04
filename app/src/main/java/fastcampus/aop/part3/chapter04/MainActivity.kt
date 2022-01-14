package fastcampus.aop.part3.chapter04

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import fastcampus.aop.part3.chapter04.adapter.BookAdapter
import fastcampus.aop.part3.chapter04.adapter.HistoryAdapter
import fastcampus.aop.part3.chapter04.api.BookService
import fastcampus.aop.part3.chapter04.databinding.ActivityMainBinding
import fastcampus.aop.part3.chapter04.model.BestSellerDto
import fastcampus.aop.part3.chapter04.model.History
import fastcampus.aop.part3.chapter04.model.SearchBookDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BookAdapter
    private lateinit var bookService: BookService
    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBookRecyclerView()
        initHistoryRecyclearView()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "BookSearchDB"
        ).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://book.interpark.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        bookService = retrofit.create(BookService::class.java)

        bookService.getBestSellerBooks(getString(R.string.interparkAPIKey))

            // enqueue 할 때에는 콜백을 파라미터로 넣어 통신 성공 및 실패 시의 처리를 핸들링한다.
            .enqueue(object: Callback<BestSellerDto>{

                override fun onResponse(
                    call: Call<BestSellerDto>, response: Response<BestSellerDto>) {
                if (response.isSuccessful.not()){
                    Log.e(TAG, "NOT!! SUCCESS")
                    return
                }
                response.body()?.let {
                    Log.d(TAG, it.toString())

                    it.books.forEach { book ->
                        Log.d(TAG, book.toString())
                    }

                    adapter.submitList(it.books)
                }
                }
                override fun onFailure(call: Call<BestSellerDto>, t: Throwable) {
                    Log.e(TAG, t.toString())
                }

            })

    }


    private fun search(keyword: String) {
        bookService.getBooksByName(getString(R.string.interparkAPIKey), keyword)
        .enqueue(object: Callback<SearchBookDto>{

            override fun onResponse(
                call: Call<SearchBookDto>, response: Response<SearchBookDto>) {

                hideHistroyView()
                saveSearchKeyword(keyword)

                if (response.isSuccessful.not()){
                    Log.e(TAG, "NOT!! SUCCESS")
                    return
                }

                adapter.submitList(response.body()?.books.orEmpty())
                response.body()?.let {
                    Log.d(TAG, it.toString())

                    it.books.forEach { book ->
                        Log.d(TAG, book.toString())
                    }

                }
            }
            override fun onFailure(call: Call<SearchBookDto>, t: Throwable) {

                hideHistroyView()
                Log.e(TAG, t.toString())
            }

        })

        bookService.getBooksByName(getString(R.string.interparkAPIKey), keyword)
    }
    private fun initBookRecyclerView(){
        adapter = BookAdapter(itemClickListener = {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("bookModel",it)
            startActivity(intent)
        })

        binding.bookRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.bookRecyclerview.adapter = adapter
    }

    private fun initHistoryRecyclearView(){
        historyAdapter = HistoryAdapter (historyDeleteClickedListener = {
            deleeSearchKeyword(it)
        })

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = historyAdapter
    }

    private fun initSearchEditText() {
        binding.searchEditText.setOnKeyListener{ v, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.MotionEvent.ACTION_DOWN){
                search(binding.searchEditText.text.toString())
                return@setOnKeyListener true
            }

            return@setOnKeyListener false
        }

        binding.searchEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                showHistoryView()
            }
            return@setOnTouchListener false

        }

    }

    private fun showHistoryView(){
        Thread {
            val keywords = db.historyDao().getAll().reversed() //최신순서대로 보여줌

            runOnUiThread{
                binding.historyRecyclerView.isVisible = true
                historyAdapter.submitList(keywords.orEmpty())
            }
        }.start()
        binding.historyRecyclerView.isVisible = true
    }

    private fun hideHistroyView(){

        binding.historyRecyclerView.isVisible = false

    }
    private fun saveSearchKeyword(keyword: String){
        Thread {
            db.historyDao().insertHistory(History(null, keyword))
        }.start()
    }

    private fun deleeSearchKeyword(keyword: String){
        Thread {
            db.historyDao().delete(keyword)
            showHistoryView()
        }.start()
    }
    companion object {
        private const val TAG = "MainActivity"
    }
}