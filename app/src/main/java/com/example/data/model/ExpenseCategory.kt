package com.example.data.model

enum class ExpenseCategory(
    val id: String,
    val displayName: String,
    val emoji: String,
    val banglaName: String
) {
    GROCERY("grocery", "Grocery", "🛒", "মুদি ও বাজার"),
    VEGETABLES("vegetables", "Vegetables", "🥬", "শাকসবজি"),
    FISH("fish", "Fish", "🐟", "মাছ"),
    MEAT("meat", "Meat", "🍗", "মাংস"),
    RICE("rice", "Rice", "🍚", "চাল"),
    EGGS("eggs", "Eggs", "🥚", "ডিম"),
    DRINKS("drinks", "Drinks", "🥤", "পানীয় ও চা"),
    COOKING("cooking", "Cooking Items", "🧂", "মসলা ও তেল"),
    HOUSEHOLD("household", "Household", "🏠", "মেস সামগ্রী"),
    UTILITIES("utilities", "Utilities", "💡", "বিদ্যুৎ ও গ্যাস"),
    TRANSPORT("transport", "Transport", "🚕", "যাতায়াত"),
    MEDICINE("medicine", "Medicine", "💊", "ওষুধ"),
    OTHER("other", "Other", "📦", "অন্যান্য");

    companion object {
        fun fromId(id: String?): ExpenseCategory {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
                ?: OTHER
        }
    }
}

enum class TransactionType {
    INCOME,   // Add Money
    EXPENSE   // Spend Money
}

enum class TimeFilter(val label: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_WEEK("This Week"),
    TODAY("Today"),
    CUSTOM_RANGE("Custom Range")
}

enum class TransactionSortOrder(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    HIGHEST_AMOUNT("Highest Amount"),
    LOWEST_AMOUNT("Lowest Amount")
}
