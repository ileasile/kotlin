/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.test_util

import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerImpl
import org.jetbrains.kotlin.scripting.ide_services.evaluator.KJvmReplEvaluatorImpl
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.*

internal class JvmTestRepl : Closeable {
    private val currentLineCounter = AtomicInteger(0)

    private val compileConfiguration = simpleScriptCompilationConfiguration
    private val evalConfiguration = simpleScriptEvaluationConfiguration

    fun nextCodeLine(code: String): SourceCode =
        SourceCodeTestImpl(
            currentLineCounter.getAndIncrement(),
            code
        )

    private val replCompiler: KJvmReplCompilerImpl by lazy {
        KJvmReplCompilerImpl()
    }

    private val compiledEvaluator: KJvmReplEvaluatorImpl by lazy {
        KJvmReplEvaluatorImpl()
    }

    fun compile(code: SourceCode) = replCompiler.compile(code, compileConfiguration)
    fun complete(code: SourceCode, cursor: Int) = replCompiler.complete(code, cursor, compileConfiguration)

    fun eval(snippet: ILinkedPushStack<out CompiledSnippet>) = compiledEvaluator.eval(snippet, evalConfiguration)

    override fun close() {

    }

}

internal class SourceCodeTestImpl(number: Int, override val text: String) : SourceCode {
    override val name: String? = "Line_$number"
    override val locationId: String? = "location_$number"
}

@JvmName("iterableToList")
fun <T> ResultWithDiagnostics<Iterable<T>>.toList() = this.valueOrNull()?.toList().orEmpty()

@JvmName("sequenceToList")
fun <T> ResultWithDiagnostics<Sequence<T>>.toList() = this.valueOrNull()?.toList().orEmpty()

internal fun captureOut(body: () -> Unit): String = captureOutAndErr(
    body
).first

internal fun captureOutAndErr(body: () -> Unit): Pair<String, String> {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(errStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.err.flush()
        System.setOut(prevOut)
        System.setErr(prevErr)
    }
    return outStream.toString().trim() to errStream.toString().trim()
}
