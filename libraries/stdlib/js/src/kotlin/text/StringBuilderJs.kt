/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

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

    @Deprecated("Use appendRange instead", ReplaceWith("appendRange(csq, start, end)"), DeprecationLevel.WARNING)
    actual override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder = this.appendRange(csq, start, end)

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

    @UseExperimental(ExperimentalStdlibApi::class)
    actual fun append(chars: CharArray): StringBuilder {
        string += chars.concatToString()
        return this
    }

    actual fun append(string: String): StringBuilder {
        this.string += string
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

    @UseExperimental(ExperimentalStdlibApi::class)
    actual fun insert(index: Int, chars: CharArray): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        string = string.substring(0, index) + chars.concatToString() + string.substring(index)
        return this
    }

    actual fun insert(index: Int, csq: CharSequence?): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        string = string.substring(0, index) + csq.toString() + string.substring(index)
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

    public fun setRange(startIndex: Int, endIndex: Int, string: String): StringBuilder {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

        this.string = this.string.substring(0, startIndex) + string + this.string.substring(endIndex)
        return this
    }

    public fun deleteAt(index: Int): StringBuilder {
        AbstractList.checkElementIndex(index, length)

        string = string.substring(0, index) + string.substring(index + 1)
        return this
    }

    public fun deleteRange(startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

        string = string.substring(0, startIndex) + string.substring(endIndex)
        return this
    }

    public fun toCharArray(destination: CharArray, destinationOffset: Int, startIndex: Int, endIndex: Int) {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, length)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + endIndex - startIndex, destination.size)

        var dstIndex = destinationOffset
        for (index in startIndex until endIndex) {
            destination[dstIndex++] = string[index]
        }
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    public fun appendRange(chars: CharArray, startIndex: Int, endIndex: Int): StringBuilder {
        string += chars.concatToString(startIndex, endIndex)
        return this
    }

    public fun appendRange(csq: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder {
        val stringCsq = csq.toString()
        AbstractList.checkBoundsIndexes(startIndex, endIndex, stringCsq.length)

        string += stringCsq.substring(startIndex, endIndex)
        return this
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    public fun insertRange(index: Int, chars: CharArray, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkPositionIndex(index, this.length)

        string = string.substring(0, index) + chars.concatToString(startIndex, endIndex) + string.substring(index)
        return this
    }

    public fun insertRange(index: Int, csq: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkPositionIndex(index, length)

        val stringCsq = csq.toString()
        AbstractList.checkBoundsIndexes(startIndex, endIndex, stringCsq.length)

        string = string.substring(0, index) + stringCsq.substring(startIndex, endIndex) + string.substring(index)
        return this
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
public actual inline fun StringBuilder.setRange(startIndex: Int, endIndex: Int, string: String): StringBuilder =
    this.setRange(startIndex, endIndex, string)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.deleteAt(index: Int): StringBuilder = this.deleteAt(index)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.deleteRange(startIndex: Int, endIndex: Int): StringBuilder = this.deleteRange(startIndex, endIndex)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int, startIndex: Int, endIndex: Int) =
    this.toCharArray(destination, destinationOffset, startIndex, endIndex)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.appendRange(chars: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
    this.appendRange(chars, startIndex, endIndex)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.appendRange(csq: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder =
    this.appendRange(csq, startIndex, endIndex)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.insertRange(index: Int, chars: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
    this.insertRange(index, chars, startIndex, endIndex)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NOTHING_TO_INLINE")
public actual inline fun StringBuilder.insertRange(index: Int, csq: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder =
    this.insertRange(index, csq, startIndex, endIndex)
