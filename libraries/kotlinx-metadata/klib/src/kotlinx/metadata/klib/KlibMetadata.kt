/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.KmPackageFragment
import kotlinx.metadata.impl.accept
import kotlinx.metadata.klib.impl.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataStringTable
import org.jetbrains.kotlin.library.MetadataLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl

/**
 * Represents Kotlin package that is split into several [KmPackageFragment].
 * Splitting allows to boost performance when dealing with a huge package.
 */
data class KlibFragmentedPackage(
    val fqName: String,
    val fragments: List<KmPackageFragment>
)

/**
 * Represents the parsed metadata of KLIB.
 */
class KlibMetadata(
    val stringTable: KlibMetadataStringTable,
    val fragmentedPackageFragments: List<KlibFragmentedPackage>
) {

    companion object {
        fun read(library: MetadataLibrary): KlibMetadata {
            val moduleHeaderProto = parseModuleHeader(library.moduleHeaderData)
            val moduleHeader = moduleHeaderProto.readHeader()
            val nameResolver = NameResolverImpl(moduleHeaderProto.strings, moduleHeaderProto.qualifiedNames)
            val fileIndex = SourceFileIndexReadExtension(moduleHeader.file)
            val packageFragments = moduleHeader.packageFragmentName.map { packageFqName ->
                val fragments = library.packageMetadataParts(packageFqName).map { part ->
                    val packageFragment = parsePackageFragment(library.packageMetadata(packageFqName, part))
                    KmPackageFragment().apply { packageFragment.accept(this, nameResolver, listOf(fileIndex)) }
                }
                KlibFragmentedPackage(packageFqName, fragments)
            }
            return KlibMetadata(moduleHeader.stringTable, packageFragments)
        }
    }

    fun write(): SerializedMetadata {
        val reverseIndex = ReverseSourceFileIndexWriteExtension()
        val packagesProtoParts = fragmentedPackageFragments.map { (_, fragments) ->
            fragments.map { KlibPackageFragmentWriter(stringTable, reverseIndex).also(it::accept).write() }
        }
        val header = KlibHeader(stringTable, reverseIndex.fileIndex, fragmentedPackageFragments.map { it.fqName })
        return SerializedMetadata(
            header.writeHeader().build().toByteArray(),
            packagesProtoParts.map { it.map(ProtoBuf.PackageFragment::toByteArray) },
            header.packageFragmentName
        )
    }
}