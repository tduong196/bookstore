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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class QuanLyDonHang : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuanLyDonHangTheme {
                QuanLyDonHangScreen()
            }
        }
    }
}

@Composable
fun QuanLyDonHangScreen() {
    var orders by remember { mutableStateOf<List<AdminOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("orders")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) return@addSnapshotListener

                orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AdminOrder::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Tìm kiếm đơn hàng") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (orders.isEmpty()) {
            Text(
                "Không có đơn hàng nào",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color.Gray
            )
        } else {
            val filteredOrders = orders.filter {
                it.id.contains(searchQuery, ignoreCase = true) ||
                        it.userName.contains(searchQuery, ignoreCase = true) ||
                        it.userEmail.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true) ||
                        it.status.name.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredOrders) { order ->
                    AdminOrderCard(order = order)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(order: AdminOrder) {
    var expanded by remember { mutableStateOf(false) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Đơn #${order.id.take(6)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        order.userName,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                AdminOrderStatusBadge(order.status)
            }

            // Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${order.items.size} sản phẩm",
                    fontSize = 14.sp
                )

                Text(
                    "${numberFormat.format(order.items.sumOf { it.price * it.quantity })}đ",
                    fontWeight = FontWeight.Bold
                )
            }

            // Expand button
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Thu gọn" else "Mở rộng"
                )
            }

            // Expanded details
            if (expanded) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Customer info
                    Text(
                        "Thông tin khách hàng",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AdminInfoRow("Email:", order.userEmail)
                    AdminInfoRow("SĐT:", order.phone)
                    AdminInfoRow("Địa chỉ:", order.address)

                    // Order items
                    Text(
                        "Sản phẩm",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    order.items.forEach { item ->
                        AdminOrderItemRow(item, numberFormat)
                    }

                    // Status actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (order.status == AdminOrderStatus.PENDING) {
                            Button(
                                onClick = { updateOrderStatus(order.id, AdminOrderStatus.APPROVED) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text("Duyệt đơn")
                            }
                            Button(
                                onClick = { updateOrderStatus(order.id, AdminOrderStatus.REJECTED) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336)
                                )
                            ) {
                                Text("Từ chối")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrderStatusBadge(status: AdminOrderStatus) {
    val (text, color) = when (status) {
        AdminOrderStatus.PENDING -> "Chờ duyệt" to Color(0xFFFFA500)
        AdminOrderStatus.APPROVED -> "Đã duyệt" to Color(0xFF4CAF50)
        AdminOrderStatus.REJECTED -> "Từ chối" to Color(0xFFF44336)
        AdminOrderStatus.DELIVERED -> "Đã giao" to Color(0xFF2196F3)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AdminOrderItemRow(item: OrderItem, numberFormat: NumberFormat) {
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
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Medium)
            Text("${numberFormat.format(item.price)}đ x ${item.quantity}",
                fontSize = 12.sp, color = Color.Gray)
        }

        Text("${numberFormat.format(item.price * item.quantity)}đ")
    }
}

@Composable
fun AdminInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF555555))
        Text(value, color = Color.Black)
    }
}

private fun updateOrderStatus(orderId: String, newStatus: AdminOrderStatus) {
    FirebaseFirestore.getInstance().collection("orders")
        .document(orderId)
        .update("status", newStatus.name)
        .addOnSuccessListener {
            // Có thể thêm thông báo thành công
        }
        .addOnFailureListener {
            // Xử lý lỗi
        }
}

data class AdminOrder(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val phone: String = "",
    val address: String = "",
    val items: List<OrderItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val status: AdminOrderStatus = AdminOrderStatus.PENDING
)

enum class AdminOrderStatus {
    PENDING,    // Chờ duyệt
    APPROVED,   // Đã duyệt
    REJECTED,   // Từ chối
    DELIVERED   // Đã giao
}

@Composable
fun QuanLyDonHangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0077B6),
            secondary = Color(0xFF023E8A),
            background = Color(0xFFF5F5F5)
        ),
        content = content
    )
}//