/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

typealias ImplicitReceiverGetter = (HierarchicalScope) -> ReceiverValueWithSmartCastInfo?

@DefaultImplementation(DefaultImplicitsExtensionsResolutionFilter::class)
interface ImplicitsExtensionsResolutionFilter {
    fun getScopesWithInfo(
        scopes: Sequence<HierarchicalScope>,
        getImplicitReceiver: ImplicitReceiverGetter
    ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo>
}

class DefaultImplicitsExtensionsResolutionFilter : ImplicitsExtensionsResolutionFilter {
    override fun getScopesWithInfo(
        scopes: Sequence<HierarchicalScope>,
        getImplicitReceiver: ImplicitReceiverGetter
    ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> = scopes.map { scope ->
        ScopeWithImplicitsExtensionsResolutionInfo(scope, getImplicitReceiver(scope), true)
    }
}

class ScopeWithImplicitsExtensionsResolutionInfo(
    val scope: HierarchicalScope,
    val implicitReceiver: ReceiverValueWithSmartCastInfo?,
    val resolveExtensionsForImplicitReceiver: Boolean,
)