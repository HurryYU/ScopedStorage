package com.hurryyu.externalscopedtest

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hurryyu.externalscopedtest.databinding.ActivityPictureBinding

class PictureActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_DELETE_PERMISSION = 1
    }

    private lateinit var viewBinding: ActivityPictureBinding
    private val imageList: MutableList<ImageBean> by lazy { mutableListOf<ImageBean>() }

    private var pendingDeleteImageUri: Uri? = null
    private var pendingDeletePosition: Int = -1

    private val pictureAdapter: PictureAdapter by lazy {
        PictureAdapter(this) { imageBean, position ->
            MaterialAlertDialogBuilder(this)
                .setTitle("确定删除？")
                .setMessage(getString(R.string.delete_dialog_message, imageBean.displayName))
                .setPositiveButton(R.string.delete_dialog_positive) { _, _ ->
                    deleteImage(imageBean.uri, position)
                }
                .setNegativeButton(R.string.delete_dialog_negative) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.rvPicture.apply {
            layoutManager = GridLayoutManager(this@PictureActivity, 2)
            adapter = pictureAdapter
        }

        viewBinding.checkboxShowAllPicture.setOnCheckedChangeListener { checkView, isChecked ->
            if (isChecked && !checkStorePermission(this)) {
                checkView.isChecked = false
                Toast.makeText(this, "请前往主页开启权限", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            loadPictures(isChecked)
        }

        loadPictures(false)
    }

    private fun loadPictures(queryAll: Boolean) {
        imageList.clear()
        imageList.addAll(queryImages(queryAll))
        pictureAdapter.setNewData(imageList)
    }

    private fun queryImages(queryAll: Boolean = false): List<ImageBean> {
        var pathKey = ""
        var pathValue = ""
        if (!queryAll) {
            pathKey = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                MediaStore.MediaColumns.DATA
            } else {
                MediaStore.MediaColumns.RELATIVE_PATH
            }
            // RELATIVE_PATH会在路径的最后自动添加/
            pathValue = getAppPicturePath()
        }
        val dataList = mutableListOf<ImageBean>()
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            if (pathKey.isEmpty()) {
                null
            } else {
                "$pathKey LIKE ?"
            },
            if (pathValue.isEmpty()) {
                null
            } else {
                arrayOf("%$pathValue%")
            },
            "${MediaStore.MediaColumns.DATE_ADDED} desc"
        )

        cursor?.also {
            while (it.moveToNext()) {
                val id = it.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val displayName =
                    it.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                val uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                dataList.add(ImageBean(id, uri, displayName))
            }
        }
        cursor?.close()
        return dataList
    }

    private fun deleteImage(imageUri: Uri, adapterPosition: Int) {
        var row = 0
        try {
            // Android 10+中,如果删除的是其它应用的Uri,则需要用户授权
            // 会抛出RecoverableSecurityException异常
            row = contentResolver.delete(imageUri, null, null)
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException
                pendingDeleteImageUri = imageUri
                pendingDeletePosition = adapterPosition
                // 我们可以使用IntentSender向用户发起授权
                requestRemovePermission(recoverableSecurityException.userAction.actionIntent.intentSender)
            } else {
                throw securityException
            }
        }

        if (row > 0) {
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
            pictureAdapter.deletePosition(adapterPosition)
        }
    }

    private fun requestRemovePermission(intentSender: IntentSender) {
        startIntentSenderForResult(
            intentSender, REQUEST_DELETE_PERMISSION,
            null, 0, 0, 0, null
        )
    }

    private fun deletePendingImageUri() {
        pendingDeleteImageUri?.let {
            pendingDeleteImageUri = null
            deleteImage(it, pendingDeletePosition)
            pendingDeletePosition = -1
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK &&
            requestCode == REQUEST_DELETE_PERMISSION
        ) {
            // 执行之前的删除逻辑
            deletePendingImageUri()
        }
    }

    data class ImageBean(val id: Long, val uri: Uri, val displayName: String)
}
