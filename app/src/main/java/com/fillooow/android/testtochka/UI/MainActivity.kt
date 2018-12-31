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
import com.fillooow.android.testtochka.R
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainActivityPresentation {

    companion object {
        const val ELEMENTS_PER_PAGE = 30 // Количество отображаемых на странице элементов
        private const val MAX_ELEMENTS = 1000 // Для OpenAPI гитхаба, можно получить лишь первую 1000 результатов.
        const val MAX_PAGE = (MAX_ELEMENTS/ELEMENTS_PER_PAGE) + 1 // Определяем максимально доступную
        // страницу исходя из количества доступных результатов и отображаемых элементов

        // Request code for loginIntent at onActivityResult()
        private const val RC_LOGIN = 9991
    }

    @Inject lateinit var mainActivityPresenter: MainActivityPresenter

    private var compositeDisposable = CompositeDisposable()

    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var rvUsers: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as BaseApp).appComponent.inject(this)

        mainActivityPresenter.initInterfaces(this)
        mainActivityPresenter.loadSocialNetworkLabel()

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

        mainActivityPresenter.setRxEditTextListener(inputET)
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
                }
            }
            true
        }
    }

    override fun setButtonsState(isNextEnables: Boolean, isPrevEnabled: Boolean) {
        nextPageButton.isEnabled = isNextEnables
        previousPageButton.isEnabled = isPrevEnabled
    }

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

    override fun setUserProfile(username: String?){
        drawer_user_name.text = username
        mainActivityPresenter.setUserPhoto(drawer_user_photo)
    }

    override fun startLoginIntent(){
        val intent = Intent(this, LoginActivity::class.java)
        drawer_layout.closeDrawers()
        startActivityForResult(intent, RC_LOGIN)
    }

    override fun setSuccessfulLoadedUiResults(lastSearchTextDB: String?){
        inputET.setText(lastSearchTextDB)
    }

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