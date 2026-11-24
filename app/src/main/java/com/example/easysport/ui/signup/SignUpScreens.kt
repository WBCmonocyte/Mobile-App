package com.example.easysport.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.ui.login.AuthTextField
import com.example.easysport.ui.login.Background
import com.example.easysport.ui.login.BodyColor
import com.example.easysport.ui.login.BrandHeader
import com.example.easysport.ui.login.ErrorColor
import com.example.easysport.ui.login.FieldIllustration
import com.example.easysport.ui.login.FieldLabel
import com.example.easysport.ui.login.IconTint
import com.example.easysport.ui.login.LogoBg
import com.example.easysport.ui.login.Primary
import com.example.easysport.ui.login.TitleColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DOB_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd / MM / yyyy")

// =====================================================================
// Màn hình 1: Join the game (Step 1 of 2)
// =====================================================================
@Composable
fun SignUpDetailsScreen(
    form: SignUpForm,
    errors: SignUpErrors,
    onFormChange: (SignUpForm) -> Unit,
    onNext: () -> Unit,
    onSignIn: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    ScreenContainer {
        Spacer(Modifier.height(16.dp))
        SignUpHeader(step = 1)

        Spacer(Modifier.height(24.dp))
        ScreenTitle("Join the game.")
        Spacer(Modifier.height(8.dp))
        ScreenSubtitle("Your next field is just a few taps away.")

        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FormField("Full name", errors.fullName) {
                AuthTextField(
                    value = form.fullName,
                    onValueChange = { onFormChange(form.copy(fullName = it)) },
                    placeholder = "Enter your full name",
                    leadingIcon = Icons.Outlined.Person,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    isError = errors.fullName != null,
                )
            }
            FormField("Day of Birth", errors.dob) {
                Box {
                    AuthTextField(
                        value = form.dob?.format(DOB_FORMAT).orEmpty(),
                        onValueChange = {},
                        placeholder = "DD / MM / YYYY",
                        leadingIcon = Icons.Outlined.CalendarMonth,
                        keyboardType = KeyboardType.Number,
                        readOnly = true,
                        isError = errors.dob != null,
                    )
                    // Lớp phủ bắt thao tác chạm để mở DatePicker
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showDatePicker = true },
                    )
                }
            }
            FormField("Phone number", errors.phone) {
                AuthTextField(
                    value = form.phone,
                    onValueChange = { onFormChange(form.copy(phone = it)) },
                    placeholder = "Enter your phone number",
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardType = KeyboardType.Phone,
                    isError = errors.phone != null,
                )
            }
            FormField("Email", errors.email) {
                AuthTextField(
                    value = form.email,
                    onValueChange = { onFormChange(form.copy(email = it)) },
                    placeholder = "you@example.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    isError = errors.email != null,
                )
            }
            FormField("Address", errors.address) {
                AuthTextField(
                    value = form.address,
                    onValueChange = { onFormChange(form.copy(address = it)) },
                    placeholder = "Enter your address",
                    leadingIcon = Icons.Outlined.LocationOn,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                    isError = errors.address != null,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Next", onClick = onNext)

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Already have an account? ", color = BodyColor, fontSize = 14.sp)
            Text(
                text = "Sign in",
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onSignIn),
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showDatePicker) {
        DobPickerDialog(
            initial = form.dob,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onFormChange(form.copy(dob = it))
                showDatePicker = false
            },
        )
    }
}

// =====================================================================
// Màn hình 2: One last step (Step 2 of 2)
// =====================================================================
@Composable
fun SignUpPasswordScreen(
    form: SignUpForm,
    errors: SignUpErrors,
    onFormChange: (SignUpForm) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    isLoading: Boolean = false,
    submitError: String? = null,
) {
    ScreenContainer {
        Spacer(Modifier.height(16.dp))
        SignUpHeader(step = 2)

        Spacer(Modifier.height(8.dp))
        FieldIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        )

        Spacer(Modifier.height(12.dp))
        ScreenTitle("One last step.")
        Spacer(Modifier.height(8.dp))
        ScreenSubtitle("Create a password to keep your account safe.\nThen you’re ready to play.")

        Spacer(Modifier.height(28.dp))
        FormField("Password", error = null) {
            PasswordField(
                value = form.password,
                onValueChange = { onFormChange(form.copy(password = it)) },
                placeholder = "Enter your password",
                isError = errors.password != null,
            )
        }

        Spacer(Modifier.height(16.dp))
        FormField("Confirm password", errors.confirmPassword) {
            PasswordField(
                value = form.confirmPassword,
                onValueChange = { onFormChange(form.copy(confirmPassword = it)) },
                placeholder = "Re-enter your password",
                isError = errors.confirmPassword != null,
                imeAction = ImeAction.Done,
            )
        }

        Spacer(Modifier.height(12.dp))
        val hintColor = if (errors.password != null) ErrorColor else BodyColor
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = hintColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Use at least 8 characters, including a number.",
                color = hintColor,
                fontSize = 12.sp,
            )
        }

        if (submitError != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = submitError,
                color = ErrorColor,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Gửi mã xác nhận",
            onClick = onComplete,
            enabled = !isLoading,
            isLoading = isLoading,
        )

        Spacer(Modifier.height(20.dp))
        val linkStyle = TextLinkStyles(
            style = SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = buildAnnotatedString {
                append("By continuing, you agree to our ")
                withLink(LinkAnnotation.Clickable("terms", linkStyle) { onTerms() }) {
                    append("Terms of Service")
                }
                append(" and ")
                withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onPrivacy() }) {
                    append("Privacy Policy.")
                }
            },
            color = BodyColor,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Back to your details",
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

// =====================================================================
// Thành phần dùng chung
// =====================================================================
@Composable
internal fun SignUpVerificationScreen(
    challengeId: String,
    email: String,
    isLoading: Boolean,
    error: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
) {
    var code by remember(challengeId) { mutableStateOf("") }
    var resendSeconds by remember(challengeId) { mutableStateOf(60) }
    LaunchedEffect(challengeId) {
        while (resendSeconds > 0) {
            delay(1000)
            resendSeconds--
        }
    }
    ScreenContainer {
        Spacer(Modifier.height(16.dp))
        SignUpHeader(step = 3)
        Spacer(Modifier.height(24.dp))
        ScreenTitle("Xác nhận email")
        Spacer(Modifier.height(8.dp))
        Text("Nhập mã 6 chữ số đã gửi tới $email", color = BodyColor)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { char -> char in '0'..'9' }.take(6) },
            label = { Text("Mã xác nhận") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = ErrorColor)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = "Xác nhận",
            onClick = { onVerify(code) },
            enabled = !isLoading && code.length == 6,
        )
        TextButton(onClick = onResend, enabled = !isLoading && resendSeconds == 0) {
            Text(if (resendSeconds > 0) "Gửi lại mã ($resendSeconds giây)" else "Gửi lại mã")
        }
        TextButton(onClick = onBack, enabled = !isLoading) {
            Text("Quay lại")
        }
    }
}

@Composable
private fun ScreenContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        content = content,
    )
}

/** Logo + chỉ báo "Step x of y" ở góc phải. */
@Composable
private fun SignUpHeader(step: Int, totalSteps: Int = 3) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BrandHeader()
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Step $step of $totalSteps",
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(totalSteps) { index ->
                    Box(
                        Modifier
                            .size(width = 22.dp, height = 3.dp)
                            .background(
                                color = if (index < step) Primary else LogoBg,
                                shape = RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        color = TitleColor,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp,
    )
}

@Composable
private fun ScreenSubtitle(text: String) {
    Text(text = text, color = BodyColor, fontSize = 16.sp, lineHeight = 24.sp)
}

/** Nhãn + ô nhập + dòng lỗi (nếu có). */
@Composable
private fun FormField(
    label: String,
    error: String?,
    content: @Composable () -> Unit,
) {
    Column {
        FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        content()
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = error, color = ErrorColor, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    imeAction: ImeAction = ImeAction.Next,
) {
    var visible by remember { mutableStateOf(false) }
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = Icons.Outlined.Lock,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Icon(
                imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                tint = IconTint,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { visible = !visible },
            )
        },
        isError = isError,
        imeAction = imeAction,
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobPickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = 1920..LocalDate.now().year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= System.currentTimeMillis()
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text("Đồng ý") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    ) {
        DatePicker(state = state)
    }
}

// =====================================================================
// Preview
// =====================================================================
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpDetailsPreview() {
    SignUpDetailsScreen(
        form = SignUpForm(),
        errors = SignUpErrors(),
        onFormChange = {},
        onNext = {},
        onSignIn = {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpPasswordPreview() {
    SignUpPasswordScreen(
        form = SignUpForm(),
        errors = SignUpErrors(),
        onFormChange = {},
        onComplete = {},
        onBack = {},
    )
}

