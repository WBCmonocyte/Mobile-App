package com.example.easysport.data.model

data class WalletTransactionItem(
    val id: String,
    val userId: String,
    val txType: String,
    val direction: String = "CREDIT",
    val amount: Int,
    val balanceAfter: Int,
    val reason: String,
    val referenceBookingId: String? = null,
    val referencePackageId: String? = null,
    val referenceSessionId: String? = null,
    val createdAt: String = "",
) {
    val isPositive: Boolean
        get() = if (direction.isNotBlank()) direction.equals("CREDIT", ignoreCase = true) else amount >= 0
}

data class WalletSummary(
    val walletId: String,
    val userId: String,
    val balance: Int,
    val currency: String = "VND",
    val updatedAt: String = "",
    val transactions: List<WalletTransactionItem> = emptyList(),
)
