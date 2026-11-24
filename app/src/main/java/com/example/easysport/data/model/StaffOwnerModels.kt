package com.example.easysport.data.model

data class StaffBookingItem(
    val id: String,
    val courtId: String,
    val courtName: String,
    val fieldDetail: String,
    val timeRange: String,
    val dateLabel: String,
    val totalPrice: Int,
    val depositAmount: Int,
    val remainingAmount: Int,
    val paymentMethod: String,
    val status: String,
    val customerName: String,
    val customerPhone: String,
    val serviceNote: String = "",
    val serviceAmount: Int = 0,
)

data class StaffShiftItem(
    val id: String,
    val staffId: String,
    val staffName: String,
    val staffPhone: String,
    val courtId: String,
    val courtName: String,
    val shiftDate: String,
    val shiftSlot: String,
    val duty: String,
    val status: String,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val lateMinutes: Int? = null,
    val notes: String = "",
)

data class ShiftTemplate(
    val code: String,
    val label: String,
    val start: String,
    val end: String,
    val hours: Double,
)

data class ScheduleSlot(
    val code: String,
    val label: String,
    val start: String,
    val end: String,
    val minStaff: Int,
    val shifts: List<StaffShiftItem>,
)

data class ScheduleDay(
    val date: String,
    val weekday: String,
    val isPast: Boolean,
    val slots: List<ScheduleSlot>,
)

data class StaffWorkload(
    val staffId: String,
    val name: String,
    val phone: String,
    val hours: Double,
    val shiftCount: Int,
    val lateCount: Int = 0,
    val totalLateMinutes: Int = 0,
    val earlyCount: Int = 0,
)

data class OwnerWeekSchedule(
    val weekStart: String,
    val weekEnd: String,
    val maxWeeklyHours: Int,
    val totalPositions: Int,
    val filledPositions: Int,
    val templates: List<ShiftTemplate>,
    val days: List<ScheduleDay>,
    val staffStats: List<StaffWorkload>,
)

data class ShiftReportItem(
    val id: String,
    val courtId: String,
    val courtName: String,
    val staffId: String,
    val staffName: String,
    val shiftDate: String,
    val cashAmount: Int,
    val qrAmount: Int,
    val totalOrders: Int,
    val handoverNote: String,
    val status: String,
    val confirmedBy: String? = null,
    val shiftId: String? = null,
)

data class StaffMemberOption(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
)

data class PosSaleItem(
    val id: String,
    val courtId: String,
    val courtName: String,
    val staffId: String,
    val staffName: String,
    val shiftId: String? = null,
    val itemsNote: String,
    val amount: Int,
    val paymentMethod: String,
    val saleDate: String,
)

data class StaffShiftSummary(
    val shiftId: String? = null,
    val cashFromBookings: Int = 0,
    val qrFromBookings: Int = 0,
    val retailCash: Int = 0,
    val retailQr: Int = 0,
    val totalCash: Int = 0,
    val totalQr: Int = 0,
    val orderCount: Int = 0,
    val retailCount: Int = 0,
    val unpaidActiveCount: Int = 0,
    val unpaidTotal: Int = 0,
)

data class BranchFieldItem(
    val id: String,
    val label: String,
    val detail: String,
    val fieldType: String = "5_VS_5",
)

data class BranchRevenueItem(
    val courtId: String,
    val courtName: String,
    val address: String,
    val sportType: String,
    val fieldCount: Int,
    val pricePerHour: Int,
    val morningPrice: Int,
    val afternoonPrice: Int,
    val eveningPrice: Int,
    val bookingCount: Int,
    val activeCount: Int,
    val revenue: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val indoor: Boolean = false,
    val fields: List<BranchFieldItem> = emptyList(),
)

data class OwnerStaffAccount(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val locked: Boolean,
    val shiftCount: Int,
    val address: String = "",
    val completedShifts: Int = 0,
    val lateCount: Int = 0,
    val earlyCount: Int = 0,
    val totalHours: Double = 0.0,
)

data class OwnerOverviewData(
    val totalRevenue: Long,
    val cashRevenue: Long,
    val qrRevenue: Long,
    val totalBookings: Int,
    val activeBookings: Int,
    val totalCourts: Int,
    val pendingReports: Int,
    val bookings: List<StaffBookingItem>,
    val branchRevenues: List<BranchRevenueItem> = emptyList(),
)
