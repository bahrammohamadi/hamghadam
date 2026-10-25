package ir.hamghadam.app.domain

/**
 * A tiny, dependency-free JSON reader/writer used by the backup file.
 *
 * Only what the backup format needs is supported, but parsing is a real
 * recursive-descent implementation: malformed files fail with a clear error
 * instead of being silently accepted.
 */
object SimpleJson {

    fun encode(value: Any?): String = StringBuilder().also { write(value, it) }.toString()

    fun parse(text: String): Any? {
        val reader = Reader(text)
        val value = reader.readValue()
        reader.skipWhitespace()
        if (!reader.atEnd()) throw IllegalArgumentException("Unexpected trailing content at ${reader.position}")
        return value
    }

    private fun write(value: Any?, out: StringBuilder) {
        when (value) {
            null -> out.append("null")
            is String -> quote(value, out)
            is Boolean -> out.append(value)
            is Int -> out.append(value)
            is Long -> out.append(value)
            is Float -> out.append(if (value % 1f == 0f) value.toLong().toString() else value.toString())
            is Double -> out.append(if (value % 1.0 == 0.0) value.toLong().toString() else value.toString())
            is Number -> out.append(value)
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((key, entry) in value) {
                    if (!first) out.append(',')
                    first = false
                    quote(key.toString(), out)
                    out.append(':')
                    write(entry, out)
                }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                var first = true
                for (entry in value) {
                    if (!first) out.append(',')
                    first = false
                    write(entry, out)
                }
                out.append(']')
            }
            else -> quote(value.toString(), out)
        }
    }

    private fun quote(text: String, out: StringBuilder) {
        out.append('"')
        for (ch in text) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> if (ch.code < 0x20) out.append("\\u%04x".format(ch.code)) else out.append(ch)
            }
        }
        out.append('"')
    }

    private class Reader(private val source: String) {
        private var index = 0

        val position: Int get() = index

        fun atEnd(): Boolean = index >= source.length

        fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }

        fun readValue(): Any? {
            skipWhitespace()
            if (atEnd()) throw IllegalArgumentException("Unexpected end of JSON")
            return when (source[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> readNumber()
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return result
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                result[key] = readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return result
                    }
                    else -> throw IllegalArgumentException("Expected , or } at $index")
                }
            }
        }

        private fun readArray(): List<Any?> {
            expect('[')
            val result = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return result
            }
            while (true) {
                result += readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return result
                    }
                    else -> throw IllegalArgumentException("Expected , or ] at $index")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw IllegalArgumentException("Unterminated string")
                when (val ch = source[index++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) throw IllegalArgumentException("Unterminated escape")
                        when (val escaped = source[index++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (index + 4 > source.length) throw IllegalArgumentException("Bad unicode escape")
                                val hex = source.substring(index, index + 4)
                                index += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> throw IllegalArgumentException("Unknown escape \\$escaped")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
        }

        private fun readNumber(): Double {
            val start = index
            if (!atEnd() && (source[index] == '-' || source[index] == '+')) index++
            while (!atEnd() && (source[index].isDigit() || source[index] == '.' || source[index] == 'e' || source[index] == 'E' || source[index] == '-' || source[index] == '+')) {
                index++
            }
            if (start == index) throw IllegalArgumentException("Expected a value at $start")
            return source.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("Bad number '${source.substring(start, index)}'")
        }

        private fun <T> readLiteral(text: String, value: T): T {
            if (!source.startsWith(text, index)) throw IllegalArgumentException("Expected $text at $index")
            index += text.length
            return value
        }

        private fun peek(): Char {
            if (atEnd()) throw IllegalArgumentException("Unexpected end of JSON")
            return source[index]
        }

        private fun expect(ch: Char) {
            skipWhitespace()
            if (atEnd() || source[index] != ch) throw IllegalArgumentException("Expected '$ch' at $index")
            index++
        }
    }
}
