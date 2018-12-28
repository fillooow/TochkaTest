package com.fillooow.android.testtochka.ui

import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.GithubUserSearchDataDao
import com.fillooow.android.testtochka.BusinessLogic.network.model.UserSearchModel
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class GithubUserSearchPresenter @Inject constructor(private val githubUserSearchDataDao: GithubUserSearchDataDao){

    val compositeDisposable = CompositeDisposable()
    var githubUserSearchPresentation: GithubUserSearchPresentation? = null

    fun initGithubUserSearch(ghUserSearchPresentation: GithubUserSearchPresentation){
        githubUserSearchPresentation = ghUserSearchPresentation
    }

    fun loadResponseList(items: ArrayList<UserSearchModel.Items>){
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
                    //githubUserSearchPresentation?.setSuccessfulLoadedItemsList(items)
                    items.clear()
                    if (t.isNotEmpty()) {
                        for (item in t) {
                            items.add(
                                UserSearchModel.Items(
                                    item.login,
                                    item.githubID,
                                    item.type,
                                    item.avatarUrl
                                )
                            )
                        }
                        // TODO: придумать что делать с методом, вообще, пусть возвращает ArrayList и будет проверка на
                        // его пустоту. Если пуст, вызываем  нижнюю ветку else

                        //userAdapter.notifyDataSetChanged()
                    } else {
                        //loadUserItems(lastSearchText ?: inputET.text.toString(), currentPage)
                    }
                    githubUserSearchPresentation?.setSuccessfulLoadedItemsList(items)

                }
            })
    }

    fun saveResponseList(items: ArrayList<UserSearchModel.Items>){
        for (item in items){
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

    fun restoreResponseList(items: ArrayList<UserSearchModel.Items>){
        Completable.fromAction {
            githubUserSearchDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveResponseList(items)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {

                }

            })
    }

    fun onDestroyGhUserSearchPresenter(){
        compositeDisposable.dispose()
        githubUserSearchPresentation = null
    }
}