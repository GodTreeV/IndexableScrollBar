package com.aceyan.indexablescrollbar

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.graphics.drawable.StateListDrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.aceyan.scrollbar.IndexableScrollBar
import com.aceyan.scrollbar.addFastScroller
import java.util.Arrays

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var indexableScrollBar: IndexableScrollBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recyclerView)
        indexableScrollBar = findViewById(R.id.scrollbar)

        recyclerView.adapter = MyAdapter()

        //indexableScrollBar.attachToRecyclerView(recyclerView)
        val verticalThumbDrawable = StateListDrawable()
        verticalThumbDrawable.addState(intArrayOf(android.R.attr.state_pressed), getDrawable(R.drawable.ic_launcher_background))
        verticalThumbDrawable.addState(intArrayOf(android.R.attr.state_pressed), getDrawable(R.drawable.ic_launcher_foreground))
        verticalThumbDrawable.addState(intArrayOf(), ColorDrawable(Color.RED))

        recyclerView.addFastScroller(
            verticalThumbDrawable,
            ColorDrawable(Color.BLUE),
            verticalThumbDrawable,
            ColorDrawable(Color.BLUE)
        )
    }

    inner class MyAdapter : RecyclerView.Adapter<MyAdapter.MyVH>() {
        inner class MyVH(v: View) : RecyclerView.ViewHolder(v)

        val data = Array<String>(50) { it.toString() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyVH {
            return MyVH(
                LayoutInflater.from(parent.context).inflate(R.layout.layout_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: MyVH, position: Int) {
            holder.itemView.findViewById<TextView>(R.id.textView).text = data[position]
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
}