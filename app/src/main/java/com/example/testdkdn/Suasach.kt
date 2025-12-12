package com.example.testdkdn

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.testdkdn.ui.theme.TestDKDNTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SuaSachActivity : ComponentActivity() {
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
        Log.d("SuaSach", "docId = $documentId")

        setContent {
            TestDKDNTheme {
                SuaSachScreen(
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
fun SuaSachScreen(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentScale = ContentScale.Fit
        )

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tên sách") })
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Tác giả") })
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Thể loại") })
        Spacer(modifier = Modifier.height(8.dp))

        if (isPriceLoading) {
            // Nếu đang tải giá thì hiển thị loading nhỏ
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            OutlinedTextField(
                value = price.toString(),
                onValueChange = { newVal -> price = newVal.toDoubleOrNull() ?: price },
                label = { Text("Giá") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Mô tả") },
            modifier = Modifier.height(120.dp),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Lưu chỉnh sửa", fontSize = 16.sp, color = Color.White)
        }
    }
}




