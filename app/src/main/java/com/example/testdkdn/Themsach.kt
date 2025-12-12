package com.example.testdkdn


import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.testdkdn.Book
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class ThemSachActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var imageUri by remember { mutableStateOf<Uri?>(null) }
            var title by remember { mutableStateOf("") }
            var author by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("") }
            var rating by remember { mutableStateOf(0.0) }
            var imageUrl by remember { mutableStateOf("") }
            var price by remember { mutableStateOf(0.0) }

            // Launcher for image selection
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                imageUri = uri
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    imagePickerLauncher.launch("image/*")
                }) {
                    Text(text = "Chọn ảnh")
                }

                imageUri?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = "Ảnh đã chọn",
                        modifier = Modifier.height(200.dp).fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nhập thông tin sách
                TextField(value = title, onValueChange = { title = it }, label = { Text("Tên sách") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = price.toString(),
                    onValueChange = { price = it.toDoubleOrNull() ?: 0.0 },
                    label = { Text("Giá") }
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = author, onValueChange = { author = it }, label = { Text("Tác giả") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = description, onValueChange = { description = it }, label = { Text("Mô tả") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = category, onValueChange = { category = it }, label = { Text("Thể loại") })
                Spacer(modifier = Modifier.height(8.dp))
//                TextField(
//                    value = rating.toString(),
//                    onValueChange = { rating = it.toDoubleOrNull() ?: 0.0 },
//                    label = { Text("Đánh giá") }
//                )


                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    if (imageUri != null && title.isNotEmpty() && author.isNotEmpty()) {
                        uploadImageToCloudinary(imageUri!!) { url ->
                            imageUrl = url
                            val book = Book(
                                title = title,
                                author = author,
                                description = description,
                                category = category,
                                rating = rating,
                                image_url = imageUrl,
                                price = price
                            )
                            saveBookToFirestore(book)
                        }
                    } else {
                        Toast.makeText(this@ThemSachActivity, "Vui lòng chọn ảnh và nhập đầy đủ thông tin sách", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Lưu sách")
                }
            }
        }
    }

    private fun uploadImageToCloudinary(uri: Uri, onSuccess: (String) -> Unit) {
        // Tải ảnh lên Cloudinary và lấy link
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageUrl = uploadImage(uri)
                withContext(Dispatchers.Main) {
                    onSuccess(imageUrl)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ThemSachActivity, "Lỗi khi upload ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadImage(uri: Uri): String {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri)

        if (inputStream == null) {
            throw Exception("Không thể mở InputStream từ URI.")
        }

        val requestBody = inputStream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
        val cloudName = "dujmhnsee" // Thay bằng cloud name của bạn
        val uploadPreset = "my_unsigned_preset" // Thay bằng preset của bạn

        val part = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("file", "image.jpg", requestBody)
                .build())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (response.isSuccessful && responseBody != null) {
            val jsonResponse = JSONObject(responseBody)
            val imageUrl = jsonResponse.getString("secure_url")
            return imageUrl
        } else {
            throw Exception("Lỗi khi upload ảnh lên Cloudinary: ${response.code}")
        }
    }

    private fun saveBookToFirestore(book: Book) {
        db.collection("books")
            .add(book)
            .addOnSuccessListener {
                Toast.makeText(this, "Thêm sách thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi thêm sách: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
