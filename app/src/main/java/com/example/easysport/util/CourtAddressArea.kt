package com.example.easysport.util

internal data class CourtAddressArea(val city: String?, val district: String?) {
    companion object {
        private val trailingNote = Regex("\\s*\\([^()]*\\)\\s*$")
        private val citySeparators = Regex("[.\\s]")
        private val districtPrefix = Regex(
            "^(?:quận\\s+|huyện\\s+|thị xã\\s+|thành phố\\s+|tp\\.?\\s*|[qh]\\.\\s*)(.+)$",
            RegexOption.IGNORE_CASE
        )

        fun fromAddress(address: String): CourtAddressArea {
            val parts = address.replace(trailingNote, "").split(',').map(String::trim)
            val city = when (parts.last().lowercase().replace(citySeparators, "")) {
                "hànội", "tphànội" -> "Hà Nội"
                "tphcm", "hcm", "tphồchíminh", "hồchíminh" -> "TP. Hồ Chí Minh"
                else -> null
            }
            val districtPart = parts.getOrNull(parts.lastIndex - 1).orEmpty()
            val stripped = districtPrefix.matchEntire(districtPart)?.groupValues?.get(1)?.trim()
            val district = when {
                stripped != null && stripped.all { it.isDigit() } -> "Quận $stripped"
                stripped != null -> stripped
                city == "TP. Hồ Chí Minh" && districtPart.equals("Tân Bình", true) -> "Tân Bình"
                city == "TP. Hồ Chí Minh" && districtPart.equals("Gò Vấp", true) -> "Gò Vấp"
                city == "TP. Hồ Chí Minh" && districtPart.equals("Bình Thạnh", true) -> "Bình Thạnh"
                city == "TP. Hồ Chí Minh" && districtPart.equals("Thủ Đức", true) -> "Thủ Đức"
                else -> null
            }
            return CourtAddressArea(city, district)
        }
    }
}
