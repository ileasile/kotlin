/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * An object to which char sequences and values can be appended.
 */
expect interface Appendable {
    /**
     * Appends the specified character [c] to this Appendable.
     *
     * @param c the character to append.
     * @return this Appendable.
     */
    fun append(c: Char): Appendable

    /**
     * Appends the specified character sequence [csq] to this Appendable.
     *
     * @param csq the character sequence to append. If [csq] is `null`, then the four characters `"null"` are appended to this Appendable.
     * @return this Appendable.
     */
    fun append(csq: CharSequence?): Appendable

    /**
     * Appends a subsequence of the specified character sequence [csq] to this Appendable.
     *
     * @param csq the character sequence from which a subsequence will be appended. If [csq] is `null`,
     *  then characters will be appended as if [csq] contained the four characters `"null"`.
     * @param start the beginning (inclusive) of the subsequence to append.
     * @param end the end (exclusive) of the subsequence to append.
     *
     * @throws IndexOutOfBoundsException if [start] is less than zero, [start] is greater than [end], or [end] is greater than the size of this Appendable.
     *
     * @return this Appendable.
     */
    fun append(csq: CharSequence?, start: Int, end: Int): Appendable
}

/**
 * A mutable sequence of characters.
 */
expect class StringBuilder : Appendable, CharSequence {
    /** Constructs an empty string builder. */
    constructor()

    /** Constructs an empty string builder with the specified initial [capacity]. */
    constructor(capacity: Int)

    /** Constructs a string builder that contains the same characters as the specified [CharSequence]. */
    constructor(content: CharSequence)

    /** Constructs a string builder that contains the same characters as the specified [String]. */
    constructor(content: String)

    override val length: Int

    override operator fun get(index: Int): Char

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    override fun append(c: Char): StringBuilder
    override fun append(csq: CharSequence?): StringBuilder
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder

    /**
     * Reverses the contents of this string builder.
     *
     * If there are any surrogate pairs included in the string builder, these are treated as single characters for the reverse operation.
     * Thus, the order of the high-low surrogates is never reversed.
     *
     * Note that the reverse operation may result in producing surrogate pairs that were unpaired low-surrogates and high-surrogates before the operation.
     * For example, reversing `"\uDC00\uD800"` produces `"\uD800\uDC00"` which is a valid surrogate pair.
     *
     * @return this string builder.
     */
    fun reverse(): StringBuilder

    /**
     * Appends the string representation of the specified object [obj].
     *
     * The overall effect is exactly as if the argument were converted to a string by the method `obj.toString()`,
     * and the characters of that string were then appended to this string builder.
     *
     * @return this string builder.
     */
    fun append(obj: Any?): StringBuilder

    /**
     * Appends the string representation of the specified [boolean] to this string builder.
     *
     * @return this string builder.
     */
    fun append(boolean: Boolean): StringBuilder

    /**
     * Appends characters in the specified [chars] array to the contents of this string builder.
     *
     * Characters are appended in order, starting at `0`.
     *
     * @return this string builder.
     */
    fun append(chars: CharArray): StringBuilder

    /**
     * Appends characters in a subarray of the specified [chars] array to the contents of this string builder.
     *
     * Characters are appended in order, starting at specified [offset].
     *
     * @param chars the array from which characters will be appended.
     * @param offset the beginning (inclusive) of the subarray to append.
     * @param length the length of the subarray to append.
     *
     * @throws IndexOutOfBoundsException if either [offset] or [length] are less than zero
     *  or `offset + length` is out of [chars] array bounds.
     *
     * @return this string builder.
     */
    fun append(chars: CharArray, offset: Int, length: Int): StringBuilder

    /**
     * Appends the specified [string] to this string builder.
     *
     * @return this string builder.
     */
    fun append(string: String): StringBuilder

    /**
     * Appends the string representation of the specified [codePoint] to this string builder.
     *
     * @param codePoint the Unicode code point to append.
     *
     * @throws IllegalArgumentException if [codePoint] is not a valid Unicode code point value.
     *
     * @return this string builder.
     */
    fun appendCodePoint(codePoint: Int): StringBuilder

    /**
     * Returns the Unicode code point before the specified [index].
     *
     * If the Char value at `index - 1` is in the low-surrogate range,
     * `index - 2` is not negative, and the Char value at `index - 2` is in the high-surrogate range,
     * then the supplementary code point corresponding to this surrogate pair is returned.
     * Otherwise, the Char value at `index - 1` is returned.
     *
     * @throws IndexOutOfBoundsException if [index] is less then one or greater then the length of this string builder.
     */
    fun codePointBefore(index: Int): Int

    /**
     * Returns the number of Unicode code points in the specified range of this string builder.
     *
     * Unpaired surrogates within the specified range count as one code point each.
     *
     * @param [startIndex] the beginning (inclusive) of the range to count code points in.
     * @param [endIndex] the end (exclusive) of the range to count code points in.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this array indices or when `startIndex > endIndex`.
     */
    fun codePointCount(startIndex: Int, endIndex: Int): Int

    /**
     * Returns the index within this sequence that is offset from the given [index] by [codePointOffset] code points.
     *
     * The specified [codePointOffset] can be negative that means the returned index is offset from the given [index]
     * towards the beginning of this string builder by the absolute value of [codePointOffset].
     *
     * Unpaired surrogates within the text range given by [index] and [codePointOffset] count as one code point each.
     *
     * @param index the index to be offset
     * @param codePointOffset the offset in code points
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] if index is negative or larger then the length of this sequence,
     *  or if [codePointOffset] is positive and the subsequence starting with [index] has fewer than [codePointOffset] code points,
     *  or if [codePointOffset] is negative and the subsequence before [index] has fewer than the absolute value of [codePointOffset] code points.
     */
    fun offsetByCodePoints(index: Int, codePointOffset: Int): Int

    /**
     * Removes characters in the specified range from this string builder.
     *
     * @param startIndex the beginning (inclusive) of the range to remove.
     * @param endIndex the end (exclusive) of the range to remove.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [startIndex] is negative, greater than the length of this string builder, or `startIndex > endIndex`.
     *
     * @return this string builder.
     */
    fun delete(startIndex: Int, endIndex: Int): StringBuilder

    /**
     * Returns the current capacity of this string builder.
     *
     * The capacity is the amount of storage available for newly inserted characters, beyond which an allocation will occur.
     */
    fun capacity(): Int

    /**
     * Ensures that the capacity is at least equal to the specified [minimumCapacity].
     *
     * If the current capacity is less than the [minimumCapacity], then a new internal buffer is allocated with greater capacity.
     * If the [minimumCapacity] is nonpositive, this method takes no action and simply returns.
     */
    fun ensureCapacity(minimumCapacity: Int)

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string].
     *
     * If the specified [string] does not occur in this string builder, `-1` is returned.
     */
    fun indexOf(string: String): Int

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string],
     * starting at the specified [startIndex].
     *
     * If the specified [string] does not occur in this string builder starting at the specified [startIndex], `-1` is returned.
     */
    fun indexOf(string: String, startIndex: Int): Int

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string].
     * The rightmost empty string `""` is considered to occur at the index equal to `this.length`.
     *
     * If the specified [string] does not occur in this string builder, `-1` is returned.
     */
    fun lastIndexOf(string: String): Int

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string],
     * starting from the specified [startIndex] toward the beginning.
     *
     * If the specified [string] does not occur in this string builder starting at the specified [startIndex], `-1` is returned.
     */
    fun lastIndexOf(string: String, startIndex: Int): Int

    /**
     * Inserts the string representation of the specified [boolean] into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, boolean: Boolean): StringBuilder

    /**
     * Inserts the specified character [char] into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, char: Char): StringBuilder

    /**
     * Inserts characters in the specified [chars] array into this string builder at the specified [index].
     *
     * The inserted characters go in same order as in the [chars] array, starting at [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, chars: CharArray): StringBuilder

    /**
     * Inserts characters in a subarray of the specified [chars] array into this string builder at the specified [index].
     *
     * The inserted characters go in same order as in the [chars] array, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param chars the array from which characters will be inserted.
     * @param offset the beginning (inclusive) of the subarray to insert.
     * @param length the length of the subarray to insert.
     *
     * @throws IndexOutOfBoundsException if either [offset] or [length] are less than zero
     *  or `offset + length` is out of [chars] array bounds.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, chars: CharArray, offset: Int, length: Int): StringBuilder

    /**
     * Inserts characters in the specified character sequence [csq] into this string builder at the specified [index].
     *
     * The inserted characters go in the same order as in the [csq] character sequence, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param csq the character sequence from which characters will be inserted. If [csq] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, csq: CharSequence?): StringBuilder

    /**
     * Inserts characters in a subsequence of the specified character sequence [csq] into this string builder at the specified [index].
     *
     * The inserted characters go in the same order as in the [csq] character sequence, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param csq the character sequence from which a subsequence will be inserted. If [csq] is `null`,
     *  then characters will be inserted as if [csq] contained the four characters `"null"`.
     * @param startIndex the beginning (inclusive) of the subsequence to append.
     * @param endIndex the end (exclusive) of the subsequence to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [csq] character sequence indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, csq: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder

    /**
     * Inserts the string representation of the specified object [obj] into this string builder at the specified [index].
     *
     * The overall effect is exactly as if the [obj] were converted to a string by the method `obj.toString()`,
     * and the characters of that string were then inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, obj: Any?): StringBuilder

    /**
     * Inserts the [string] into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, string: String): StringBuilder

    /**
     * Replaces characters in the specified range of this string builder with characters in the specified [string].
     *
     * @param startIndex the beginning (inclusive) of the range to replace.
     * @param endIndex the end (exclusive) of the range to replace.
     * @param string the string to replace with.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [startIndex] is less than zero, greater than the length of this string builder, or `startIndex > endIndex`.
     *
     * @return this string builder.
     */
    fun replace(startIndex: Int, endIndex: Int, string: String): StringBuilder

    /**
     *  Sets the length of this string builder.
     *
     *  If the [newLength] is less than the current length, the length is changed to the specified length.
     *  Otherwise, null characters '\u0000' are appended to this string builder until its length is less than the [newLength].
     *
     *  @throws IndexOutOfBoundsException or [IllegalArgumentException] if [newLength] is less than zero.
     */
    fun setLength(newLength: Int)

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [length] (exclusive).
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of this string builder.
     */
    fun substring(startIndex: Int): String

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [endIndex] (exclusive).
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     */
    fun substring(startIndex: Int, endIndex: Int): String

    /**
     * Attempts to reduce storage used for this string builder.
     *
     * If the backing storage of this string builder is larger than necessary to hold its current sequence of characters,
     * then it may be resized to become more space efficient.
     * Calling this method may, but is not required to, affect the value of the [capacity] property.
     */
    fun trimToSize()
}


/**
 * Clears the content of this string builder making it empty.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
public expect fun StringBuilder.clear(): StringBuilder

/**
 * Sets the character at the specified [index] to the specified [value].
 *
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than or equal to the length of this string builder.
 */
public expect operator fun StringBuilder.set(index: Int, value: Char)

/**
 * Returns the Unicode code point at the specified [index].
 *
 * If the Char value at the specified [index] is in the high-surrogate range, the following index is less than the length of this string builder,
 * and the Char value at the following index is in the low-surrogate range, then the supplementary code point corresponding to this surrogate pair is returned.
 * Otherwise, the Char value at the given [index] is returned.
 *
 * @throws IndexOutOfBoundsException if [index] is negative or not less than the length of this string builder.
 */
public expect fun StringBuilder.codePoint(index: Int): Int

/**
 * Removes the character at the specified [index] from this string builder.
 *
 * If the `Char` at the specified index is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is negative or greater than or equal to the length of this string builder.
 *
 * @return this string builder.
 */
public expect fun StringBuilder.delete(index: Int): StringBuilder

/**
 * Copies characters from this string builder into the [destination] character array.
 *
 * @param destination the array to copy to.
 * @param destinationOffset the position in the array to copy to.
 * @param startIndex the beginning (inclusive) of the range to copy.
 * @param endIndex the end (exclusive) of the range to copy.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
 *  or when that index is out of the [destination] array indices range.
 */
public expect fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int, startIndex: Int, endIndex: Int)
