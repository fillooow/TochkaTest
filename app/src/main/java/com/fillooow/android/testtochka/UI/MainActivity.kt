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
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.fillooow.android.testtochka.BaseApp
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.network.ApiError
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.ui.MainActivity.ConnectivityUtils.hasConnection
import com.fillooow.android.testtochka.BusinessLogic.network.GithubApiService
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.jakewharton.rxbinding2.widget.RxTextView
import com.vk.sdk.VKAccessToken
import com.vk.sdk.VKAccessTokenTracker
import com.vk.sdk.VKSdk
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainActivityPresentation {

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
        private const val STATE_HAS_BTN_CLICKED = "hasBtnClicked"
    }

    object ConnectivityUtils{
        fun hasConnection(context: Context): Boolean{
            val cm: ConnectivityManager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val connectInfo = cm.activeNetworkInfo
            if (connectInfo != null && connectInfo.isConnected){
                return true
            }
            return false
        }
    }

    private val githubApiService by lazy {
        GithubApiService.create()
    }

    @Inject lateinit var socialNetworkPresenter: SocialNetworkPresenter
    @Inject lateinit var mainActivityPresenter: MainActivityPresenter

    private var compositeDisposable = CompositeDisposable()

    private var userName: String? = ""
    private var userPhotoUrl: String? = null
    private var lastSearchText: String? = null
    private var socialNetworkLabel: String? = null // если null - пользователь не вошел, кидаем на логин активити
    private var totalPages = 0
    private var totalCount = 0 // Найдено элементов
    private var currentPage = 0
    private var currentPageBeforeChanging = 0 // Required to prevent wrong changing the counter of the current page
    private var isNextBtnEnabled = false
    private var isPrevBtnEnabled = false
    private var hasBtnClicked = false // Отслеживает, был ли сделан запрос с кнопок навигации (Далее, назад)
    private var itemsList = ArrayList<UserSearchModel.Items>()
    private lateinit var gso: GoogleSignInOptions
    private lateinit var mGoogleApiClient: GoogleApiClient

    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var rvUsers: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as BaseApp).appComponent.inject(this)

        mainActivityPresenter.initInterfaces(this)
        mainActivityPresenter.loadSocialNetworkLabel()

        currentPageBeforeChanging = currentPage

        setSupportActionBar(toolbar)
        initialiseDrawer()
        rvUsers = findViewById(R.id.rvTest)
        rvUsers.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        userAdapter = UserSearchAdapter(itemsList, applicationContext)
        rvUsers.adapter = userAdapter

        nextPageButton.setOnClickListener {
            loadNextPage()
        }
        previousPageButton.setOnClickListener {
            loadPrevPage()
        }

        restoreInstanceState(savedInstanceState)

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
                        mainActivityPresenter.showToast(getString(R.string.no_internet_connection))
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
            mainActivityPresenter.restoreNetworkDB(socialNetworkLabel, userPhotoUrl, userName)
        }
    }

    private fun initialiseDrawer(){
        val drawableToggle:ActionBarDrawerToggle = object : ActionBarDrawerToggle(this, drawer_layout,
            toolbar, R.string.drawer_open, R.string.drawer_close) {
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
                R.id.action_bip -> {
                    mainActivityPresenter.showToast("bip")
                }
                R.id.action_bup -> {
                    mainActivityPresenter.showToast("bup")
                }
                R.id.action_bop -> {
                    mainActivityPresenter.showToast("bop")
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
                    updateUI()
                }, {
                    val errMessage = ApiError(it).errorMessage
                    if (errMessage.contains("API rate limit exceeded")){
                        mainActivityPresenter.showToast(getString(R.string.API_rate_limit_exceeded))
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
        mainActivityPresenter.restoreUiInfo(lastSearchText, isNextBtnEnabled, isPrevBtnEnabled, totalPages,
            currentPage, totalCount, hasBtnClicked)
        mainActivityPresenter.restoreResponseList(itemsList)
    }

    private fun setUpCurrentPage() {
        if (hasBtnClicked == false){
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
                mainActivityPresenter.showToast(getString(R.string.reached_max_page))
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
        mainActivityPresenter.setUserPhoto(userPhotoUrl, drawer_user_photo)
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
            mainActivityPresenter.restoreNetworkDB(socialNetworkLabel, userPhotoUrl, userName)
            startLoginIntent()
        } else {
            mainActivityPresenter.showToast(getString(R.string.no_internet_connection))
        }
    }

    private fun startLoginIntent(){
        val intent = Intent(this, LoginActivity::class.java)
        drawer_layout.closeDrawers()
        startActivityForResult(intent, RC_LOGIN)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(STATE_LAST_SEARCH_TEXT, lastSearchText)
        outState?.putBoolean(STATE_IS_NEXT_BTN_ENABLED, isNextBtnEnabled)
        outState?.putBoolean(STATE_IS_PREV_BTN_ENABLED, isPrevBtnEnabled)
        outState?.putInt(STATE_TOTAL_PAGES, totalPages)
        outState?.putInt(STATE_CURRENT_PAGE, currentPage)
        outState?.putInt(STATE_TOTAL_COUNT, totalCount)
        outState?.putBoolean(STATE_HAS_BTN_CLICKED, hasBtnClicked)
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            lastSearchText = savedInstanceState.getString(STATE_LAST_SEARCH_TEXT)
            isNextBtnEnabled = savedInstanceState.getBoolean(STATE_IS_NEXT_BTN_ENABLED)
            isPrevBtnEnabled = savedInstanceState.getBoolean(STATE_IS_PREV_BTN_ENABLED)
            totalPages = savedInstanceState.getInt(STATE_TOTAL_PAGES)
            currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE)
            totalCount = savedInstanceState.getInt(STATE_TOTAL_COUNT)
            hasBtnClicked = savedInstanceState.getBoolean(STATE_HAS_BTN_CLICKED)
        }
    }

    override fun setSuccessfulLoadedUiResults(lastSearchTextDB: String?, nextBtnEnabledDB: Boolean,
        prevBtnEnabledDB: Boolean, totalPagesDB: Int, currentPageDB: Int, totalCountDB: Int, hasBtnClickedDB: Boolean) {
        lastSearchText = lastSearchTextDB
        isNextBtnEnabled = nextBtnEnabledDB
        isPrevBtnEnabled = prevBtnEnabledDB
        totalPages = totalPagesDB
        currentPage = currentPageDB
        totalCount = totalCountDB
        hasBtnClicked = hasBtnClickedDB
        inputET.setText(lastSearchText)
        updateUI()
    }

    override fun setSuccessfulLoadedItemsList(items: ArrayList<UserSearchModel.Items>) {
        if (items.isEmpty()){
            loadUserItems(lastSearchText ?: inputET.text.toString(), currentPage)
        } else {
            itemsList = items
            userAdapter.notifyDataSetChanged()
        }
    }

    override fun setSocialNetworkResults(t: SocialNetworkData) {
        // TODO: загнать в переменные метода, чтобы не плодить 3 строки лишних (лул)
        socialNetworkLabel = t.label
        userName = t.username
        userPhotoUrl = t.photoURL
        setUserProfile()
        if (hasConnection(applicationContext)) {
            checkSocialNetworkTokenState(socialNetworkLabel)
        }
    }

    override fun errorEmptyResult() {
        startLoginIntent()
    }

    fun checkSocialNetworkTokenState(label: String?) {
        when (label){
            LoginActivity.GOOGLE_LABEL -> {
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

    private fun expiredSession(){
        mainActivityPresenter.showToast(getString(R.string.session_token_expired))
        startLoginIntent()
    }

    override fun onStart() {
        super.onStart()
        mainActivityPresenter.loadUiInfo()
        mainActivityPresenter.loadResponseList(itemsList)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        mainActivityPresenter.onDestroyPresenter()
        super.onDestroy()
    }
}