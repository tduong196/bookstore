package com.bookstore.ui.review

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookstore.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

/* ===== UI COLORS (HIỂN THỊ ONLY) ===== */
private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val ApproveColor = Color(0xFF2E7D32)
private val RejectColor = Color(0xFFC62828)
private val StarActive = Color(0xFFFFC107)
private val StarInactive = Color(0xFFE0E0E0)

class ReviewManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReviewManagementScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        isLoading = true
        val query = if (selectedTab == 0) {
            db.collection("reviews")
                .whereEqualTo("approved", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        } else {
            db.collection("reviews")
                .whereEqualTo("approved", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        }

        query.addSnapshotListener { snapshot, error ->
            isLoading = false
            if (error != null) return@addSnapshotListener

            reviews = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Review::class.java)?.copy(id = doc.id)
            } ?: emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            /* ===== HEADER + TABS ===== */
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column {
                    Text(
                        text = "Quản lý đánh giá",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = GreenDark,
                        modifier = Modifier.padding(24.dp)
                    )

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CardColor,
                        contentColor = GreenPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GreenPrimary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Chờ duyệt") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Đã duyệt") }
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }

                reviews.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0)
                                "Không có đánh giá nào chờ duyệt"
                            else
                                "Chưa có đánh giá nào được duyệt",
                            color = Color.Gray
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reviews) { review ->
                            ReviewCard(
                                review = review,
                                showActions = selectedTab == 0,
                                onApprove = {
                                    db.collection("reviews").document(review.id)
                                        .update("approved", true)
                                        .addOnSuccessListener {
                                            updateBookRating(db, review.bookId)
                                        }
                                },
                                onReject = {
                                    db.collection("reviews").document(review.id).delete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCard(
    review: Review,
    showActions: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = review.bookTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = GreenDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = review.userName,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${formatTimestamp(review.timestamp)}",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (index < review.rating.toInt())
                            StarActive else StarInactive,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(review.rating.toString(), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = review.comment,
                color = Color(0xFF424242)
            )

            if (showActions) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RejectColor
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Từ chối")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApproveColor
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duyệt")
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}

private fun updateBookRating(db: FirebaseFirestore, bookId: String) {
    db.collection("reviews")
        .whereEqualTo("bookId", bookId)
        .whereEqualTo("approved", true)
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
