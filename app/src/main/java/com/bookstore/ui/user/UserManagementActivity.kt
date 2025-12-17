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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookstore.ui.theme.BookstoreTheme
import com.google.firebase.firestore.FirebaseFirestore

/* ===== UI COLORS (CHỈ HIỂN THỊ) ===== */
private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val AdminColor = Color(0xFF2E7D32)

class UserManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookstoreTheme {
                UserManagementScreen {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(onBack: () -> Unit) {
    var danhSachNguoiDung by remember { mutableStateOf<List<NguoiDung>>(emptyList()) }
    var userToDelete by remember { mutableStateOf<NguoiDung?>(null) }
    val db = FirebaseFirestore.getInstance()

    fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val nguoiDungList = result.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val email = doc.getString("email") ?: return@mapNotNull null
                    val role = doc.getLong("role")?.toInt() ?: return@mapNotNull null
                    NguoiDung(
                        id = doc.id,
                        name = name,
                        email = email,
                        role = role
                    )
                }.sortedBy { it.role }
                danhSachNguoiDung = nguoiDungList
            }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Quản lý Người dùng",
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
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BackgroundSoft)
                .padding(16.dp)
        ) {

            LazyColumn {
                items(danhSachNguoiDung) { nguoiDung ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {

                        Column(modifier = Modifier.padding(16.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column {
                                    Text(
                                        text = nguoiDung.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = GreenDark
                                    )
                                    Text(
                                        text = nguoiDung.email,
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (nguoiDung.role == 2)
                                        AdminColor.copy(alpha = 0.15f)
                                    else
                                        Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (nguoiDung.role == 2) "Admin" else "User",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (nguoiDung.role == 2) AdminColor else Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                if (nguoiDung.role != 2) {
                                    Button(
                                        onClick = { userToDelete = nguoiDung },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("Xóa", color = Color.White)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(100.dp))
                                }

                                if (nguoiDung.role != 2) {
                                    OutlinedButton(
                                        onClick = {
                                            db.collection("users")
                                                .document(nguoiDung.id)
                                                .update("role", 2)
                                                .addOnSuccessListener { loadUsers() }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = GreenPrimary
                                        )
                                    ) {
                                        Text("Cấp quyền admin")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    "Xác nhận xóa",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Bạn có chắc chắn muốn xóa người dùng này không?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("users")
                            .document(userToDelete!!.id)
                            .delete()
                            .addOnSuccessListener {
                                userToDelete = null
                                loadUsers()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

data class NguoiDung(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: Int = 0
)
