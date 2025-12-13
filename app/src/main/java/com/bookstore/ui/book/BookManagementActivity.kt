package com.bookstore.ui.book

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.bookstore.data.model.Book
import com.bookstore.ui.theme.BookstoreTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class BookManagementActivity : ComponentActivity() {
    private val shouldReload = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookstoreTheme {
                BookManagementScreen(
                    shouldReload = shouldReload.value,
                    onReloadComplete = { shouldReload.value = false }
                ) {
                    finish() // Đóng activity khi nhấn back
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Đánh dấu cần reload khi quay lại activity
        shouldReload.value = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun BookManagementScreen(
    shouldReload: Boolean = false,
    onReloadComplete: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val books = remember { mutableStateListOf<Book>() }
    val firestore = FirebaseFirestore.getInstance()

    // Hàm load lại danh sách sách
    fun loadBooks() {
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                books.clear()
                for (document in result) {
                    val book = document.toObject(Book::class.java).apply {
                        id = document.id
                    }
                    books.add(book)
                }
            }
    }

    // Load sách lần đầu
    LaunchedEffect(Unit) {
        loadBooks()
    }

    // Reload khi shouldReload thay đổi
    LaunchedEffect(shouldReload) {
        if (shouldReload) {
            loadBooks()
            onReloadComplete()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quản lý Sách") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0077B6),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, AddBookActivity::class.java)
                    context.startActivity(intent)
                },
                containerColor = Color(0xFF0077B6),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm sách")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE3F2FD))
        ) {
            items(books) { book ->
                BookItemAdmin(
                    book = book,
                    context = context,
                    onBookDeleted = { loadBooks() } // Load lại danh sách khi xóa thành công
                )
            }
        }
    }
}

@Composable
fun BookItemAdmin(book: Book, context: android.content.Context, onBookDeleted: () -> Unit) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val formattedPrice = formatter.format(book.price)
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Dialog xác nhận xóa
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa sách '${book.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val bookId = book.id

                                // Bước 1: Xóa tất cả reviews của sách này
                                firestore.collection("reviews")
                                    .whereEqualTo("bookId", bookId)
                                    .get()
                                    .addOnSuccessListener { reviewSnapshot ->
                                        val batch = firestore.batch()
                                        reviewSnapshot.documents.forEach { doc ->
                                            batch.delete(doc.reference)
                                        }
                                        batch.commit()
                                    }

                                // Bước 2: Xóa/cập nhật đơn hàng có chứa sách này
                                firestore.collection("orders")
                                    .get()
                                    .addOnSuccessListener { orderSnapshot ->
                                        orderSnapshot.documents.forEach { doc ->
                                            val items = doc.get("items") as? List<*>
                                            items?.let { itemList ->
                                                val updatedItems = itemList.mapNotNull { item ->
                                                    (item as? Map<*, *>)?.let { map ->
                                                        if (map["bookId"] == bookId) {
                                                            null // Xóa item này
                                                        } else {
                                                            map // Giữ lại item này
                                                        }
                                                    }
                                                }

                                                // Nếu đơn hàng không còn item nào, xóa đơn hàng
                                                if (updatedItems.isEmpty()) {
                                                    doc.reference.delete()
                                                } else if (updatedItems.size < itemList.size) {
                                                    // Nếu có item bị xóa, cập nhật lại đơn hàng
                                                    doc.reference.update("items", updatedItems)
                                                }
                                            }
                                        }
                                    }

                                // Bước 3: Xóa sách
                                firestore.collection("books").document(bookId)
                                    .delete()
                                    .addOnSuccessListener {
                                        onBookDeleted()
                                        Toast.makeText(context, "Đã xóa sách và các dữ liệu liên quan", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Lỗi khi xóa sách: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.image_url,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Tác giả: ${book.author}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Giá: $formattedPrice",
                    fontSize = 14.sp
                )
            }

            Column {
                IconButton(
                    onClick = {
                        val intent = Intent(context, BookDetailActivity::class.java).apply {
                            putExtra("title", book.title)
                            putExtra("author", book.author)
                            putExtra("description", book.description)
                            putExtra("image_url", book.image_url)
                            putExtra("category", book.category)
                            putExtra("rating", book.rating)
                            putExtra("price", book.price)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Xem",
                        tint = Color(0xFF0077B6)
                    )
                }

                IconButton(
                    onClick = {
                        val intent = Intent(context, EditBookActivity::class.java).apply {
                            putExtra("docId", book.id)
                            putExtra("title", book.title)
                            putExtra("author", book.author)
                            putExtra("description", book.description)
                            putExtra("image_url", book.image_url)
                            putExtra("category", book.category)
                            putExtra("rating", book.rating)
                            putExtra("price", book.price)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Sửa",
                        tint = Color(0xFFFFC107)
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}
