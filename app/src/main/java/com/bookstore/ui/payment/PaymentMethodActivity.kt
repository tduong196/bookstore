package com.bookstore.ui.payment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.bookstore.ui.theme.BookstoreTheme
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext


class PaymentMethodActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            BookstoreTheme {
                PaymentMethodScreen()
            }
        }
    }
}
@Composable
fun PaymentMethodScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val paymentOptions = listOf(
        PaymentMethod("Thanh toán tiền mặt", "Thanh toán khi nhận hàng", Icons.Default.Money),
        PaymentMethod("Credit or debit card", "Thẻ Visa hoặc Mastercard", Icons.Default.CreditCard),
        PaymentMethod("Chuyển khoản ngân hàng", "Tự động xác nhận", Icons.Default.AccountBalance),
        PaymentMethod("ZaloPay", "Tự động xác nhận", Icons.Default.AccountBalanceWallet),
    )

    var selectedOption by remember { mutableStateOf(-1) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
        .padding(16.dp)) {
        Text("Phương thức thanh toán", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(paymentOptions) { index, method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = index }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = method.icon,
                        contentDescription = method.title,
                        tint = Color(0xFF0066CC),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(method.title, fontWeight = FontWeight.SemiBold)
                        Text(method.subtitle, color = Color.Gray, fontSize = 13.sp)
                    }
                    RadioButton(
                        selected = selectedOption == index,
                        onClick = { selectedOption = index }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedOption != -1) {
                    val intent = Intent().apply {
                        putExtra("payment_method", paymentOptions[selectedOption].title)
                    }
                    activity?.setResult(Activity.RESULT_OK, intent)
                    activity?.finish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = selectedOption != -1,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
        ) {
            Text("Xác nhận", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

data class PaymentMethod(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)
