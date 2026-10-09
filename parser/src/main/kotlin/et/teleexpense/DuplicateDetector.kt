package et.teleexpense

import java.time.Duration

object DuplicateDetector {
    private val airtimeFamily = setOf("airtime", "airtime_recharge")

    fun sameId(a: Candidate, b: Candidate): Boolean =
        (a.transferId != null && a.transferId == b.transferId) ||
            (a.transactionId != null && a.transactionId == b.transactionId)

    fun conflictingIds(a: Candidate, b: Candidate): Boolean =
        (a.transferId != null && b.transferId != null && a.transferId != b.transferId) ||
            (a.transactionId != null && b.transactionId != null && a.transactionId != b.transactionId)

    fun near(a: Candidate, b: Candidate, window: Duration): Boolean =
        Duration.between(a.receivedAt, b.receivedAt).abs() <= window ||
            Duration.between(a.dateTime, b.dateTime).abs() <= window

    fun sameRecipient(a: Candidate, b: Candidate): Boolean {
        val x = a.recipient?.takeLast(9)
        val y = b.recipient?.takeLast(9)
        return x == null || y == null || x == y
    }

    fun sameCategory(a: Candidate, b: Candidate): Boolean =
        a.category == b.category || (a.category in airtimeFamily && b.category in airtimeFamily)

    /** Two COUNT messages describing the same real-world payment (duplicate SMS, or a second provider reporting it). */
    fun isSameExpense(a: Candidate, b: Candidate): Boolean {
        if (a.source != b.source) return false
        if (sameId(a, b)) return true
        if (conflictingIds(a, b)) return false
        if (a.amount == null || a.amount != b.amount) return false
        if (!sameCategory(a, b) || !sameRecipient(a, b)) return false
        if (a.packageName != null && b.packageName != null && a.packageName != b.packageName) return false
        val window = if (a.category != b.category) Duration.ofMinutes(3) else Duration.ofMinutes(2)
        return near(a, b, window)
    }

    /** A non-COUNT message that belongs to an existing expense. */
    fun supports(expense: Candidate, other: Candidate, window: Duration): Boolean {
        if (sameId(expense, other)) return true
        if (conflictingIds(expense, other)) return false
        if (!near(expense, other, window)) return false
        val amountOk = other.amount == null || other.amount == 0.0 || other.amount == expense.amount
        val pkgOk = other.packageName != null && other.packageName == expense.packageName
        val recipientOk = other.recipient != null && expense.recipient != null &&
            other.recipient.takeLast(9) == expense.recipient.takeLast(9)
        val categoryOk = sameCategory(expense, other) ||
            (other.category == "package_activation" && expense.category.endsWith("package"))
        return categoryOk && amountOk && (pkgOk || recipientOk)
    }
}
