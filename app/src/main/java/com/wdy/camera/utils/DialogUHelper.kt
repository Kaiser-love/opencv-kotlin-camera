package com.wdy.camera.utils

import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View

import com.kongzue.dialog.listener.OnMenuItemClickListener
import com.kongzue.dialog.v2.BottomMenu
import com.kongzue.dialog.v2.Pop
import com.qmuiteam.qmui.widget.dialog.QMUIDialog

import com.kongzue.dialog.v2.Pop.COLOR_TYPE_ERROR
import com.kongzue.dialog.v2.Pop.SHOW_UP

/**
 * @author dongyang_wu
 * @date 2019/7/28 01:39
 */
object DialogUHelper {

    fun shopTips(context: Context, view: View, content: String, dismissTime: Long?) {
        val pop = Pop.show(context, view, content, SHOW_UP, COLOR_TYPE_ERROR)
        Handler().postDelayed({ pop.dismiss() }, dismissTime!!)
    }

    fun shopSingleCheckableDialog(context: Context, items: Array<String>, index: Int, callback: DialogInterface.OnClickListener) {
        QMUIDialog.CheckableDialogBuilder(context)
                .setCheckedIndex(index)
                .addItems(items, callback)
                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()
    }

    fun shopBottomSheets(context: Context, items: List<String>, callback: OnMenuItemClickListener) {
        BottomMenu.show(context as AppCompatActivity, items, callback, true)
        //        BottomMenu.show((AppCompatActivity) context, items, new OnMenuItemClickListener() {
        //            @Override
        //            public void onClick(String text, int index) {
        //                System.out.println(index);
        //            }
        //        }, true);
    }
}
