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
import com.fillooow.android.testtochka.Tests.TestFacebookActivity
import com.fillooow.android.testtochka.Tests.TestGoogleActivity
import com.fillooow.android.testtochka.Tests.TestVkActivity
import com.fillooow.android.testtochka.UI.MainActivity.ConnectivityUtils.hasConnection
import com.fillooow.android.testtochka.network.GithubApiService
import com.fillooow.android.testtochka.network.model.UserSearchModel
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jakewharton.rxbinding2.widget.RxTextView
import com.squareup.picasso.Picasso
import com.vk.sdk.VKSdk
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

// TODO: Вывести уведомление на максиманой странице

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ELEMENTS_PER_PAGE = 30 // Количество отображаемых на странице элементов
        private const val MAX_ELEMENTS = 1000 // Для OpenAPI гитхаба, можно получить лишь первую 1000 результатов.
        private const val MAX_PAGE = (MAX_ELEMENTS/ELEMENTS_PER_PAGE) + 1 // Исходя из количества доступных результатов
        // и количества отображаемых элементов, определяем максимально доступную страницу
        private const val GITHUB_TAG = "GITHUB_TAG"
        private const val RC_LOGIN = 9991
    }

    object ConnectivityUtils{
        fun hasConnection(context: Context): Boolean{
            val cm: ConnectivityManager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            // TODO: чекнуть
            /*var connectInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (connectInfo != null && connectInfo.isConnected){
                return true
            }
            connectInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (connectInfo != null && connectInfo.isConnected){
                return true
            }*/
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

    var disposable: Disposable? = null
    var disposableET: Disposable? = null

    private var userName: String? = null
    private var userPhotoUrl: String? = null
    private var socialNetworkLabel: String? = null //TODO: если null - кидаем на логин активити
    private var totalPages = 0
    private var totalCount = 0 // Найдено элементов
    private var currentPage = 0
    private var isNextBtnEnabled = false
    private var isPrevBtnEnabled = false
    private var itemsList = ArrayList<UserSearchModel.Items>()
    private lateinit var gso: GoogleSignInOptions
    private lateinit var mGoogleApiClient: GoogleApiClient

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
                if(hasConnection(applicationContext)) {
                    var searchText = inputET.text.toString()
                    searchText = removeSpaces(searchText)
                    loadUserItems(searchText, currentPage)
                } else {
                    showToast(getString(R.string.no_internet_connection))
                }
            }


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
            when (it.itemId){
                R.id.action_google -> {
                    val intent = Intent(this, TestGoogleActivity::class.java)
                    drawer_layout.closeDrawers()
                    startActivity(intent)
                }
                R.id.action_facebook -> {
                    val intent = Intent(this, TestFacebookActivity::class.java)
                    drawer_layout.closeDrawers()
                    startActivity(intent)
                }
                R.id.action_vk -> {
                    val intent = Intent(this, TestVkActivity::class.java)
                    drawer_layout.closeDrawers()
                    startActivity(intent)
                }
                R.id.action_login -> {
                    startLoginIntent()
                }
                R.id.action_logout -> {
                    setupLogOutBtn(socialNetworkLabel)
                }
            }
            //TODO: перенести все closeDrawers() сюда
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
                    val errMessage = ApiError(it).errorMessage
                    if (errMessage.contains("API rate limit exceeded")){
                        // TODO: в стрингу загнать
                        showToast("Превышено число запросов за минуту. Подождите минутку и попробуйте снова")
                    }
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

    fun setUserPhoto(url: String?){
        // google+ may return "null" as an answer, if user don't have profile picture
        Picasso.get()
            .load(url)
            .into(drawer_user_photo)
    }

    // TODO: можно и переименовать
    fun setupLogOutBtn(label: String?){
        if (hasConnection(applicationContext)) {
            when (label) {
                LoginActivity.GOOGLE_LABEL -> {
                    //TODO: закинуть всё в onStart (и интентовскую хрень тоже)
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

    fun startLoginIntent(){
        val intent = Intent(this, LoginActivity::class.java)
        drawer_layout.closeDrawers()
        startActivityForResult(intent, RC_LOGIN)
    }

    fun showToast(toastText: String){
        runOnUiThread {
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show()
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
}
