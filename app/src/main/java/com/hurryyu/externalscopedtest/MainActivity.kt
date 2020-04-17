package com.hurryyu.externalscopedtest

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.hurryyu.externalscopedtest.databinding.ActivityMainBinding
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    companion object {
        const val REQUEST_PERMISSION = 0
        const val REQUEST_OPEN_PDF = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        updatePermissionState()

        viewBinding.btnRequestPermission.setOnClickListener {
            if (checkStorePermission(this)) {
                Toast.makeText(this, "已经拥有了存储权限", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSION
            )
        }

        viewBinding.btnAddImage.setOnClickListener {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.die)
            val displayName = "${System.currentTimeMillis()}.jpg"
            val mimeType = "image/jpeg"
            val compressFormat = Bitmap.CompressFormat.JPEG

            saveBitmapToPicturePublicFolder(bitmap, displayName, mimeType, compressFormat)
        }

        viewBinding.btnQueryImage.setOnClickListener {
            startActivity(Intent(this, PictureActivity::class.java))
        }

        viewBinding.btnSelectPdf.setOnClickListener {
            selectPdfUseSAF()
        }

        viewBinding.btnDownloadFile.setOnClickListener {
            downloadApkAndInstall(
                "https://down.qq.com/qqweb/QQ_1/android_apk/Android_8.3.3.4515_537063791.apk",
                "test_qq.apk"
            )
        }
    }

    private fun downloadApkAndInstall(fileUrl: String, apkName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "请使用原始方式", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "开始下载...", Toast.LENGTH_SHORT).show()
        thread {
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val inputStream = connection.inputStream
                val bis = BufferedInputStream(inputStream)
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, apkName)
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, getAppDownloadPath())
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.also {
                    val outputStream = contentResolver.openOutputStream(uri) ?: return@thread
                    val bos = BufferedOutputStream(outputStream)
                    val buffer = ByteArray(1024)
                    var bytes = bis.read(buffer)
                    while (bytes >= 0) {
                        bos.write(buffer, 0, bytes)
                        bos.flush()
                        bytes = bis.read(buffer)
                    }
                    bos.close()
                    runOnUiThread {
                        installAPK(uri)
                    }
                }
                bis.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun installAPK(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        startActivity(intent)
    }

    private fun selectPdfUseSAF() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            // 我们需要使用ContentResolver.openFileDescriptor读取数据
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_OPEN_PDF)
    }

    private fun saveBitmapToPicturePublicFolder(
        bitmap: Bitmap,
        displayName: String,
        mimeType: String,
        compressFormat: Bitmap.CompressFormat
    ) {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        val path = getAppPicturePath()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path)
        } else {
            val fileDir = File(path)
            if (!fileDir.exists()) {
                fileDir.mkdir()
            }
            contentValues.put(MediaStore.MediaColumns.DATA, path + displayName)
        }
        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.also {
            val outputStream = contentResolver.openOutputStream(it)
            outputStream?.also { os ->
                bitmap.compress(compressFormat, 100, os)
                os.close()
                Toast.makeText(this, "添加图片成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePermissionState() {
        viewBinding.tvReadExternalStorageState.text = if (checkStorePermission(this)) {
            "是"
        } else {
            "否"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "必须同意所有权限", Toast.LENGTH_SHORT).show()
                }
            }
            updatePermissionState()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OPEN_PDF -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.also { documentUri ->
                        val fileDescriptor =
                            contentResolver.openFileDescriptor(documentUri, "r") ?: return
                        // 现在,我们可以使用PdfRenderer等类通过fileDescriptor读取pdf内容
                        Toast.makeText(this, "pdf读取成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

}
