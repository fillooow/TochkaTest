package com.fillooow.android.testtochka.ui

import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkDataDao
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchDataDao
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchDataDao
import com.fillooow.android.testtochka.BusinessLogic.network.ApiError
import com.fillooow.android.testtochka.BusinessLogic.network.ConnectivityUtils
import com.fillooow.android.testtochka.BusinessLogic.network.GithubApiService
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import com.fillooow.android.testtochka.R
import com.google.android.gms.auth.api.Auth
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
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivityPresenter @Inject constructor(private val githubUserSearchDataDao: GithubUserSearchDataDao,
                                                private val uiInfoUserSearchDataDao: UiInfoUserSearchDataDao,
                                                private val socialNetworkDataDao: SocialNetworkDataDao,
                                                private val googleApiClient: GoogleApiClient,
                                                private val githubApiService: GithubApiService){

    val compositeDisposable = CompositeDisposable()
    var context: Context? = null
    var mainActivityPresentation: MainActivityPresentation? = null
    val connectivityUtils = ConnectivityUtils()

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
    private var hasBtnClicked = false // Отслеживает, был ли сделан запрос с кнопок навигации (далее, назад)
    var itemsList = ArrayList<UserSearchModel.Items>()

    fun initInterfaces(context: Context){
        this.context = context
        mainActivityPresentation = context as MainActivityPresentation
    }

    fun loadResponseList(editTextString: String){
        githubUserSearchDataDao.getAll()
            .subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(object : SingleObserver<List<GithubUserSearchData>> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {

                }

                override fun onSuccess(t: List<GithubUserSearchData>) {
                    itemsList.clear()
                    if (t.isNotEmpty()) {
                        for (item in t) {
                            itemsList.add(
                                UserSearchModel.Items(
                                    item.login,
                                    item.githubID,
                                    item.type,
                                    item.avatarUrl
                                )
                            )
                        }
                    }
                    if (itemsList.isEmpty()){
                        checkBeforeLoadingUserItems(lastSearchText ?: editTextString, currentPage)
                    } else {
                        mainActivityPresentation?.updateUI(currentPage, totalPages, totalCount)
                    }


                }
            })
    }

    fun checkBeforeLoadingUserItems(searchText: String, page: Int){
        itemsList.clear()
        if (searchText == ""){
            totalPages = 0
            currentPage = 0
            mainActivityPresentation?.updateUI(currentPage, totalPages, totalCount)
            mainActivityPresentation?.setSupportActionBarTitle(context?.getString(R.string.empty_request)!!)
        } else {
            loadUserItems(searchText, page)
        }
        lastSearchText = searchText
    }

    fun setOnActivityResultData(label: String?, username: String?, photoUrl: String?){
        socialNetworkLabel = label
        userName = username
        userPhotoUrl = photoUrl
        // google+ may return "null" as an answer, if user don't have profile picture
        if (userPhotoUrl == "null"){
            userPhotoUrl = "https://www.scirra.com/images/articles/windows-8-user-account.jpg"
        }
        mainActivityPresentation?.setUserProfile(userName)
        restoreNetworkDB(socialNetworkLabel, userPhotoUrl, userName)
    }

    fun saveResponseList(){
        for (item in itemsList){
            val githubUserSearchData = GithubUserSearchData()
            githubUserSearchData.githubID = item.id
            githubUserSearchData.avatarUrl = item.avatar_url
            githubUserSearchData.login = item.login
            githubUserSearchData.type = item.type

            Completable.fromAction {
                githubUserSearchDataDao.insert(githubUserSearchData)
            }.subscribeOn(Schedulers.io())
                .subscribe(object : CompletableObserver {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onError(e: Throwable) {
                    }

                })
        }
    }

    fun restoreResponseList(){
        Completable.fromAction {
            githubUserSearchDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveResponseList()
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {

                }

            })
    }


    fun loadUiInfo(){
        uiInfoUserSearchDataDao.getRecord().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UiInfoUserSearchData> {
                override fun onSuccess(t: UiInfoUserSearchData) {
                    lastSearchText = t.lastSearchTextDB
                    isNextBtnEnabled = t.isNextBtnEnabledDB
                    isPrevBtnEnabled = t.isPrevBtnEnabledDB
                    totalPages = t.totalPagesDB
                    currentPage = t.currentPageDB
                    totalCount = t.totalCountDB
                    hasBtnClicked = t.hasBtnClickedDB
                    mainActivityPresentation?.setSuccessfulLoadedUiResults(t.lastSearchTextDB)
                    mainActivityPresentation?.updateUI(currentPage, totalPages, totalCount)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load UI info error $e")
                }

            })
    }

    fun saveUiInfo(){
        val uiUserSearchData = UiInfoUserSearchData()
        uiUserSearchData.lastSearchTextDB = lastSearchText
        uiUserSearchData.isNextBtnEnabledDB = isNextBtnEnabled
        uiUserSearchData.isPrevBtnEnabledDB = isPrevBtnEnabled
        uiUserSearchData.totalPagesDB = totalPages
        uiUserSearchData.currentPageDB = currentPage
        uiUserSearchData.totalCountDB = totalCount
        uiUserSearchData.hasBtnClickedDB = hasBtnClicked

        Completable.fromAction {
            uiInfoUserSearchDataDao.insert(uiUserSearchData)
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Save ui info error $e")}

            })
    }

    fun restoreUiInfo(){
        Completable.fromAction {
            uiInfoUserSearchDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveUiInfo()
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {}

            })
    }


    fun loadSocialNetworkLabel() {
        socialNetworkDataDao.getRecord()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(object : SingleObserver<SocialNetworkData> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(t: SocialNetworkData) {
                    socialNetworkLabel = t.label
                    userName = t.username
                    userPhotoUrl = t.photoURL
                    checkSocialNetworkTokenState(socialNetworkLabel)
                    mainActivityPresentation?.setUserProfile(userName)
                }

                override fun onError(e: Throwable) {
                    if (e.message?.contains("Query returned empty result")!!){
                        mainActivityPresentation?.startLoginIntent()
                    }
                }

            })
    }

    fun saveSocialNetworkLabel(label: String?, photoURL: String?, userName: String?){
        val socialNetworkData = SocialNetworkData()
        socialNetworkData.label = label
        socialNetworkData.photoURL = photoURL
        socialNetworkData.username = userName

        Completable.fromAction {
            socialNetworkDataDao.insert(socialNetworkData)
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver {
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

    fun restoreNetworkDB(label: String?, photoURL: String?, userName: String?) {
        Completable.fromAction {
            socialNetworkDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveSocialNetworkLabel(label, photoURL, userName)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load label error $e")
                }

            })
    }

    fun setUpCurrentPage() {
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
        setUpSearchButtons(currentPage, totalPages)
    }

    fun setRxEditTextListener(inputET: EditText){
        currentPageBeforeChanging = currentPage
        compositeDisposable.add(
            RxTextView.afterTextChangeEvents(inputET)
                .debounce(600, TimeUnit.MILLISECONDS)
                .subscribe{
                    if(connectivityUtils.hasConnection(context?.applicationContext)) {
                        hasBtnClicked = false
                        var searchText = inputET.text.toString()
                        searchText = removeSpaces(searchText)
                        if ((lastSearchText == "") && (searchText == "")){
                            mainActivityPresentation?.setSupportActionBarTitle(context?.getString(R.string.empty_request)!!)
                        } else if (searchText != lastSearchText){
                            currentPage = 1
                            checkBeforeLoadingUserItems(searchText, currentPage)
                        }
                    } else {
                        showToast(context?.getString(R.string.no_internet_connection)!!)
                    }
                })
    }

    fun removeSpaces(text: String) : String{
        return text.trim().replace("[\\s]{2,}", " ")
    }

    fun setUserPhoto(imageView: ImageView){
        Picasso.get()
            .load(userPhotoUrl)
            .error(R.drawable.default_user_profile_image_png_5)
            .networkPolicy(NetworkPolicy.OFFLINE)
            .into(imageView, object : Callback {
                override fun onSuccess() {

                }

                override fun onError(e: Exception?) {
                    Picasso.get()
                        .load(userPhotoUrl)
                        .error(R.drawable.default_user_profile_image_png_5)
                        .into(imageView, object : Callback {
                            override fun onSuccess() {

                            }

                            override fun onError(e: Exception?) {
                                Log.e("Picasso error", "Error $e")
                            }

                        })
                }

            })
    }

    fun loadUserItems(searchText: String, page: Int){
            compositeDisposable.add(
                githubApiService
                    .searchUser(searchText, page)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        totalCount = it.total_count
                        totalPages = countPages(totalCount)
                        for (item in it.items){
                            itemsList.add(item)
                        }
                        mainActivityPresentation?.updateUI(currentPage, totalPages, totalCount)

                    }, {
                        val errMessage = ApiError(it).errorMessage
                        if (errMessage.contains("API rate limit exceeded")){
                            showToast(context?.getString(R.string.API_rate_limit_exceeded)!!)
                            currentPage = currentPageBeforeChanging
                        }
                    }))

    }

    fun countPages(totalCount: Int): Int {
        if (totalCount <= 0) {
            return 0
        }
        return (totalCount/ MainActivity.ELEMENTS_PER_PAGE) + 1
    }

    fun loadNextPage(editTextString: String){
        hasBtnClicked = true
        currentPageBeforeChanging = currentPage
        currentPage++
        checkBeforeLoadingUserItems(editTextString, currentPage)
    }

    fun loadPrevPage(editTextString: String){
        hasBtnClicked = true
        currentPageBeforeChanging = currentPage
        currentPage--
        checkBeforeLoadingUserItems(editTextString, currentPage)
    }

    fun setUpSearchButtons(currentPage: Int, totalPages: Int){
        var isNextBtnEnabled: Boolean = false
        var isPrevBtnEnabled: Boolean = false
        when{
            currentPage == 0 -> {
                isNextBtnEnabled = false
                isPrevBtnEnabled = false
            }
            currentPage == 1 -> {
                isNextBtnEnabled = totalPages >= 2
                isPrevBtnEnabled = false
            }
            currentPage == MainActivity.MAX_PAGE -> {
                isNextBtnEnabled = false
                showToast(context?.getString(R.string.reached_max_page)!!)
            }
            currentPage > 1 -> {
                isNextBtnEnabled = totalPages > currentPage
                isPrevBtnEnabled = currentPage > 1
            }

        }
        mainActivityPresentation?.setButtonsState(isNextBtnEnabled, isPrevBtnEnabled)
    }

    fun showToast(toastText: String){
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
    }

    fun checkSocialNetworkTokenState(label: String?) {
        if (connectivityUtils.hasConnection(context)) {
            when (label) {
                LoginActivity.GOOGLE_LABEL -> {
                    googleApiClient.connect()
                    val opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient)
                    if (!opr.isDone) {
                        expiredSession()
                    }
                }
                LoginActivity.FACEBOOK_LABEL -> {
                    val token = AccessToken.getCurrentAccessToken()
                    if (token == null || token.isExpired) {
                        expiredSession()
                    }
                }
                LoginActivity.VKONTAKTE_LABEL -> {
                    val vkAccessTokenTracker = object : VKAccessTokenTracker() {
                        override fun onVKAccessTokenChanged(oldToken: VKAccessToken?, newToken: VKAccessToken?) {
                            if (newToken == null) {
                                expiredSession()
                            }
                        }
                    }
                    vkAccessTokenTracker.startTracking()
                }
                null -> {
                    mainActivityPresentation?.startLoginIntent()
                }
            }
        }
    }

    fun initializeLogoutBtn(){
        if (connectivityUtils.hasConnection(context?.applicationContext)) {
            when (socialNetworkLabel) {
                LoginActivity.GOOGLE_LABEL -> {
                    Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback {

                    }
                }
                LoginActivity.FACEBOOK_LABEL -> {
                    LoginManager.getInstance().logOut()
                }
                LoginActivity.VKONTAKTE_LABEL -> {
                    VKSdk.logout()
                }
                null -> {
                    showToast(context?.getString(R.string.logout_null_error)!!)
                }
                else -> {
                    showToast(context?.getString(R.string.logout_null_error)!!)
                }
            }
            restoreNetworkDB(socialNetworkLabel, userPhotoUrl, userName)
            mainActivityPresentation?.startLoginIntent()
        } else {
            showToast(context?.getString(R.string.no_internet_connection)!!)
        }
    }

    fun expiredSession(){
        showToast(context?.getString(R.string.session_token_expired) ?: "Session token expired")
        mainActivityPresentation?.startLoginIntent()
    }


    fun onDestroyPresenter(){
        compositeDisposable.dispose()
        mainActivityPresentation = null
        context = null
    }

}