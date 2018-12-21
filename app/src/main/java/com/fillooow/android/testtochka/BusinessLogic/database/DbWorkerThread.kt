package com.fillooow.android.testtochka.BusinessLogic.database

import android.os.Handler
import android.os.HandlerThread

class DbWorkerThread(threadName: String) : HandlerThread(threadName) {

    // private lateinit var mWorkerHandler: Handler
    private var mWorkerHandler = Handler()

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        mWorkerHandler = Handler(looper)
    }

    fun postTask(task: Runnable) {
        mWorkerHandler.post(task)
    }

}