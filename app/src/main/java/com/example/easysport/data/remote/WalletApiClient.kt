package com.example.easysport.data.remote

import com.example.easysport.data.model.WalletSummary
import com.example.easysport.data.model.WalletTransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object WalletApiClient {

    suspend fun getMyWallet(): Result<WalletSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = sendGet("${PaymentApiClient.baseUrl}/api/wallet")
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không tải được Ví EasySport (HTTP $code)"))
            }
            val data = JSONObject(body).getJSONObject("data")
            val txArr = data.optJSONArray("transactions") ?: JSONArray()
            WalletSummary(
                walletId = data.optString("walletId", ""),
                userId = data.optString("userId", ""),
                balance = data.optInt("balance", 0),
                currency = data.optString("currency", "VND"),
                updatedAt = data.optString("updatedAt", ""),
                transactions = parseTransactions(txArr),
            )
        }
    }

    suspend fun getTransactions(): Result<List<WalletTransactionItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = sendGet("${PaymentApiClient.baseUrl}/api/wallet/transactions")
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không tải được lịch sử Ví (HTTP $code)"))
            }
            val arr = JSONObject(body).optJSONArray("data") ?: JSONArray()
            parseTransactions(arr)
        }
    }

    private fun parseTransactions(arr: JSONArray): List<WalletTransactionItem> {
        val list = ArrayList<WalletTransactionItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val rawAmount = o.optInt("amount", 0)
            list.add(
                WalletTransactionItem(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    txType = o.optString("type", o.optString("txType", "")),
                    direction = o.optString("direction", if (rawAmount >= 0) "CREDIT" else "DEBIT"),
                    amount = kotlin.math.abs(rawAmount),
                    balanceAfter = o.optInt("balanceAfter", 0),
                    reason = o.optString("reason", ""),
                    referenceBookingId = o.optNullable("referenceBookingId"),
                    referencePackageId = o.optNullable("referencePackageId"),
                    referenceSessionId = o.optNullable("referenceSessionId"),
                    createdAt = o.optString("createdAt", ""),
                )
            )
        }
        return list
    }

    private fun JSONObject.optNullable(k: String): String? =
        if (isNull(k)) null else optString(k).takeIf { it.isNotBlank() && it != "null" }

    private fun parseError(body: String, fallback: String): String {
        if (body.isBlank()) return fallback
        return try {
            val j = JSONObject(body)
            j.optString("message", j.optString("error", fallback)).ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun sendGet(urlStr: String): Pair<Int, String> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            PaymentApiClient.authToken?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: ""
            Pair(code, text)
        } catch (e: Exception) {
            throw IllegalStateException("Không kết nối được backend: ${e.localizedMessage ?: e.message}")
        } finally {
            conn.disconnect()
        }
    }
}
