package com.fillooow.android.testtochka

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fillooow.android.testtochka.UI.TestActivity
import com.google.gson.JsonParser
import com.vk.sdk.api.*
import kotlinx.android.synthetic.main.fragment_vk_login.*

class VkLoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vk_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var v = activity as TestActivity
        vk_sign_in_button.setOnClickListener{
            v.signInVk()
        }
        vk_log_out_button.setOnClickListener{
            v.logOutVk()
        }
    }

}


/*
Log.d("VkTAG", "OnComplete ${response?.parsedModel.toString()}")
var usersArray = response?.parsedModel as VKUsersArray
Log.d("VkTAG", "Response: ${response.json}")
for (userFull in usersArray){
    Log.d("VkTAG", "URL = ${userFull.photo_100}")
} */


// https://bitbucket.org/NeuraDev/kotlin-vs-sdk-sample/src/a327a3e97f49cfcb4086ef65e9fd6d1ed425fc8a/app/src/main/java/ru/agaldkov/kotlin_vk_sdk_template/providers/FriendsProvider.kt?at=master&fileviewer=file-view-default
// TODO
fun getUserInfo(){
    var vkRequest = VKApi.users().get(VKParameters.from(VKApiConst.COUNT, 16, VKApiConst.FIELDS, "sex"))
    vkRequest.executeWithListener(object : VKRequest.VKRequestListener(){
        override fun onComplete(response: VKResponse?) {
            super.onComplete(response)

            val jsonParser = JsonParser()
            val parsedJson = jsonParser.parse(response?.json.toString()).asJsonObject
            val friendsList: ArrayList<FriendModel> = ArrayList()

            parsedJson.get("response").asJsonArray.forEach {
                Log.d("VkTAG", "first name: ${it.asJsonObject.get("first_name").asString}")
                //Log.d("VkTAG", "second name: ${it.asJsonObject.get("second_name").asString}")
                Log.d("VkTAG", "last name: ${it.asJsonObject.get("last_name").asString}")
                Log.d("VkTAG", "id: ${it.asJsonObject.get("id").asString}")
            }

        }

        override fun onError(error: VKError?) {
            super.onError(error)
            Log.d("VkTAG", "Error")
        }

        override fun onProgress(progressType: VKRequest.VKProgressType?, bytesLoaded: Long, bytesTotal: Long) {
            super.onProgress(progressType, bytesLoaded, bytesTotal)
            Log.d("VkTAG", "Progress")
        }

        override fun attemptFailed(request: VKRequest?, attemptNumber: Int, totalAttempts: Int) {
            super.attemptFailed(request, attemptNumber, totalAttempts)
            Log.d("VkTag", "Failed")
        }
    })
}

//onActivityResult
/*if (requestCode == VKServiceActivity.VKServiceType.Authorization.outerCode) {

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
        } else {
             super.onActivityResult(requestCode, resultCode, data)

             if (requestCode == GOOGLE_RC_SIGN_IN){
                var result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

                 Log.d("GOOOGLE", "start activityonresult")
                handleSignInResult(result)
             }
             if (FacebookSdk.isFacebookRequestCode(requestCode)) {
                 Log.d("FACEEBOOK", "request code: $requestCode, and FB code: ${FacebookSdk.getCallbackRequestCodeOffset()}")
                callbackManager?.onActivityResult(requestCode, resultCode, data)
             }
        }*/