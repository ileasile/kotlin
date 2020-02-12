/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.utils.KotlinReplError
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler

class ReplListErrorsTest : TestCase() {
    @Test
    fun testErrorsListing() {
        checkEvaluateInRepl(
            simpleScriptCompilationConfiguration,
            sequenceOf(
                """
                    data class AClass(val memx: Int, val memy: String)
                    data class BClass(val memz: String, val mema: AClass)
                    val foobar = 42
                    var foobaz = "string"
                    val v = BClass("KKK", AClass(5, "25"))
                """.trimIndent()
                        to ExpectedResult(emptyList()),

                """
                    val a = AClass("42", 3.14)
                    val b: Int = "str"
                    val c = foob
                """.trimIndent()
                        to ExpectedResult(
                    listOf(
                        KotlinReplError(1, 16, 1, 20, "Type mismatch: inferred type is String but Int was expected", "ERROR"),
                        KotlinReplError(1, 22, 1, 26, "The floating-point literal does not conform to the expected type String", "ERROR"),
                        KotlinReplError(2, 14, 2, 19, "Type mismatch: inferred type is String but Int was expected", "ERROR"),
                        KotlinReplError(3, 9, 3, 13, "Unresolved reference: foob", "ERROR")
                    )
                )
            )
        )
    }

    enum class ComparisonType {
        INCLUDES, EQUALS
    }

    data class ExpectedResult(val errors: List<KotlinReplError>, val compType: ComparisonType = ComparisonType.EQUALS)

    private val currentLineCounter = AtomicInteger()

    private fun nextCodeLine(code: String): ReplCodeLine = ReplCodeLine(currentLineCounter.getAndIncrement(), 0, code)

    private fun evaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        snippets: List<String>
    ): List<ResultWithDiagnostics<List<KotlinReplError>>> {
        val compiler = JvmReplCompiler(compilationConfiguration)
        val stateLock = ReentrantReadWriteLock()
        val state = compiler.createState(stateLock)
        return snippets.map { snippetText ->
            val codeLine = nextCodeLine(snippetText)
            val res = compiler.listErrors(state, codeLine).asSuccess()

            val codeLineForCompilation = nextCodeLine(snippetText)
            compiler.compile(state, codeLineForCompilation)
            res
        }
    }

    private fun checkEvaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        testData: Sequence<Pair<String, ExpectedResult>>
    ) {
        val (snippets, expected) = testData.unzip()
        val expectedIter = expected.iterator()
        evaluateInRepl(compilationConfiguration, snippets).forEachIndexed { index, res ->
            when (res) {
                is ResultWithDiagnostics.Failure -> Assert.fail("#$index: Expected result, got $res")
                is ResultWithDiagnostics.Success -> {
                    val (expectedVal, compType) = expectedIter.next()
                    val resVal = res.value
                    when (compType) {
                        ComparisonType.EQUALS -> Assert.assertEquals(
                            "#$index: Expected $expectedVal, got $resVal",
                            expectedVal,
                            resVal
                        )
                        ComparisonType.INCLUDES -> Assert.assertTrue(
                            "#$index: Expected $resVal to include $expectedVal",
                            resVal.containsAll(expectedVal)
                        )
                    }
                }
            }
        }
    }

}

