package com.fillooow.android.testtochka.ui

import android.util.Log
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchData
import com.fillooow.android.testtochka.BusinessLogic.database.UserSearch.UiInfoUserSearchDataDao
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class UiInfoUserSearchPresenter @Inject constructor(private val uiInfoUserSearchDataDao: UiInfoUserSearchDataDao){

    val compositeDisposable = CompositeDisposable()
    var uiInfoUserSearchPresentation: UiInfoUserSearchPresentation? = null

    fun initUiInfoUserSearch(uiInfUserSearchPresentation: UiInfoUserSearchPresentation){
        uiInfoUserSearchPresentation = uiInfUserSearchPresentation
    }

    fun loadUiInfo(){
            uiInfoUserSearchDataDao.getRecord().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UiInfoUserSearchData> {
                override fun onSuccess(t: UiInfoUserSearchData) {
                    uiInfoUserSearchPresentation?.setSuccessfulLoadedUiResults(t.lastSearchTextDB,
                        t.isNextBtnEnabledDB,
                        t.isPrevBtnEnabledDB,
                        t.totalPagesDB,
                        t.currentPageDB,
                        t.totalCountDB,
                        t.hasBtnClickedDB)
                    //inputET.setText(lastSearchText)
                    //updateUI()
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {
                    Log.d("Error", "Load UI info error $e")
                }

            })
}

    fun saveUiInfo(lastSearchText: String?, isNextBtnEnabled: Boolean, isPrevBtnEnabled: Boolean,
                   totalPages: Int, currentPage: Int, totalCount: Int, hasBtnClicked: Boolean){
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

    fun restoreUiInfo(lastSearchText: String?, isNextBtnEnabled: Boolean, isPrevBtnEnabled: Boolean,
                              totalPages: Int, currentPage: Int, totalCount: Int, hasBtnClicked: Boolean){
        Completable.fromAction {
            uiInfoUserSearchDataDao.deleteAll()
        }.subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver{
                override fun onComplete() {
                    saveUiInfo(lastSearchText, isNextBtnEnabled, isPrevBtnEnabled,
                    totalPages, currentPage, totalCount, hasBtnClicked)
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(e: Throwable) {}

            })
    }

    fun onDestroyUiInfoUserSearchPresenter(){
        compositeDisposable.dispose()
        uiInfoUserSearchPresentation = null
    }
}