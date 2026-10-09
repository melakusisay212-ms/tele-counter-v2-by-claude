package et.teleexpense

import et.teleexpense.Classification.COUNT
import java.time.Duration

object Validator {
    fun isValid(t: TelecomTransaction): Boolean =
        t.primary.classification == COUNT && (t.primary.amount ?: 0.0) > 0.0 && t.primary.status == "successful"
}

/**
 * SMS event -> candidate -> grouped transaction -> validated expense.
 * Only COUNT messages can create an expense. IGNORE and REFERENCE messages attach to one or are dropped.
 */
class TransactionGrouper(private val window: Duration = Duration.ofMinutes(15)) {

    fun group(input: List<Candidate>): List<TelecomTransaction> {
        val sorted = input.sortedBy { it.receivedAt }
        val txs = mutableListOf<TelecomTransaction>()

        for (c in sorted.filter { it.classification == COUNT }) {
            val existing = txs.firstOrNull { DuplicateDetector.isSameExpense(it.primary, c) }
            if (existing != null) existing.related += c else txs += TelecomTransaction(c)
        }

        for (c in sorted.filter { it.classification != COUNT }) {
            txs.filter { DuplicateDetector.supports(it.primary, c, window) }
                .minByOrNull { Duration.between(it.primary.receivedAt, c.receivedAt).abs() }
                ?.related?.add(c)
        }
        return txs.filter { Validator.isValid(it) }
    }
}
