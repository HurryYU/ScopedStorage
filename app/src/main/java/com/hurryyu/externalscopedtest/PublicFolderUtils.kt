package com.hurryyu.externalscopedtest

import android.os.Build
import android.os.Environment

/**
 * ===================================================================
 * Author: HurryYu http://www.hurryyu.com & https://github.com/HurryYU
 * Email: cqbbyzh@gmial.com or 1037914505@qq.com
 * Time: 2020/4/16
 * Version: 1.0
 * Description:
 * ===================================================================
 */
const val APP_FOLDER_NAME = "ExternalScopeTestApp"

fun getAppPicturePath(): String {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // full path
        "${Environment.getExternalStorageDirectory().absolutePath}/" +
                "${Environment.DIRECTORY_PICTURES}/$APP_FOLDER_NAME/"
    } else {
        // relative path
        "${Environment.DIRECTORY_PICTURES}/$APP_FOLDER_NAME/"
    }
}

fun getAppDownloadPath():String = "${Environment.DIRECTORY_DOWNLOADS}/$APP_FOLDER_NAME/"
