/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerState
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.KJvmReplCompilerProxy
import org.jetbrains.kotlin.utils.CompletionVariant
import org.jetbrains.kotlin.utils.KotlinReplError
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

/**
 * REPL Compilation wrapper for "legacy" REPL APIs defined in the org.jetbrains.kotlin.cli.common.repl package
 */
class JvmReplCompiler(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    val replCompilerProxy: KJvmReplCompilerProxy = KJvmReplCompilerImpl(
        hostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration)
    )
) : IDELikeReplCompiler {

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = JvmReplCompilerState(replCompilerProxy, lock)

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult = state.lock.write {
        val replCompilerState = state.asState(JvmReplCompilerState::class.java)
        val compilation = replCompilerState.getCompilationState(scriptCompilationConfiguration)
        val res =
            replCompilerProxy.checkSyntax(
                codeLine.toSourceCode(scriptCompilationConfiguration),
                compilation.baseScriptCompilationConfiguration,
                compilation.environment.project
            )
        when {
            // TODO: implement diagnostics rendering
            res is ResultWithDiagnostics.Success && res.value -> ReplCheckResult.Ok()
            res is ResultWithDiagnostics.Success && !res.value -> ReplCheckResult.Incomplete()
            else -> ReplCheckResult.Error(res.reports.joinToString("\n") { it.message })
        }
    }

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult = state.lock.write {
        val replCompilerState = state.asState(JvmReplCompilerState::class.java)
        val compilation = replCompilerState.getCompilationState(scriptCompilationConfiguration)
        val snippet = codeLine.toSourceCode(scriptCompilationConfiguration)
        val snippetId = ReplSnippetIdImpl(codeLine.no, codeLine.generation, snippet)
        when (val res = replCompilerProxy.compileReplSnippet(compilation, snippet, snippetId, replCompilerState.history)) {
            is ResultWithDiagnostics.Success ->
                ReplCompileResult.CompiledClasses(
                    LineId(codeLine),
                    replCompilerState.history.map { it.id },
                    snippet.name!!,
                    emptyList(),
                    res.value.resultField != null,
                    emptyList(),
                    res.value.resultField?.second?.typeName,
                    res.value
                )
            else -> ReplCompileResult.Error(
                res.reports.joinToString("\n") { report ->
                    report.location?.let { loc ->
                        CompilerMessageLocation.create(
                            report.sourcePath,
                            loc.start.line,
                            loc.start.col,
                            loc.end?.line,
                            loc.end?.col,
                            null
                        )?.toString()?.let {
                            "$it "
                        }
                    }.orEmpty() + report.message
                },
                res.reports.firstOrNull {
                    when (it.severity) {
                        ScriptDiagnostic.Severity.ERROR -> true
                        ScriptDiagnostic.Severity.FATAL -> true
                        else -> false
                    }
                }?.let {
                    val loc = it.location ?: return@let null
                    CompilerMessageLocation.create(it.sourcePath, loc.start.line, loc.start.col, loc.end?.line, loc.end?.col, null)
                })
        }
    }

    override fun complete(state: IReplStageState<*>, codeLine: ReplCodeLine, cursor: Int): List<CompletionVariant> = state.lock.write {
        val replCompilerState = state.asState(JvmReplCompilerState::class.java)
        val compilation = replCompilerState.getCompilationState(scriptCompilationConfiguration)
        val snippet = codeLine.toSourceCode(scriptCompilationConfiguration)
        val snippetId = ReplSnippetIdImpl(codeLine.no, codeLine.generation, snippet)
        return getCompletion(compilation, snippet, snippetId, cursor)
    }

    override fun listErrors(state: IReplStageState<*>, codeLine: ReplCodeLine): List<KotlinReplError> {
        val replCompilerState = state.asState(JvmReplCompilerState::class.java)
        val compilation = replCompilerState.getCompilationState(scriptCompilationConfiguration)
        val snippet = codeLine.toSourceCode(scriptCompilationConfiguration)
        val snippetId = ReplSnippetIdImpl(codeLine.no, codeLine.generation, snippet)
        return getErrorsList(compilation, snippet, snippetId)
    }

    private fun getCompletion(
        compilation: JvmReplCompilerState.Compilation,
        snippet: SourceCode,
        snippetId: ReplSnippetId,
        cursor: Int
    ): List<CompletionVariant> {
        return when (val res = replCompilerProxy.getCompletion(compilation, snippet, snippetId, cursor)) {
            is ResultWithDiagnostics.Success -> res.value
            else -> throw Exception(res.reports.joinToString("\n") { it.message })
        }
    }

    private fun getErrorsList(
        compilation: JvmReplCompilerState.Compilation,
        snippet: SourceCode,
        snippetId: ReplSnippetId
    ): List<KotlinReplError> {
        return replCompilerProxy.getErrors(compilation, snippet, snippetId)
            .mapNotNull { diag ->
                KotlinReplError(
                    diag.location?.let { loc ->
                        KotlinReplError.Location(
                            loc.start.let { KotlinReplError.Pos(it.line, it.col) },
                            loc.end?.let { KotlinReplError.Pos(it.line, it.col) }
                        )
                    },
                    diag.message,
                    diag.severity.let {
                        when (it) {
                            ScriptDiagnostic.Severity.FATAL -> KotlinReplError.Severity.FATAL
                            ScriptDiagnostic.Severity.ERROR -> KotlinReplError.Severity.ERROR
                            ScriptDiagnostic.Severity.WARNING -> KotlinReplError.Severity.WARNING
                            else -> return@mapNotNull null
                        }
                    }
                )
            }
    }
}


internal class SourceCodeFromReplCodeLine(
    val codeLine: ReplCodeLine,
    compilationConfiguration: ScriptCompilationConfiguration
) : SourceCode {
    override val text: String get() = codeLine.code
    override val name: String =
        "${compilationConfiguration[ScriptCompilationConfiguration.repl.makeSnippetIdentifier]!!(
            compilationConfiguration, ReplSnippetIdImpl(codeLine.no, codeLine.generation, 0)
        )}.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}"
    override val locationId: String? = null
}

internal fun ReplCodeLine.toSourceCode(compilationConfiguration: ScriptCompilationConfiguration): SourceCode =
    SourceCodeFromReplCodeLine(this, compilationConfiguration)
