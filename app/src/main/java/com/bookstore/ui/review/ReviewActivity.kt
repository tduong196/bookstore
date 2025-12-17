package com.bookstore.ui.review

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bookstore.data.model.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/* ===== UI COLORS (CHỈ HIỂN THỊ) ===== */
private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val StarActive = Color(0xFFFFC107)
private val StarInactive = Color(0xFFE0E0E0)

class ReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val orderId = intent.getStringExtra("orderId") ?: ""
        val bookId = intent.getStringExtra("bookId") ?: ""
        val bookTitle = intent.getStringExtra("bookTitle") ?: ""
        val bookImage = intent.getStringExtra("bookImage") ?: ""

        setContent {
            ReviewScreen(
                orderId = orderId,
                bookId = bookId,
                bookTitle = bookTitle,
                bookImage = bookImage,
                onReviewSubmitted = {
                    Toast.makeText(
                        this,
                        "Đánh giá của bạn đã được gửi thành công",
                        Toast.LENGTH_LONG
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    orderId: String,
    bookId: String,
    bookTitle: String,
    bookImage: String,
    onReviewSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }

    // Lấy thông tin user
    LaunchedEffect(Unit) {
        auth.currentUser?.email?.let { email ->
            db.collection("users").document(email).get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("name") ?: ""
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            /* ===== HEADER ===== */
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Text(
                    text = "Đánh giá sản phẩm",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark,
                    modifier = Modifier.padding(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /* ===== BOOK INFO ===== */
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = bookImage,
                            contentDescription = bookTitle,
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = bookTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                /* ===== RATING ===== */
                Text(
                    text = "Đánh giá của bạn",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating)
                                Icons.Filled.Star
                            else
                                Icons.Outlined.Star,
                            contentDescription = "Star $i",
                            tint = if (i <= rating) StarActive else StarInactive,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { rating = i }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (rating) {
                        1 -> "Rất tệ"
                        2 -> "Tệ"
                        3 -> "Bình thường"
                        4 -> "Tốt"
                        5 -> "Rất tốt"
                        else -> "Chọn số sao"
                    },
                    color = Color(0xFF757575),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                /* ===== COMMENT ===== */
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Nhận xét của bạn") },
                    placeholder = { Text("Chia sẻ trải nghiệm của bạn về sản phẩm này...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = GreenPrimary.copy(alpha = 0.4f),
                        focusedLabelColor = GreenPrimary,
                        cursorColor = GreenPrimary
                    ),
                    maxLines = 8
                )

                Spacer(modifier = Modifier.height(32.dp))

                /* ===== SUBMIT ===== */
                Button(
                    onClick = {
                        if (rating == 0) {
                            Toast.makeText(
                                context,
                                "Vui lòng chọn số sao đánh giá",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        if (comment.isBlank()) {
                            Toast.makeText(
                                context,
                                "Vui lòng viết nhận xét",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isSubmitting = true
                        val review = Review(
                            orderId = orderId,
                            userId = auth.currentUser?.uid ?: "",
                            userEmail = auth.currentUser?.email ?: "",
                            userName = userName,
                            bookId = bookId,
                            bookTitle = bookTitle,
                            rating = rating.toDouble(),
                            comment = comment,
                            timestamp = System.currentTimeMillis(),
                            approved = true
                        )

                        db.collection("reviews")
                            .add(review)
                            .addOnSuccessListener {
                                updateBookRating(db, bookId)
                                db.collection("orders")
                                    .document(orderId)
                                    .update("reviewed", true)
                                    .addOnSuccessListener {
                                        isSubmitting = false
                                        onReviewSubmitted()
                                    }
                                    .addOnFailureListener {
                                        isSubmitting = false
                                        onReviewSubmitted()
                                    }
                            }
                            .addOnFailureListener { e ->
                                isSubmitting = false
                                Toast.makeText(
                                    context,
                                    "Lỗi khi gửi đánh giá: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            "GỬI ĐÁNH GIÁ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun updateBookRating(db: FirebaseFirestore, bookId: String) {
    db.collection("reviews")
        .whereEqualTo("bookId", bookId)
        .get()
        .addOnSuccessListener { snapshot ->
            val reviews = snapshot.documents.mapNotNull {
                it.toObject(Review::class.java)
            }
            if (reviews.isNotEmpty()) {
                val avgRating = reviews.map { it.rating }.average()
                db.collection("books")
                    .document(bookId)
                    .update("rating", avgRating)
            }
        }
}
