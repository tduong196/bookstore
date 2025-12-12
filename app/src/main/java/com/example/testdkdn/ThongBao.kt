package com.example.testdkdn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class ThongBao : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrderConfirmationTheme {
                OrderHistoryScreen()
            }
        }
    }
}

@Composable
fun OrderHistoryScreen() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    orders = snapshot?.documents?.mapNotNull { doc ->
                        // Chuyển trường "status" từ String sang enum OrderStatus, nếu null thì mặc định PENDING
                        val order = doc.toObject(Order::class.java)
                        val statusString = doc.getString("status") ?: "PENDING"
                        val status = try {
                            OrderStatus.valueOf(statusString)
                        } catch (e: Exception) {
                            OrderStatus.PENDING
                        }
                        order?.copy(id = doc.id, status = status)
                    } ?: emptyList()
                    isLoading = false
                }
        } ?: run { isLoading = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            orders.isEmpty() -> Text(
                "Bạn chưa có đơn hàng nào",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(orders) { order ->
                        OrderCard(order = order)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: Order) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        ) {
            // Header đơn hàng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0077B6), Color(0xFF023E8A))
                        )
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Đơn hàng #${order.id.take(8).uppercase()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ngày: ${formatTimestamp(order.timestamp)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                // Hiển thị trạng thái đơn hàng dưới dạng badge
                OrderStatusBadge(order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thông tin tóm tắt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Số lượng: ${order.items.sumOf { it.quantity }}", fontSize = 14.sp)
                    Text(
                        "Tổng tiền: ${numberFormat.format(order.items.sumOf { it.price * it.quantity })}đ",
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                        tint = Color(0xFF0077B6)
                    )
                }
            }

            // Chi tiết đơn hàng
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Thông tin giao hàng", fontWeight = FontWeight.Bold, color = Color(0xFF0077B6))
                    InfoRow("Địa chỉ:", order.address)
                    InfoRow("SĐT:", order.phone)

                    Text("Sản phẩm", fontWeight = FontWeight.Bold, color = Color(0xFF0077B6), modifier = Modifier.padding(top = 8.dp))

                    order.items.forEach { item ->
                        OrderItemRow(item, numberFormat)
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (text, color) = when (status) {
        OrderStatus.PENDING -> "Chờ xử lý" to Color(0xFFFFA500)
        OrderStatus.APPROVED -> "Đã duyệt" to Color(0xFF4CAF50)
        OrderStatus.REJECTED -> "Đã từ chối" to Color(0xFFF44336)
        OrderStatus.DELIVERED -> "Đã giao" to Color(0xFF2196F3)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun OrderItemRow(item: OrderItem, numberFormat: NumberFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Medium)
            Text(
                "${numberFormat.format(item.price)}đ x ${item.quantity}",
                fontSize = 14.sp, color = Color.Gray
            )
        }

        Text(
            "${numberFormat.format(item.price * item.quantity)}đ",
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF555555))
        Text(value, color = Color.Black)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}

enum class OrderStatus {
    PENDING, APPROVED, REJECTED, DELIVERED
}

// Dữ liệu đơn hàng
data class Order(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val phone: String = "",
    val address: String = "",
    val items: List<OrderItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val status: OrderStatus = OrderStatus.PENDING
)

// Sản phẩm trong đơn hàng
data class OrderItem(
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
)

@Composable
fun OrderConfirmationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0077B6),
            secondary = Color(0xFF023E8A),
            background = Color(0xFFF5F5F5)
        ),
        content = content
    )
}
