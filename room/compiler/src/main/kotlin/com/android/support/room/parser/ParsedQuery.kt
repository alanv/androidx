/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.support.room.parser

import com.android.support.room.parser.SectionType.BIND_VAR
import com.android.support.room.parser.SectionType.NEWLINE
import com.android.support.room.parser.SectionType.TEXT
import org.antlr.v4.runtime.tree.TerminalNode

enum class SectionType {
    BIND_VAR,
    TEXT,
    NEWLINE
}

data class Section(val text: String, val type: SectionType) {
    companion object {
        fun text(text: String) = Section(text, SectionType.TEXT)
        fun newline() = Section("", SectionType.NEWLINE)
        fun bindVar(text: String) = Section(text, SectionType.BIND_VAR)
    }
}

data class ParsedQuery(val original: String, val inputs: List<TerminalNode>,
                       val syntaxErrors: List<String>) {
    companion object {
        val STARTS_WITH_NUMBER = "^\\?[0-9]".toRegex()
        val MISSING = ParsedQuery("missing query", emptyList(), emptyList())
    }

    val sections by lazy {
        val lines = original.lines()
        val inputsByLine = inputs.groupBy { it.symbol.line }
        val sections = arrayListOf<Section>()
        lines.forEachIndexed { index, line ->
            var charInLine = 0
            inputsByLine[index + 1]?.forEach { bindVar ->
                if (charInLine < bindVar.symbol.charPositionInLine) {
                    sections.add(Section.text(line.substring(charInLine,
                            bindVar.symbol.charPositionInLine)))
                }
                sections.add(Section.bindVar(bindVar.text))
                charInLine = bindVar.symbol.charPositionInLine + bindVar.symbol.text.length
            }
            if (charInLine < line.length) {
                sections.add(Section.text(line.substring(charInLine)))
            }
            if (index + 1 < lines.size) {
                sections.add(Section.newline())
            }
        }
        sections
    }

    val errors by lazy {
        val hasUnnamed = inputs.any { it.text == "?" }
        inputs.filter {
            it.text.matches(STARTS_WITH_NUMBER)
        }.map {
            ParserErrors.cannotUseVariableIndices(it.text, it.symbol.charPositionInLine)
        } + (if (hasUnnamed && inputs.size > 1) arrayListOf(ParserErrors.TOO_MANY_UNNAMED_VARIABLES)
        else emptyList<String>()) + syntaxErrors
    }

    val queryWithReplacedBindParams by lazy {
        sections.joinToString("") {
            when(it.type) {
                TEXT -> ""
                BIND_VAR -> "?"
                NEWLINE -> "\n"
                else -> throw IllegalArgumentException("??")
            }
        }
    }
}
