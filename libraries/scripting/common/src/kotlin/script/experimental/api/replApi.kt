/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

typealias CompiledSnippet = CompiledScript<Any>

data class ReplCompletionVariant(
    val text: String,
    val displayText: String,
    val tail: String,
    val icon: String
)

typealias ReplCompletionResult = Sequence<ReplCompletionVariant>

data class ReplDiagnosticMessage(val loc: Location?, val message: String = "", val severity: Severity) {

    constructor(startLine: Int, startCol: Int, endLine: Int, endCol: Int, message: String, severity: String) : this(
        Location(Pos(startLine, startCol), Pos(endLine, endCol)), message, Severity.valueOf(severity)
    )

    data class Pos(val line: Int, val col: Int)
    data class Location(val start: Pos, val end: Pos?)
    enum class Severity {
        ERROR, FATAL, WARNING
    }
}

typealias ReplAnalyzerResult = Iterable<ReplDiagnosticMessage>

interface ReplCompiler<CompiledSnippetT : CompiledSnippet> {
    val lastCompiledSnippet: ILinkedPushStack<CompiledSnippetT>?

    fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration
    )
            : ResultWithDiagnostics<ILinkedPushStack<CompiledSnippetT>>

    fun compile(
        snippet: SourceCode,
        configuration: ScriptCompilationConfiguration
    )
            : ResultWithDiagnostics<ILinkedPushStack<CompiledSnippetT>> = compile(listOf(snippet), configuration)
}

interface ReplCompleter {

    fun complete(
        snippet: SourceCode,
        cursor: Int,
        configuration: ScriptCompilationConfiguration
    )
            : ResultWithDiagnostics<ReplCompletionResult>
}

interface ReplCodeAnalyzer {

    fun analyze(
        snippet: SourceCode,
        cursor: Int,
        configuration: ScriptCompilationConfiguration
    )
            : ResultWithDiagnostics<ReplAnalyzerResult>
}

interface EvaluatedSnippet {
    val compiledSnippet: CompiledSnippet

    val configuration: ScriptEvaluationConfiguration

    val error: Throwable?

    val result: Any?

    val hasResult: Boolean

    val isValueResult: Boolean
        get() = hasResult && error == null

    val isUnitResult: Boolean
        get() = !hasResult && error == null

    val isErrorResult: Boolean
        get() = error != null

}

interface ReplEvaluator<CompiledSnippetT : CompiledSnippet, EvaluatedSnippetT : EvaluatedSnippet> {

    val lastEvaluatedSnippet: ILinkedPushStack<EvaluatedSnippetT>?

    // asserts that snippet sequence is valid
    fun eval(snippet: ILinkedPushStack<out CompiledSnippetT>, configuration: ScriptEvaluationConfiguration)
            : ResultWithDiagnostics<ILinkedPushStack<EvaluatedSnippetT>>
}
