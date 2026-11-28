package com.example.easysport.ui.common

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.easysport.data.model.User
import com.example.easysport.data.repository.UserStore

private fun String?.cleanOrEmpty(): String =
    this?.trim()?.takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) } ?: ""

@Composable
fun StaffOwnerProfileDialog(
    user: User?,
    roleLabel: String,
    onDismiss: () -> Unit,
    onRequestLogout: () -> Unit,
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var name by remember(user) { mutableStateOf(user?.name.cleanOrEmpty()) }
    var phone by remember(user) { mutableStateOf(user?.phone.cleanOrEmpty()) }
    var dob by remember(user) { mutableStateOf(user?.dateOfBirth.cleanOrEmpty()) }
    var address by remember(user) { mutableStateOf(user?.address.cleanOrEmpty()) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isEditing) "Chỉnh sửa hồ sơ" else "Tài khoản cá nhân",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProColors.Ink,
                    )
                    IconButton(
                        onClick = { if (!isSaving) onDismiss() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = ProColors.Muted)
                    }
                }

                // Header Profile Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ProHeroBrush)
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProAvatar(user?.name ?: "U", size = 52.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = user?.name?.ifBlank { "Người dùng EasySport" } ?: "Người dùng EasySport",
                                color = Color.White,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = user?.email?.ifBlank { user.phone } ?: "",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = roleLabel,
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }

                if (!isEditing) {
                    Surface(
                        color = ProColors.Surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ProColors.Border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProfileInfoRow(Icons.Rounded.Person, "Họ và tên", user?.name.cleanOrEmpty().ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = ProColors.Border)
                            ProfileInfoRow(Icons.Rounded.Phone, "Số điện thoại", user?.phone.cleanOrEmpty().ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = ProColors.Border)
                            ProfileInfoRow(Icons.Rounded.Email, "Email", user?.email.cleanOrEmpty().ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = ProColors.Border)
                            ProfileInfoRow(Icons.Rounded.Cake, "Ngày sinh", user?.dateOfBirth.cleanOrEmpty().ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = ProColors.Border)
                            ProfileInfoRow(Icons.Rounded.LocationOn, "Địa chỉ", user?.address.cleanOrEmpty().ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = ProColors.Border)
                            ProfileInfoRow(Icons.Rounded.Badge, "Vai trò", roleLabel)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                name = user?.name.cleanOrEmpty()
                                phone = user?.phone.cleanOrEmpty()
                                dob = user?.dateOfBirth.cleanOrEmpty()
                                address = user?.address.cleanOrEmpty()
                                errorMsg = null
                                isEditing = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cập nhật hồ sơ", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onRequestLogout()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ProColors.Rose),
                            modifier = Modifier.height(46.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Đăng xuất", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        label = { Text("Họ và tên *") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMsg = null },
                        label = { Text("Số điện thoại *") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Ngày sinh (dd/MM/yyyy)") },
                        placeholder = { Text("VD: 15/05/1995") },
                        leadingIcon = { Icon(Icons.Rounded.Cake, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Địa chỉ") },
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = ProColors.Rose, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text("Hủy", color = ProColors.SubInk, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                if (name.trim().isEmpty()) {
                                    errorMsg = "Vui lòng nhập họ và tên"
                                    return@Button
                                }
                                if (phone.trim().length < 9) {
                                    errorMsg = "Số điện thoại không hợp lệ"
                                    return@Button
                                }
                                isSaving = true
                                errorMsg = null
                                UserStore.updateProfile(
                                    name = name,
                                    phone = phone,
                                    dateOfBirth = dob,
                                    email = user?.email ?: "",
                                    address = address,
                                ) { success, err ->
                                    isSaving = false
                                    if (success) {
                                        isEditing = false
                                        Toast.makeText(context, "Đã cập nhật thông tin tài khoản", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMsg = err ?: "Cập nhật thất bại, vui lòng thử lại"
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ProColors.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 12.5.sp, color = ProColors.Muted, modifier = Modifier.width(96.dp))
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProColors.Ink,
            modifier = Modifier.weight(1f),
        )
    }
}
