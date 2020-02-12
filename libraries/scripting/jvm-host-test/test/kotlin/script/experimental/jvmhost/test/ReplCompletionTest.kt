/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.push
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
    fun testTrivial() = test {
        run {
            code = """
                data class AClass(val memx: Int, val memy: String)
                data class BClass(val memz: String, val mema: AClass)
                val foobar = 42
                var foobaz = "string"
                val v = BClass("KKK", AClass(5, "25"))
            """.trimIndent()
        }

        run {
            code = "foo"
            cursor = 3
            expect {
                add("foobar", "foobar", "Int", "property")
                add("foobaz", "foobaz", "String", "property")
            }
        }

        run {
            code = "v.mema."
            cursor = 7
            expect {
                mode(ComparisonType.INCLUDES)
                add("memx", "memx", "Int", "property")
                add("memy", "memy", "String", "property")
            }
        }

        run {
            code = "listO"
            cursor = 5
            expect {
                add("listOf(", "listOf(T)", "List<T>", "method")
                add("listOf()", "listOf()", "List<T>", "method")
                add("listOf(", "listOf(vararg T)", "List<T>", "method")
                add("listOfNotNull(", "listOfNotNull(T?)", "List<T>", "method")
                add("listOfNotNull(", "listOfNotNull(vararg T?)", "List<T>", "method")
            }
        }
    }


    @Test
    fun testPackagesImport() = test {
        run {
            cursor = 17
            code = "import java.lang."
            expect {
                mode(ComparisonType.INCLUDES)
                add("Process", "Process", " (java.lang)", "class")
            }
        }
    }

    @Test
    fun testExtensionMethods() = test {
        run {
            code = """
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
        }

        run {
            code = "df.filter{ c_ }"
            cursor = 13
            expect {
                add("c_prop_x", "c_prop_x", "Int", "property")
                add("c_zzz", "c_zzz", "String", "property")
                add("c_prop_y", "c_prop_y", "Int", "property")
                add("c_meth_z(", "c_meth_z(Int)", "Int", "method")
            }
        }

        run {
            code = "df.fil"
            cursor = 6
            expect {
                add("filter { ", "filter(Line_1_simplescript.AClass.() -> ...", "Line_1_simplescript.AClass", "method")
            }
        }
    }

    @Test
    fun testBacktickedFields() = test {
        run {
            code = """
                class AClass(val `c_prop   x y z`: Int)
                val df = AClass(33)
            """.trimIndent()
        }

        run {
            code = "df.c_"
            cursor = 5
            expect {
                add("`c_prop   x y z`", "`c_prop   x y z`", "Int", "property")
            }
        }
    }

    private class TestConf {
        private val runs = mutableListOf<Run>()

        fun run(setup: (Run).() -> Unit) {
            val r = Run()
            r.setup()
            runs.push(r)
        }

        fun collect() = runs.map { it.collect() }

        class Run {
            var cursor: Int? = null
            var code: String = ""
            private var _expected: Expected = Expected()

            fun expect(setup: (Expected).() -> Unit) {
                _expected = Expected()
                _expected.setup()
            }

            fun collect(): Pair<Pair<Int?, String>, ExpectedResult> {
                return Pair(cursor, code) to _expected.collect()
            }

            class Expected {
                private val variants = mutableListOf<CompletionVariant>()
                private var _mode: ComparisonType = ComparisonType.EQUALS

                fun collect(): ExpectedResult {
                    return ExpectedResult(variants, _mode)
                }

                fun add(text: String, displayText: String, tail: String, icon: String) {
                    variants.push(CompletionVariant(text, displayText, tail, icon))
                }

                fun mode(mode: ComparisonType) {
                    _mode = mode
                }
            }

        }
    }

    private fun test(setup: (TestConf).() -> Unit) {
        val test = TestConf()
        test.setup()
        checkEvaluateInRepl(simpleScriptCompilationConfiguration, test.collect())
    }

    enum class ComparisonType {
        INCLUDES, EQUALS
    }

    data class ExpectedResult(val completions: List<CompletionVariant>, val compType: ComparisonType = ComparisonType.EQUALS)

    private val currentLineCounter = AtomicInteger()

    private fun nextCodeLine(code: String): ReplCodeLine = ReplCodeLine(currentLineCounter.getAndIncrement(), 0, code)

    private fun evaluateInRepl(
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

    private fun checkEvaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        testData: List<Pair<Pair<Int?, String>, ExpectedResult>>
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

