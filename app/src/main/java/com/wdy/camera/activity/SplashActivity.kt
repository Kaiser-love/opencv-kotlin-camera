package com.wdy.camera.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler

import com.hjq.permissions.OnPermission
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.wdy.camera.R


class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        XXPermissions.with(this)
                .permission(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE, Permission.CAMERA)
                .request(object : OnPermission {

                    override fun hasPermission(granted: List<String>, isAll: Boolean) {
                        Handler().postDelayed(
                                {
                                    val i = Intent(this@SplashActivity, MainActivity::class.java)
                                    startActivity(i)
                                    // close this activity
                                    finish()
                                }, SPLASH_TIME_OUT.toLong())
                    }

                    override fun noPermission(denied: List<String>, quick: Boolean) {

                    }
                })
    }

    companion object {
        // Splash screen timer
        private const val SPLASH_TIME_OUT = 1800
    }


}