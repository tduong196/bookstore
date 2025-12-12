package com.example.testdkdn

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.testdkdn.ui.theme.TestDKDNTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore


class ChitietActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUrl = intent.getStringExtra("image_url") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val author = intent.getStringExtra("author") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val formattedPrice = intent.getStringExtra("price") ?: ""
        val rating = intent.getDoubleExtra("rating", 0.0)


        setContent {
            TestDKDNTheme {
                ChitietScreen(
                    imageUrl = imageUrl,
                    title = title,
                    author = author,
                    description = description,
                    category = category,
                    rating = rating,
                    formattedPrice = formattedPrice
                )
            }
        }
    }
}

@Composable
fun ChitietScreen(
    imageUrl: String,
    title: String,
    author: String,
    description: String,
    category: String,
    rating: Double,
    formattedPrice: String,
) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val scrollState = rememberScrollState()
    var commentText by remember { mutableStateOf("") }
    val commentList = remember { mutableStateListOf<Comment>() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val userEmail = currentUser?.email ?: "Người dùng ẩn danh"
    var userRole by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(userEmail) {
        if (userEmail != "Người dùng ẩn danh") {
            Firebase.firestore.collection("users")
                .document(userEmail)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userRole = document.getLong("role")?.toInt()
                    }
                }
        }
    }



    // Load comments from Firestore on first load
    DisposableEffect(title) {
        val listener = db.collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let {
                    val updatedComments = it.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)
                    }.filter { it.bookTitle.trim().equals(title.trim(), ignoreCase = true) }

                    commentList.clear()
                    commentList.addAll(updatedComments)
                }
            }


        onDispose {
            listener.remove() // Hủy listener khi composable biến mất
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF023E8A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = "Tác giả: $author", fontSize = 16.sp, color = Color(0xFF0077B6))

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Thể loại: $category", fontSize = 14.sp, color = Color(0xFF023E8A))

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Đánh giá: $rating ★", fontSize = 14.sp, color = Color(0xFF0077B6))

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Giá: $formattedPrice VNĐ",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFd00000)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            color = Color.DarkGray,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val book = Book(
                    id = "$title$author",
                    title = title,
                    author = author,
                    description = description,
                    category = category,
                    rating = rating,
                    image_url = imageUrl,
                    price = formattedPrice.toDoubleOrNull() ?: 0.0
                )
                CartManager.addToCart(context, book)
                Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Thêm vào giỏ hàng", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Bình luận",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF023E8A),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            label = { Text("Nhập bình luận") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))


            Button(
                onClick = {
                    val comment = Comment(
                        bookTitle = title,
                        content = commentText,
                        timestamp = System.currentTimeMillis(),
                        userEmail = userEmail
                    )
                    db.collection("comments")
                        .add(comment)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Bình luận đã gửi", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Lỗi gửi bình luận", Toast.LENGTH_SHORT).show()
                        }

                    commentText = ""
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Gửi", color = Color.White)
            }


        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commentList.forEach { comment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = comment.userEmail,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0077B6)
                            )


                            if (userRole == 2) {
                                TextButton(onClick = {
                                    // Xoá comment từ Firestore
                                    db.collection("comments")
                                        .whereEqualTo("bookTitle", comment.bookTitle)
                                        .whereEqualTo("content", comment.content)
                                        .whereEqualTo("timestamp", comment.timestamp)
                                        .whereEqualTo("userEmail", comment.userEmail)
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            for (document in documents) {
                                                db.collection("comments").document(document.id).delete()
                                            }
                                            Toast.makeText(context, "Đã xoá bình luận", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Lỗi khi xoá", Toast.LENGTH_SHORT).show()
                                        }
                                }) {
                                    Text("Xoá", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = comment.content,
                            fontSize = 14.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        comment.timestamp.takeIf { it > 0 }?.let {
                            val time = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(it))
                            Text(
                                text = time,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                }
            }
        }

    }
}
