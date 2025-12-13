package com.bookstore.ui.book


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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.bookstore.data.model.Book
import com.google.firebase.firestore.FirebaseFirestore

class AddBookActivity : ComponentActivity() {

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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "Thêm Sách Mới",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Image Section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUri),
                                        contentDescription = "Ảnh đã chọn",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                            contentDescription = "Add Image",
                                            modifier = Modifier.size(48.dp),
                                            tint = Color(0xFFB0BEC5)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Chọn ảnh bìa",
                                            color = Color(0xFF90A4AE),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF546E7A)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Choose Image",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (imageUri != null) "Thay đổi ảnh" else "Chọn ảnh")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Input Fields
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Tên sách") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF546E7A),
                                unfocusedBorderColor = Color(0xFFCFD8DC)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = { Text("Tác giả") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF546E7A),
                                unfocusedBorderColor = Color(0xFFCFD8DC)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Thể loại") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF546E7A),
                                unfocusedBorderColor = Color(0xFFCFD8DC)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = if (price == 0.0) "" else price.toString(),
                            onValueChange = { price = it.toDoubleOrNull() ?: 0.0 },
                            label = { Text("Giá (VNĐ)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF546E7A),
                                unfocusedBorderColor = Color(0xFFCFD8DC)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Mô tả") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF546E7A),
                                unfocusedBorderColor = Color(0xFFCFD8DC)
                            ),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Save Button
                        Button(
                            onClick = {
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
                                    Toast.makeText(
                                        this@AddBookActivity,
                                        "Vui lòng chọn ảnh và nhập đầy đủ thông tin sách",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF546E7A)
                            )
                        ) {
                            Text(
                                "Lưu Sách",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
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
                    Toast.makeText(this@AddBookActivity, "Lỗi khi upload ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
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
