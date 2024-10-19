package com.gfms.koinoor.ui.activities.base

import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination


abstract class BaseActivity {
    protected fun hideSoftKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    protected fun onDestinationChanged(context: Context, view: View) {
        hideSoftKeyboard(context, view)
    }
}