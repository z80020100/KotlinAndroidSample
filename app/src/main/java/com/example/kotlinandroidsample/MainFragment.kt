package com.example.kotlinandroidsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class MainFragment : Fragment() {

    private lateinit var rvDemoList: RecyclerView
    private lateinit var demoAdapter: DemoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvDemoList = view.findViewById(R.id.rv_demo_list)

        setupDemoList()
    }

    private fun setupDemoList() {
        val demoList = listOf(
            DemoItem(
                title = getString(R.string.demo_lifecycle_scope_title),
                description = getString(R.string.demo_lifecycle_scope_description),
                navigationActionId = R.id.action_mainFragment_to_lifecycleScopeFragment
            )
        )

        demoAdapter = DemoAdapter(demoList)
        rvDemoList.adapter = demoAdapter
    }
}
