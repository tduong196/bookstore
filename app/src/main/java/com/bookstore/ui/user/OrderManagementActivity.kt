package com.bookstore.ui.user

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
import androidx.compose.material3.HorizontalDivider
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

/* ===== UI COLORS (HIỂN THỊ ONLY) ===== */
private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val ApproveColor = Color(0xFF2E7D32)
private val RejectColor = Color(0xFFC62828)
private val PendingColor = Color(0xFFFFA000)
private val DeliveredColor = Color(0xFF1565C0)

class OrderManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuanLyDonHangTheme {
                OrderManagementScreen()
            }
        }
    }
}

@Composable
fun OrderManagementScreen() {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {

        /* ===== SEARCH BAR ===== */
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Tìm kiếm đơn hàng") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = GreenPrimary.copy(alpha = 0.4f),
                focusedLabelColor = GreenPrimary,
                cursorColor = GreenPrimary
            )
        )

        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = GreenPrimary
                )
            }

            orders.isEmpty() -> {
                Text(
                    "Không có đơn hàng nào",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Color.Gray
                )
            }

            else -> {
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            /* ===== HEADER ===== */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Đơn #${order.id.take(6)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GreenDark
                    )
                    Text(
                        order.userName,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                AdminOrderStatusBadge(order.status)
            }

            /* ===== SUMMARY ===== */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${order.items.size} sản phẩm")
                Text(
                    "${numberFormat.format(order.items.sumOf { it.price * it.quantity })}đ",
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }

            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            /* ===== EXPANDED ===== */
            if (expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        "Thông tin khách hàng",
                        fontWeight = FontWeight.Bold
                    )
                    AdminInfoRow("Email:", order.userEmail)
                    AdminInfoRow("SĐT:", order.phone)
                    AdminInfoRow("Địa chỉ:", order.address)

                    Text(
                        "Sản phẩm",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    order.items.forEach { item ->
                        AdminOrderItemRow(item, numberFormat)
                    }

                    if (order.status == AdminOrderStatus.PENDING) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    updateOrderStatus(order.id, AdminOrderStatus.APPROVED)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ApproveColor)
                            ) {
                                Text("Duyệt đơn")
                            }

                            Button(
                                onClick = {
                                    updateOrderStatus(order.id, AdminOrderStatus.REJECTED)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RejectColor)
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
        AdminOrderStatus.PENDING -> "Chờ duyệt" to PendingColor
        AdminOrderStatus.APPROVED -> "Đã duyệt" to ApproveColor
        AdminOrderStatus.REJECTED -> "Từ chối" to RejectColor
        AdminOrderStatus.DELIVERED -> "Đã giao" to DeliveredColor
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Medium)
            Text(
                "${numberFormat.format(item.price)}đ x ${item.quantity}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Text(
            "${numberFormat.format(item.price * item.quantity)}đ",
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AdminInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun updateOrderStatus(orderId: String, newStatus: AdminOrderStatus) {
    FirebaseFirestore.getInstance().collection("orders")
        .document(orderId)
        .update("status", newStatus.name)
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
    PENDING,
    APPROVED,
    REJECTED,
    DELIVERED
}

@Composable
fun QuanLyDonHangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = GreenPrimary,
            secondary = GreenDark,
            background = BackgroundSoft
        ),
        content = content
    )
}
