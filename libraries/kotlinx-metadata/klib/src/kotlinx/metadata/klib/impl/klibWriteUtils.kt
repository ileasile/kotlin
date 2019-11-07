/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlinx.metadata.klib.KlibHeader
import kotlinx.metadata.klib.KlibSourceFile
import kotlinx.metadata.klib.UniqId
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf

internal fun UniqId.writeUniqId(): KlibMetadataProtoBuf.DescriptorUniqId.Builder =
    KlibMetadataProtoBuf.DescriptorUniqId.newBuilder().apply {
        index = this@writeUniqId.index
    }

internal fun KlibHeader.writeHeader(): KlibMetadataProtoBuf.Header.Builder =
    KlibMetadataProtoBuf.Header.newBuilder().also { proto ->
        val (strings, qualifiedNames) = stringTable.buildProto()
        proto.qualifiedNames = qualifiedNames
        proto.strings = strings
        proto.addAllPackageFragmentName(packageFragmentName)
        proto.addAllFile(file.map { it.writeFile().build() })
    }

internal fun KlibSourceFile.writeFile(): KlibMetadataProtoBuf.File.Builder =
    KlibMetadataProtoBuf.File.newBuilder().also { proto ->
        proto.name = name
    }
