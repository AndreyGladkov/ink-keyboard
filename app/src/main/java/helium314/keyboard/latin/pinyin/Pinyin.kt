// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.pinyin

import android.content.Context
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.dictionary.Dictionary
import kotlin.concurrent.thread

object Pinyin {
    const val COMBINING_SPEC = "pinyin"
    private const val LEXICON_ASSET = "pinyin/zh-CN.txt"

    @Volatile private var decoder: PinyinDecoder? = null

    @JvmStatic
    fun preload(context: Context) {
        if (decoder == null) thread(name = "PinyinLexicon") { decoder(context) }
    }

    @Synchronized
    private fun decoder(context: Context): PinyinDecoder = decoder ?: PinyinDecoder(
        context.assets.open(LEXICON_ASSET).bufferedReader().use { PinyinLexicon.read(it) }
    ).also { decoder = it }

    @JvmStatic
    fun isInputCodePoint(codePoint: Int) = codePoint in 'a'.code..'z'.code || codePoint in 'A'.code..'Z'.code
            || codePoint == '\''.code

    @JvmStatic
    fun candidates(context: Context, typedWord: String): List<CandidateInfo> =
        decoder(context).decode(typedWord).mapIndexed { rank, candidate -> CandidateInfo(candidate, typedWord, rank) }

    @JvmStatic
    fun suggestedWords(typedWordInfo: SuggestedWordInfo, candidates: List<CandidateInfo>, sequenceNumber: Int) =
        SuggestedWords(ArrayList(listOf(typedWordInfo) + candidates), null, typedWordInfo,
            false, false, false, SuggestedWords.INPUT_STYLE_TYPING, sequenceNumber)

    @JvmStatic
    fun convert(context: Context, typedWord: String): String {
        val converted = StringBuilder()
        var rest = typedWord
        while (rest.isNotEmpty()) {
            val best = candidates(context, rest).firstOrNull() ?: return converted.append(rest).toString()
            converted.append(best.mWord)
            rest = remainderAfter(best, rest)!!
        }
        return converted.toString()
    }

    /** Null when [candidate] was decoded for pinyin that [typedWord] no longer starts with. */
    @JvmStatic
    fun remainderAfter(candidate: CandidateInfo, typedWord: String): String? {
        val converted = candidate.input.substring(0, candidate.consumed)
        if (!typedWord.startsWith(converted)) return null
        return typedWord.substring(converted.length).trimStart('\'')
    }

    class CandidateInfo(candidate: PinyinCandidate, val input: String, rank: Int) : SuggestedWordInfo(
        candidate.text, "", SuggestedWordInfo.MAX_SCORE - rank, SuggestedWordInfo.KIND_CORRECTION,
        Dictionary.DICTIONARY_HARDCODED, SuggestedWordInfo.NOT_AN_INDEX, SuggestedWordInfo.NOT_A_CONFIDENCE
    ) {
        val consumed = candidate.consumed
    }
}
