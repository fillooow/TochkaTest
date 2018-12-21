package com.fillooow.android.testtochka.ui

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
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkDataBase
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchDataBase
import com.fillooow.android.testtochka.BusinessLogic.network.ApiError
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.ui.MainActivity.ConnectivityUtils.hasConnection
import com.fillooow.android.testtochka.BusinessLogic.network.GithubApiService
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.jakewharton.rxbinding2.widget.RxTextView
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.vk.sdk.VKAccessToken
import com.vk.sdk.VKAccessTokenTracker
import com.vk.sdk.VKSdk
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
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

    private var singleGetLabelDB: Single<List<SocialNetworkData>>? = null
    private var completableSetLabelDB: Completable? = null
    //private var singleGetLabelDB: Single<List<SocialNetworkData>>? = null
    private var compositeDisposable = CompositeDisposable()
    private var db: GithubUserSearchDataBase? = null
    private var socialDB: SocialNetworkDataBase? = null
    //private lateinit var mDbWorkerThread: DbWorkerThread
    //private val mUiHandler = Handler()

    private var userName: String = ""
    private var userPhotoUrl: String? = null
    private var lastSearchText: String? = null
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

        db = GithubUserSearchDataBase.getInstance(this)
        socialDB = SocialNetworkDataBase.getInstance(this)
        singleGetLabelDB = socialDB?.socialNetworkDataDao()?.getAll()

        loadSocialNetworkLabel()

        currentPageBeforeChanging = currentPage

        setSupportActionBar(toolbar)
        initialiseDrawer()
        rvUsers = findViewById(R.id.rvTest)
        rvUsers.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        //loadUserItems("biba")
        //socialNetworkLabel = "boba"

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
                        if ((lastSearchText == "") && (searchText == "")){
                            runOnUiThread{
                                supportActionBar?.title = getString(R.string.empty_request)
                            }
                        } else if (searchText != lastSearchText){
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
            // Если была нажата кнопка Назад в LoginActivity или результатом пришла неудача
            finish()
            return
        }
        if(requestCode == RC_LOGIN){
            socialNetworkLabel = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_SOCIAL_NETWORK_LABEL)
            userName = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_NAME).toString()
            userPhotoUrl = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_PHOTO_URL)
            setUserProfile()
            restoreNetworkDB()
        }
    }

    private fun initialiseDrawer(){
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

    // Gets users from Github Open API
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
                    //insertAtDb(it.items)
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

    private fun setUserProfile(){
        drawer_user_name.text = userName
        // google+ may return "null" as an answer, if user don't have profile picture
        if (userPhotoUrl == "null"){
            userPhotoUrl = "https://www.scirra.com/images/articles/windows-8-user-account.jpg"
        }
        setUserPhoto(userPhotoUrl)
    }

    private fun setUserPhoto(url: String?){
        // Google+ may return "null" as an answer, if user don't have profile picture
        Picasso.get()
            .load(url)
            .error(R.drawable.default_user_profile_image_png_5)
            .networkPolicy(NetworkPolicy.OFFLINE)
            .into(drawer_user_photo, object : Callback{
                override fun onSuccess() {

                }

                override fun onError(e: Exception?) {
                    Picasso.get()
                        .load(url)
                        .error(R.drawable.default_user_profile_image_png_5)
                        .into(drawer_user_photo, object : Callback {
                            override fun onSuccess() {

                            }

                            override fun onError(e: Exception?) {
                                Log.e("Picasso error", "Error $e")
                            }

                        })
                }

            })

    }

    private fun initializeLogoutBtn(label: String?){
        if (hasConnection(applicationContext)) {
            socialNetworkLabel = null
            when (label) {
                LoginActivity.GOOGLE_LABEL -> {
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback {

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
            socialNetworkLabel = null
            restoreNetworkDB()
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

    // TODO db
    /*fun insertAtDb(itemsList: ArrayList<UserSearchModel.Items>){
        val task = Runnable {
            var searchData = GithubUserSearchData()
            for (item in itemsList){
                searchData.githubID = item.id
                searchData.login = item.login
                searchData.avatarUrl = item.avatar_url
                searchData.type = item.type
                db?.githubUserSearchDataDao()?.insert(searchData)
            }

        }
        mDbWorkerThread.postTask(task)

    }*/

    /*fun loadFromDb() {
        val task = Runnable {
            val userData = db?.githubUserSearchDataDao()?.getAll()
            mUiHandler.post {
                if (userData?.size != 0){
                    itemsList.clear()
                    var userItem: UserSearchModel.Items
                    if (userData != null) {
                        for (item in userData){
                            userItem = UserSearchModel.Items(
                                item.login,
                                item.githubID,
                                item.type,
                                item.avatarUrl)
                            itemsList.add(userItem)
                        }
                    }
                }
            }
        }
        mDbWorkerThread.postTask(task)
    }*/

    private fun loadSocialNetworkLabel() {
        singleGetLabelDB?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(object : SingleObserver<List<SocialNetworkData>>{
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(t: List<SocialNetworkData>) {
                    if (t.isNotEmpty()) {
                        socialNetworkLabel = t[0].label
                        userName = t[0].username
                        userPhotoUrl = t[0].photoURL
                    }
                    setUserProfile()
                    if (hasConnection(applicationContext)) {
                        checkSocialNetworkTokenState(socialNetworkLabel)
                    }

                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load label error $e")
                }

            })
    }

    fun saveSocialNetworkLabel(){
        val socialNetworkData = SocialNetworkData()
        socialNetworkData.label = socialNetworkLabel
        socialNetworkData.photoURL = userPhotoUrl
        socialNetworkData.username = userName

        Completable.fromAction {
            socialDB?.socialNetworkDataDao()?.insert(socialNetworkData)
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load label error $e")
                }

            })
    }

    private fun restoreNetworkDB() {
        Completable.fromAction {
            socialDB?.socialNetworkDataDao()
                ?.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveSocialNetworkLabel()
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load label error $e")
                }

            })
    }

    fun checkSocialNetworkTokenState(label: String?) {
        when (label){
            LoginActivity.GOOGLE_LABEL -> {
                // TODO: проверка токена для гугла
                gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                mGoogleApiClient = GoogleApiClient.Builder(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build()
                mGoogleApiClient.connect()
                val opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient)
                if (!opr.isDone){
                    expiredSession()
                }
            }
            LoginActivity.FACEBOOK_LABEL -> {
                val token = AccessToken.getCurrentAccessToken()
                if (token == null || token.isExpired){
                    expiredSession()
                }
            }
            LoginActivity.VKONTAKTE_LABEL -> {
                val vkAccessTokenTracker = object : VKAccessTokenTracker(){
                    override fun onVKAccessTokenChanged(oldToken: VKAccessToken?, newToken: VKAccessToken?) {
                        if (newToken == null){
                            expiredSession()
                        }
                    }
                }
                vkAccessTokenTracker.startTracking()
            }
            null -> {
                startLoginIntent()
            }
        }
    }

    fun expiredSession(){
        showToast(getString(R.string.session_token_expired))
        startLoginIntent()
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        GithubUserSearchDataBase.destroyInstance()
        SocialNetworkDataBase.destroyInstance()
        super.onDestroy()
    }
}

// TODO: Жизненный цикл