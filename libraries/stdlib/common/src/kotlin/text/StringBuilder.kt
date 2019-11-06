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
     * @param csq the character sequence from which a subsequence is appended. If [csq] is `null`,
     *  then characters are appended as if [csq] contained the four characters `"null"`.
     * @param start the beginning (inclusive) of the subsequence to append.
     * @param end the end (exclusive) of the subsequence to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [start] is negative, [end] is greater than the length of this Appendable, or `start > end`.
     *
     * @return this Appendable.
     */
    fun append(csq: CharSequence?, start: Int, end: Int): Appendable
}

/**
 * A mutable sequence of characters.
 *
 * String builder can be used to efficiently perform multiple string manipulation operations.
 */
expect class StringBuilder : Appendable, CharSequence {
    /** Constructs an empty string builder. */
    constructor()

    /** Constructs an empty string builder with the specified initial [capacity]. */
    constructor(capacity: Int)

    /** Constructs a string builder that contains the same characters as the specified [content] char sequence. */
    constructor(content: CharSequence)

    /** Constructs a string builder that contains the same characters as the specified [content] string. */
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
     * Surrogate pairs included in this string builder are treated as single characters.
     * Therefore, the order of the high-low surrogates is never reversed.
     *
     * Note that the reverse operation may produce new surrogate pairs that were unpaired low-surrogates and high-surrogates before the operation.
     * For example, reversing `"\uDC00\uD800"` produces `"\uD800\uDC00"` which is a valid surrogate pair.
     *
     * @return this string builder.
     */
    fun reverse(): StringBuilder

    /**
     * Appends the string representation of the specified [obj] object to this string builder.
     *
     * The overall effect is exactly as if the [obj] were converted to a string by the `obj.toString()` method,
     * and then that string was appended to this string builder.
     *
     * @return this string builder.
     */
    fun append(obj: Any?): StringBuilder

    /**
     * Appends the string representation of the specified [boolean] to this string builder.
     *
     * The overall effect is exactly as if the [boolean] were converted to a string by the `boolean.toString()` method,
     * and then that string was appended to this string builder.
     *
     * @return this string builder.
     */
    fun append(boolean: Boolean): StringBuilder

    /**
     * Appends characters in the specified [chars] array to this string builder.
     *
     * Characters are appended in order, starting at `0`.
     *
     * @return this string builder.
     */
    fun append(chars: CharArray): StringBuilder

    /**
     * Appends characters in a subarray of the specified [chars] array to this string builder.
     *
     * Characters are appended in order, starting at specified [offset].
     *
     * @param chars the array from which characters are appended.
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
     * The capacity is the maximum length this string builder can have before an allocation occurs.
     */
    fun capacity(): Int

    /**
     * Ensures that the capacity of this string builder is at least equal to the specified [minimumCapacity].
     *
     * If the current capacity is less than the [minimumCapacity], a new backing storage is allocated with greater capacity.
     * Otherwise, this method takes no action and simply returns.
     */
    fun ensureCapacity(minimumCapacity: Int)

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    fun indexOf(string: String): Int

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string],
     * starting at the specified [startIndex].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    fun indexOf(string: String, startIndex: Int): Int

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string].
     * The last occurrence of empty string `""` is considered to be at the index equal to `this.length`.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    fun lastIndexOf(string: String): Int

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string],
     * starting from the specified [startIndex] toward the beginning.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    fun lastIndexOf(string: String, startIndex: Int): Int

    /**
     * Inserts the string representation of the specified [boolean] into this string builder at the specified [index].
     *
     * The overall effect is exactly as if the [boolean] were converted to a string by the `boolean.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
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
     * @param chars the array from which characters are inserted.
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
     * @param csq the character sequence from which characters are inserted. If [csq] is `null`, then the four characters `"null"` are inserted.
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
     * @param csq the character sequence from which a subsequence is inserted. If [csq] is `null`,
     *  then characters will be inserted as if [csq] contained the four characters `"null"`.
     * @param startIndex the beginning (inclusive) of the subsequence to insert.
     * @param endIndex the end (exclusive) of the subsequence to insert.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [csq] character sequence indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     *
     * @return this string builder.
     */
    fun insert(index: Int, csq: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder

    /**
     * Inserts the string representation of the specified [obj] object into this string builder at the specified [index].
     *
     * The overall effect is exactly as if the [obj] were converted to a string by the `obj.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
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
     *  Sets the length of this string builder to the specified [newLength].
     *
     *  If the [newLength] is less than the current length, it is changed to the specified [newLength].
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
     * If the backing storage of this string builder is larger than necessary to hold its current contents,
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
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
public expect operator fun StringBuilder.set(index: Int, value: Char)

/**
 * Removes the character at the specified [index] from this string builder.
 *
 * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 *
 * @return this string builder.
 */
public expect fun StringBuilder.deleteAt(index: Int): StringBuilder

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
