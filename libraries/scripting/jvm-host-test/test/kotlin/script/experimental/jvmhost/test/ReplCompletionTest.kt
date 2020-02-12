/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.utils.CompletionVariant
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler

class ReplCompletionTest : TestCase() {
    @Test
    fun testTrivial() {
        checkEvaluateInReplForCompletion(
            simpleScriptCompilationConfiguration,
            sequenceOf(
                Pair(
                    null,
                    """
                        data class AClass(val memx: Int, val memy: String)
                        data class BClass(val memz: String, val mema: AClass)
                        val foobar = 42
                        var foobaz = "string"
                        val v = BClass("KKK", AClass(5, "25"))
                    """.trimIndent()
                ) to ExpectedResult(emptyList()),

                Pair(
                    3,
                    """
                        foo
                    """.trimIndent()
                ) to ExpectedResult(
                    listOf(
                        CompletionVariant(
                            "foobar",
                            "foobar",
                            "Int",
                            "property"
                        ),
                        CompletionVariant(
                            "foobaz",
                            "foobaz",
                            "String",
                            "property"
                        )
                    )
                ),

                Pair(
                    7,
                    """
                        v.mema.
                    """.trimIndent()
                ) to ExpectedResult(
                    listOf(
                        CompletionVariant(
                            "memx",
                            "memx",
                            "Int",
                            "property"
                        ),
                        CompletionVariant(
                            "memy",
                            "memy",
                            "String",
                            "property"
                        )
                    ),
                    ComparisonType.INCLUDES
                ),

                Pair(
                    5,
                    """
                        listO
                    """.trimIndent()
                ) to ExpectedResult(
                    listOf(
                        CompletionVariant(
                            "listOf(",
                            "listOf(T)",
                            "List<T>",
                            "method"
                        ),
                        CompletionVariant(
                            "listOf()",
                            "listOf()",
                            "List<T>",
                            "method"
                        ),
                        CompletionVariant(
                            "listOf(",
                            "listOf(vararg T)",
                            "List<T>",
                            "method"
                        ),
                        CompletionVariant(
                            "listOfNotNull(",
                            "listOfNotNull(T?)",
                            "List<T>",
                            "method"
                        ),
                        CompletionVariant(
                            "listOfNotNull(",
                            "listOfNotNull(vararg T?)",
                            "List<T>",
                            "method"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun testPackagesImport() {
        checkEvaluateInReplForCompletion(
            simpleScriptCompilationConfiguration,
            sequenceOf(
                Pair(
                    17,
                    """
                        import java.lang.
                    """.trimIndent()
                ) to ExpectedResult(
                    listOf(
                        CompletionVariant(
                            "Process",
                            "Process",
                            " (java.lang)",
                            "class"
                        )
                    ), ComparisonType.INCLUDES
                )
            )
        )
    }

    @Test
    fun testExtensionMethods() {
        checkEvaluateInReplForCompletion(
            simpleScriptCompilationConfiguration,
            sequenceOf(
                Pair(
                    null,
                    """
                        class AClass(val c_prop_x: Int) {
                            fun filter(xxx: (AClass).() -> Boolean): AClass {
                                return this
                            }
                        }
                        val AClass.c_prop_y: Int
                            get() = c_prop_x * c_prop_x
                        
                        fun AClass.c_meth_z(v: Int) = v * c_prop_y
                        val df = AClass(10)
                        val c_zzz = "some string"
                    """.trimIndent()
                ) to ExpectedResult(
                    emptyList()
                ),
                Pair(
                    13,
                    """
                        df.filter{ c_ }
                    """.trimIndent()
                ) to ExpectedResult(
                    listOf(
                        CompletionVariant("c_prop_x", "c_prop_x", "Int", "property"),
                        CompletionVariant("c_zzz", "c_zzz", "String", "property"),
                        CompletionVariant("c_prop_y", "c_prop_y", "Int", "property"),
                        CompletionVariant("c_meth_z(", "c_meth_z(Int)", "Int", "method")
                    )
                ),
                Pair(
                    6,
                    """
                        df.fil
                    """.trimIndent()
                ) to ExpectedResult(
                    listOf(
                        CompletionVariant(
                            "filter { ",
                            "filter(Line_1_simplescript.AClass.() -> ...",
                            "Line_1_simplescript.AClass",
                            "method"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun testBacktickedFields() {
        checkEvaluateInReplForCompletion(
            simpleScriptCompilationConfiguration,
            sequenceOf(
                Pair(
                    null,
                    """
                        class AClass(val `c_prop   x y z`: Int)
                        val df = AClass(33)
                    """.trimIndent()
                ) to ExpectedResult(
                    emptyList()
                ),
                Pair(
                    5,
                    """
                        df.c_
                    """.trimIndent()
                ) to ExpectedResult(
                    listOf(
                        CompletionVariant("`c_prop   x y z`", "`c_prop   x y z`", "Int", "property")
                    )
                )
            )
        )
    }

    enum class ComparisonType {
        INCLUDES, EQUALS
    }

    data class ExpectedResult(val completions: List<CompletionVariant>, val compType: ComparisonType = ComparisonType.EQUALS)

    private val currentLineCounter = AtomicInteger()

    private fun nextCodeLine(code: String): ReplCodeLine = ReplCodeLine(currentLineCounter.getAndIncrement(), 0, code)

    private fun evaluateInReplForCompletion(
        compilationConfiguration: ScriptCompilationConfiguration,
        snippets: List<Pair<Int?, String>>
    ): List<ResultWithDiagnostics<List<CompletionVariant>>> {
        val compiler = JvmReplCompiler(compilationConfiguration)
        val stateLock = ReentrantReadWriteLock()
        val state = compiler.createState(stateLock)
        return snippets.map { (cursor, snippetText) ->
            val codeLine = nextCodeLine(snippetText)
            val res = if (cursor == null) {
                emptyList<CompletionVariant>().asSuccess()
            } else {
                compiler.complete(state, codeLine, cursor).filter { it.tail != "keyword" }.asSuccess()
            }

            val codeLineForCompilation = nextCodeLine(snippetText)
            compiler.compile(state, codeLineForCompilation)
            res
        }
    }

    private fun checkEvaluateInReplForCompletion(
        compilationConfiguration: ScriptCompilationConfiguration,
        testData: Sequence<Pair<Pair<Int?, String>, ExpectedResult>>
    ) {
        val (snippets, expected) = testData.unzip()
        val expectedIter = expected.iterator()
        evaluateInReplForCompletion(compilationConfiguration, snippets).forEachIndexed { index, res ->
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

