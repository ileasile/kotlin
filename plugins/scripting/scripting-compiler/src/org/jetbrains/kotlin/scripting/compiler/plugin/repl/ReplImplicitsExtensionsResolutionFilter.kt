/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import org.jetbrains.kotlin.resolve.calls.tower.ImplicitReceiverGetter
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.calls.tower.ScopeWithImplicitsExtensionsResolutionInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.scripting.resolve.LazyScriptDescriptor
import kotlin.reflect.KClass

abstract class AbstractReplImplicitsExtensionsResolutionFilter : ImplicitsExtensionsResolutionFilter {
    private val processedGroups = mutableSetOf<ReceiverGroup>()

    abstract fun filter(receiverGroup: ReceiverGroup): SkippingPolicy

    abstract fun getGroup(receiver: ReceiverValueWithSmartCastInfo): ReceiverGroup

    final override fun getScopesWithInfo(
        scopes: Sequence<HierarchicalScope>,
        getImplicitReceiver: ImplicitReceiverGetter
    ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> {
        clear()
        return scopes.map { scope ->
            val receiver = getImplicitReceiver(scope)
            val keep = receiver?.let {
                val group = getGroup(it)
                when (filter(group)) {
                    SkippingPolicy.DONT_SKIP -> true
                    SkippingPolicy.SKIP_ALL -> false
                    SkippingPolicy.SKIP_AFTER_FIRST -> {
                        val processed = group in processedGroups
                        processedGroups.add(group)
                        !processed
                    }
                }
            } ?: true

            ScopeWithImplicitsExtensionsResolutionInfo(scope, receiver, keep)
        }
    }

    fun clear() {
        processedGroups.clear()
    }

    data class ReceiverGroup(val name: String)

    enum class SkippingPolicy {
        DONT_SKIP, SKIP_ALL, SKIP_AFTER_FIRST;
    }
}

class BaseReplImplicitsExtensionsResolutionFilter(
    baseScriptClass: KClass<*>? = null,
    classesToSkip: Collection<KClass<*>> = emptyList(),
    classesToSkipAfterFirstTime: Collection<KClass<*>> = emptyList()
) : AbstractReplImplicitsExtensionsResolutionFilter() {
    private val baseScriptClassName = baseScriptClass?.qualifiedName
    private val classesToSkipNames = classesToSkip.map { it.qualifiedName!! }.toHashSet()
    private val classesToSkipFirstTimeNames = classesToSkipAfterFirstTime.map { it.qualifiedName!! }.toHashSet()

    override fun filter(receiverGroup: ReceiverGroup): SkippingPolicy {
        return when (receiverGroup) {
            DEFAULT_GROUP -> SkippingPolicy.DONT_SKIP
            LINE_CLASS_GROUP -> SkippingPolicy.SKIP_AFTER_FIRST
            SKIP_GROUP -> SkippingPolicy.SKIP_ALL
            else -> {
                if (receiverGroup.name.startsWith(FIRST_TIME_SKIP_PREFIX)) SkippingPolicy.SKIP_AFTER_FIRST
                else SkippingPolicy.DONT_SKIP
            }
        }
    }

    override fun getGroup(receiver: ReceiverValueWithSmartCastInfo): ReceiverGroup {
        val receiverValue = receiver.receiverValue
        if (receiverValue !is ImplicitClassReceiver) return DEFAULT_GROUP

        val descriptor = receiverValue.declarationDescriptor
        val descriptorFqName = descriptor.fqNameSafe.asString()
        if (descriptorFqName in classesToSkipNames) return SKIP_GROUP
        if (descriptorFqName in classesToSkipFirstTimeNames) return ReceiverGroup("$FIRST_TIME_SKIP_PREFIX$descriptorFqName")

        if (descriptor !is LazyScriptDescriptor) return DEFAULT_GROUP
        val superClass = descriptor.getSuperClassNotAny() ?: return DEFAULT_GROUP

        if (superClass.fqNameSafe.asString() == baseScriptClassName) return LINE_CLASS_GROUP

        return DEFAULT_GROUP
    }

    companion object {
        private val DEFAULT_GROUP = ReceiverGroup("default")
        private val LINE_CLASS_GROUP = ReceiverGroup("line")
        private val SKIP_GROUP = ReceiverGroup("skip")

        private const val FIRST_TIME_SKIP_PREFIX = "first_"
    }
}