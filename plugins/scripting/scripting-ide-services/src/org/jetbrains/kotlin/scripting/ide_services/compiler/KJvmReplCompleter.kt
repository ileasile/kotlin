/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KotlinResolutionFacade
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import kotlin.script.experimental.api.ReplCompletionResult
import kotlin.script.experimental.api.ReplCompletionVariant

class KJvmReplCompleter(
    private val ktScript: KtFile,
    private val bindingContext: BindingContext,
    private val resolutionFacade: KotlinResolutionFacade,
    private val moduleDescriptor: ModuleDescriptor,
    private val cursor: Int
) {

    private fun getElementAt(cursorPos: Int): PsiElement? {
        var element: PsiElement? = ktScript.findElementAt(cursorPos)
        while (element !is KtExpression && element != null) {
            element = element.parent
        }
        return element
    }

    fun getCompletion(): ReplCompletionResult {
        val element = getElementAt(cursor)

        var descriptors: Collection<DeclarationDescriptor>? = null
        var isTipsManagerCompletion = true
        var isSortNeeded = true

        if (element == null)
            return emptySequence()

        val simpleExpression = when {
            element is KtSimpleNameExpression -> element
            element.parent is KtSimpleNameExpression -> element.parent as KtSimpleNameExpression
            else -> null
        }

        if (simpleExpression != null) {
            val inDescriptor: DeclarationDescriptor = simpleExpression.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor
            val prefix = element.text.substringBeforeLast(INSERTED_STRING)
            isSortNeeded = false
            descriptors = ReferenceVariantsHelper(
                bindingContext,
                resolutionFacade,
                moduleDescriptor,
                VisibilityFilter(inDescriptor)
            ).getReferenceVariants(
                simpleExpression,
                DescriptorKindFilter.ALL,
                { name: Name -> !name.isSpecial && name.identifier.startsWith(prefix) },
                filterOutJavaGettersAndSetters = true,
                filterOutShadowed = false, // setting to true makes it slower up to 4 times
                excludeNonInitializedVariable = true,
                useReceiverType = null
            )

        } else if (element is KtStringTemplateExpression) {
            if (element.hasInterpolation()) {
                return emptySequence()
            }

            val stringVal = element.entries.joinToString("") {
                val t = it.text
                if (it.startOffset <= cursor && cursor <= it.endOffset) {
                    val s = cursor - it.startOffset
                    val e = s + INSERTED_STRING.length
                    t.substring(0, s) + t.substring(e)
                } else t
            }

            val separatorIndex = stringVal.lastIndexOfAny(charArrayOf('/', '\\'))
            val dir = if (separatorIndex != -1) {
                stringVal.substring(0, separatorIndex + 1)
            } else {
                "."
            }
            val namePrefix = stringVal.substring(separatorIndex + 1)

            val file = File(dir)

            return file.listFiles { p, f -> p == file && f.startsWith(namePrefix, true) }?.map {
                ReplCompletionVariant(it.name, it.name, "file", "file")
            }?.asSequence() ?: emptySequence()

        } else {
            isTipsManagerCompletion = false
            val resolutionScope: LexicalScope?
            val parent = element.parent

            val qualifiedExpression = when {
                element is KtQualifiedExpression -> {
                    isTipsManagerCompletion = true
                    element
                }
                parent is KtQualifiedExpression -> parent
                else -> null
            }

            if (qualifiedExpression != null) {
                val receiverExpression = qualifiedExpression.receiverExpression
                val expressionType = bindingContext.get(
                    BindingContext.EXPRESSION_TYPE_INFO,
                    receiverExpression
                )?.type
                if (expressionType != null) {
                    isSortNeeded = false
                    descriptors = ReferenceVariantsHelper(
                        bindingContext,
                        resolutionFacade,
                        moduleDescriptor,
                        { true }
                    ).getReferenceVariants(
                        receiverExpression,
                        CallTypeAndReceiver.DOT(receiverExpression),
                        DescriptorKindFilter.ALL,
                        ALL_NAME_FILTER
                    )
                }
            } else {
                resolutionScope = bindingContext.get(
                    BindingContext.LEXICAL_SCOPE,
                    element as KtExpression?
                )
                descriptors = (resolutionScope?.getContributedDescriptors(
                    DescriptorKindFilter.ALL,
                    ALL_NAME_FILTER
                )
                    ?: return emptySequence())
            }
        }

        val result: MutableList<ReplCompletionVariant> = ArrayList()

        if (descriptors != null) {
            var prefix = (if (isTipsManagerCompletion) element.text else element.parent.text).substringBeforeLast(INSERTED_STRING)
            val cursorWithinElement = cursor - element.startOffset
            val dotIndex = prefix.lastIndexOf('.', cursorWithinElement)

            prefix = if (dotIndex >= 0) {
                prefix.substring(dotIndex + 1, cursorWithinElement)
            } else {
                prefix.substring(0, cursorWithinElement)
            }

            if (descriptors !is ArrayList<*>) {
                descriptors = ArrayList(descriptors)
            }

            (descriptors as ArrayList<DeclarationDescriptor>)
                .map {
                    val presentation =
                        getPresentation(
                            it
                        )
                    Triple(it, presentation, (presentation.presentableText + presentation.tailText).toLowerCase())
                }
                .onlyIf({ isSortNeeded }) { it.sortedBy { descTriple -> descTriple.third } }
                .forEach {
                    val descriptor = it.first
                    val (rawName, presentableText, tailText, completionText) = it.second
                    if (rawName.startsWith(prefix)) {
                        val fullName: String =
                            formatName(
                                presentableText
                            )
                        result.add(
                            ReplCompletionVariant(
                                completionText,
                                fullName,
                                tailText,
                                getIconFromDescriptor(
                                    descriptor
                                )
                            )
                        )
                    }
                }

            result.addAll(
                keywordsCompletionVariants(
                    KtTokens.KEYWORDS,
                    prefix
                )
            )
            result.addAll(
                keywordsCompletionVariants(
                    KtTokens.SOFT_KEYWORDS,
                    prefix
                )
            )
        }

        return result.asSequence()

    }

    private inner class VisibilityFilter(
        private val inDescriptor: DeclarationDescriptor
    ) : (DeclarationDescriptor) -> Boolean {
        override fun invoke(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

            if (descriptor is DeclarationDescriptorWithVisibility) {
                return try {
                    descriptor.visibility.isVisible(null, descriptor, inDescriptor)
                } catch (e: IllegalStateException) {
                    true
                }
            }

            return true
        }

        private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
            val owner = typeParameter.containingDeclaration
            var parent: DeclarationDescriptor? = inDescriptor
            while (parent != null) {
                if (parent == owner) return true
                if (parent is ClassDescriptor && !parent.isInner) return false
                parent = parent.containingDeclaration
            }
            return true
        }
    }

    companion object {
        const val INSERTED_STRING = "ABCDEF"
        private const val NUMBER_OF_CHAR_IN_COMPLETION_NAME = 40

        private fun keywordsCompletionVariants(
            keywords: TokenSet,
            prefix: String
        ): List<ReplCompletionVariant> {
            return keywords.types.mapNotNull {
                val token = (it as KtKeywordToken).value
                if (!token.startsWith(prefix)) null
                else ReplCompletionVariant(token, token, "keyword", "keyword")
            }
        }

        private val RENDERER =
            IdeDescriptorRenderers.SOURCE_CODE.withOptions {
                this.classifierNamePolicy =
                    ClassifierNamePolicy.SHORT
                this.typeNormalizer =
                    IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
                this.parameterNameRenderingPolicy =
                    ParameterNameRenderingPolicy.NONE
                this.renderDefaultAnnotationArguments = false
                this.typeNormalizer = lambda@{ kotlinType: KotlinType ->
                    if (kotlinType.isFlexible()) {
                        return@lambda kotlinType.asFlexibleType().upperBound
                    }
                    kotlinType
                }
            }

        private fun getIconFromDescriptor(descriptor: DeclarationDescriptor): String = when (descriptor) {
            is FunctionDescriptor -> "method"
            is PropertyDescriptor -> "property"
            is LocalVariableDescriptor -> "property"
            is ClassDescriptor -> "class"
            is PackageFragmentDescriptor -> "package"
            is PackageViewDescriptor -> "package"
            is ValueParameterDescriptor -> "genericValue"
            is TypeParameterDescriptorImpl -> "class"
            else -> ""
        }

        private fun formatName(builder: String, symbols: Int = NUMBER_OF_CHAR_IN_COMPLETION_NAME): String {
            return if (builder.length > symbols) {
                builder.substring(0, symbols) + "..."
            } else builder
        }

        // Insert a constant string right after a cursor position to make this identifiable as a simple reference
        // For example, code line
        //   import java.
        //               ^
        // is converted to
        //   import java.ABCDEF
        // and it makes token after dot (for which reference variants are looked) discoverable in PSI
        fun prepareCode(code: String, cursor: Int) = code.substring(0, cursor) + INSERTED_STRING + code.substring(cursor)

        data class DescriptorPresentation(
            val rawName: String,
            val presentableText: String,
            val tailText: String,
            val completionText: String
        )

        fun getPresentation(descriptor: DeclarationDescriptor): DescriptorPresentation {
            val rawDescriptorName = descriptor.name.asString()
            val descriptorName = rawDescriptorName.quoteIfNeeded()
            var presentableText = descriptorName
            var typeText = ""
            var tailText = ""
            var completionText = ""
            if (descriptor is FunctionDescriptor) {
                val returnType = descriptor.returnType
                typeText =
                    if (returnType != null) RENDERER.renderType(returnType) else ""
                presentableText += RENDERER.renderFunctionParameters(
                    descriptor
                )
                val parameters = descriptor.valueParameters
                if (parameters.size == 1 && parameters.first().type.isFunctionType)
                    completionText = "$descriptorName { "

                val extensionFunction = descriptor.extensionReceiverParameter != null
                val containingDeclaration = descriptor.containingDeclaration
                if (extensionFunction) {
                    tailText += " for " + RENDERER.renderType(
                        descriptor.extensionReceiverParameter!!.type
                    )
                    tailText += " in " + DescriptorUtils.getFqName(containingDeclaration)
                }
            } else if (descriptor is VariableDescriptor) {
                val outType =
                    descriptor.type
                typeText = RENDERER.renderType(outType)
            } else if (descriptor is ClassDescriptor) {
                val declaredIn = descriptor.containingDeclaration
                tailText = " (" + DescriptorUtils.getFqName(declaredIn) + ")"
            } else {
                typeText = RENDERER.render(descriptor)
            }
            tailText = if (typeText.isEmpty()) tailText else typeText

            if (completionText.isEmpty()) {
                completionText = presentableText
                var position = completionText.indexOf('(')
                if (position != -1) { //If this is a string with a package after
                    if (completionText[position - 1] == ' ') {
                        position -= 2
                    }
                    //if this is a method without args
                    if (completionText[position + 1] == ')') {
                        position++
                    }
                    completionText = completionText.substring(0, position + 1)
                }
                position = completionText.indexOf(":")
                if (position != -1) {
                    completionText = completionText.substring(0, position - 1)
                }
            }

            return DescriptorPresentation(
                rawDescriptorName,
                presentableText,
                tailText,
                completionText
            )
        }
    }
}
