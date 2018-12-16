package com.fillooow.android.testtochka

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fillooow.android.testtochka.UI.TestActivity
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