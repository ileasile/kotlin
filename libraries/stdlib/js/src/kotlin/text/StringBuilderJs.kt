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
        var reversed = ""
        var index = string.length - 1
        while (index >= 0) {
            val low = string[index--]
            if (low.isLowSurrogate() && index >= 0) {
                val high = string[index--]
                if (high.isHighSurrogate()) {
                    reversed = reversed + high + low
                } else {
                    reversed = reversed + low + high
                }
            } else {
                reversed += low
            }
        }
        string = reversed
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
public actual inline fun StringBuilder.delete(index: Int): StringBuilder = this.delete(index)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int, startIndex: Int, endIndex: Int) =
    this.toCharArray(destination, destinationOffset, startIndex, endIndex)
