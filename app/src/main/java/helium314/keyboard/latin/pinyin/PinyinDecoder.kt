// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.pinyin

data class PinyinCandidate(val text: String, val consumed: Int)

class PinyinDecoder(private val lexicon: PinyinLexicon) {

    /**
     * Candidates for the start of [input], best first. When the input splits into several words,
     * the most likely sentence comes first, then words covering the longest part of the input.
     */
    fun decode(input: String): List<PinyinCandidate> {
        val text = String(CharArray(input.length) { input[it].lowercaseChar() })
        val start = firstLetter(text, 0)
        if (start == text.length) return emptyList()
        val segments = Array(text.length) { if (isLetter(text[it])) segmentsAt(text, it) else emptyList() }
        val matches = Array(text.length) { if (isLetter(text[it])) matchesFrom(text, it, segments) else emptyList() }

        val words = matches[start].sortedWith(compareBy({ -it.end }, { it.cost })).map { PinyinCandidate(it.word, it.end) }
        return (listOfNotNull(bestSentence(text, matches)) + words).distinctBy { it.text }.take(MAX_CANDIDATES)
    }

    private class Segment(val end: Int, val syllables: IntRange, val penalty: Float)

    private class Match(val end: Int, val word: String, val cost: Float)

    private fun segmentsAt(text: String, start: Int): List<Segment> {
        val letterRunEnd = firstNonLetter(text, start)
        val segments = ArrayList<Segment>()
        for (end in start + 1..minOf(letterRunEnd, start + LONGEST_SYLLABLE)) {
            val id = lexicon.syllableId(spelledSyllable(text.substring(start, end)))
            if (id >= 0) segments.add(Segment(end, id..id, 0f))
        }
        val initial = initialAt(text, start)
        // An initial right before a vowel starts a spelled syllable rather than abbreviating one.
        if (initial != null && !isVowelAt(text, start + initial.length))
            segments.add(Segment(start + initial.length, lexicon.syllableIdsWithPrefix(initial), ABBREVIATION_PENALTY))
        val unfinished = text.substring(start, letterRunEnd)
        if (unfinished.length > (initial?.length ?: 0) && unfinished.length <= LONGEST_SYLLABLE) {
            val ids = lexicon.syllableIdsWithPrefix(unfinished)
            val onlyItself = ids.first == ids.last && ids.first == lexicon.syllableId(unfinished)
            if (!ids.isEmpty() && !onlyItself) segments.add(Segment(letterRunEnd, ids, UNFINISHED_PENALTY))
        }
        return segments
    }

    private fun matchesFrom(text: String, start: Int, segments: Array<List<Segment>>): List<Match> {
        val best = HashMap<Pair<Int, String>, Match>()
        fun visit(from: Int, to: Int, depth: Int, position: Int, penalty: Float) {
            val longerKeysStart = lexicon.endOfKeysWithLength(from, to, depth)
            if (depth > 0) {
                for (entry in from until longerKeysStart) {
                    val match = Match(position, lexicon.word(entry), lexicon.cost(entry) + penalty)
                    best.merge(position to match.word, match) { old, new -> if (new.cost < old.cost) new else old }
                }
            }
            val next = firstLetter(text, position)
            if (next == text.length || longerKeysStart == to) return
            for (segment in segments[next]) {
                lexicon.forEachContinuation(longerKeysStart, to, depth, segment.syllables) { childFrom, childTo ->
                    visit(childFrom, childTo, depth + 1, segment.end, penalty + segment.penalty)
                }
            }
        }
        visit(0, lexicon.size, 0, start, 0f)
        return best.values.toList()
    }

    /** The cheapest split of the whole input into words, if it needs more than one word. */
    private fun bestSentence(text: String, matches: Array<List<Match>>): PinyinCandidate? {
        val cost = FloatArray(text.length + 1) { Float.POSITIVE_INFINITY }
        val lastWord = arrayOfNulls<Match>(text.length + 1)
        val wordStart = IntArray(text.length + 1)
        cost[0] = 0f
        for (position in text.indices) {
            if (cost[position] == Float.POSITIVE_INFINITY) continue
            if (!isLetter(text[position])) {
                if (cost[position] < cost[position + 1]) {
                    cost[position + 1] = cost[position]
                    lastWord[position + 1] = null
                    wordStart[position + 1] = position
                }
                continue
            }
            for (match in matches[position]) {
                val total = cost[position] + match.cost
                if (total < cost[match.end]) {
                    cost[match.end] = total
                    lastWord[match.end] = match
                    wordStart[match.end] = position
                }
            }
        }
        if (cost[text.length] == Float.POSITIVE_INFINITY) return null
        val words = ArrayList<String>()
        var position = text.length
        while (position > 0) {
            lastWord[position]?.let { words.add(it.word) }
            position = wordStart[position]
        }
        if (words.size < 2) return null
        return PinyinCandidate(words.asReversed().joinToString(""), text.length)
    }

    private fun initialAt(text: String, start: Int): String? {
        val twoLetters = text.regionMatches(start, "zh", 0, 2) || text.regionMatches(start, "ch", 0, 2)
                || text.regionMatches(start, "sh", 0, 2)
        if (twoLetters) return text.substring(start, start + 2)
        return if (text[start] in CONSONANT_INITIALS) text.substring(start, start + 1) else null
    }

    private fun isVowelAt(text: String, position: Int) = position < text.length && text[position] in VOWELS

    private fun firstLetter(text: String, from: Int): Int {
        var position = from
        while (position < text.length && !isLetter(text[position])) position++
        return position
    }

    private fun firstNonLetter(text: String, from: Int): Int {
        var position = from
        while (position < text.length && isLetter(text[position])) position++
        return position
    }

    companion object {
        private const val MAX_CANDIDATES = 60
        private const val LONGEST_SYLLABLE = 6
        // Costs are natural logs of word frequency, so a penalty of 4 needs a word about 50 times
        // more frequent to outrank a fully spelled one.
        private const val ABBREVIATION_PENALTY = 4f
        private const val UNFINISHED_PENALTY = 4f
        private const val CONSONANT_INITIALS = "bpmfdtnlgkhjqxrzcsyw"
        private const val VOWELS = "aeiouv"

        private fun isLetter(char: Char) = char in 'a'..'z'

        // The lexicon spells ü as v, and üe as ue even after l and n.
        private fun spelledSyllable(typed: String) = when (typed) {
            "lve" -> "lue"
            "nve" -> "nue"
            else -> typed
        }
    }
}
