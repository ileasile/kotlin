/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.KmPackageFragment
import kotlinx.metadata.impl.WriteContext
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
 * Allows to modify the way fragments of the single package are read by [KlibMetadata.read].
 * For example, it may be convenient to join fragments into a single one.
 */
interface KlibPackageFragmentReadStrategy {
    fun processPackageParts(packageFqName: String, parts: List<KmPackageFragment>): List<Pair<String, KmPackageFragment>>

    companion object {
        val DEFAULT = object : KlibPackageFragmentReadStrategy {
            override fun processPackageParts(packageFqName: String, parts: List<KmPackageFragment>) =
                parts.map { packageFqName to it }
        }
    }
}

/**
 * Allows to modify the way package fragments are written by [KlibMetadata.write].
 * For example, splitting big fragments into several small one allows to improve IDE performance.
 */
interface KlibPackageFragmentWriteStrategy {
    fun processPackageParts(packageFqName: String, parts: List<KmPackageFragment>): List<KmPackageFragment>

    companion object {
        val DEFAULT = object : KlibPackageFragmentWriteStrategy {
            override fun processPackageParts(packageFqName: String, parts: List<KmPackageFragment>): List<KmPackageFragment> =
                parts
        }
    }
}

/**
 * Represents the parsed metadata of KLIB.
 */
class KlibMetadata(val namedPackageFragments: List<Pair<String, KmPackageFragment>>) {

    companion object {
        /**
         * Deserializes metadata from the given [library].
         * @param readStrategy specifies the way package fragments of a single package are modified (e.g. merged) after deserialization.
         */
        // TODO: exposes MetadataLibrary which is internal!
        fun read(
            library: MetadataLibrary,
            readStrategy: KlibPackageFragmentReadStrategy = KlibPackageFragmentReadStrategy.DEFAULT
        ): KlibMetadata {
            val moduleHeaderProto = parseModuleHeader(library.moduleHeaderData)
            val moduleHeader = moduleHeaderProto.readHeader()
            val nameResolver = NameResolverImpl(moduleHeaderProto.strings, moduleHeaderProto.qualifiedNames)
            val fileIndex = SourceFileIndexReadExtension(moduleHeader.file)
            val packageFragments = moduleHeader.packageFragmentName.flatMap { packageFqName ->
                library.packageMetadataParts(packageFqName).map { part ->
                    val packageFragment = parsePackageFragment(library.packageMetadata(packageFqName, part))
                    KmPackageFragment().apply { packageFragment.accept(this, nameResolver, listOf(fileIndex)) }
                }.let { readStrategy.processPackageParts(packageFqName, it) }
            }
            return KlibMetadata(packageFragments)
        }
    }

    /**
     * Writes metadata back to serialized representation.
     * @param writeStrategy specifies the way package fragments are modified (e.g. splitted) before serialization.
     */
    // TODO: exposes SerializedMetadata which is internal!
    fun write(
        writeStrategy: KlibPackageFragmentWriteStrategy = KlibPackageFragmentWriteStrategy.DEFAULT
    ): SerializedMetadata {
        val reverseIndex = ReverseSourceFileIndexWriteExtension()
        val c = WriteContext(KlibMetadataStringTable(), listOf(reverseIndex))

        val groupedPackageFragmentsProtos = namedPackageFragments
            .groupBy({ it.first }, { it.second })
            .mapValues { writeStrategy.processPackageParts(it.key, it.value) }
            .mapValues { (_, fragments) ->
                fragments.map {
                    KlibPackageFragmentWriter(c.strings as KlibMetadataStringTable, c.contextExtensions).also(it::accept).write()
                }
            }
        val header = KlibHeader(reverseIndex.fileIndex, groupedPackageFragmentsProtos.map { it.key })
        return SerializedMetadata(
            header.writeHeader(c).build().toByteArray(),
            groupedPackageFragmentsProtos.map { it.value.map(ProtoBuf.PackageFragment::toByteArray) },
            header.packageFragmentName
        )
    }
}