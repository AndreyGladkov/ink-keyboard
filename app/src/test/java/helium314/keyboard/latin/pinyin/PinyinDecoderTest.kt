// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.pinyin

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PinyinDecoderTest {
    private fun first(input: String) = decoder.decode(input).first()

    @Test fun fullSyllablesGiveTheWord() {
        assertEquals(PinyinCandidate("你好", 5), first("nihao"))
        assertEquals(PinyinCandidate("中国", 8), first("zhongguo"))
        assertEquals(PinyinCandidate("你", 2), first("ni"))
    }

    @Test fun shorterWordsFollowTheFullMatch() {
        val candidates = decoder.decode("nihao")
        assertContains(candidates, PinyinCandidate("你", 2))
        assertTrue(candidates.indexOf(PinyinCandidate("你", 2)) > candidates.indexOf(PinyinCandidate("你好", 5)))
    }

    @Test fun eachWordIsOfferedOnceWithItsLongestMatch() {
        val candidates = decoder.decode("nihaoma")
        assertEquals(candidates.distinctBy { it.text }, candidates)
        assertEquals(listOf(PinyinCandidate("你好吗", 7), PinyinCandidate("你好", 5)), candidates.take(2))
    }

    @Test fun initialBeforeVowelIsNoAbbreviation() {
        assertTrue(decoder.decode("zai").all { it.text.length == 1 })
    }

    @Test fun initialsAbbreviateSyllables() {
        assertEquals(PinyinCandidate("你好", 2), first("nh"))
        assertEquals(PinyinCandidate("我们", 3), first("wom"))
    }

    @Test fun lastSyllableMayBeUnfinished() {
        assertEquals(PinyinCandidate("中国", 6), first("zhongg"))
        assertEquals(PinyinCandidate("中国", 7), first("zhonggu"))
    }

    @Test fun apostropheSeparatesSyllables() {
        assertEquals(PinyinCandidate("西安", 5), first("xi'an"))
    }

    @Test fun sentenceIsBuiltFromSeveralWords() {
        assertEquals(PinyinCandidate("我想吃饭", 13), first("woxiangchifan"))
    }

    @Test fun unknownTailLeavesTheMatchingPrefix() {
        assertEquals(PinyinCandidate("你好", 5), first("nihaoi"))
    }

    @Test fun uppercaseIsReadAsLowercase() {
        assertEquals(PinyinCandidate("你好", 5), first("NiHao"))
    }

    @Test fun vSpellsUmlaut() {
        assertEquals("女", first("nv").text)
        assertContains(decoder.decode("lve").map { it.text }, "略")
    }

    @Test fun inputWithoutSyllablesHasNoCandidates() {
        assertEquals(emptyList(), decoder.decode("i"))
        assertEquals(emptyList(), decoder.decode("'"))
    }

    @Test fun onlySimplifiedCharacters() {
        assertTrue(decoder.decode("men").none { it.text == "們" })
    }

    companion object {
        private val decoder by lazy {
            PinyinDecoder(File("src/main/assets/pinyin/zh-CN.txt").bufferedReader().use { PinyinLexicon.read(it) })
        }
    }
}
