/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

class KlibSourceFile(
    val name: String
)

internal interface SourceFileIndex {
    fun getSourceFile(index: Int): KlibSourceFile
}

internal interface ReverseSourceFileIndex {
    fun getIndexOf(file: KlibSourceFile): Int
}