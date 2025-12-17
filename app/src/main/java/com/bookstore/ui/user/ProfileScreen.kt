package com.bookstore.ui.user

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.bookstore.R
import com.bookstore.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/* ===================== PALETTE ĐỒNG BỘ ===================== */

private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val BackgroundSoft = Color(0xFFF5F7F4)
private val CardColor = Color(0xFFFDFCFB)
private val ChipBg = Color(0xFFE6EFE9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val db = FirebaseFirestore.getInstance()

    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    /* ===== LOAD USER DATA – GIỮ NGUYÊN LOGIC ===== */
    LaunchedEffect(Unit) {
        currentUser?.email?.let { email ->
            db.collection("users").document(email)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("name") ?: "Khách"
                        userEmail = document.getString("email") ?: currentUser.email ?: ""
                        val role = document.getLong("role")?.toInt() ?: 1
                        userRole = if (role == 2) "Quản trị viên" else "Người dùng"
                    } else {
                        userName = currentUser.displayName ?: "Khách"
                        userEmail = currentUser.email ?: ""
                        userRole = "Người dùng"
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    userName = currentUser?.displayName ?: "Khách"
                    userEmail = currentUser?.email ?: ""
                    userRole = "Người dùng"
                    isLoading = false
                }
        } ?: run { isLoading = false }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Hồ sơ cá nhân",
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        val intent = Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CardColor
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BackgroundSoft)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (isLoading) {
                CircularProgressIndicator(color = GreenPrimary)
                return@Column
            }

            /* ===== AVATAR ===== */
            AvatarSection(userName)

            Spacer(Modifier.height(16.dp))

            Text(
                text = userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )

            Text(
                text = userEmail,
                fontSize = 14.sp,
                color = Color(0xFF6B6B6B)
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = ChipBg
            ) {
                Text(
                    text = userRole,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GreenDark
                )
            }

            Spacer(Modifier.height(32.dp))

            /* ===== ACTIONS ===== */
            ProfileActionCard(
                icon = Icons.Default.Receipt,
                text = "Đơn hàng của tôi"
            ) {
                context.startActivity(Intent(context, UserOrderHistoryActivity::class.java))
            }

            ProfileActionCard(
                icon = Icons.Default.RateReview,
                text = "Đánh giá của tôi"
            ) {
                context.startActivity(
                    Intent(context, com.bookstore.ui.review.MyReviewsActivity::class.java)
                )
            }
        }
    }
}

/* ===================== COMPONENTS ===================== */

@Composable
private fun AvatarSection(userName: String) {
    val initials = remember(userName) {
        userName.split(" ")
            .take(2)
            .joinToString("") { it.firstOrNull()?.toString() ?: "" }
            .uppercase()
    }

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ChipBg)
                .border(4.dp, CardColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreenDark
            )
        }

        Surface(
            modifier = Modifier
                .size(36.dp)
                .clickable { /* TODO chọn ảnh */ },
            shape = CircleShape,
            color = GreenPrimary,
            border = BorderStroke(2.dp, CardColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = GreenDark
            )

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9E9E9E)
            )
        }
    }
}
