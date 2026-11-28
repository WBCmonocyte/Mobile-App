package com.example.easysport.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.Booking
import com.example.easysport.data.model.User
import com.example.easysport.data.repository.BookingStore
import com.example.easysport.data.repository.UserStore
import com.example.easysport.ui.common.EsBottomBar
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsLogoMark
import com.example.easysport.ui.home.HomeTab

@Composable
fun AccountScreen(
    user: User? = null,
    bookings: List<Booking>? = null,
    onTabSelected: (HomeTab) -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenBooking: (String) -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
) {
    val currentUser = user ?: remember {
        User(
            id = "user_preview",
            email = "",
            name = "Nguyễn Văn A",
            phone = "0901 234 567",
            dateOfBirth = "",
            address = "",
        )
    }

    val allBookings = bookings ?: remember { emptyList() }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showBookingsDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            AccountHeader()

            Spacer(Modifier.height(20.dp))
            UserProfileCard(
                user = currentUser,
                onEditClick = { showEditProfileDialog = true }
            )

            Spacer(Modifier.height(24.dp))
            PersonalInfoSection(user = currentUser)

            Spacer(Modifier.height(24.dp))
            AccountSection(
                onOpenBookings = { showBookingsDialog = true },
                onOpenWallet = onOpenWallet,
                onOpenRecurring = onOpenRecurring,
                onOpenSecurity = { showSecurityDialog = true },
                onOpenHelp = { showHelpDialog = true },
            )

            Spacer(Modifier.height(28.dp))
            LogoutButton(onClick = { showLogoutDialog = true })

            Spacer(Modifier.height(24.dp))
        }

        EsBottomBar(selected = HomeTab.ACCOUNT, onSelected = onTabSelected)
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            user = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, dob, email, address ->
                UserStore.updateProfile(
                    name = name,
                    phone = phone,
                    dateOfBirth = dob,
                    email = email,
                    address = address,
                )
                showEditProfileDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Đăng xuất",
                    fontWeight = FontWeight.Bold,
                    color = EsColors.Title,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản EasySport?",
                    color = EsColors.Body,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EsColors.Danger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hủy", color = EsColors.Title)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSecurityDialog) {
        SecuritySettingsDialog(onDismiss = { showSecurityDialog = false })
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showBookingsDialog) {
        MyBookingsDialog(
            bookings = allBookings,
            onDismiss = { showBookingsDialog = false },
            onOpenBooking = { bookingId ->
                showBookingsDialog = false
                onOpenBooking(bookingId)
            }
        )
    }
}

// ---------------------------------------------------------------------
@Composable
private fun AccountHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Hồ sơ",
            color = EsColors.Title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Easy Sport",
                color = Color(0xFF64748B),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(8.dp))
            EsLogoMark(markSize = 32.dp)
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun UserProfileCard(user: User, onEditClick: () -> Unit) {
    val cardBg = Color(0xFFF8FAFC)
    val initials = remember(user.name) {
        val parts = user.name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        when {
            parts.isEmpty() -> "US"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> ("" + parts.first().first() + parts.last().first()).uppercase()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2EDF8)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        color = EsColors.Primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = user.name.ifBlank { "Khách hàng" },
                        color = EsColors.Title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = user.phone.ifBlank { "Chưa có số điện thoại" },
                        color = Color(0xFF64748B),
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onEditClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Chỉnh sửa hồ sơ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun PersonalInfoSection(user: User) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Thông tin cá nhân",
            color = EsColors.Title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                InfoRow(label = "Họ và tên", value = user.name, isBold = true)
                HorizontalDivider(color = Color(0xFFEEF2F6))
                InfoRow(label = "Ngày sinh", value = user.dateOfBirth)
                HorizontalDivider(color = Color(0xFFEEF2F6))
                InfoRow(label = "Số điện thoại", value = user.phone, isBold = true)
                HorizontalDivider(color = Color(0xFFEEF2F6))
                InfoRow(label = "Email", value = user.email)
                HorizontalDivider(color = Color(0xFFEEF2F6))
                InfoRow(label = "Địa chỉ", value = user.address)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isBold: Boolean = false) {
    val displayValue = value.ifBlank { "Chưa cập nhật" }
    val isFilled = value.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 15.sp,
        )
        Text(
            text = displayValue,
            color = if (isFilled) EsColors.Title else Color(0xFF94A3B8),
            fontSize = 15.sp,
            fontWeight = if (isFilled && isBold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ---------------------------------------------------------------------
@Composable
private fun AccountSection(
    onOpenBookings: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Tài khoản",
            color = EsColors.Title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        ) {
            Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                AccountMenuItem(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Ví EasySport",
                    onClick = onOpenWallet,
                )
                HorizontalDivider(color = Color(0xFFEEF2F6), modifier = Modifier.padding(horizontal = 14.dp))
                AccountMenuItem(
                    icon = Icons.Outlined.EventRepeat,
                    title = "Lịch cố định của tôi",
                    onClick = onOpenRecurring,
                )
                HorizontalDivider(color = Color(0xFFEEF2F6), modifier = Modifier.padding(horizontal = 14.dp))
                AccountMenuItem(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Lịch đặt sân của tôi",
                    onClick = onOpenBookings,
                )
                HorizontalDivider(color = Color(0xFFEEF2F6), modifier = Modifier.padding(horizontal = 14.dp))
                AccountMenuItem(
                    icon = Icons.Outlined.Shield,
                    title = "Bảo mật tài khoản",
                    onClick = onOpenSecurity,
                )
                HorizontalDivider(color = Color(0xFFEEF2F6), modifier = Modifier.padding(horizontal = 14.dp))
                AccountMenuItem(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "Trợ giúp",
                    onClick = onOpenHelp,
                )
            }
        }
    }
}

@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EsColors.Primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            color = EsColors.Title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp),
        )
    }
}

// ---------------------------------------------------------------------
@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = "Đăng xuất",
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Đăng xuất",
            color = Color(0xFF64748B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ---------------------------------------------------------------------
@Composable
private fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, dob: String, email: String, address: String) -> Unit,
) {
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var dob by remember { mutableStateOf(user.dateOfBirth) }
    var email by remember { mutableStateOf(user.email) }
    var address by remember { mutableStateOf(user.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chỉnh sửa thông tin", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Ngày sinh (dd/MM/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, dob, email, address) },
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Lưu thay đổi", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Hủy", color = EsColors.Title)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

// ---------------------------------------------------------------------
@Composable
private fun SecuritySettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Bảo mật tài khoản", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Đổi mật khẩu", color = EsColors.Title, fontSize = 15.sp)
                    TextButton(onClick = {}) {
                        Text("Thay đổi", color = EsColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Color(0xFFEEF2F6))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Xác thực hai bước (2FA)", color = EsColors.Title, fontSize = 15.sp)
                        Text("Bảo vệ tài khoản qua OTP SMS", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                    Text("Đã bật", color = EsColors.Success, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                HorizontalDivider(color = Color(0xFFEEF2F6))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Đăng nhập sinh trắc học", color = EsColors.Title, fontSize = 15.sp)
                        Text("Vân tay / FaceID", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                    Text("Sẵn sàng", color = EsColors.Primary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Đóng", color = Color.White)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

// ---------------------------------------------------------------------
@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Trung tâm trợ giúp", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tổng đài hỗ trợ EasySport:", color = Color(0xFF64748B), fontSize = 14.sp)
                Text("📞 1900 6868 (8:00 - 22:00)", fontWeight = FontWeight.Bold, color = EsColors.Primary, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Email tiếp nhận phản hồi:", color = Color(0xFF64748B), fontSize = 14.sp)
                Text("✉️ support@easysport.vn", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("Cổng thanh toán hỗ trợ:", color = Color(0xFF64748B), fontSize = 14.sp)
                Text("💳 Cổng SePay (QR Transfer & Cash on Site)", fontWeight = FontWeight.Medium, color = EsColors.Title, fontSize = 14.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Đã hiểu", color = Color.White)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

// ---------------------------------------------------------------------
@Composable
private fun MyBookingsDialog(
    bookings: List<Booking>,
    onDismiss: () -> Unit,
    onOpenBooking: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Lịch đặt sân của tôi", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 18.sp)
        },
        text = {
            if (bookings.isEmpty()) {
                Text("Bạn chưa có lịch đặt sân nào.", color = Color(0xFF64748B), fontSize = 14.sp)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    bookings.forEach { booking ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBooking(booking.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(booking.courtName, fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 15.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("${booking.fieldDetail} · ${booking.timeRange}", color = EsColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(booking.dateLabel, color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Đóng", color = Color.White)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountPreview() {
    AccountScreen()
}

@Preview(name = "Hồ sơ - Có System UI", showBackground = true, showSystemUi = true)
@Preview(name = "Hồ sơ - Thiết bị 390x844", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AccountScreenPreview() {
    AccountScreen(
        user = User(
            id = "user_preview",
            email = "",
            name = "Nguyễn Văn A",
            phone = "0901 234 567",
            dateOfBirth = "",
            address = "",
        ),
        bookings = emptyList(),
    )
}

@Preview(name = "Thẻ Hồ sơ người dùng", showBackground = true)
@Composable
fun UserProfileCardPreview() {
    Box(Modifier.padding(16.dp)) {
        UserProfileCard(
            user = User(
                id = "1",
                email = "",
                name = "Nguyễn Văn A",
                phone = "0901 234 567",
                dateOfBirth = "",
                address = "",
            ),
            onEditClick = {}
        )
    }
}

@Preview(name = "Mục Thông tin cá nhân", showBackground = true)
@Composable
fun PersonalInfoSectionPreview() {
    Box(Modifier.padding(16.dp)) {
        PersonalInfoSection(
            user = User(
                id = "1",
                email = "",
                name = "Nguyễn Văn A",
                phone = "0901 234 567",
                dateOfBirth = "",
                address = "",
            )
        )
    }
}

