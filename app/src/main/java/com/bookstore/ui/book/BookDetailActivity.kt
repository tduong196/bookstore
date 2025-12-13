package com.bookstore.ui.book

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import com.bookstore.data.manager.CartManager
import com.bookstore.data.model.Book
import com.bookstore.data.model.Comment
import com.bookstore.ui.theme.BookstoreTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore


class BookDetailActivity : ComponentActivity() {
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
            BookstoreTheme {
                BookDetailScreen(
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
fun BookDetailScreen(
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
    val reviewList = remember { mutableStateListOf<com.bookstore.data.model.Review>() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val userEmail = currentUser?.email ?: "Người dùng ẩn danh"
    var userRole by remember { mutableStateOf<Int?>(null) }
    var bookId by remember { mutableStateOf("") }

    // Tìm bookId từ title
    LaunchedEffect(title) {
        db.collection("books")
            .whereEqualTo("title", title)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    bookId = snapshot.documents[0].id

                    // Load reviews cho sách này (không cần duyệt)
                    db.collection("reviews")
                        .whereEqualTo("bookId", bookId)
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { reviewSnapshot, _ ->
                            reviewSnapshot?.let {
                                val reviews = it.documents.mapNotNull { doc ->
                                    doc.toObject(com.bookstore.data.model.Review::class.java)?.copy(id = doc.id)
                                }
                                reviewList.clear()
                                reviewList.addAll(reviews)
                            }
                        }
                }
            }
    }

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
    ) {
        // Header với ảnh và thông tin cơ bản
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Thể loại", fontSize = 12.sp, color = Color.Gray)
                    Text(text = category, fontSize = 14.sp, color = Color(0xFF023E8A), fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Đánh giá", fontSize = 12.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$rating", fontSize = 14.sp, color = Color(0xFF023E8A), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$formattedPrice VNĐ",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFd00000)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
        }

        // TabRow
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = Color(0xFF0077B6)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        "Chi tiết",
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        "Đánh giá (${reviewList.size})",
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> DetailTab(
                description = description,
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                commentList = commentList,
                userRole = userRole,
                userEmail = userEmail,
                title = title,
                db = db,
                context = context,
                scrollState = scrollState
            )
            1 -> ReviewTab(
                reviewList = reviewList,
                scrollState = rememberScrollState()
            )
        }

    }
}

@Composable
fun DetailTab(
    description: String,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    commentList: List<Comment>,
    userRole: Int?,
    userEmail: String,
    title: String,
    db: com.google.firebase.firestore.FirebaseFirestore,
    context: android.content.Context,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Mô tả sản phẩm
        Text(
            text = "Mô tả sản phẩm",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF023E8A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            color = Color.DarkGray,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Phần bình luận
        Text(
            text = "Bình luận",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF023E8A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = commentText,
            onValueChange = onCommentTextChange,
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

                onCommentTextChange("")
            },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Gửi", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Danh sách bình luận
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commentList.forEach { comment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
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
                            val time = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(it))
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

@Composable
fun ReviewTab(
    reviewList: List<com.bookstore.data.model.Review>,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        if (reviewList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Chưa có đánh giá nào",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            Text(
                text = "${reviewList.size} đánh giá",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF023E8A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                reviewList.forEach { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = review.userName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF212121)
                                    )
                                    Text(
                                        text = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                            .format(java.util.Date(review.timestamp)),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { index ->
                                        Icon(
                                            imageVector = if (index < review.rating.toInt())
                                                androidx.compose.material.icons.Icons.Filled.Star
                                            else
                                                androidx.compose.material.icons.Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = if (index < review.rating.toInt()) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = review.comment,
                                fontSize = 14.sp,
                                color = Color(0xFF424242),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

