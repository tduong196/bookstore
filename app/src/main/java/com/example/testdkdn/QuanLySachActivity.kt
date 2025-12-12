package com.example.testdkdn

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.testdkdn.ui.theme.TestDKDNTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.Alignment.Companion as Alignment1
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

class QuanLySachActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestDKDNTheme {
                QuanLySachScreen {
                    finish() // Đóng activity khi nhấn back
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun QuanLySachScreen(onBack: () -> Unit) {
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quản lý Sách") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
                    val intent = Intent(context, ThemSachActivity::class.java)
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
                                firestore.collection("books").document(book.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        onBookDeleted()
                                        Toast.makeText(context, "Đã xóa sách thành công", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(context, ChitietActivity::class.java).apply {
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
                        val intent = Intent(context, SuaSachActivity::class.java).apply {
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