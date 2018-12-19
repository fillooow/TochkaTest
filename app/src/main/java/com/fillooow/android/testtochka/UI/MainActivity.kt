package com.fillooow.android.testtochka.UI

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.Tests.TestFacebookActivity
import com.fillooow.android.testtochka.Tests.TestGoogleActivity
import com.fillooow.android.testtochka.Tests.TestVkActivity
import com.fillooow.android.testtochka.network.GithubApiService
import com.fillooow.android.testtochka.network.model.UserSearchModel
import com.google.gson.JsonObject
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

// TODO: Вывести уведомление на максиманой странице

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ELEMENTS_PER_PAGE = 30 // Количество отображаемых на странице элементов
        private const val MAX_ELEMENTS = 1000 // Для OpenAPI гитхаба, можно получить лишь первую 1000 результатов.
        private const val MAX_PAGE = (MAX_ELEMENTS/ELEMENTS_PER_PAGE) + 1 // Исходя из количества доступных результатов
        // и количества отображаемых элементов, определяем максимально доступную страницу
        private const val GITHUB_TAG = "GITHUB_TAG"
    }

    private val githubApiService by lazy {
        GithubApiService.create()
    }

    var disposable: Disposable? = null
    var disposableET: Disposable? = null

    private var totalPages = 0
    private var totalCount = 0 // Найдено элементов
    private var currentPage = 0
    private var isNextBtnEnabled = false
    private var isPrevBtnEnabled = false
    private var itemsList = ArrayList<UserSearchModel.Items>()

    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var rvUsers: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(GITHUB_TAG, MAX_PAGE.toString())

        setSupportActionBar(toolbar)

        initialiseDrawer()
        rvUsers = findViewById(R.id.rvTest)
        rvUsers.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        //loadUserItems("biba")
        userAdapter = UserSearchAdapter(itemsList, applicationContext)
        rvUsers.adapter = userAdapter

        nextPageButton.setOnClickListener {
            loadNextPage()
        }
        previousPageButton.setOnClickListener {
            loadPrevPage()
        }

        updateUI()

        disposableET = RxTextView.afterTextChangeEvents(inputET)
            .debounce(600, TimeUnit.MILLISECONDS)
            .subscribe{
                var searchText = inputET.text.toString()
                searchText = removeSpaces(searchText)
                loadUserItems(searchText, currentPage)
            }


    }

    fun initialiseDrawer(){
        val drawableToggle:ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
            }
        }

        drawableToggle.isDrawerIndicatorEnabled = true
        drawer_layout.addDrawerListener(drawableToggle)
        drawableToggle.syncState()

        navigation_view.setNavigationItemSelectedListener {
            when (it.itemId){
                R.id.action_google -> {
                    drawer_layout.closeDrawers()
                    val intent = Intent(this, TestGoogleActivity::class.java)
                    startActivity(intent)
                }
                R.id.action_facebook -> {
                    val intent = Intent(this, TestFacebookActivity::class.java)
                    startActivity(intent)
                    drawer_layout.closeDrawers()
                }
                R.id.action_vk -> {
                    val intent = Intent(this, TestVkActivity::class.java)
                    startActivity(intent)
                    drawer_layout.closeDrawers()
                }
                R.id.action_login -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    drawer_layout.closeDrawers()
                }
            }
            true
        }
    }

    // TODO: проверка, если слишком в минус уйти

    fun loadNextPage(){
        currentPage++
        loadUserItems(inputET.text.toString(), currentPage)
    }

    fun loadPrevPage(){
        currentPage--
        loadUserItems(inputET.text.toString(), currentPage)
    }

    fun removeSpaces(text: String) : String{
        return text.trim().replace("[\\s]{2,}", " ")
    }

    // gets users from Github Open API
    fun loadUserItems(searchText: String, page: Int){
        itemsList.clear()
        if (searchText == ""){
            runOnUiThread {
                totalPages = 0
                currentPage = 0
                updateUI()
                supportActionBar?.title = getString(R.string.empty_request)
            }
        } else {
            disposable = githubApiService
                //TODO: pages
                .searchUser(searchText, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(GITHUB_TAG, "error message: ${it.message}")
                    totalCount = it.total_count
                    totalPages = countPages(totalCount)
                    // TODO: мб в onNext закинуть
                    // TODO: прочекать, мб я уже в onNext и делаю всё это 30 раз (но вроде нет)
                    for (item in it.items) {
                        itemsList.add(item)
                    }
                    updateUI()
                }, {
                    // TODO: достать содержимое сообщения
                    Log.d(GITHUB_TAG, it.message)
                    Log.d(GITHUB_TAG, it.localizedMessage)
                    var jsonError = JsonObject()
                })
        }

    }

    fun countPages(totalCount: Int): Int {
        if (totalCount <= 0) {
            return 0
        }
        return (totalCount/ELEMENTS_PER_PAGE) + 1
    }

    fun updateUI(){
        setUpCurrentPage()
        pageCounterTV.text = getString(R.string.page_counter, currentPage, totalPages)
        supportActionBar?.title = getString(R.string.total_found, totalCount)
        pageCounterTV.text  = getString(R.string.page_counter, currentPage, totalPages)
        userAdapter.notifyDataSetChanged()
    }

    fun setUpCurrentPage() {
        if (totalPages == 0){
            currentPage = 0
        }
        if (totalPages > 0){
            if (currentPage <= 1){
                currentPage = 1
            }
        }
        setUpSearchButtons()
    }

    fun setUpSearchButtons(){
        when{
            currentPage == 0 -> {
                isNextBtnEnabled = false
                isPrevBtnEnabled = false
            }
            currentPage == 1 -> {
                isNextBtnEnabled = totalPages >= 2
                isPrevBtnEnabled = false
            }
            currentPage > 1 -> {
                isNextBtnEnabled = totalPages > currentPage
                isPrevBtnEnabled = currentPage > 1
            }
            currentPage == MAX_PAGE -> {
                isNextBtnEnabled = false
            }
        }
        nextPageButton.isEnabled = isNextBtnEnabled
        previousPageButton.isEnabled = isPrevBtnEnabled
    }

}
