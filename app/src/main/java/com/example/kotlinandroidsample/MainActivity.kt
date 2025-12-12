package com.example.kotlinandroidsample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var rvDemoList: RecyclerView
    private lateinit var demoAdapter: DemoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        rvDemoList = findViewById(R.id.rv_demo_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDemoList()
    }

    private fun setupDemoList() {
        val demoList = listOf(
            DemoItem(
                title = getString(R.string.demo_lifecycle_scope_title),
                description = getString(R.string.demo_lifecycle_scope_description),
                targetActivity = LifecycleScopeActivity::class
            )
        )

        demoAdapter = DemoAdapter(demoList)
        rvDemoList.adapter = demoAdapter
    }
}
