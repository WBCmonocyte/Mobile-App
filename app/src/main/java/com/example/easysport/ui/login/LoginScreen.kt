package com.example.easysport.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Màu sắc ----------
internal val Background = Color(0xFFF5F9FF)
internal val Primary = Color(0xFF4472B4)
internal val TitleColor = Color(0xFF1F3550)
internal val BodyColor = Color(0xFF7C8CA3)
internal val FieldBorder = Color(0xFFE1E8F2)
internal val IconTint = Color(0xFF7C8CA3)
internal val FieldBg = Color.White
internal val FieldPaleBlue = Color(0xFFD6E4F8)
internal val FieldPaleBlueDark = Color(0xFFC3D8F3)
internal val LogoBg = Color(0xFFDCE9FA)
internal val ErrorColor = Color(0xFFC62828)

// ---------- Màn hình ----------
@Composable
fun LoginScreen(
    onSignIn: (email: String, password: String) -> Unit = { _, _ -> },
    onForgotPassword: () -> Unit = {},
    onSignUp: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onInputChange: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        BrandHeader()

        Spacer(Modifier.height(8.dp))
        FieldIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Chào mừng trở lại!",
            color = TitleColor,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Đăng nhập để đặt sân nhanh chóng và sẵn sàng cho trận đấu tiếp theo.",
            color = BodyColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(32.dp))
        FieldLabel("Địa chỉ Email")
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = email,
            onValueChange = { email = it; onInputChange() },
            placeholder = "user@example.com",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
        )

        Spacer(Modifier.height(20.dp))
        FieldLabel("Mật khẩu")
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it; onInputChange() },
            placeholder = "Nhập mật khẩu của bạn",
            leadingIcon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                Icon(
                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                    else Icons.Outlined.Visibility,
                    contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                    tint = IconTint,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { passwordVisible = !passwordVisible },
                )
            },
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Quên mật khẩu?",
            color = Primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.End)
                .clickable(onClick = onForgotPassword),
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = ErrorColor,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onSignIn(email, password) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White,
                disabledContainerColor = Primary.copy(alpha = 0.7f),
                disabledContentColor = Color.White,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = "Đăng nhập", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Đăng nhập nhanh (Test chức năng)",
            color = BodyColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple("Khách hàng", "nguyenvana@gmail.com", Color(0xFF0EA5E9)),
                Triple("Nhân viên", "staff@easysport.vn", Color(0xFF10B981)),
                Triple("Chủ sân", "owner@easysport.vn", Primary),
            ).forEach { (label, demoEmail, accent) ->
                OutlinedButton(
                    onClick = {
                        email = demoEmail
                        password = "123456"
                        onInputChange()
                        onSignIn(demoEmail, "123456")
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = accent.copy(alpha = 0.08f),
                        contentColor = accent,
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Chưa có tài khoản? ", color = BodyColor, fontSize = 14.sp)
            Text(
                text = "Đăng ký ngay",
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onSignUp),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ---------- Thành phần con ----------
@Composable
internal fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(LogoBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val stroke = Stroke(width = 1.8.dp.toPx())
                drawRoundRect(
                    color = Primary,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke,
                )
                drawLine(Primary, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5.dp.toPx())
                drawCircle(Primary, radius = size.minDimension * 0.18f, style = Stroke(1.5.dp.toPx()))
            }
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Easy Sport",
            color = TitleColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TitleColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text(placeholder, color = BodyColor.copy(alpha = 0.8f), fontSize = 16.sp) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = IconTint, modifier = Modifier.size(24.dp))
        },
        trailingIcon = trailingIcon,
        isError = isError,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedBorderColor = Primary,
            unfocusedBorderColor = FieldBorder,
            cursorColor = Primary,
            focusedTextColor = TitleColor,
            unfocusedTextColor = TitleColor,
            errorBorderColor = ErrorColor,
            errorContainerColor = FieldBg,
            errorTextColor = TitleColor,
            errorCursorColor = ErrorColor,
        ),
    )
}

/** Hình minh họa sân bóng nghiêng, vẽ bằng Canvas. */
@Composable
internal fun FieldIllustration(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Chấm nhỏ trang trí
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 0.dp, top = 6.dp)
                .size(8.dp)
                .background(FieldPaleBlueDark, CircleShape),
        )

        // Vầng sáng phía sau
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(110.dp)
                .background(Color(0xFFE6EFFC), CircleShape),
        )

        // Sân bóng
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(100.dp)
                .rotate(-6f),
        ) {
            val w = size.width
            val h = size.height
            val line = Color.White.copy(alpha = 0.85f)
            val sw = 1.5.dp.toPx()

            // Nền sân (bo góc)
            drawRoundRect(
                color = FieldPaleBlue,
                size = Size(w, h),
                cornerRadius = CornerRadius(18.dp.toPx()),
            )

            // Khung đường biên
            val pad = 10.dp.toPx()
            drawRoundRect(
                color = line,
                topLeft = Offset(pad, pad),
                size = Size(w - pad * 2, h - pad * 2),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(sw),
            )

            // Vạch giữa sân + vòng tròn giữa sân
            drawLine(line, Offset(w / 2, pad), Offset(w / 2, h - pad), sw)
            drawCircle(line, radius = h * 0.17f, center = Offset(w / 2, h / 2), style = Stroke(sw))
            drawCircle(line, radius = 2.dp.toPx(), center = Offset(w / 2, h / 2))

            // Vòng cấm địa hai bên
            val boxW = w * 0.14f
            val boxH = h * 0.42f
            drawRect(
                color = line,
                topLeft = Offset(pad, (h - boxH) / 2),
                size = Size(boxW, boxH),
                style = Stroke(sw),
            )
            drawRect(
                color = line,
                topLeft = Offset(w - pad - boxW, (h - boxH) / 2),
                size = Size(boxW, boxH),
                style = Stroke(sw),
            )
        }

        // Hai nét "chuyển động" bên phải
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp, top = 56.dp)
                .size(width = 18.dp, height = 14.dp),
        ) {
            val sw = 1.5.dp.toPx()
            drawLine(
                FieldPaleBlueDark, Offset(0f, 0f), Offset(size.width, 4.dp.toPx()),
                strokeWidth = sw, cap = StrokeCap.Round,
            )
            drawLine(
                FieldPaleBlueDark, Offset(2.dp.toPx(), 10.dp.toPx()),
                Offset(size.width - 4.dp.toPx(), size.height),
                strokeWidth = sw, cap = StrokeCap.Round,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    LoginScreen()
}
