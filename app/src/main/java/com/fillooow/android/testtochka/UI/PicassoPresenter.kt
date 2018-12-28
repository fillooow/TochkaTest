package com.fillooow.android.testtochka.ui

import android.util.Log
import android.widget.ImageView
import com.fillooow.android.testtochka.R
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import javax.inject.Inject

class PicassoPresenter @Inject constructor(){

    fun setUserPhoto(url: String?, imageView: ImageView){
        // Google+ may return "null" as an answer, if user don't have profile picture
        Picasso.get()
            .load(url)
            .error(R.drawable.default_user_profile_image_png_5)
            .networkPolicy(NetworkPolicy.OFFLINE)
            .into(imageView, object : Callback {
                override fun onSuccess() {

                }

                override fun onError(e: Exception?) {
                    Picasso.get()
                        .load(url)
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

}