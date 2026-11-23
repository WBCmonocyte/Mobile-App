package com.example.easysport.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.easysport.ui.common.EsColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RentalDateButton(date: LocalDate, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    Text("Chọn ngày thuê", color = EsColors.Body, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Button(onClick = {
        DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
            date.year, date.monthValue - 1, date.dayOfMonth).apply {
            datePicker.minDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.show()
    }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary, contentColor = Color.White)) {
        Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Mở lịch chọn ngày", modifier = Modifier.size(24.dp))
    }
}
