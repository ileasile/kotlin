/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.evaluator

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.util.ReplHistory
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

class KJvmReplEvaluatorImpl(val scriptEvaluator: ScriptEvaluator = BasicJvmScriptEvaluator()) :
    ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet> {
    override var lastEvaluatedSnippet: LinkedPushStack<KJvmEvaluatedSnippet>? = null
        private set

    private val history = ReplHistory<KClass<*>?, Any?>()

    override fun eval(
        snippet: ILinkedPushStack<out CompiledSnippet>,
        configuration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<ILinkedPushStack<KJvmEvaluatedSnippet>> {

        val lastSnippetClass = history.lastItem()?.first
        val historyBeforeSnippet = history.items.map { it.second }
        val currentConfiguration = ScriptEvaluationConfiguration(configuration) {
            if (historyBeforeSnippet.isNotEmpty()) {
                previousSnippets.put(historyBeforeSnippet)
            }
            if (lastSnippetClass != null) {
                jvm {
                    baseClassLoader(lastSnippetClass.java.classLoader)
                }
            }
        }

        val snippetVal = snippet()
        val newEvalRes = when (val res = runBlocking { scriptEvaluator(snippetVal, currentConfiguration) }) {
            is ResultWithDiagnostics.Success -> {
                when (val retVal = res.value.returnValue) {
                    is ResultValue.Error -> {
                        history.add(retVal.scriptClass, null)
                        val error = (retVal.error as? Throwable) ?: retVal.wrappingException
                        KJvmEvaluatedSnippet(snippetVal, currentConfiguration, retVal, error, null, false)
                    }
                    is ResultValue.Value -> {
                        history.add(retVal.scriptClass, retVal.scriptInstance)
                        KJvmEvaluatedSnippet(snippetVal, currentConfiguration, retVal, null, retVal.value, true)
                    }
                    is ResultValue.Unit -> {
                        history.add(retVal.scriptClass, retVal.scriptInstance)

                        KJvmEvaluatedSnippet(snippetVal, currentConfiguration, retVal, null, null, false)
                    }
                    else -> throw IllegalStateException("Unexpected snippet result value $retVal")
                }
            }
            else ->
                KJvmEvaluatedSnippet(
                    snippetVal, currentConfiguration, null,
                    res.reports.find { it.exception != null }?.exception, null, false
                )
        }

        val newNode = lastEvaluatedSnippet.add(newEvalRes)
        lastEvaluatedSnippet = newNode
        return newNode.asSuccess()
    }

}

class KJvmEvaluatedSnippet(
    override val compiledSnippet: CompiledSnippet,
    override val configuration: ScriptEvaluationConfiguration,
    resultValue: ResultValue?,
    override val error: Throwable?,
    override val result: Any?,
    override val hasResult: Boolean
) : EvaluatedSnippet {
    val kClass = resultValue?.scriptClass
    val kInstance = resultValue?.scriptInstance
}
