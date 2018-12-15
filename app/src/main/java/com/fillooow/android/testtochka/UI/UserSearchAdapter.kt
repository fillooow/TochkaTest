package com.fillooow.android.testtochka.UI

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fillooow.android.testtochka.R
import com.fillooow.android.testtochka.network.model.UserSearchModel

class UserSearchAdapter (var itemsList: ArrayList<UserSearchModel.Items>)
    : RecyclerView.Adapter<UserSearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTest = itemView.findViewById<TextView>(R.id.testTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_test, parent, false)
        return SearchViewHolder(v)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        var item: UserSearchModel.Items = itemsList[position]
        holder.tvTest.text = item.login
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    fun setSearchResult(items: ArrayList<UserSearchModel.Items>){
        itemsList = items
        notifyDataSetChanged()
    }

}