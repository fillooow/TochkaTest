package com.fillooow.android.testtochka.UI

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginResult
import com.fillooow.android.testtochka.R
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.vk.sdk.*
import com.vk.sdk.api.VKError
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    // Request codes for onActivityResult()
    val GOOGLE_RC_SIGN_IN: Int = 9001
    val FB_RC_SIGN_IN: Int = FacebookSdk.getCallbackRequestCodeOffset()
    val VK_RC_SIGN_IN: Int = VKServiceActivity.VKServiceType.Authorization.outerCode


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
        when(requestCode){
            GOOGLE_RC_SIGN_IN -> {
                super.onActivityResult(requestCode, resultCode, data)
                val result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                handleSignInResult(result)
            }
            FB_RC_SIGN_IN -> {
                super.onActivityResult(requestCode, resultCode, data)
                callbackManager?.onActivityResult(requestCode, resultCode, data)
            }
            VK_RC_SIGN_IN -> {
                if (!VKSdk.onActivityResult(requestCode, resultCode, data, object : VKCallback<VKAccessToken> {
                        override fun onResult(res: VKAccessToken?) {
                            Log.d("VkTAG", "${res?.accessToken}")
                            //getUserPhoto()
                        }

                        override fun onError(error: VKError?) {
                            Log.d("VkTAG", "error: ${error.toString()}")
                        }
                    }))
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }


    fun initializeLoginButtons(){
        initializeGoogleButton()
        initializeFBButton()
        initializeVkButton()
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
        var signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, GOOGLE_RC_SIGN_IN)
    }

    fun handleSignInResult(signInResult: GoogleSignInResult){
        if (signInResult.isSuccess){
            val account : GoogleSignInAccount? = signInResult.signInAccount
            Toast.makeText(this, "Test ${account?.photoUrl}", Toast.LENGTH_LONG).show()
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

    fun signInFB(){
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
    }

    fun getUserInfoFb(result: LoginResult){
        // Log.d("FacebookTag", "Facebook token ${result.accessToken.token}")
        val request = GraphRequest.newMeRequest(result.accessToken){ `object`, response ->
            Log.d("FacebookTag", response.jsonObject.get("id").toString())
            Log.d("FacebookTag", response.jsonObject.toString())
            // https://graph.facebook.com/" + userId + "/picture?width=500&height=500
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
        VKSdk.login(this,
            VKScope.PHOTOS,
            VKScope.EMAIL,
            VKScope.FRIENDS)
    }

}
