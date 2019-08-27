package com.wdy.camera.activity

import android.graphics.Color
import android.view.View
import com.github.clans.fab.FloatingActionButton
import com.wdy.camera.R
import com.wdy.camera.constant.enums.FabSortItem
import com.wdy.camera.constant.preferences.UserPreferences
import com.wdy.camera.utils.ColorUtils
import kotlinx.android.synthetic.main.activity_main.*
import luyao.util.ktx.base.BaseActivity
import luyao.util.ktx.ext.startKtxActivity

class MainActivity : BaseActivity() {
    override fun getLayoutResId() = R.layout.activity_main

    private lateinit var fabs: Array<FloatingActionButton>
    override fun initView() {
        setSupportActionBar(toolbar)
        initFloatButtons()
        configFabSortItems()

    }

    override fun initData() {
        println("initData")
    }

    private fun initFloatButtons() {
        menu.setOnMenuToggleListener { opened -> rl_menu_container.visibility = if (opened) View.VISIBLE else View.GONE }
        rl_menu_container.setOnClickListener { menu.close(true) }
        rl_menu_container.setBackgroundResource(R.color.menu_container_dark)
        fabs = arrayOf(fab0, fab1)
        for (i in fabs.indices) {
            fabs[i].colorNormal = R.style.LightThemeBlue
            fabs[i].colorPressed = R.style.LightThemeBlue
            fabs[i].setOnClickListener { resolveFabClick(i) }
        }
    }

    private fun resolveFabClick(index: Int) {
        menu.close(true)
        val fabSortItem = UserPreferences.getInstance().fabSortResult[index]
        when (fabSortItem) {
            FabSortItem.CAMERA -> {
                startKtxActivity<CameraActivity>()
            }

            FabSortItem.SCAN -> {
                startKtxActivity<ManualDCScannerActivity>()
            }
        }
    }

    private fun configFabSortItems() {
        try {
            val fabSortItems = UserPreferences.getInstance().fabSortResult
            for (i in fabs.indices) {
                fabs[i].setImageDrawable(ColorUtils.tintDrawable(this, fabSortItems[i].iconRes, Color.WHITE))
                fabs[i].labelText = getString(fabSortItems[i].nameRes)
            }
        } catch (e: Exception) {
        }

    }

}