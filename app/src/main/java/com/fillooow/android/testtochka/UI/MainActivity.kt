package com.fillooow.android.testtochka.UI

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.facebook.login.LoginManager
import com.fillooow.android.testtochka.BusinessLogic.ApiError
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.UI.MainActivity.ConnectivityUtils.hasConnection
import com.fillooow.android.testtochka.network.GithubApiService
import com.fillooow.android.testtochka.network.model.UserSearchModel
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.jakewharton.rxbinding2.widget.RxTextView
import com.squareup.picasso.Picasso
import com.vk.sdk.VKSdk
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ELEMENTS_PER_PAGE = 30 // Количество отображаемых на странице элементов
        private const val MAX_ELEMENTS = 1000 // Для OpenAPI гитхаба, можно получить лишь первую 1000 результатов.
        private const val MAX_PAGE = (MAX_ELEMENTS/ELEMENTS_PER_PAGE) + 1 // Определяем максимально доступную
        // страницу исходя из количества доступных результатов и отображаемых элементов

        private const val GITHUB_TAG = "GITHUB_TAG"
        // Request code for loginIntent at onActivityResult()
        private const val RC_LOGIN = 9991

        // for instance state
        private const val STATE_LAST_SEARCH_TEXT = "lastSearchText"
        private const val STATE_IS_NEXT_BTN_ENABLED = "isNextBtnEnabled"
        private const val STATE_IS_PREV_BTN_ENABLED = "isPrevBtnEnabled"
        private const val STATE_TOTAL_PAGES = "totalPages"
        private const val STATE_CURRENT_PAGE = "currentPage"
        private const val STATE_TOTAL_COUNT = "totalCount"
    }

    object ConnectivityUtils{
        fun hasConnection(context: Context): Boolean{
            val cm: ConnectivityManager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var connectInfo = cm.activeNetworkInfo
            if (connectInfo != null && connectInfo.isConnected){
                return true
            }
            return false
        }
    }

    private val githubApiService by lazy {
        GithubApiService.create()
    }

    private var compositeDisposable = CompositeDisposable()

    private var userName: String? = null
    private var userPhotoUrl: String? = null
    private var lastSearchText: String = ""
    private var socialNetworkLabel: String? = null //TODO: если null - кидаем на логин активити
    private var totalPages = 0
    private var totalCount = 0 // Найдено элементов
    private var currentPage = 0
    private var currentPageBeforeChanging = 0 // Required to prevent wrong changing the counter of the current page
    private var isNextBtnEnabled = false
    private var isPrevBtnEnabled = false
    private var hasBtnClicked = false
    private var itemsList = ArrayList<UserSearchModel.Items>()
    private lateinit var gso: GoogleSignInOptions
    private lateinit var mGoogleApiClient: GoogleApiClient

    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var rvUsers: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentPageBeforeChanging = currentPage

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

        restoreInstanceState(savedInstanceState)

        updateUI()

        compositeDisposable.add(
            RxTextView.afterTextChangeEvents(inputET)
                .debounce(600, TimeUnit.MILLISECONDS)
                .subscribe{
                    if(hasConnection(applicationContext)) {
                        hasBtnClicked = false
                        var searchText = inputET.text.toString()
                        searchText = removeSpaces(searchText)
                        if (searchText != lastSearchText) {
                            loadUserItems(searchText, currentPage)
                        }
                    } else {
                        showToast(getString(R.string.no_internet_connection))
                    }
                }
        )


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK){
            return
        }
        if(requestCode == RC_LOGIN){
            socialNetworkLabel = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_SOCIAL_NETWORK_LABEL)
            userName = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_NAME)
            userPhotoUrl = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_PHOTO_URL)
            drawer_user_name.text = userName
            // google+ may return "null" as an answer, if user don't have profile picture
            if (userPhotoUrl == "null"){
                userPhotoUrl = "https://www.scirra.com/images/articles/windows-8-user-account.jpg"
            }
            setUserPhoto(userPhotoUrl)
            //Log.d(GITHUB_TAG, data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_NAME))
            //Log.d(GITHUB_TAG, data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_PHOTO_URL))
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
            drawer_layout.closeDrawers()
            when (it.itemId){
                R.id.action_login -> {
                    startLoginIntent()
                }
                R.id.action_logout -> {
                    initializeLogoutBtn(socialNetworkLabel)
                }
            }
            true
        }
    }

    private fun loadNextPage(){
        hasBtnClicked = true
        currentPageBeforeChanging = currentPage
        currentPage++
        loadUserItems(inputET.text.toString(), currentPage)
    }

    private fun loadPrevPage(){
        hasBtnClicked = true
        currentPageBeforeChanging = currentPage
        currentPage--
        loadUserItems(inputET.text.toString(), currentPage)
    }

    private fun removeSpaces(text: String) : String{
        return text.trim().replace("[\\s]{2,}", " ")
    }

    // gets users from Github Open API
    private fun loadUserItems(searchText: String, page: Int){
        itemsList.clear()
        if (searchText == ""){
            runOnUiThread {
                totalPages = 0
                currentPage = 0
                updateUI()
                supportActionBar?.title = getString(R.string.empty_request)
            }
        } else {
            compositeDisposable.add(
                githubApiService
                .searchUser(searchText, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    totalCount = it.total_count
                    totalPages = countPages(totalCount)
                    for (item in it.items) {
                        itemsList.add(item)
                    }
                    updateUI()
                }, {
                    val errMessage = ApiError(it).errorMessage
                    if (errMessage.contains("API rate limit exceeded")){
                        showToast(getString(R.string.API_rate_limit_exceeded))
                        currentPage = currentPageBeforeChanging
                    }
                }))
        }
        lastSearchText = searchText
    }

    private fun countPages(totalCount: Int): Int {
        if (totalCount <= 0) {
            return 0
        }
        return (totalCount/ELEMENTS_PER_PAGE) + 1
    }

    private fun updateUI(){
        setUpCurrentPage()
        pageCounterTV.text = getString(R.string.page_counter, currentPage, totalPages)
        supportActionBar?.title = getString(R.string.total_found, totalCount)
        pageCounterTV.text  = getString(R.string.page_counter, currentPage, totalPages)
        userAdapter.notifyDataSetChanged()
    }

    private fun setUpCurrentPage() {
        if (!hasBtnClicked){
            currentPage = 1
        }
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

    private fun setUpSearchButtons(){
        when{
            currentPage == 0 -> {
                isNextBtnEnabled = false
                isPrevBtnEnabled = false
            }
            currentPage == 1 -> {
                isNextBtnEnabled = totalPages >= 2
                isPrevBtnEnabled = false
            }
            currentPage == MAX_PAGE -> {
                isNextBtnEnabled = false
                showToast(getString(R.string.reached_max_page))
            }
            currentPage > 1 -> {
                isNextBtnEnabled = totalPages > currentPage
                isPrevBtnEnabled = currentPage > 1
            }

        }
        nextPageButton.isEnabled = isNextBtnEnabled
        previousPageButton.isEnabled = isPrevBtnEnabled
    }

    private fun setUserPhoto(url: String?){
        // google+ may return "null" as an answer, if user don't have profile picture
        Picasso.get()
            .load(url)
            .error(R.drawable.default_user_profile_image_png_5)
            .into(drawer_user_photo)

    }

    private fun initializeLogoutBtn(label: String?){
        if (hasConnection(applicationContext)) {
            when (label) {
                LoginActivity.GOOGLE_LABEL -> {
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback {
                        //startLoginIntent()
                    }
                }
                LoginActivity.FACEBOOK_LABEL -> {
                    LoginManager.getInstance().logOut()
                }
                LoginActivity.VKONTAKTE_LABEL -> {
                    VKSdk.logout()
                }
                null -> {
                    Toast.makeText(this, getString(R.string.logout_null_error), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, getString(R.string.logout_null_error), Toast.LENGTH_SHORT).show()
                }
            }
            startLoginIntent()
        } else {
            showToast(getString(R.string.no_internet_connection))
        }
    }

    private fun startLoginIntent(){
        val intent = Intent(this, LoginActivity::class.java)
        drawer_layout.closeDrawers()
        startActivityForResult(intent, RC_LOGIN)
    }

    private fun showToast(toastText: String){
        runOnUiThread {
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show()
        }
    }

    // TODO: onCreate
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(STATE_LAST_SEARCH_TEXT, lastSearchText)
        outState?.putBoolean(STATE_IS_NEXT_BTN_ENABLED, isNextBtnEnabled)
        outState?.putBoolean(STATE_IS_PREV_BTN_ENABLED, isPrevBtnEnabled)
        outState?.putInt(STATE_TOTAL_PAGES, totalPages)
        outState?.putInt(STATE_CURRENT_PAGE, currentPage)
        outState?.putInt(STATE_TOTAL_COUNT, totalCount)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            lastSearchText = savedInstanceState.getString(STATE_LAST_SEARCH_TEXT)
            isNextBtnEnabled = savedInstanceState.getBoolean(STATE_IS_NEXT_BTN_ENABLED)
            isPrevBtnEnabled = savedInstanceState.getBoolean(STATE_IS_PREV_BTN_ENABLED)
            totalPages = savedInstanceState.getInt(STATE_TOTAL_PAGES)
            currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE)
            totalCount = savedInstanceState.getInt(STATE_TOTAL_COUNT)
            Log.d(GITHUB_TAG, "onRestoreState")
        }
    }


    override fun onStart() {
        super.onStart()
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()
        mGoogleApiClient.connect()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}

// TODO: Жизненный цикл