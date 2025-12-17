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

/* ================= UI COLORS (HIỂN THỊ) ================= */
private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val BlueView = Color(0xFF4A90E2)
private val YellowEdit = Color(0xFFFFC107)

class BookManagementActivity : ComponentActivity() {
    private val shouldReload = mutableStateOf(false)
    private var isFirstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookstoreTheme {
                BookManagementScreen(
                    shouldReload = shouldReload.value,
                    onReloadComplete = { shouldReload.value = false }
                ) {
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
        } else {
            shouldReload.value = true
        }
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

    LaunchedEffect(Unit) {
        loadBooks()
    }

    LaunchedEffect(shouldReload) {
        if (shouldReload) {
            loadBooks()
            onReloadComplete()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Quản lý Sách",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CardColor,
                    titleContentColor = GreenDark,
                    navigationIconContentColor = GreenDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, AddBookActivity::class.java)
                    context.startActivity(intent)
                },
                containerColor = GreenPrimary,
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
                .background(BackgroundSoft)
        ) {
            items(books) { book ->
                BookItemAdmin(
                    book = book,
                    context = context,
                    onBookDeleted = { loadBooks() }
                )
            }
        }
    }
}

@Composable
fun BookItemAdmin(
    book: Book,
    context: android.content.Context,
    onBookDeleted: () -> Unit
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val formattedPrice = formatter.format(book.price)
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa sách '${book.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val bookId = book.id

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

                                firestore.collection("orders")
                                    .get()
                                    .addOnSuccessListener { orderSnapshot ->
                                        orderSnapshot.documents.forEach { doc ->
                                            val items = doc.get("items") as? List<*>
                                            items?.let { itemList ->
                                                val updatedItems = itemList.mapNotNull { item ->
                                                    (item as? Map<*, *>)?.let { map ->
                                                        if (map["bookId"] == bookId) null else map
                                                    }
                                                }
                                                if (updatedItems.isEmpty()) {
                                                    doc.reference.delete()
                                                } else if (updatedItems.size < itemList.size) {
                                                    doc.reference.update("items", updatedItems)
                                                }
                                            }
                                        }
                                    }

                                firestore.collection("books").document(bookId)
                                    .delete()
                                    .addOnSuccessListener {
                                        onBookDeleted()
                                        Toast.makeText(
                                            context,
                                            "Đã xóa sách và các dữ liệu liên quan",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Lỗi: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(6.dp)
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
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenDark
                )
                Text(
                    text = "Tác giả: ${book.author}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Giá: $formattedPrice",
                    fontSize = 14.sp,
                    color = GreenPrimary
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
                        tint = BlueView
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
                        tint = YellowEdit
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
