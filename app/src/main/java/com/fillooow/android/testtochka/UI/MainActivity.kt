package com.fillooow.android.testtochka.UI

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.TestFacebookActivity
import com.fillooow.android.testtochka.TestGoogleActivity
import com.fillooow.android.testtochka.TestVkActivity
import com.fillooow.android.testtochka.network.GithubApiService
import com.fillooow.android.testtochka.network.model.UserSearchModel
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        val GITHUB_TAG = "GITHUB_TAG"
    }

    val githubApiService by lazy {
        GithubApiService.create()
    }

    var disposable: Disposable? = null
    var disposableET: Disposable? = null

    private var itemsList = ArrayList<UserSearchModel.Items>()

    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var rvUsers: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.total_found, "0")

        initialiseDrawer()
        itemsList = ArrayList()

        rvUsers = findViewById(R.id.rvTest)
        rvUsers.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        getItems("biba")
        userAdapter = UserSearchAdapter(itemsList)
        rvUsers.adapter = userAdapter

        disposableET = RxTextView.afterTextChangeEvents(inputET)
            .debounce(600, TimeUnit.MILLISECONDS)
                //TODO: не выход, потому что надо обрабатывать пустые строки
            //.filter {
             //  inputET.text.toString() != ""
            //}
            .subscribe{
                //if (inputET.text.toString() != "")
                    getItems(inputET.text.toString())
                //else
                //    supportActionBar?.title = getString(R.string.empty_request)

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
                    val intent = Intent(this, TestActivity::class.java)
                    startActivity(intent)
                    drawer_layout.closeDrawers()
                }
            }
            true
        }
    }

    // gets users from Github Open API
    fun getItems(searchText: String){

        if (searchText == "") run {
            runOnUiThread {
                supportActionBar?.title = getString(R.string.empty_request)
            }
        } else {

            itemsList.clear()
            disposable = githubApiService
                //TODO: pages
                .searchUser(searchText, 1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {

                    supportActionBar?.title = getString(R.string.total_found, it.total_count.toString())
                    //Log.d(GITHUB_TAG, it.total_count.toString())
                    for (item in it.items) {
                        itemsList.add(item)
                    }
                    userAdapter.notifyDataSetChanged()
                }
        }

    }
}
