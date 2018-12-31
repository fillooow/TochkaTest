package com.fillooow.android.testtochka.ui

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginResult
import com.fillooow.android.testtochka.BusinessLogic.network.ConnectivityUtils
import com.fillooow.android.testtochka.R
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.gson.JsonParser
import com.vk.sdk.*
import com.vk.sdk.api.*
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    companion object {
        const val EXTRA_LOGIN_USER_NAME = "com.fillooow.android.testtochka.loginactivity.user_name"
        const val EXTRA_LOGIN_USER_PHOTO_URL = "com.fillooow.android.testtochka.loginactivity.user_photo_url"
        const val EXTRA_LOGIN_SOCIAL_NETWORK_LABEL = "com.fillooow.android.testtochka.loginactivity.social_network_name"
        const val GOOGLE_LABEL: String = "Google"
        const val FACEBOOK_LABEL: String = "Facebook"
        const val VKONTAKTE_LABEL: String = "VKontakte"
        // Request codes for onActivityResult()
        const val GOOGLE_RC_SIGN_IN: Int = 9001
        val FB_RC_SIGN_IN: Int = FacebookSdk.getCallbackRequestCodeOffset()
        val VK_RC_SIGN_IN: Int = VKServiceActivity.VKServiceType.Authorization.outerCode
    }

    private var userName: String? = null
    private var userPhotoUrl: String? = null
    private var connectivityUtils = ConnectivityUtils()
    var socialNetworkLabel: String? = null

    // google
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions
    lateinit var mGoogleApiClient: GoogleApiClient

    // Facebook
    private var callbackManager: CallbackManager? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeLoginButtons()
    }

    // VKSdk подразумевает вызов super после проверок, а не до, как у остальных sdk.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show()
            return
        }

        when(requestCode){
            GOOGLE_RC_SIGN_IN -> {
                super.onActivityResult(requestCode, resultCode, data)
                val result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                handleSignInResult(result)
                intentSetResult()
            }
            FB_RC_SIGN_IN -> {
                super.onActivityResult(requestCode, resultCode, data)
                callbackManager?.onActivityResult(requestCode, resultCode, data)
            }
            VK_RC_SIGN_IN -> {
                if (!VKSdk.onActivityResult(requestCode, resultCode, data, object : VKCallback<VKAccessToken> {
                        override fun onResult(res: VKAccessToken?) {
                            //Log.d("VkTAG", "${res?.accessToken}")
                            getUserInfoVk()
                        }

                        override fun onError(error: VKError?) {
                            Log.d("VkTAG", "error: ${error.toString()}")
                        }
                    }))
                super.onActivityResult(requestCode, resultCode, data)
            }
        }


    }

    private fun initializeLoginButtons(){
        initializeGoogleButton()
        initializeFBButton()
        initializeVkButton()
    }

    fun setNameAndUrl(name: String?, url: String?){
        userName = name
        userPhotoUrl = url
    }

    fun intentSetResult() {
        val loginData = Intent()
        loginData.putExtra(EXTRA_LOGIN_USER_NAME, userName)
        loginData.putExtra(EXTRA_LOGIN_USER_PHOTO_URL, userPhotoUrl)
        loginData.putExtra(EXTRA_LOGIN_SOCIAL_NETWORK_LABEL, socialNetworkLabel)
        setResult(Activity.RESULT_OK, loginData)
        finish()
    }

    private fun showToast(toastText: String){
        runOnUiThread {
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show()
        }
    }

    // google

    private fun initializeGoogleButton() {
        initializeGoogleServices()

        signInGoogleButton.setSize(SignInButton.SIZE_STANDARD)
        signInGoogleButton.setColorScheme(SignInButton.COLOR_LIGHT)
        signInGoogleButton.setOnClickListener {
            signInGoogle()
        }
    }

    private fun initializeGoogleServices() {
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
    }

    private fun signInGoogle() {
        if (connectivityUtils.hasConnection(applicationContext)) {
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
            startActivityForResult(signInIntent, GOOGLE_RC_SIGN_IN)
        } else {
            showToast(getString(R.string.no_internet_connection))
        }
    }

    private fun handleSignInResult(signInResult: GoogleSignInResult){
        if (signInResult.isSuccess){
            val account : GoogleSignInAccount? = signInResult.signInAccount
            val name = account?.displayName
            val url = account?.photoUrl.toString()
            socialNetworkLabel = GOOGLE_LABEL
            setNameAndUrl(name, url)
        } else{
            Toast.makeText(this, "Error occurred" , Toast.LENGTH_LONG).show()
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d("Google", "Google connection failed")
    }


    // facebook

    private fun initializeFBButton() {
        callbackManager = CallbackManager.Factory.create()
        signInFacebookButton.setReadPermissions("public_profile")
        signInFB()
    }

    private fun signInFB(){
        if (connectivityUtils.hasConnection(applicationContext)) {
            signInFacebookButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    getUserInfoFb(result)
                }

                override fun onCancel() {
                    Log.d("FacebookTag", "Facebook onCancel")
                }

                override fun onError(error: FacebookException?) {
                    Log.d("FacebookTag", "Facebook onError: $error")
                }
            })
        } else {
            showToast(getString(R.string.no_internet_connection))
        }
    }

    fun getUserInfoFb(result: LoginResult){
        val request = GraphRequest.newMeRequest(result.accessToken){ `object`, response ->
            Log.d("FacebookTag", "ID: ${response.jsonObject.get("id")}")
            val name = response.jsonObject.get("name").toString()
            val url = "https://graph.facebook.com/${response.jsonObject.get("id")}/picture?width=200&height=200"
            socialNetworkLabel = FACEBOOK_LABEL
            setNameAndUrl(name, url)
            intentSetResult()
        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name")
        request.parameters = parameters
        request.executeAsync()
    }

    // vk

    private fun initializeVkButton() {
        signInVkButton.setOnClickListener {
            signInVk()
        }
    }

    fun signInVk(){
        if (connectivityUtils.hasConnection(applicationContext)) {
            VKSdk.login(
                this,
                VKScope.PHOTOS,
                VKScope.EMAIL
            )
        } else {
            showToast(getString(R.string.no_internet_connection))
        }
    }

    fun getUserInfoVk(){
        val vkRequest = VKApi.users().get(VKParameters.from(VKApiConst.FIELDS, "photo_200"))
        vkRequest.executeWithListener(object : VKRequest.VKRequestListener(){
            override fun onComplete(response: VKResponse?) {
                super.onComplete(response)

                val jsonParser = JsonParser()
                val parsedJson = jsonParser.parse(response?.json.toString()).asJsonObject

                parsedJson.get("response").asJsonArray.forEach {
                    val name = "${it.asJsonObject.get("first_name").asString} ${it.asJsonObject.get("last_name").asString}"
                    val url = it.asJsonObject.get("photo_200").asString
                    setNameAndUrl(name, url)
                }
                socialNetworkLabel = VKONTAKTE_LABEL
                intentSetResult()
            }

            override fun onError(error: VKError?) {
                super.onError(error)
                Log.d("VkTAG", "Error")
            }
        })
    }

}
