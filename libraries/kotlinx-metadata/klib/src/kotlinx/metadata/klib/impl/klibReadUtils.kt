/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlinx.metadata.klib.KlibHeader
import kotlinx.metadata.klib.KlibSourceFile
import kotlinx.metadata.klib.UniqId
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataStringTable
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf

internal fun KlibMetadataProtoBuf.DescriptorUniqId.readUniqId(): UniqId =
    UniqId(index)

// TODO: Refactor KlibMetadataStringTable so it could be prepopulated
//  with data from protobuf
internal fun KlibMetadataProtoBuf.Header.readHeader(): KlibHeader =
    KlibHeader(
        stringTable = KlibMetadataStringTable(),
        file = fileList.map(KlibMetadataProtoBuf.File::readFile),
        packageFragmentName = packageFragmentNameList
    )

internal fun KlibMetadataProtoBuf.File.readFile(): KlibSourceFile =
    KlibSourceFile(name)
