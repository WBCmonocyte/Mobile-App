package com.example.easysport.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.easysport.data.remote.PaymentApiClient
import kotlinx.coroutines.launch

@Composable
internal fun PasswordResetDialog(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var waitSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(waitSeconds) {
        if (waitSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            waitSeconds--
        }
    }

    fun requestCode() {
        busy = true
        scope.launch {
            PaymentApiClient.resetPassword(email.trim()).onSuccess {
                sent = true
                waitSeconds = 60
                message = "Nếu email đã đăng ký, mã sẽ đến hộp thư của bạn. Có thể yêu cầu mã mới sau 60 giây."
            }.onFailure { message = it.localizedMessage ?: "Gửi mã thất bại." }
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text(if (done) "Đã đổi mật khẩu" else "Quên mật khẩu") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (done) Text("Bạn có thể đăng nhập bằng mật khẩu mới.") else {
                    OutlinedTextField(email, { email = it }, enabled = !sent && !busy, singleLine = true, label = { Text("Email đã đăng ký") })
                    if (sent) {
                        OutlinedTextField(code, { code = it.filter { c -> c in '0'..'9' }.take(6) }, singleLine = true, label = { Text("Mã 6 chữ số") })
                        OutlinedTextField(password, { password = it }, singleLine = true, label = { Text("Mật khẩu mới") }, visualTransformation = PasswordVisualTransformation())
                        OutlinedTextField(repeatPassword, { repeatPassword = it }, singleLine = true, label = { Text("Nhập lại mật khẩu") }, visualTransformation = PasswordVisualTransformation())
                        Text("Dùng 8–128 ký tự, gồm ít nhất một chữ số.")
                        TextButton(onClick = { requestCode() }, enabled = !busy && waitSeconds == 0) {
                            Text(if (waitSeconds == 0) "Gửi lại mã" else "Gửi lại sau ${waitSeconds}s")
                        }
                    }
                    if (message.isNotEmpty()) Text(message)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                when {
                    done -> onClose()
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> message = "Nhập địa chỉ email hợp lệ."
                    !sent -> requestCode()
                    code.length != 6 || password.length !in 8..128 || password.none { it in '0'..'9' } -> message = "Kiểm tra mã và độ dài mật khẩu, gồm ít nhất một chữ số."
                    password != repeatPassword -> message = "Hai mật khẩu chưa khớp."
                    else -> {
                        busy = true
                        scope.launch {
                            PaymentApiClient.resetPassword(email.trim(), code, password).onSuccess { done = true }
                                .onFailure { message = it.localizedMessage ?: "Đổi mật khẩu thất bại." }
                            busy = false
                        }
                    }
                }
            }) { Text(if (done) "Đăng nhập" else if (sent) "Đổi mật khẩu" else "Gửi mã") }
        },
        dismissButton = { if (!done) TextButton(onClick = onClose, enabled = !busy) { Text("Hủy") } }
    )
}
