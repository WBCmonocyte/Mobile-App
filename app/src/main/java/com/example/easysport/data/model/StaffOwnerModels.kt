package com.example.easysport.data.model

data class StaffQrCheckoutSession(
    val paymentId: String,
    val orderInvoiceNumber: String,
    val checkoutUrl: String,
    val amount: Int,
    val status: String,
    val isPaid: Boolean,
    val isExpired: Boolean,
    val expiresAt: String? = null,
    val serverNow: String? = null,
)

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
    val courtRevenue: Long = 0L,
    val posRevenue: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val indoor: Boolean = false,
    val fields: List<BranchFieldItem> = emptyList(),
)

data class FieldRevenueStat(
    val courtId: String,
    val courtName: String,
    val fieldName: String,
    val bookingCount: Int,
    val courtRevenue: Long,
    val serviceRevenue: Long,
    val totalRevenue: Long,
)

data class TimeSlotRevenueStat(
    val slot: String,
    val label: String,
    val bookingCount: Int,
    val revenue: Long,
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
    val walletRevenue: Long = 0L,
    val courtRevenue: Long = 0L,
    val posRevenue: Long = 0L,
    val totalBookings: Int,
    val activeBookings: Int,
    val totalCourts: Int,
    val pendingReports: Int,
    val pendingRecurringRequests: Int = 0,
    val bookings: List<StaffBookingItem>,
    val branchRevenues: List<BranchRevenueItem> = emptyList(),
    val fieldRevenues: List<FieldRevenueStat> = emptyList(),
    val timeSlotRevenues: List<TimeSlotRevenueStat> = emptyList(),
)

data class InventoryItemModel(
    val id: String,
    val courtId: String,
    val courtName: String,
    val name: String,
    val category: String,
    val unit: String,
    val importPrice: Int,
    val sellPrice: Int,
    val stockQuantity: Int,
    val minAlertThreshold: Int,
    val lowStock: Boolean,
    val sku: String = "",
    val barcode: String = "",
    val supplierName: String = "",
    val updatedAt: String = "",
)

data class InventoryTransactionModel(
    val id: String,
    val itemId: String,
    val itemName: String,
    val courtId: String,
    val type: String,
    val quantityDelta: Int,
    val stockAfter: Int,
    val unitCost: Int,
    val note: String,
    val createdBy: String,
    val createdAt: String,
)

data class OwnerInventoryData(
    val items: List<InventoryItemModel>,
    val transactions: List<InventoryTransactionModel>,
    val totalItems: Int,
    val totalUnits: Int,
    val lowStockCount: Int,
    val totalStockValue: Long,
)

data class TopSellingItemModel(
    val itemId: String,
    val itemName: String,
    val courtName: String,
    val unit: String,
    val soldUnits: Long,
    val revenue: Long,
    val profit: Long,
    val currentStock: Int,
)

data class CategoryStockStatModel(
    val category: String,
    val label: String,
    val itemCount: Int,
    val stockUnits: Int,
    val stockValue: Long,
    val soldUnits: Long,
    val revenue: Long,
)

data class BranchStockStatModel(
    val courtId: String,
    val courtName: String,
    val itemCount: Int,
    val stockUnits: Int,
    val stockValue: Long,
    val lowStockCount: Int,
    val revenue: Long,
    val profit: Long,
)

data class ReorderSuggestionModel(
    val itemId: String,
    val name: String,
    val courtId: String,
    val courtName: String,
    val unit: String,
    val currentStock: Int,
    val minAlertThreshold: Int,
    val suggestedQty: Int,
    val estimatedCost: Long,
)

data class OwnerInventoryStatsData(
    val period: String,
    val totalRevenue: Long,
    val totalCogs: Long,
    val grossProfit: Long,
    val profitMarginPercent: Double,
    val totalSoldUnits: Int,
    val totalImportSpend: Long,
    val totalImportedUnits: Int,
    val totalWastageCost: Long,
    val totalWastageUnits: Int,
    val totalTransferCount: Int,
    val totalStockValue: Long,
    val totalPotentialRevenue: Long,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val topSelling: List<TopSellingItemModel>,
    val categoryBreakdown: List<CategoryStockStatModel>,
    val branchBreakdown: List<BranchStockStatModel>,
    val reorderSuggestions: List<ReorderSuggestionModel>,
)

data class PosOrderItemRequest(
    val itemId: String,
    val quantity: Int,
)
