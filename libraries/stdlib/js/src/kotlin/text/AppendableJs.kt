/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

public actual interface Appendable {
    public actual fun append(c: Char): Appendable
    public actual fun append(csq: CharSequence?): Appendable
    @Deprecated("", level = DeprecationLevel.WARNING)
    public actual fun append(csq: CharSequence?, start: Int, end: Int): Appendable
}