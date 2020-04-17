package com.hurryyu.externalscopedtest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * ===================================================================
 * Author: HurryYu http://www.hurryyu.com & https://github.com/HurryYU
 * Email: cqbbyzh@gmial.com or 1037914505@qq.com
 * Time: 2020/4/16
 * Version: 1.0
 * Description:
 * ===================================================================
 */
class PictureAdapter(
    private val context: Context,
    private val onClick: (PictureActivity.ImageBean, Int) -> Unit
) : RecyclerView.Adapter<PictureAdapter.ViewHolder>() {

    private val dataList = mutableListOf<PictureActivity.ImageBean>()

    class ViewHolder(itemView: View, onClick: (PictureActivity.ImageBean, Int) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        val ivPicture: ImageView = itemView.findViewById(R.id.ivPicture)
        val tvDisplayName: TextView = itemView.findViewById(R.id.tvPictureDisplayName)

        init {
            itemView.setOnClickListener {
                val image = itemView.tag as? PictureActivity.ImageBean ?: return@setOnClickListener
                onClick(image, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_picture,
                parent,
                false
            ), onClick
        )
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageBean = dataList[position]
        holder.itemView.tag = imageBean
        holder.tvDisplayName.text = imageBean.displayName
        Glide.with(context).load(imageBean.uri).into(holder.ivPicture)

//        val openFileDescriptor = context.contentResolver.openFileDescriptor(imageBean.uri, "r")
//        openFileDescriptor?.apply {
//            val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
//            holder.ivPicture.setImageBitmap(bitmap)
//        }
//        openFileDescriptor?.close()
    }

    fun setNewData(dataList: List<PictureActivity.ImageBean>) {
        this.dataList.clear()
        this.dataList.addAll(dataList)
        notifyDataSetChanged()
    }

    fun deletePosition(position: Int) {
        if (position < 0) {
            return
        }
        dataList.removeAt(position)
        notifyItemRemoved(position)
    }
}