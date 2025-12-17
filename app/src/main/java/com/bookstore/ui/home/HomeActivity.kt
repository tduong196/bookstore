package com.bookstore.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.bookstore.data.model.Book
import com.bookstore.ui.book.BookDetailActivity
import com.bookstore.ui.book.BookManagementActivity
import com.bookstore.ui.book.EditBookActivity
import com.bookstore.ui.cart.CartScreen
import com.bookstore.ui.chat.ChatActivity
import com.bookstore.ui.user.OrderManagementActivity
import com.bookstore.ui.user.ProfileScreen
import com.bookstore.ui.user.UserManagementActivity
import com.bookstore.ui.theme.BookstoreTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

/* ===================== PALETTE (ĐỒNG BỘ) ===================== */

private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val ChipBg = Color(0xFFE6EFE9)
private val PriceColor = Color(0xFF2E7D32)

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = intent.getIntExtra("ROLE", 1)

        setContent {
            BookstoreTheme {
                MainScreen(role)
            }
        }
    }

    @Composable
    fun MainScreen(role: Int) {
        var selectedIndex by remember { mutableIntStateOf(0) }
        val isAdmin = role == 2

        Column(modifier = Modifier.fillMaxSize().background(BackgroundSoft)) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedIndex) {
                    0 -> Trangchu(role)
                    1 -> CartScreen()
                    2 -> ProfileScreen()
                    3 -> if (isAdmin) AdminScreen()
                    4 -> ChatEntryScreen()
                }
            }

            NavigationBar(
                containerColor = CardColor,
                tonalElevation = 10.dp
            ) {
                NavItem(
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                    label = "Trang chủ"
                )

                NavItem(
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Giỏ hàng") },
                    label = "Giỏ hàng"
                )

                NavItem(
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Cá nhân") },
                    label = "Cá nhân"
                )

                if (isAdmin) {
                    NavItem(
                        selected = selectedIndex == 3,
                        onClick = { selectedIndex = 3 },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                        label = "Admin"
                    )
                }

                NavItem(
                    selected = selectedIndex == 4,
                    onClick = { selectedIndex = 4 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat AI") },
                    label = "Chat"
                )
            }
        }
    }

    @Composable
    private fun RowScope.NavItem(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: String
    ) {
        NavigationBarItem(
            icon = icon,
            label = { Text(label) },
            selected = selected,
            onClick = onClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = ChipBg,
                unselectedIconColor = Color(0xFF6B6B6B),
                unselectedTextColor = Color(0xFF6B6B6B)
            )
        )
    }

    // ==================== TRANG CHỦ ====================
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Trangchu(role: Int) {
        val context = LocalContext.current
        val books = remember { mutableStateListOf<Book>() }
        val categories = remember { mutableStateListOf<String>() }
        var selectedCategory by remember { mutableStateOf("Tất cả") }
        var searchQuery by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            FirebaseFirestore.getInstance().collection("books")
                .get()
                .addOnSuccessListener { result ->
                    books.clear()
                    categories.clear()
                    for (document in result) {
                        val book = document.toObject(Book::class.java).apply { id = document.id }
                        books.add(book)
                        if (book.category.isNotBlank() && !categories.contains(book.category)) {
                            categories.add(book.category)
                        }
                    }
                }
        }

        val filteredBooks = books.filter { book ->
            (selectedCategory == "Tất cả" || book.category == selectedCategory) &&
                    (book.title.contains(searchQuery, ignoreCase = true) ||
                            book.author.contains(searchQuery, ignoreCase = true))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSoft)
                .padding(top = 14.dp, start = 16.dp, end = 16.dp)
        ) {

            // ===== TOP HEADER CARD =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Chào mừng trở lại,",
                        fontSize = 14.sp,
                        color = Color(0xFF6B6B6B)
                    )
                    Text(
                        text = "BookStore",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenDark
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tìm kiếm sách, tác giả...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color(0xFFF7FAF8),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = GreenPrimary,
                            cursorColor = GreenPrimary,
                            focusedLabelColor = GreenPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== CATEGORIES =====
            Text(
                text = "Thể loại",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    CategoryChip(
                        text = "Tất cả",
                        selected = selectedCategory == "Tất cả",
                        onClick = { selectedCategory = "Tất cả" }
                    )
                }
                items(categories) { category ->
                    CategoryChip(
                        text = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== SECTION TITLE =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sách nổi bật",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${filteredBooks.size} sách",
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B)
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredBooks) { book ->
                    FirebaseBookCard(book, role)
                }
            }
        }
    }

    @Composable
    private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(text) },
            shape = RoundedCornerShape(999.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GreenPrimary,
                selectedLabelColor = Color.White,
                containerColor = ChipBg,
                labelColor = GreenDark
            )
        )
    }

    // ==================== ADMIN ====================
    @Composable
    fun AdminScreen() {
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(BackgroundSoft, Color(0xFFE9F0EC))
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Trang quản trị",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenDark
                )

                Text(
                    text = "Quản lý hệ thống BookStore",
                    fontSize = 14.sp,
                    color = Color(0xFF6B6B6B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                AdminFunctionButton(Icons.AutoMirrored.Filled.List, "Quản lý Sách") {
                    context.startActivity(Intent(context, BookManagementActivity::class.java))
                }
                AdminFunctionButton(Icons.Default.People, "Quản lý Người dùng") {
                    context.startActivity(Intent(context, UserManagementActivity::class.java))
                }
                AdminFunctionButton(Icons.Default.BarChart, "Quản lý đơn hàng") {
                    context.startActivity(Intent(context, OrderManagementActivity::class.java))
                }
            }
        }
    }

    @Composable
    fun AdminFunctionButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        text: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ChipBg
                ) {
                    Icon(
                        icon,
                        contentDescription = text,
                        tint = GreenPrimary,
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(text, fontWeight = FontWeight.Bold, color = GreenDark)
                    Text("Xem chi tiết", fontSize = 12.sp, color = Color(0xFF6B6B6B))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GreenPrimary)
            }
        }
    }

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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.startActivity(Intent(context, BookDetailActivity::class.java).apply {
                        putExtra("title", book.title)
                        putExtra("author", book.author)
                        putExtra("description", book.description)
                        putExtra("image_url", book.image_url)
                        putExtra("category", book.category)
                        putExtra("price", formattedPrice)
                        putExtra("rating", book.rating)
                    })
                },
            elevation = CardDefaults.cardElevation(6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Box(
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(book.image_url),
                        contentDescription = book.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Rating badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = CardColor.copy(alpha = 0.92f),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(book.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp)
                )

                Text(
                    text = book.author,
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = formattedPrice,
                    fontSize = 14.sp,
                    color = PriceColor,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, BookDetailActivity::class.java).apply {
                                putExtra("title", book.title)
                                putExtra("author", book.author)
                                putExtra("description", book.description)
                                putExtra("image_url", book.image_url)
                                putExtra("category", book.category)
                                putExtra("price", formattedPrice)
                                putExtra("rating", book.rating)
                            })
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text("Xem", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (role == 2) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(context, EditBookActivity::class.java).apply {
                                    putExtra("docId", book.id)
                                    putExtra("title", book.title)
                                    putExtra("author", book.author)
                                    putExtra("description", book.description)
                                    putExtra("image_url", book.image_url)
                                    putExtra("category", book.category)
                                    putExtra("rating", book.rating.toString())
                                    putExtra("price", formattedPrice)
                                })
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
                        ) {
                            Text("Sửa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
