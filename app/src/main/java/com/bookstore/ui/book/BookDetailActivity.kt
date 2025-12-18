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
import androidx.compose.material.icons.Icons
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
import com.bookstore.data.model.Review
import com.bookstore.ui.theme.BookstoreTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

/* ================= UI COLORS (HIỂN THỊ ONLY) ================= */
private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val PriceColor = Color(0xFFC62828)
private val StarActive = Color(0xFFFFC107)
private val StarInactive = Color(0xFFE0E0E0)

class BookDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUrl = intent.getStringExtra("image_url") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val author = intent.getStringExtra("author") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val price = intent.getDoubleExtra("price", 0.0)
        val rating = intent.getDoubleExtra("rating", 0.0)

        setContent {
            BookstoreTheme {
                BookDetailScreen(
                    imageUrl,
                    title,
                    author,
                    description,
                    category,
                    rating,
                    price
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
    price: Double
) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val commentList = remember { mutableStateListOf<Comment>() }
    val reviewList = remember { mutableStateListOf<Review>() }
    var commentText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var bookId by remember { mutableStateOf("") }

    /* ================= GIỮ NGUYÊN LOGIC ================= */

    LaunchedEffect(title) {
        db.collection("books")
            .whereEqualTo("title", title)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    bookId = snapshot.documents[0].id

                    db.collection("reviews")
                        .whereEqualTo("bookId", bookId)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .addSnapshotListener { rs, _ ->
                            rs?.let {
                                reviewList.clear()
                                reviewList.addAll(
                                    it.documents.mapNotNull { d ->
                                        d.toObject(Review::class.java)
                                    }
                                )
                            }
                        }
                }
            }
    }

    DisposableEffect(title) {
        val listener = db.collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    commentList.clear()
                    commentList.addAll(
                        it.documents.mapNotNull { d ->
                            d.toObject(Comment::class.java)
                        }.filter { c ->
                            c.bookTitle.equals(title, ignoreCase = true)
                        }
                    )
                }
            }
        onDispose { listener.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {

        /* ================= HEADER ================= */
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardColor),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                Text("Tác giả: $author", color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            if (i < rating.toInt()) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (i < rating.toInt()) StarActive else StarInactive
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("%.2f".format(rating))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${price.toInt()} VNĐ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriceColor
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            price = price
                        )
                        CartManager.addToCart(context, book)
                        Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Thêm vào giỏ hàng", fontWeight = FontWeight.Bold)
                }
            }
        }

        /* ================= TAB ================= */
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardColor,
            contentColor = GreenPrimary
        ) {
            Tab(selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chi tiết") })
            Tab(selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Đánh giá") })
        }

        when (selectedTab) {
            0 -> DetailTab(
                description,
                commentText,
                { commentText = it },
                commentList,
                currentUser?.email ?: "Ẩn danh",
                title,
                db
            )

            1 -> ReviewTab(reviewList)
        }
    }
}

/* ================= TAB CHI TIẾT ================= */
@Composable
fun DetailTab(
    description: String,
    commentText: String,
    onCommentChange: (String) -> Unit,
    commentList: List<Comment>,
    userEmail: String,
    bookTitle: String,
    db: com.google.firebase.firestore.FirebaseFirestore
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text("Mô tả", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(description)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Bình luận", fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = commentText,
            onValueChange = onCommentChange,
            placeholder = { Text("Viết bình luận...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (commentText.isNotBlank()) {
                    val comment = Comment(
                        bookTitle = bookTitle,
                        userEmail = userEmail,
                        content = commentText,
                        timestamp = System.currentTimeMillis()
                    )
                    db.collection("comments").add(comment)
                    onCommentChange("")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text("Gửi")
        }

        Spacer(modifier = Modifier.height(16.dp))

        commentList.forEach { c ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(c.userEmail, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(c.content)
                }
            }
        }
    }
}

/* ================= TAB ĐÁNH GIÁ ================= */
@Composable
fun ReviewTab(reviews: List<Review>) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (reviews.isEmpty()) {
            Text("Chưa có đánh giá nào", color = Color.Gray)
        } else {
            reviews.forEach { r ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(r.userName, fontWeight = FontWeight.Bold)
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (i < r.rating.toInt()) StarActive else StarInactive
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(r.comment)
                    }
                }
            }
        }
    }
}
