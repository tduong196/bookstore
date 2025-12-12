package com.example.testdkdn

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy
//import com.cloudinary.android.uploadstrategy.UploadRequest
//import com.cloudinary.utils.ErrorInfo

object CloudinaryManager {
    private var initialized = false

    // 🛠 Khởi tạo Cloudinary (chỉ gọi 1 lần khi app chạy)
    fun initCloudinary(context: Context) {
        if (!initialized) {
            val config = mapOf(
                "cloud_name" to "dujmhnsee",  // Thay bằng cloud_name của bạn
                "api_key" to "469854791381627", // Thay bằng api_key của bạn
                "api_secret" to "9DHTsnT0dmoMuJbJrrmoSfSAs2k" // Thay bằng api_secret của bạn
            )
            MediaManager.init(context, config)
            initialized = true
            Log.d("Cloudinary", "✅ Cloudinary đã được khởi tạo!")
        }
    }

    // 📤 Hàm tải ảnh lên Cloudinary
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
