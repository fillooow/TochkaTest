package com.fillooow.android.testtochka.ui

import android.util.Log
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkData
import com.fillooow.android.testtochka.BusinessLogic.database.SocialNetwork.SocialNetworkDataDao
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class SocialNetworkPresenter @Inject constructor(private val socialNetworkDataDao: SocialNetworkDataDao){

    val compositeDisposable = CompositeDisposable()
    var socialNetworkPresentation: SocialNetworkPresentation? = null

    fun initSocialNetwork(socNetPresentation: SocialNetworkPresentation){
        socialNetworkPresentation = socNetPresentation
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
                    socialNetworkPresentation?.setSocialNetworkResults(t)
                }

                override fun onError(e: Throwable) {
                    if (e.message?.contains("Query returned empty result")!!){
                        socialNetworkPresentation?.errorEmptyResult()
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

    fun onDestroySocialNetworkPresenter(){
        compositeDisposable.dispose()
        socialNetworkPresentation = null
    }


}