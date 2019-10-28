/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

public actual interface Appendable {
    public actual fun append(csq: CharSequence?): Appendable
    public actual fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public actual fun append(c: Char): Appendable
}

public actual class StringBuilder actual constructor(content: String) : Appendable, CharSequence {
    actual constructor(capacity: Int) : this() {
        _capacity = capacity
    }

    actual constructor(content: CharSequence) : this(content.toString()) {}

    actual constructor() : this("")

    private var string: String = content
    private var _capacity = content.length

    actual override val length: Int
        get() = string.asDynamic().length

    actual override fun get(index: Int): Char =
        string.getOrElse(index) { throw IndexOutOfBoundsException("index: $index, length: $length}") }

    actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.substring(startIndex, endIndex)

    actual override fun append(c: Char): StringBuilder {
        string += c
        return this
    }

    actual override fun append(csq: CharSequence?): StringBuilder {
        string += csq.toString()
        return this
    }

    actual override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder {
        string += csq.toString().substring(start, end)
        return this
    }

    actual fun reverse(): StringBuilder {
        string = string.asDynamic().split("").reverse().join("")
        return this
    }

    actual fun append(obj: Any?): StringBuilder {
        string += obj.toString()
        return this
    }

    actual fun append(boolean: Boolean): StringBuilder {
        string += boolean
        return this
    }

    actual fun append(chars: CharArray): StringBuilder {
        string += String(chars)
        return this
    }

    actual fun append(chars: CharArray, offset: Int, length: Int): StringBuilder {
        string += String(chars, offset, length)
        return this
    }

    actual fun append(string: String): StringBuilder {
        this.string += string
        return this
    }

    actual fun appendCodePoint(codePoint: Int): StringBuilder {
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            throw IllegalArgumentException("Invalid Unicode code point value: $codePoint")
        }
        string += js("String.fromCodePoint(codePoint)")
        return this
    }

    actual fun codePointBefore(index: Int): Int {
        if (index < 1 || index > length) {
            val message = if (isEmpty()) "StringBuilder is empty." else "Index must be in 1..$length, provided index: $index"
            throw IndexOutOfBoundsException(message)
        }

        val low = string[index - 1]
        if (low.isLowSurrogate() && index > 1) {
            val high = string[index - 2]
            if (high.isHighSurrogate()) {
                return 0x10000 + ((high.toInt() and 0x3FF) shl 10) or (low.toInt() and 0x3FF)
            }
        }
        return low.toInt()
    }

    actual fun codePointCount(startIndex: Int, endIndex: Int): Int {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

        var count = 0
        var wasHighSurrogate = false

        for (i in startIndex until endIndex) {
            val char = string[i]
            if (char.isLowSurrogate() && wasHighSurrogate) {
                wasHighSurrogate = false
            } else {
                count++
                wasHighSurrogate = char.isHighSurrogate()
            }
        }

        return count
    }

    actual fun offsetByCodePoints(index: Int, codePointOffset: Int): Int {
        AbstractList.checkPositionIndex(index, length)

        if (codePointOffset == 0) return index

        if (codePointOffset > 0) {
            var count = 0
            var wasHighSurrogate = false

            for (i in index until length) {
                val char = string[i]
                if (char.isLowSurrogate() && wasHighSurrogate) {
                    wasHighSurrogate = false
                } else {
                    count++
                    wasHighSurrogate = char.isHighSurrogate()

                    if (count == codePointOffset) {
                        if (wasHighSurrogate && i + 1 < length && string[i + 1].isLowSurrogate()) {
                            return i + 2
                        }
                        return i + 1
                    }
                }
            }

            throw IllegalArgumentException("Where are fewer code points at $index..${length - 1} than the provided codePointOffset: $codePointOffset")

        } else {
            var count = 0
            var wasLowSurrogate = false

            for (i in index - 1 downTo 0) {
                val char = string[i]
                if (char.isHighSurrogate() && wasLowSurrogate) {
                    wasLowSurrogate = false
                } else {
                    count++
                    wasLowSurrogate = char.isLowSurrogate()

                    if (count == -codePointOffset) {
                        if (wasLowSurrogate && i > 0 && string[i - 1].isHighSurrogate()) {
                            return i - 1
                        }
                        return i
                    }
                }
            }

            throw IllegalArgumentException("Where are fewer code points at 0..${index - 1} than the provided codePointOffset: $codePointOffset")
        }
    }

    actual fun delete(startIndex: Int, endIndex: Int): StringBuilder {
        if (startIndex < 0 || startIndex > length) {
            throw IndexOutOfBoundsException("startIndex: $startIndex, length: $length")
        }
        if (startIndex > endIndex) {
            throw IllegalArgumentException("startIndex($startIndex) > endIndex($endIndex)")
        }

        string = string.substring(0, startIndex) + string.substring(endIndex)
        return this
    }

    actual fun capacity(): Int = maxOf(_capacity, length)

    actual fun ensureCapacity(minimumCapacity: Int) {
        if (minimumCapacity > capacity()) {
            _capacity = minimumCapacity
        }
    }

    actual fun indexOf(string: String): Int = this.string.asDynamic().indexOf(string)

    actual fun indexOf(string: String, startIndex: Int): Int = this.string.asDynamic().indexOf(string, startIndex)

    actual fun lastIndexOf(string: String): Int = this.string.asDynamic().lastIndexOf(string)

    actual fun lastIndexOf(string: String, startIndex: Int): Int {
        if (string.isEmpty() && startIndex < 0) return -1
        return this.string.asDynamic().lastIndexOf(string, startIndex)
    }

    actual fun insert(index: Int, boolean: Boolean): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        string = string.substring(0, index) + boolean + string.substring(index)
        return this
    }

    actual fun insert(index: Int, char: Char): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        string = string.substring(0, index) + char + string.substring(index)
        return this
    }

    actual fun insert(index: Int, chars: CharArray): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        string = string.substring(0, index) + String(chars) + string.substring(index)
        return this
    }

    actual fun insert(index: Int, chars: CharArray, offset: Int, length: Int): StringBuilder {
        AbstractList.checkPositionIndex(index, this.length)
        AbstractList.checkBoundsIndexes(offset, offset + length, chars.size)

        string = string.substring(0, index) + String(chars, offset, length) + string.substring(index)
        return this
    }

    actual fun insert(index: Int, csq: CharSequence?): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        string = string.substring(0, index) + csq.toString() + string.substring(index)
        return this
    }

    actual fun insert(index: Int, csq: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        val stringCsq = csq.toString()

        AbstractList.checkBoundsIndexes(startIndex, endIndex, stringCsq.length)

        string = string.substring(0, index) + stringCsq.substring(startIndex, endIndex) + string.substring(index)
        return this
    }

    actual fun insert(index: Int, obj: Any?): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        string = string.substring(0, index) + obj.toString() + string.substring(index)
        return this
    }

    actual fun insert(index: Int, string: String): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        this.string = this.string.substring(0, index) + string + this.string.substring(index)
        return this
    }

    actual fun replace(startIndex: Int, endIndex: Int, string: String): StringBuilder {
        if (startIndex < 0 || startIndex > length) {
            throw IndexOutOfBoundsException("startIndex: $startIndex, length: $length")
        }
        if (startIndex > endIndex) {
            throw IllegalArgumentException("startIndex($startIndex) > endIndex($endIndex)")
        }

        this.string = this.string.substring(0, startIndex) + string + this.string.substring(endIndex)
        return this
    }

    actual fun setLength(newLength: Int) {
        if (newLength < 0) {
            throw IllegalArgumentException("Negative new length: $newLength.")
        }

        if (newLength <= length) {
            string = string.substring(0, newLength)
        } else {
            for (i in length until newLength) {
                string += '\u0000'
            }
        }
    }

    actual fun substring(startIndex: Int): String {
        AbstractList.checkPositionIndex(startIndex, length)
        return string.substring(startIndex)
    }

    actual fun substring(startIndex: Int, endIndex: Int): String {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, length)
        return string.substring(startIndex, endIndex)
    }

    actual fun trimToSize() {
        _capacity = length
    }

    override fun toString(): String = string

    /**
     * Clears the content of this string builder making it empty.
     *
     * @sample samples.text.Strings.clearStringBuilder
     */
    @SinceKotlin("1.3")
    public fun clear(): StringBuilder {
        string = ""
        return this
    }

    public operator fun set(index: Int, value: Char) {
        AbstractList.checkElementIndex(index, length)

        string = string.substring(0, index) + value + string.substring(index + 1)
    }

    public fun codePoint(index: Int): Int {
        AbstractList.checkElementIndex(index, length)

        val high = string[index]

        if (high.isHighSurrogate() && index < length - 1) {
            val low = string[index + 1]
            if (low.isLowSurrogate()) {
                return 0x10000 + ((high.toInt() and 0x3FF) shl 10) or (low.toInt() and 0x3FF)
            }
        }

        return high.toInt()
    }

    public fun delete(index: Int): StringBuilder {
        AbstractList.checkElementIndex(index, length)

        string = string.substring(0, index) + string.substring(index + 1)
        return this
    }

    public fun toCharArray(destination: CharArray, destinationOffset: Int, startIndex: Int, endIndex: Int) {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

        if (destinationOffset < 0 || destinationOffset >= destination.size) {
            throw IndexOutOfBoundsException("Destination offset: $destinationOffset, size: ${destination.size}")
        }
        if (destinationOffset + endIndex - startIndex > destination.size) {
            throw IndexOutOfBoundsException("Subrange size: ${endIndex - startIndex}, destination offset: $destinationOffset, size: ${destination.size}")
        }

        var dstIndex = destinationOffset
        for (index in startIndex until endIndex) {
            destination[dstIndex++] = string[index]
        }
    }
}


/**
 * Clears the content of this string builder making it empty.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.clear(): StringBuilder = this.clear()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline operator fun StringBuilder.set(index: Int, value: Char) = this.set(index, value)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.codePoint(index: Int): Int = this.codePoint(index)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.delete(index: Int): StringBuilder = this.delete(index)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int, startIndex: Int, endIndex: Int) =
    this.toCharArray(destination, destinationOffset, startIndex, endIndex)
