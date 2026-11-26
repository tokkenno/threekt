package com.aitorgf.threekt.utils

/**
 * Minimal JSON reader for loaders in common code. Objects become [Map]s, arrays [List]s, numbers
 * [Double]s, and `true`, `false` and `null` their Kotlin values. Throws [IllegalArgumentException] on
 * malformed input.
 */
internal object Json {
    fun parse(text: String): Any? {
        val reader = Reader(text)
        reader.skipWhitespace()
        val value = reader.readValue()
        reader.skipWhitespace()
        require(reader.index == text.length) { "Unexpected trailing JSON content at ${reader.index}." }
        return value
    }

    private class Reader(val text: String) {
        var index = 0

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun readValue(): Any? {
            skipWhitespace()
            require(index < text.length) { "Unexpected end of JSON." }
            return when (val c = text[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) readNumber() else throw IllegalArgumentException("Unexpected '$c' at $index.")
            }
        }

        private fun literal(word: String, value: Any?): Any? {
            require(text.startsWith(word, index)) { "Expected $word at $index." }
            index += word.length
            return value
        }

        private fun readObject(): Map<String, Any?> {
            val result = linkedMapOf<String, Any?>()
            index++
            skipWhitespace()
            if (text[index] == '}') {
                index++
                return result
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                require(text[index] == ':') { "Expected ':' at $index." }
                index++
                result[key] = readValue()
                skipWhitespace()
                when (text[index++]) {
                    ',' -> continue
                    '}' -> return result
                    else -> throw IllegalArgumentException("Expected ',' or '}' at ${index - 1}.")
                }
            }
        }

        private fun readArray(): List<Any?> {
            val result = mutableListOf<Any?>()
            index++
            skipWhitespace()
            if (text[index] == ']') {
                index++
                return result
            }
            while (true) {
                result.add(readValue())
                skipWhitespace()
                when (text[index++]) {
                    ',' -> continue
                    ']' -> return result
                    else -> throw IllegalArgumentException("Expected ',' or ']' at ${index - 1}.")
                }
            }
        }

        private fun readString(): String {
            require(text[index] == '"') { "Expected a string at $index." }
            index++
            val builder = StringBuilder()
            while (true) {
                val c = text[index++]
                when (c) {
                    '"' -> return builder.toString()
                    '\\' -> when (val escape = text[index++]) {
                        '"', '\\', '/' -> builder.append(escape)
                        'b' -> builder.append('\b')
                        'f' -> builder.append('\u000C')
                        'n' -> builder.append('\n')
                        'r' -> builder.append('\r')
                        't' -> builder.append('\t')
                        'u' -> {
                            builder.append(text.substring(index, index + 4).toInt(16).toChar())
                            index += 4
                        }
                        else -> throw IllegalArgumentException("Invalid escape '\\$escape' at ${index - 1}.")
                    }
                    else -> builder.append(c)
                }
            }
        }

        private fun readNumber(): Double {
            val start = index
            if (text[index] == '-') index++
            while (index < text.length && (text[index].isDigit() || text[index] in ".eE+-")) index++
            return text.substring(start, index).toDouble()
        }
    }
}
