package com.fillooow.android.testtochka.UI

import android.content.Context
import android.content.res.Resources
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.network.model.UserSearchModel
import kotlinx.android.synthetic.main.github_user_info_card.view.*

class UserSearchAdapter (var itemsList: ArrayList<UserSearchModel.Items>, val context: Context)
    : RecyclerView.Adapter<UserSearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGithubUserLogin = itemView.findViewById<TextView>(R.id.tvGithubUserLogin)
        val tvGithubUserId = itemView.findViewById<TextView>(R.id.tvGithubUserId)
        val tvGithubUserType = itemView.findViewById<TextView>(R.id.tvGithubUserType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        var v = LayoutInflater.from(parent.context).inflate(R.layout.github_user_info_card, parent, false)
        return SearchViewHolder(v)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val cardView = holder.itemView
        val item: UserSearchModel.Items = itemsList[position]

        cardView.tvGithubUserLogin.text = context.getString(R.string.user_login, item.login)
        cardView.tvGithubUserId.text = context.getString(R.string.user_id, item.id.toString())
        cardView.tvGithubUserType.text = context.getString(R.string.user_type, item.type)
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    fun setSearchResult(items: ArrayList<UserSearchModel.Items>){
        itemsList = items
        notifyDataSetChanged()
    }

}