package com.bookstore.ui.cart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.bookstore.data.manager.CartManager
import com.bookstore.data.model.Book
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import com.bookstore.ui.theme.BookstoreTheme
import com.bookstore.ui.payment.PaymentMethodActivity
import com.bookstore.ui.user.NotificationActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class CartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookstoreTheme {
                CartScreen()
            }
        }
    }
}

@Composable
fun CartScreen() {
    val context = LocalContext.current
    var cartItems by remember { mutableStateOf(CartManager.getCart(context)) }
    var firestoreBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    val user = Firebase.auth.currentUser
    var expanded by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isFirestoreLoaded by remember { mutableStateOf(false) }

    // Load dữ liệu sách từ Firestore
    LaunchedEffect(true) {
        Firebase.firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val books = result.mapNotNull { it.toObject(Book::class.java).copy(id = it.id) }
                firestoreBooks = books
                isFirestoreLoaded = true
                Log.d("FirestoreLoad", "Dữ liệu sách đã tải xong: ${books.size} sách")
            }
            .addOnFailureListener {
                Log.e("FirestoreLoad", "Lỗi khi tải dữ liệu sách", it)
            }
    }

    // Cập nhật giỏ hàng với giá và ID từ Firestore khi đã có dữ liệu
    val updatedCartItems = remember(cartItems, firestoreBooks) {
        if (isFirestoreLoaded) {
            cartItems.map { cartBook ->
                firestoreBooks.find { it.title == cartBook.title }?.let { matchedBook ->
                    cartBook.copy(
                        id = matchedBook.id, // Lấy bookId từ Firestore
                        price = matchedBook.price,
                        image_url = matchedBook.image_url // Đảm bảo có image_url
                    ).also {
                        Log.d("CartUpdate", "Cập nhật sách ${cartBook.title}: ID=${matchedBook.id}, giá=${matchedBook.price}")
                    }
                } ?: cartBook.also {
                    Log.w("CartUpdate", "Không tìm thấy sách tương ứng cho ${cartBook.title}")
                }
            }
        } else {
            cartItems.also {
                Log.d("CartUpdate", "Chưa tải xong dữ liệu Firestore, giữ nguyên giỏ hàng")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(updatedCartItems) { item ->
                CartItem(
                    book = item,
                    onRemove = {
                        CartManager.removeFromCart(context, item)
                        cartItems = CartManager.getCart(context)
                        Log.d("CartAction", "Đã xóa sách: ${item.title}")
                    },
                    onQuantityChange = { newQuantity ->
                        val updatedBook = item.copy(quantity = newQuantity)
                        CartManager.updateCartItem(context, updatedBook)
                        cartItems = CartManager.getCart(context)
                        Log.d("CartAction", "Cập nhật số lượng ${item.title}: $newQuantity")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                CheckoutOptionsSection()
                Spacer(modifier = Modifier.height(8.dp))

                // Địa chỉ giao hàng - expandable section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Địa chỉ giao hàng", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Số điện thoại") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Địa chỉ") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        CheckoutSummary(
            totalPrice = updatedCartItems.sumOf { it.price * it.quantity }.toInt(),
            onOrderClick = {
                if (!isFirestoreLoaded) {
                    Toast.makeText(context, "Đang tải dữ liệu sách, vui lòng chờ...", Toast.LENGTH_SHORT).show()
                    return@CheckoutSummary
                }

                val finalCart = updatedCartItems

                if (finalCart.isEmpty()) {
                    Toast.makeText(context, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show()
                    return@CheckoutSummary
                }

                if (phone.isBlank() || address.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập số điện thoại và địa chỉ giao hàng!", Toast.LENGTH_SHORT).show()
                    return@CheckoutSummary
                }

                // Kiểm tra lại giá và bookId trước khi đặt hàng
                finalCart.forEach { item ->
                    if (item.price <= 0) {
                        Toast.makeText(context, "Giá sản phẩm ${item.title} không hợp lệ!", Toast.LENGTH_SHORT).show()
                        return@CheckoutSummary
                    }
                    if (item.id.isBlank()) {
                        Toast.makeText(context, "Không tìm thấy ID sản phẩm ${item.title}. Vui lòng thử lại!", Toast.LENGTH_SHORT).show()
                        Log.e("OrderError", "BookId trống cho sách: ${item.title}")
                        return@CheckoutSummary
                    }
                }

                Log.d("OrderDebug", "Tạo đơn hàng với ${finalCart.size} sản phẩm")
                finalCart.forEach { item ->
                    Log.d("OrderDebug", "Sản phẩm: ${item.title}, ID: ${item.id}, Giá: ${item.price}")
                }

                val orderData = mapOf(
                    "userId" to user?.uid,
                    "userEmail" to user?.email,
                    "userName" to user?.displayName,
                    "phone" to phone,
                    "address" to address,
                    "items" to finalCart.map {
                        mapOf(
                            "bookId" to it.id,
                            "title" to it.title,
                            "quantity" to it.quantity,
                            "price" to it.price,
                            "imageUrl" to it.image_url,
                            "total" to (it.price * it.quantity)
                        )
                    },
                    "totalAmount" to finalCart.sumOf { it.price * it.quantity },
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "PENDING",
                    "reviewed" to false
                )

                Firebase.firestore.collection("orders")
                    .add(orderData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
                        CartManager.clearCart(context)
                        Log.d("OrderSuccess", "Đã tạo đơn hàng: ${it.id}")

                        val intent = Intent(context, NotificationActivity::class.java)
                        intent.putExtra("orderId", it.id)
                        context.startActivity(intent)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Lỗi khi đặt hàng!", Toast.LENGTH_SHORT).show()
                        Log.e("OrderError", "Lỗi khi tạo đơn hàng", it)
                    }
            }
        )
    }
}

@Composable
fun CartItem(
    book: Book,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf(book.quantity) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.image_url,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(book.title, fontWeight = FontWeight.Bold)
                Text("${book.price.toInt()} VNĐ", color = Color(0xFF0077B6))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (quantity > 1) {
                                quantity--
                                onQuantityChange(quantity)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("-", fontSize = 18.sp)
                    }

                    Text("$quantity", modifier = Modifier.padding(horizontal = 8.dp))

                    IconButton(
                        onClick = {
                            quantity++
                            onQuantityChange(quantity)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("+", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = Color(0xFFD00000)
                        )
                    }
                }
            }
        }
    }
}

// Các composable khác giữ nguyên như bản gốc
@Composable
fun CheckoutOptionsSection() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp)
    ) {
        CheckoutOption("Phương thức thanh toán", "Chưa chọn") {
            val intent = Intent(context, PaymentMethodActivity::class.java)
            context.startActivity(intent)
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        CheckoutOption("Khuyến mãi", "Chưa áp dụng") {
            /* Mở màn hình khuyến mãi */
        }
    }
}

@Composable
fun CheckoutOption(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = Color.Gray, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Chọn")
    }
}

@Composable
fun CheckoutSummary(totalPrice: Int, onOrderClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White)
    ) {
        Text("Thanh toán", fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tổng tiền:")
            Text("$totalPrice VNĐ", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onOrderClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0077B6)
            )
        ) {
            Text("Đặt đơn hàng", color = Color.White)
        }
    }
}
