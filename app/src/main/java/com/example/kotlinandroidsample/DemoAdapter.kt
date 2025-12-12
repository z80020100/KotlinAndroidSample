package com.example.kotlinandroidsample

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DemoAdapter(
    private val demoList: List<DemoItem>
) : RecyclerView.Adapter<DemoAdapter.DemoViewHolder>() {

    inner class DemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)

        fun bind(demoItem: DemoItem) {
            tvTitle.text = demoItem.title
            tvDescription.text = demoItem.description

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, demoItem.targetActivity.java)
                itemView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_demo, parent, false)
        return DemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
        holder.bind(demoList[position])
    }

    override fun getItemCount(): Int = demoList.size
}
