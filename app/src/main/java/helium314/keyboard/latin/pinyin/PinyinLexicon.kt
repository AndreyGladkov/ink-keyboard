// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.pinyin

import java.io.BufferedReader
import kotlin.math.ln

/**
 * Words keyed by their syllable sequence. Entries are ordered by syllable ids, and syllable ids
 * follow the alphabetical order of syllables, so all entries starting with a given syllable
 * sequence form one contiguous range, with the entries of exactly that sequence at its start.
 */
class PinyinLexicon private constructor(
    private val syllables: Array<String>,
    private val keyOffsets: IntArray,
    private val keys: ShortArray,
    private val wordOffsets: IntArray,
    private val wordText: String,
    private val costs: FloatArray,
) {
    val size get() = costs.size

    fun syllableId(syllable: String): Int = syllables.binarySearch(syllable).coerceAtLeast(-1)

    fun syllableIdsWithPrefix(prefix: String): IntRange {
        val first = syllableLowerBound(prefix)
        val end = syllableLowerBound(prefix + Char.MAX_VALUE)
        return first until end
    }

    fun word(entry: Int): String = wordText.substring(wordOffsets[entry], wordOffsets[entry + 1])

    fun cost(entry: Int): Float = costs[entry]

    fun keyLength(entry: Int): Int = keyOffsets[entry + 1] - keyOffsets[entry]

    fun endOfKeysWithLength(from: Int, to: Int, depth: Int): Int =
        lowerBound(from, to) { keyLength(it) > depth }

    /**
     * Calls [action] with the range of entries for each syllable in [ids] that continues the keys
     * of [from, to) at [depth]. All keys in [from, to) must be longer than [depth].
     */
    inline fun forEachContinuation(from: Int, to: Int, depth: Int, ids: IntRange, action: (Int, Int) -> Unit) {
        var start = lowerBound(from, to) { syllableAt(it, depth) >= ids.first }
        while (start < to) {
            val id = syllableAt(start, depth)
            if (id > ids.last) break
            val end = lowerBound(start, to) { syllableAt(it, depth) > id }
            action(start, end)
            start = end
        }
    }

    fun syllableAt(entry: Int, depth: Int): Int = keys[keyOffsets[entry] + depth].toInt()

    private fun syllableLowerBound(value: String): Int {
        var low = 0
        var high = syllables.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (syllables[middle] < value) low = middle + 1 else high = middle
        }
        return low
    }

    companion object {
        inline fun lowerBound(from: Int, to: Int, isAtOrAfter: (Int) -> Boolean): Int {
            var low = from
            var high = to
            while (low < high) {
                val middle = (low + high) ushr 1
                if (isAtOrAfter(middle)) high = middle else low = middle + 1
            }
            return low
        }

        /** Reads lines of "syllables<TAB>word<TAB>frequency", ordered as described for the class. */
        fun read(reader: BufferedReader): PinyinLexicon {
            val lines = reader.lineSequence().filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
            val syllables = lines.flatMapTo(HashSet()) { it.substringBefore('\t').split(' ') }.sorted().toTypedArray()
            val syllableIds = syllables.withIndex().associate { it.value to it.index.toShort() }
            val keyOffsets = IntArray(lines.size + 1)
            val keys = ShortArray(lines.sumOf { line -> line.substringBefore('\t').count { it == ' ' } + 1 })
            val wordOffsets = IntArray(lines.size + 1)
            val wordText = StringBuilder()
            val frequencies = DoubleArray(lines.size)
            lines.forEachIndexed { index, line ->
                val (key, word, frequency) = line.split('\t')
                var keyEnd = keyOffsets[index]
                key.split(' ').forEach { keys[keyEnd++] = syllableIds.getValue(it) }
                keyOffsets[index + 1] = keyEnd
                wordText.append(word)
                wordOffsets[index + 1] = wordText.length
                frequencies[index] = frequency.toDouble()
            }
            val logTotal = ln(frequencies.sum())
            val costs = FloatArray(lines.size) { (logTotal - ln(frequencies[it])).toFloat() }
            return PinyinLexicon(syllables, keyOffsets, keys, wordOffsets, wordText.toString(), costs)
        }
    }
}
