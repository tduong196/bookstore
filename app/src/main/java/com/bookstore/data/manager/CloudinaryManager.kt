package com.bookstore.data.manager

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import androidx.core.net.toUri

object CloudinaryManager {
    private var initialized = false

    // Khởi tạo Cloudinary (chỉ gọi 1 lần khi app chạy)
    fun initCloudinary(context: Context) {
        if (!initialized) {
            try {
                // Sử dụng CLOUDINARY_URL từ BuildConfig
                val cloudinaryUrl = com.bookstore.BuildConfig.CLOUDINARY_URL

                // Parse URL to extract cloud_name, api_key, api_secret
                val uri = cloudinaryUrl.toUri()
                val cloudName = uri.host
                val apiKey = uri.userInfo?.split(":")?.get(0)
                val apiSecret = uri.userInfo?.split(":")?.get(1)

                val config = mapOf(
                    "cloud_name" to cloudName,
                    "api_key" to apiKey,
                    "api_secret" to apiSecret
                )

                MediaManager.init(context, config)
                initialized = true
                Log.d("Cloudinary", "✅ Cloudinary đã được khởi tạo!")
            } catch (e: Exception) {
                Log.e("Cloudinary", "❌ Lỗi khởi tạo Cloudinary: ${e.message}")
            }
        }
    }

    // Hàm tải ảnh lên Cloudinary
    fun uploadImage(filePath: String, callback: (String?) -> Unit) {
        MediaManager.get().upload(filePath)
            .option("folder", "notes_images") // 📂 Lưu ảnh vào thư mục notes_images
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "🔄 Bắt đầu upload ảnh...")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val imageUrl = resultData?.get("url") as? String
                    Log.d("Cloudinary", "✅ Upload thành công: $imageUrl")
                    callback(imageUrl) // Trả về URL ảnh
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "❌ Lỗi upload: ${error?.description}")
                    callback(null)
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }
}
