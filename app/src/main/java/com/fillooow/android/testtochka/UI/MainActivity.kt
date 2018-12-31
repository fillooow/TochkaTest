package com.fillooow.android.testtochka.ui

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.fillooow.android.testtochka.BaseApp
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.network.ConnectivityUtils
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainActivityPresentation {

    companion object {
        const val ELEMENTS_PER_PAGE = 30 // Количество отображаемых на странице элементов
        private const val MAX_ELEMENTS = 1000 // Для OpenAPI гитхаба, можно получить лишь первую 1000 результатов.
        const val MAX_PAGE = (MAX_ELEMENTS/ELEMENTS_PER_PAGE) + 1 // Определяем максимально доступную
        // страницу исходя из количества доступных результатов и отображаемых элементов

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

    private var connectivityUtils = ConnectivityUtils()

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

    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var rvUsers: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as BaseApp).appComponent.inject(this)

        mainActivityPresenter.initInterfaces(this)
        mainActivityPresenter.loadSocialNetworkLabel()

        //currentPageBeforeChanging = currentPage

        setSupportActionBar(toolbar)
        initialiseDrawer()
        rvUsers = findViewById(R.id.rvMain)
        rvUsers.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        userAdapter = UserSearchAdapter(mainActivityPresenter.itemsList, applicationContext)
        rvUsers.adapter = userAdapter

        nextPageButton.setOnClickListener {
            mainActivityPresenter.loadNextPage(inputET.text.toString())
        }
        previousPageButton.setOnClickListener {
            mainActivityPresenter.loadPrevPage(inputET.text.toString())
        }

        // TODO:
        //restoreInstanceState(savedInstanceState)

        mainActivityPresenter.setRxEditTextListener(inputET)

        /*compositeDisposable.add(
            RxTextView.afterTextChangeEvents(inputET)
                .debounce(600, TimeUnit.MILLISECONDS)
                .subscribe{
                    if(connectivityUtils.hasConnection(applicationContext)) {
                        hasBtnClicked = false
                        var searchText = inputET.text.toString()
                        searchText = removeSpaces(searchText)
                        if ((lastSearchText == "") && (searchText == "")){
                            setSupportActionBarTitle(getString(R.string.empty_request))
                        } else if (searchText != lastSearchText){
                            checkBeforeLoadingUserItems(searchText, currentPage)
                        }
                    } else {
                        mainActivityPresenter.showToast(getString(R.string.no_internet_connection))
                    }
                }
        )*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK){
            finish() // Если была нажата кнопка Назад в LoginActivity или результатом пришла неудача
            return
        }
        if(requestCode == RC_LOGIN){
            mainActivityPresenter.setOnActivityResultData(
                data?.getStringExtra(LoginActivity.EXTRA_LOGIN_SOCIAL_NETWORK_LABEL),
                data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_NAME),
                data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_PHOTO_URL)
            )
            /*socialNetworkLabel = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_SOCIAL_NETWORK_LABEL)
            userName = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_NAME)//.toString()
            userPhotoUrl = data?.getStringExtra(LoginActivity.EXTRA_LOGIN_USER_PHOTO_URL)*/
            //setUserProfile()
            //mainActivityPresenter.restoreNetworkDB(socialNetworkLabel, userPhotoUrl, userName)
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
                    mainActivityPresenter.initializeLogoutBtn()
                    //socialNetworkLabel = null
                }
            }
            true
        }
    }

    override fun setButtonsState(isNextEnables: Boolean, isPrevEnabled: Boolean) {
        nextPageButton.isEnabled = isNextEnables
        previousPageButton.isEnabled = isPrevEnabled
    }

    /*private fun loadNextPage(){
        hasBtnClicked = true
        currentPageBeforeChanging = currentPage
        currentPage++
        checkBeforeLoadingUserItems(inputET.text.toString(), currentPage)
    }*/

    /*private fun loadPrevPage(){
        hasBtnClicked = true
        currentPageBeforeChanging = currentPage
        currentPage--
        checkBeforeLoadingUserItems(inputET.text.toString(), currentPage)
    }*/

    /*override fun updateItems(items: ArrayList<UserSearchModel.Items>) {
        for (item in items){
            itemsList.add(item)
        }
    }*/

    /*override fun onErrorLoadItems() {
        currentPage = currentPageBeforeChanging
    }*/

    /*fun checkBeforeLoadingUserItems(searchText: String, page: Int){
        itemsList.clear()
        if (searchText == ""){
            totalPages = 0
            currentPage = 0
            updateUI()
            setSupportActionBarTitle(getString(R.string.empty_request))
        } else {
            mainActivityPresenter.loadUserItems(searchText, page)
        }
        lastSearchText = searchText
    }*/

    override fun updateUI(currentPage: Int,totalPages: Int, totalCount: Int){
        mainActivityPresenter.setUpCurrentPage()
        setSupportActionBarTitle(getString(R.string.total_found, totalCount))
        pageCounterTV.text = getString(R.string.page_counter, currentPage, totalPages)
        userAdapter.notifyDataSetChanged()
        mainActivityPresenter.restoreUiInfo()
        mainActivityPresenter.restoreResponseList()
    }

    override fun setSupportActionBarTitle(barTitle: String) {
        runOnUiThread{
            supportActionBar?.title = barTitle
        }
    }

    /*private fun setUpCurrentPage() {
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
        mainActivityPresenter.setUpSearchButtons(currentPage, totalPages)
    }*/

    override fun setUserProfile(username: String?){
        drawer_user_name.text = username
        /*// google+ may return "null" as an answer, if user don't have profile picture
        if (userPhotoUrl == "null"){
            userPhotoUrl = "https://www.scirra.com/images/articles/windows-8-user-account.jpg"
        }*/
        mainActivityPresenter.setUserPhoto(drawer_user_photo)
    }

    override fun startLoginIntent(){
        val intent = Intent(this, LoginActivity::class.java)
        drawer_layout.closeDrawers()
        startActivityForResult(intent, RC_LOGIN)
    }

    // TODO:
    /*override fun onSaveInstanceState(outState: Bundle?) {
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
    }*/

    override fun setSuccessfulLoadedUiResults(lastSearchTextDB: String?){
        inputET.setText(lastSearchTextDB)
    }

    /*override fun setSuccessfulLoadedItemsList(items: ArrayList<UserSearchModel.Items>, editTextString: String) {
        if (items.isEmpty()){
            checkBeforeLoadingUserItems(lastSearchText ?: editTextString, currentPage)
        } else {
            itemsList = items
            userAdapter.notifyDataSetChanged()
        }
    }*/

    /*override fun setSocialNetworkResults(t: SocialNetworkData) {
        // TODO: загнать в переменные метода, чтобы не плодить 3 строки лишних (лул)
        /*socialNetworkLabel = t.label
        userName = t.username
        userPhotoUrl = t.photoURL*/
        setUserProfile()
        //mainActivityPresenter.checkSocialNetworkTokenState(socialNetworkLabel)
    }*/

    override fun onStart() {
        super.onStart()
        mainActivityPresenter.loadUiInfo()
        mainActivityPresenter.loadResponseList(inputET.text.toString())
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