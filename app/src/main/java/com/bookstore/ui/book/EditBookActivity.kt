package com.bookstore.ui.book

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bookstore.ui.theme.BookstoreTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditBookActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentId = intent.getStringExtra("docId") ?: ""
        val imageUrl = intent.getStringExtra("image_url") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val author = intent.getStringExtra("author") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val rating = intent.getStringExtra("rating") ?: ""
        Log.d("EditBook", "docId = $documentId")

        setContent {
            BookstoreTheme {
                EditBookScreen(
                    imageUrl = imageUrl,
                    initTitle = title,
                    initAuthor = author,
                    initDescription = description,
                    initCategory = category,
                    initRating = rating,
                    documentId = documentId,
                    onUpdateSuccess = {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onUpdateFail = {
                        Toast.makeText(this, "Lỗi khi cập nhật!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun EditBookScreen(
    imageUrl: String,
    initTitle: String,
    initAuthor: String,
    initDescription: String,
    initCategory: String,
    initRating: String,
    documentId: String,
    onUpdateSuccess: () -> Unit,
    onUpdateFail: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Các biến trạng thái các trường bình thường
    var title by remember { mutableStateOf(initTitle) }
    var author by remember { mutableStateOf(initAuthor) }
    var description by remember { mutableStateOf(initDescription) }
    var category by remember { mutableStateOf(initCategory) }
    var rating by remember { mutableStateOf(initRating) }

    // Biến trạng thái riêng cho price, khởi tạo tạm 0.0
    var price by remember { mutableStateOf(0.0) }
    var isPriceLoading by remember { mutableStateOf(true) }

    // Lấy giá price từ Firestore theo documentId khi compose bắt đầu
    LaunchedEffect(documentId) {
        isPriceLoading = true
        db.collection("books").document(documentId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    price = doc.getDouble("price") ?: 0.0
                }
                isPriceLoading = false
            }
            .addOnFailureListener {
                isPriceLoading = false
                Toast.makeText(context, "Lỗi tải giá!", Toast.LENGTH_SHORT).show()
            }
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
                    text = "Chỉnh Sửa Sách",
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
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
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

                if (isPriceLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF546E7A)
                    )
                } else {
                    OutlinedTextField(
                        value = if (price == 0.0) "" else price.toString(),
                        onValueChange = { newVal -> price = newVal.toDoubleOrNull() ?: price },
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
                }

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
                        val ratingValue = try {
                            rating.toDouble()
                        } catch (e: NumberFormatException) {
                            0.0
                        }

                        val updatedBook = hashMapOf(
                            "title" to title,
                            "author" to author,
                            "description" to description,
                            "category" to category,
                            "rating" to ratingValue,
                            "image_url" to imageUrl,
                            "price" to price
                        )

                        db.collection("books")
                            .document(documentId)
                            .set(updatedBook, SetOptions.merge())
                            .addOnSuccessListener { onUpdateSuccess() }
                            .addOnFailureListener { onUpdateFail() }
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
                        "Lưu Chỉnh Sửa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}




