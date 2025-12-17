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

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                imageUri = uri
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F7F4))   // nền dịu hơn
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    /* ===== HEADER ===== */
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFDFCFB),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = "Thêm sách mới",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3F5D4A),
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        /* ===== IMAGE CARD ===== */
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFCFB)),
                            elevation = CardDefaults.cardElevation(6.dp)
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
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color(0xFF5B7F6A)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Chọn ảnh bìa",
                                            color = Color(0xFF5B7F6A),
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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF5B7F6A)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (imageUri != null) "Thay đổi ảnh" else "Chọn ảnh")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        /* ===== INPUTS (GIỮ NGUYÊN BIẾN) ===== */
                        inputField("Tên sách", title) { title = it }
                        inputField("Tác giả", author) { author = it }
                        inputField("Thể loại", category) { category = it }

                        inputField(
                            label = "Giá (VNĐ)",
                            value = if (price == 0.0) "" else price.toString(),
                            keyboardType = KeyboardType.Number
                        ) { price = it.toDoubleOrNull() ?: 0.0 }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Mô tả") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5B7F6A),
                                unfocusedBorderColor = Color(0xFF5B7F6A).copy(alpha = 0.4f),
                                focusedLabelColor = Color(0xFF5B7F6A),
                                cursorColor = Color(0xFF5B7F6A)
                            ),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        /* ===== SAVE BUTTON ===== */
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
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5B7F6A)
                            )
                        ) {
                            Text(
                                "LƯU SÁCH",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    /* ================= LOGIC GIỮ NGUYÊN ================= */

    private fun uploadImageToCloudinary(uri: Uri, onSuccess: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageUrl = uploadImage(uri)
                withContext(Dispatchers.Main) { onSuccess(imageUrl) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddBookActivity, "Lỗi khi upload ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadImage(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("Không thể mở InputStream")

        val requestBody = inputStream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
        val cloudName = "dujmhnsee"
        val uploadPreset = "my_unsigned_preset"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart("file", "image.jpg", requestBody)
                    .build()
            )
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string()

        if (response.isSuccessful && body != null) {
            return JSONObject(body).getString("secure_url")
        } else {
            throw Exception("Upload thất bại")
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

/* ================= UI HELPER (KHÔNG ĐỘNG LOGIC) ================= */

@Composable
private fun inputField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(14.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF5B7F6A),
            unfocusedBorderColor = Color(0xFF5B7F6A).copy(alpha = 0.4f),
            focusedLabelColor = Color(0xFF5B7F6A),
            cursorColor = Color(0xFF5B7F6A)
        )
    )
}
