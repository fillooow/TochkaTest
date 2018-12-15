package com.fillooow.android.testtochka.UI

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.network.GithubApiService
import com.fillooow.android.testtochka.network.model.UserSearchModel
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

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

        //supportActionBar?.title = "Test"
        initialiseDrawer()
        itemsList = ArrayList()

        rvUsers = findViewById(R.id.rvTest)
        rvUsers.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        getItems("biba")
        userAdapter = UserSearchAdapter(itemsList)
        rvUsers.adapter = userAdapter

        disposableET = RxTextView.afterTextChangeEvents(inputET)
            .debounce(600, TimeUnit.MILLISECONDS)
            .filter {
                inputET.text.toString() != ""
            }
            .subscribe{
                getItems(inputET.text.toString())
                Log.d("TAG", "subscribed $it")
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
                R.id.action_cut -> {
                    Toast.makeText(this, "Biba boba", Toast.LENGTH_LONG).show()
                }
            }
            true
        }
    }

    fun getItems(searchText: String){
        itemsList.clear()
        disposable = githubApiService
            .searchUser(searchText, 1)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                for (item in it.items) {
                    itemsList.add(item)
                    Log.d("TAG", item.login)
                }
                userAdapter.notifyDataSetChanged()
            }
    }
}
