package com.bookstore.ui.cart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.bookstore.ui.payment.PaymentMethodActivity
import com.bookstore.ui.user.NotificationActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

/* ===================== COLORS ===================== */

private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val PriceColor = Color(0xFF2E7D32)

/* ===================== ACTIVITY ===================== */

class CartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CartScreen() }
    }
}

/* ===================== SCREEN ===================== */

@Composable
fun CartScreen() {
    val context = LocalContext.current
    var cartItems by remember { mutableStateOf(CartManager.getCart(context)) }
    var firestoreBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    val user = Firebase.auth.currentUser

    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isFirestoreLoaded by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf("Chưa chọn") }

    val paymentMethodLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val paymentMethod = result.data?.getStringExtra("payment_method")
            if (paymentMethod != null) {
                selectedPaymentMethod = paymentMethod
            }
        }
    }

    LaunchedEffect(true) {
        Firebase.firestore.collection("books")
            .get()
            .addOnSuccessListener {
                firestoreBooks = it.mapNotNull { doc ->
                    doc.toObject(Book::class.java).copy(id = doc.id)
                }
                isFirestoreLoaded = true
            }
    }

    val updatedCartItems = remember(cartItems, firestoreBooks) {
        if (isFirestoreLoaded) {
            cartItems.map { cartBook ->
                firestoreBooks.find { it.title == cartBook.title }?.let {
                    cartBook.copy(
                        id = it.id,
                        price = it.price,
                        image_url = it.image_url
                    )
                } ?: cartBook
            }
        } else cartItems
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .padding(16.dp)
    ) {

        Text(
            text = "Giỏ hàng của bạn",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GreenDark,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {

            items(updatedCartItems) { item ->
                CartItemUI(
                    book = item,
                    onRemove = {
                        CartManager.removeFromCart(context, item)
                        cartItems = CartManager.getCart(context)
                    },
                    onQuantityChange = {
                        CartManager.updateCartItem(context, item.copy(quantity = it))
                        cartItems = CartManager.getCart(context)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                CheckoutOptionsSection(
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodClick = {
                        paymentMethodLauncher.launch(Intent(context, PaymentMethodActivity::class.java))
                    }
                )
                Spacer(Modifier.height(16.dp))

                /* ADDRESS */
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = GreenPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Địa chỉ giao hàng", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null
                            )
                        }

                        if (expanded) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Số điện thoại") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Địa chỉ") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors()
                            )
                        }
                    }
                }
            }
        }

        CheckoutSummaryUI(
            totalPrice = updatedCartItems.sumOf { it.price * it.quantity }.toInt(),
            onOrderClick = {
                if (updatedCartItems.isEmpty()) {
                    Toast.makeText(context, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
                    return@CheckoutSummaryUI
                }
                if (phone.isBlank() || address.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                    return@CheckoutSummaryUI
                }
                if (selectedPaymentMethod == "Chưa chọn") {
                    Toast.makeText(context, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show()
                    return@CheckoutSummaryUI
                }

                val orderData = mapOf(
                    "userId" to user?.uid,
                    "userEmail" to user?.email,
                    "phone" to phone,
                    "address" to address,
                    "paymentMethod" to selectedPaymentMethod,
                    "items" to updatedCartItems.map {
                        mapOf(
                            "bookId" to it.id,
                            "title" to it.title,
                            "quantity" to it.quantity,
                            "price" to it.price
                        )
                    },
                    "totalAmount" to updatedCartItems.sumOf { it.price * it.quantity },
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "PENDING"
                )

                Firebase.firestore.collection("orders")
                    .add(orderData)
                    .addOnSuccessListener {
                        CartManager.clearCart(context)
                        Toast.makeText(context, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
                        context.startActivity(
                            Intent(context, NotificationActivity::class.java)
                                .putExtra("orderId", it.id)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        (context as? Activity)?.finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Đặt hàng thất bại: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
        )
    }
}

/* ===================== CART ITEM ===================== */

@Composable
fun CartItemUI(
    book: Book,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf(book.quantity) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = book.image_url,
                contentDescription = null,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    book.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    "${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN")).format(book.price)}",
                    color = PriceColor,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityButton("-", onClick = {
                        if (quantity > 1) {
                            quantity--
                            onQuantityChange(quantity)
                        }
                    })
                    Text(
                        "$quantity",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontWeight = FontWeight.Bold
                    )
                    QuantityButton("+", onClick = {
                        quantity++
                        onQuantityChange(quantity)
                    })
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                    }
                }
            }
        }
    }
}

/* ===================== CHECKOUT SUMMARY ===================== */

@Composable
fun CheckoutSummaryUI(totalPrice: Int, onOrderClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tổng thanh toán", fontWeight = FontWeight.Medium)
                Text(
                    "${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN")).format(totalPrice)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = GreenDark
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOrderClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("ĐẶT HÀNG", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ===================== SMALL COMPONENTS ===================== */

@Composable
fun QuantityButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CheckoutOptionsSection(
    selectedPaymentMethod: String,
    onPaymentMethodClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(Modifier.padding(12.dp)) {
            CheckoutOption("Phương thức thanh toán", selectedPaymentMethod, onPaymentMethodClick)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            CheckoutOption("Khuyến mãi", "Chưa áp dụng") {}
        }
    }
}

@Composable
fun CheckoutOption(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = Color.Gray)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,

    focusedIndicatorColor = GreenPrimary,
    unfocusedIndicatorColor = GreenPrimary.copy(alpha = 0.4f),

    focusedLabelColor = GreenPrimary,
    cursorColor = GreenPrimary
)
