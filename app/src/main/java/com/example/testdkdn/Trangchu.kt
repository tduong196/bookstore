package com.example.testdkdn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.testdkdn.ui.theme.TestDKDNTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class TrangchuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = intent.getIntExtra("ROLE", 1)

        setContent {
            TestDKDNTheme {
                MainScreen(role)
            }
        }
    }

    @Composable
    fun MainScreen(role: Int) {
        var selectedIndex by remember { mutableStateOf(0) }
        val isAdmin = role == 2

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedIndex) {
                    0 -> Trangchu(role)
                    1 -> GioHangScreen()
                    2 -> CaNhan()
                    3 -> if (isAdmin) AdminScreen()
                    4 -> ChatEntryScreen()
                }
            }

            NavigationBar(containerColor = Color(0xFF0077B6)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                    label = { Text("Trang chủ", color = Color.White) },
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Giỏ hàng") },
                    label = { Text("Giỏ hàng", color = Color.White) },
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Cá nhân") },
                    label = { Text("Cá nhân", color = Color.White) },
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 }
                )

                if (isAdmin) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                        label = { Text("Admin", color = Color.White) },
                        selected = selectedIndex == 3,
                        onClick = { selectedIndex = 3 }
                    )
                }

                // Tab Chat
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat AI") },
                    label = { Text("Chat AI", color = Color.White) },
                    selected = selectedIndex == 4,
                    onClick = { selectedIndex = 4 }
                )
            }
        }
    }

    // ==================== TRANG CHỦ ====================
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Trangchu(role: Int) {
        val context = LocalContext.current
        val books = remember { mutableStateListOf<Book>() }
        val categories = remember { mutableStateListOf<String>() }
        var selectedCategory by remember { mutableStateOf("Tất cả") }
        var expanded by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        // Lấy dữ liệu sách từ Firebase Firestore
        LaunchedEffect(Unit) {
            FirebaseFirestore.getInstance().collection("books")
                .get()
                .addOnSuccessListener { result ->
                    books.clear()
                    for (document in result) {
                        val book = document.toObject(Book::class.java).apply { id = document.id }
                        books.add(book)
                        if (!categories.contains(book.category)) categories.add(book.category)
                    }
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .background(Color(0xFFE3F2FD)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("Tìm kiếm theo tên hoặc tác giả") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF0077B6),
                    unfocusedBorderColor = Color.LightGray,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // Dropdown category
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chọn thể loại") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF0077B6),
                            unfocusedBorderColor = Color.LightGray,
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Tất cả") }, onClick = { selectedCategory = "Tất cả"; expanded = false })
                        categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; expanded = false })
                        }
                    }
                }
            }

            Text(
                text = "Sách nổi bật",
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color(0xFF0077B6),
                modifier = Modifier.padding(8.dp)
            )

            val filteredBooks = books.filter { book ->
                (selectedCategory == "Tất cả" || book.category == selectedCategory) &&
                        (book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true))
            }

            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(4.dp), modifier = Modifier.weight(1f)) {
                items(filteredBooks) { book -> FirebaseBookCard(book, role) }
            }
        }
    }

    // ==================== ADMIN ====================
    @Composable
    fun AdminScreen() {
        val context = LocalContext.current
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).background(Color(0xFFE3F2FD)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Trang Quản Trị", fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color(0xFF0077B6))
            Spacer(modifier = Modifier.height(24.dp))
            AdminFunctionButton(Icons.Default.List, "Quản lý Sách") {
                context.startActivity(Intent(context, QuanLySachActivity::class.java))
            }
            AdminFunctionButton(Icons.Default.People, "Quản lý Người dùng") {
                context.startActivity(Intent(context, QuanLyNguoiDung::class.java))
            }
            AdminFunctionButton(Icons.Default.BarChart, "Quản lý đơn hàng") {
                context.startActivity(Intent(context, QuanLyDonHang::class.java))
            }
        }
    }

    @Composable
    fun AdminFunctionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0077B6))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = "Xem chi tiết", modifier = Modifier.size(20.dp))
            }
        }
    }

    // ==================== GIỎ HÀNG ====================
//    @Composable
//    fun GioHangScreen() {
//        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            Text("Giỏ hàng")
//        }
//    }

    // ==================== CÁ NHÂN ====================
//    @Composable
//    fun CaNhan() {
//        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            Text("Cá nhânnn")
//        }
//    }

    // ==================== MỞ CHAT ====================
    @Composable
    fun ChatEntryScreen() {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, ChatActivity::class.java))
        }
    }

    // ==================== BOOK CARD ====================
    @Composable
    fun FirebaseBookCard(book: Book, role: Int) {
        val context = LocalContext.current
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val formattedPrice = formatter.format(book.price)
        val formattedRating = formatter.format(book.rating)

        Card(modifier = Modifier.width(180.dp).padding(4.dp), elevation = CardDefaults.cardElevation(6.dp), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = rememberAsyncImagePainter(book.image_url), contentDescription = null, modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Text(book.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Giá: $formattedPrice")
                Text("Tác giả: ${book.author}", fontSize = 12.sp, color = Color.Gray)
                Text("Thể loại: ${book.category}", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Button(onClick = {
                    context.startActivity(Intent(context, ChitietActivity::class.java).apply {
                        putExtra("title", book.title)
                        putExtra("author", book.author)
                        putExtra("description", book.description)
                        putExtra("image_url", book.image_url)
                        putExtra("category", book.category)
                        putExtra("price", formattedPrice)
                        putExtra("rating", book.rating)
                    })
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))) {
                    Text("Xem", fontSize = 12.sp)
                }

                if (role == 2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = {
                        context.startActivity(Intent(context, SuaSachActivity::class.java).apply {
                            putExtra("docId", book.id)
                            putExtra("title", book.title)
                            putExtra("author", book.author)
                            putExtra("description", book.description)
                            putExtra("image_url", book.image_url)
                            putExtra("category", book.category)
                            putExtra("rating", formattedRating)
                            putExtra("price", formattedPrice)
                        })
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))) {
                        Text("Sửa", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
