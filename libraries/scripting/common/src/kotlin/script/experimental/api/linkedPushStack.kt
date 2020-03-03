/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

interface ILinkedPushStack<T> {
    val previous: ILinkedPushStack<T>?
    operator fun invoke(): T
}

fun <T> ILinkedPushStack<T>?.toList(): List<T> = toList { it }

fun <T, R> ILinkedPushStack<T>?.toList(mapper: (T) -> R): List<R> {
    val res = ArrayList<R>()
    var el = this

    while (el != null) {
        res.add(mapper(el()))
        el = el.previous
    }

    res.reverse()
    return res
}

operator fun <T> ILinkedPushStack<T>?.invoke(): T? = if (this == null) null else this()

class LinkedPushStack<T>(private val _val: T, override val previous: LinkedPushStack<T>?) : ILinkedPushStack<T> {
    override operator fun invoke(): T = _val
}

fun <T> LinkedPushStack<T>?.add(value: T) = LinkedPushStack(value, this)
