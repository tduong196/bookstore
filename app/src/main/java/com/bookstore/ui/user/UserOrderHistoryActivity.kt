package com.bookstore.ui.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bookstore.ui.review.ReviewActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class UserOrderHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UserOrderHistoryScreen()
        }
    }
}

@Composable
fun UserOrderHistoryScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var orders by remember { mutableStateOf<List<UserOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Activity Result Launcher for ReviewActivity
    val reviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Đánh giá thành công, refresh dữ liệu
            refreshTrigger++
        }
    }

    LaunchedEffect(refreshTrigger) {
        auth.currentUser?.email?.let { email ->
            db.collection("orders")
                .whereEqualTo("userEmail", email)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        android.util.Log.e("OrderHistory", "Error loading orders", error)
                        return@addSnapshotListener
                    }

                    orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val statusString = doc.getString("status") ?: "PENDING"
                            val status = try {
                                UserOrderStatus.valueOf(statusString)
                            } catch (e: Exception) {
                                UserOrderStatus.PENDING
                            }

                            UserOrder(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                userEmail = doc.getString("userEmail") ?: "",
                                phone = doc.getString("phone") ?: "",
                                address = doc.getString("address") ?: "",
                                items = (doc.get("items") as? List<*>)?.mapNotNull { item ->
                                    (item as? Map<*, *>)?.let { map ->
                                        UserOrderItem(
                                            bookId = map["bookId"] as? String ?: "",
                                            title = map["title"] as? String ?: "",
                                            price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                                            quantity = (map["quantity"] as? Number)?.toInt() ?: 1,
                                            imageUrl = map["imageUrl"] as? String ?: ""
                                        )
                                    }
                                } ?: emptyList(),
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                status = status,
                                reviewed = doc.getBoolean("reviewed") ?: false
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("OrderHistory", "Error parsing order ${doc.id}", e)
                            null
                        }
                    } ?: emptyList()

                    android.util.Log.d("OrderHistory", "Loaded ${orders.size} orders")
                    orders.forEach { order ->
                        android.util.Log.d("OrderHistory", "Order ${order.id}: status=${order.status}, reviewed=${order.reviewed}, items=${order.items.size}")
                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "Đơn hàng của tôi",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(24.dp)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF546E7A))
                }
            } else if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Bạn chưa có đơn hàng nào",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        UserOrderCard(
                            order = order,
                            reviewLauncher = reviewLauncher
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserOrderCard(
    order: UserOrder,
    reviewLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Đơn hàng #${order.id.take(8)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        formatTimestamp(order.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                UserOrderStatusBadge(order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order items
            order.items.take(if (expanded) order.items.size else 1).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2
                        )
                        Text(
                            "x${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Text(
                        "${numberFormat.format(item.price * item.quantity)}đ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            if (order.items.size > 1 && !expanded) {
                TextButton(onClick = { expanded = true }) {
                    Text("Xem thêm ${order.items.size - 1} sản phẩm")
                }
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                Text(
                    "Thông tin giao hàng",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                UserInfoRow("Số điện thoại:", order.phone)
                UserInfoRow("Địa chỉ:", order.address)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            // Total and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tổng tiền",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "${numberFormat.format(order.items.sumOf { it.price * it.quantity })}đ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF546E7A)
                    )
                }

                // Review button for approved/delivered orders
                if ((order.status == UserOrderStatus.APPROVED || order.status == UserOrderStatus.DELIVERED)
                    && !order.reviewed) {
                    Button(
                        onClick = {
                            // Cho phép đánh giá sản phẩm đầu tiên trong đơn
                            if (order.items.isNotEmpty()) {
                                val firstItem = order.items[0]
                                val intent = Intent(context, ReviewActivity::class.java).apply {
                                    putExtra("orderId", order.id)
                                    putExtra("bookId", firstItem.bookId)
                                    putExtra("bookTitle", firstItem.title)
                                    putExtra("bookImage", firstItem.imageUrl)
                                }
                                reviewLauncher.launch(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF546E7A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = "Review",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đánh giá")
                    }
                } else if (order.reviewed) {
                    Chip(
                        text = "Đã đánh giá",
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            if (!expanded && order.items.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Chi tiết đơn hàng")
                }
            }
        }
    }
}

@Composable
fun UserOrderStatusBadge(status: UserOrderStatus) {
    val (text, color) = when (status) {
        UserOrderStatus.PENDING -> "Chờ duyệt" to Color(0xFFFFA500)
        UserOrderStatus.APPROVED -> "Đã duyệt" to Color(0xFF4CAF50)
        UserOrderStatus.REJECTED -> "Đã hủy" to Color(0xFFF44336)
        UserOrderStatus.DELIVERED -> "Đã giao" to Color(0xFF2196F3)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun UserInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF212121)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}

data class UserOrder(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val phone: String = "",
    val address: String = "",
    val items: List<UserOrderItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val status: UserOrderStatus = UserOrderStatus.PENDING,
    val reviewed: Boolean = false
)

data class UserOrderItem(
    val bookId: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
)

enum class UserOrderStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DELIVERED
}

