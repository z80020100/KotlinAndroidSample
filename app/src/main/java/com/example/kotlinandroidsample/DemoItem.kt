package com.example.kotlinandroidsample

import android.app.Activity
import kotlin.reflect.KClass

data class DemoItem(
    val title: String,
    val description: String,
    val targetActivity: KClass<out Activity>
)
