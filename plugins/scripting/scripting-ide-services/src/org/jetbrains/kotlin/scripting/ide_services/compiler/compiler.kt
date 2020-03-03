/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.PackageFragmentFromClassLoaderProviderExtension
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.ide_services.util.failure
import org.jetbrains.kotlin.scripting.ide_services.util.getScriptKtFile
import org.jetbrains.kotlin.scripting.ide_services.util.makeCompiledScript
import org.jetbrains.kotlin.scripting.ide_services.util.withMessageCollector
import org.jetbrains.kotlin.scripting.ide_services.util.withMessageCollectorAndDisposable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.api.ReplCompletionResult
import kotlin.script.experimental.jvm.JvmDependencyFromClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript

class KJvmReplCompilerImpl(private val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration) :
    ReplCompiler<KJvmCompiledScript<Any>>,
    ReplCompleter, ReplCodeAnalyzer, ScriptCompiler {

    private val state = JvmCompilerState(this)
    private val history = JvmReplCompilerStageHistory(state)
    private val scriptPriority = AtomicInteger()

    override var lastCompiledSnippet: LinkedPushStack<KJvmCompiledScript<Any>>? = null
        private set

    fun createReplCompilationState(scriptCompilationConfiguration: ScriptCompilationConfiguration): JvmCompilerState.Compilation {
        val context = withMessageCollectorAndDisposable(disposeOnSuccess = false) { messageCollector, disposable ->
            createIsolatedCompilationContext(
                scriptCompilationConfiguration,
                hostConfiguration,
                messageCollector,
                disposable
            ).asSuccess()
        }.valueOr { throw IllegalStateException("Unable to initialize repl compiler:\n  ${it.reports.joinToString("\n  ")}") }
        return ReplCompilationState(context)
    }

    override fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ILinkedPushStack<KJvmCompiledScript<Any>>> =
        snippets.map { snippet ->
            withMessageCollector(snippet) { messageCollector ->
                val compilationState = state.getCompilationState(configuration)

                val (context, errorHolder, snippetKtFile) = prepareForAnalyze(
                    snippet,
                    messageCollector,
                    compilationState,
                    checkSyntaxErrors = true
                ).valueOr { return@withMessageCollector it }

                val (sourceFiles, sourceDependencies) = collectRefinedSourcesAndUpdateEnvironment(
                    context,
                    snippetKtFile,
                    messageCollector
                )

                val firstFailure = sourceDependencies.firstOrNull { it.sourceDependencies is ResultWithDiagnostics.Failure }
                    ?.let { it.sourceDependencies as ResultWithDiagnostics.Failure }

                if (firstFailure != null)
                    return firstFailure

                if (history.isEmpty()) {
                    val updatedConfiguration = ScriptDependenciesProvider.getInstance(context.environment.project)
                        ?.getScriptConfiguration(snippetKtFile)?.configuration
                        ?: context.baseScriptCompilationConfiguration
                    registerPackageFragmentProvidersIfNeeded(
                        updatedConfiguration,
                        context.environment
                    )
                }

                val no = scriptPriority.getAndIncrement()

                val analysisResult =
                    compilationState.analyzerEngine.analyzeReplLineWithImportedScripts(snippetKtFile, sourceFiles.drop(1), snippet, no)
                AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

                val scriptDescriptor = when (analysisResult) {
                    is CodeAnalyzer.ReplLineAnalysisResult.WithErrors -> return failure(
                        messageCollector
                    )
                    is CodeAnalyzer.ReplLineAnalysisResult.Successful -> {
                        (analysisResult.scriptDescriptor as? ScriptDescriptor)
                            ?: return failure(
                                snippet,
                                messageCollector,
                                "Unexpected script descriptor type ${analysisResult.scriptDescriptor::class}"
                            )
                    }
                    else -> return failure(
                        snippet,
                        messageCollector,
                        "Unexpected result ${analysisResult::class.java}"
                    )
                }

                val generationState = GenerationState.Builder(
                    snippetKtFile.project,
                    ClassBuilderFactories.BINARIES,
                    compilationState.analyzerEngine.module,
                    compilationState.analyzerEngine.trace.bindingContext,
                    sourceFiles,
                    compilationState.environment.configuration
                ).build().apply {
                    scriptSpecific.earlierScriptsForReplInterpreter = history.map { it.item }
                    beforeCompile()
                }
                KotlinCodegenFacade.generatePackage(generationState, snippetKtFile.script!!.containingKtFile.packageFqName, sourceFiles)

                history.push(LineId(no, snippet), scriptDescriptor)

                val dependenciesProvider = ScriptDependenciesProvider.getInstance(context.environment.project)
                val compiledScript =
                    makeCompiledScript(
                        generationState,
                        snippet,
                        sourceFiles.first(),
                        sourceDependencies
                    ) { ktFile ->
                        dependenciesProvider?.getScriptConfiguration(ktFile)?.configuration
                            ?: context.baseScriptCompilationConfiguration
                    }

                lastCompiledSnippet = lastCompiledSnippet.add(compiledScript)

                lastCompiledSnippet?.asSuccess(messageCollector.diagnostics)
                    ?: failure(
                        snippet,
                        messageCollector,
                        "last compiled snippet should not be null"
                    )
            }
        }.last()

    override suspend fun invoke(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        return when (val res = compile(script, scriptCompilationConfiguration)) {
            is ResultWithDiagnostics.Success -> res.value().asSuccess(res.reports)
            is ResultWithDiagnostics.Failure -> res
        }
    }

    override fun complete(
        snippet: SourceCode,
        cursor: Int,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ReplCompletionResult> =
        withMessageCollector(snippet) { messageCollector ->

            val newText =
                KJvmReplCompleter.prepareCode(snippet.text, cursor)
            val newSnippet = object : SourceCode {
                override val text: String
                    get() = newText
                override val name: String?
                    get() = snippet.name
                override val locationId: String?
                    get() = snippet.locationId

            }

            val compilationState = state.getCompilationState(configuration)

            val (_, errorHolder, snippetKtFile) = prepareForAnalyze(
                newSnippet,
                messageCollector,
                compilationState,
                checkSyntaxErrors = false
            ).valueOr { return@withMessageCollector it }

            val analysisResult =
                compilationState.analyzerEngine.statelessAnalyzeWithImportedScripts(snippetKtFile, emptyList(), scriptPriority.get() + 1)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

            val (_, bindingContext, resolutionFacade, moduleDescriptor) = when (analysisResult) {
                is CodeAnalyzer.ReplLineAnalysisResult.Stateless -> {
                    analysisResult
                }
                else -> return failure(
                    newSnippet,
                    messageCollector,
                    "Unexpected result ${analysisResult::class.java}"
                )
            }

            val completer = KJvmReplCompleter(
                snippetKtFile,
                bindingContext,
                resolutionFacade,
                moduleDescriptor,
                cursor
            )

            return completer.getCompletion().asSuccess(messageCollector.diagnostics)
        }

    private fun List<ScriptDiagnostic>.toAnalyzeResult() = this.mapNotNull { diag ->
        ReplDiagnosticMessage(
            diag.location?.let { loc ->
                ReplDiagnosticMessage.Location(
                    loc.start.let { ReplDiagnosticMessage.Pos(it.line, it.col) },
                    loc.end?.let { ReplDiagnosticMessage.Pos(it.line, it.col) }
                )
            },
            diag.message,
            diag.severity.let {
                when (it) {
                    ScriptDiagnostic.Severity.FATAL -> ReplDiagnosticMessage.Severity.FATAL
                    ScriptDiagnostic.Severity.ERROR -> ReplDiagnosticMessage.Severity.ERROR
                    ScriptDiagnostic.Severity.WARNING -> ReplDiagnosticMessage.Severity.WARNING
                    else -> return@mapNotNull null
                }
            })
    }

    override fun analyze(
        snippet: SourceCode,
        cursor: Int,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ReplAnalyzerResult> {
        return withMessageCollector(snippet) { messageCollector ->
            val compilationState = state.getCompilationState(configuration)

            val (_, errorHolder, snippetKtFile) = prepareForAnalyze(
                snippet,
                messageCollector,
                compilationState,
                checkSyntaxErrors = true
            ).valueOr { return@withMessageCollector messageCollector.diagnostics.toAnalyzeResult().asSuccess() }

            val analysisResult =
                compilationState.analyzerEngine.statelessAnalyzeWithImportedScripts(snippetKtFile, emptyList(), scriptPriority.get() + 1)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

            messageCollector.diagnostics.toAnalyzeResult().asSuccess()
        }
    }

    private data class AnalyzePreparationResult(
        val context: SharedScriptCompilationContext,
        val errorHolder: MessageCollectorBasedReporter,
        val snippetKtFile: KtFile
    )

    private fun prepareForAnalyze(
        snippet: SourceCode,
        parentMessageCollector: MessageCollector,
        compilationState: JvmCompilerState.Compilation,
        checkSyntaxErrors: Boolean
    ): ResultWithDiagnostics<AnalyzePreparationResult> =
        withMessageCollector(
            snippet,
            parentMessageCollector
        ) { messageCollector ->
            val context =
                (compilationState as? ReplCompilationState)?.context
                    ?: return failure(
                        snippet, messageCollector, "Internal error: unknown parameter passed as compilationState: $compilationState"
                    )

            setIdeaIoUseFallback()

            val errorHolder = object : MessageCollectorBasedReporter {
                override val messageCollector = messageCollector
            }

            val snippetKtFile =
                getScriptKtFile(
                    snippet,
                    context.baseScriptCompilationConfiguration,
                    context.environment.project,
                    messageCollector
                )
                    .valueOr { return it }

            if (checkSyntaxErrors) {
                val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(snippetKtFile, errorHolder)
                if (syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof) return failure(
                    messageCollector, ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code")
                )
                if (syntaxErrorReport.isHasErrors) return failure(
                    messageCollector
                )
            }

            return AnalyzePreparationResult(
                context,
                errorHolder,
                snippetKtFile
            ).asSuccess()
        }
}

internal class ReplCompilationState(val context: SharedScriptCompilationContext) : JvmCompilerState.Compilation {
    override val disposable: Disposable? get() = context.disposable
    override val baseScriptCompilationConfiguration: ScriptCompilationConfiguration get() = context.baseScriptCompilationConfiguration
    override val environment: KotlinCoreEnvironment get() = context.environment
    override val analyzerEngine: CodeAnalyzer by lazy {
        CodeAnalyzer(context.environment)
    }
}

internal fun registerPackageFragmentProvidersIfNeeded(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    environment: KotlinCoreEnvironment
) {
    scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies]?.forEach { dependency ->
        if (dependency is JvmDependencyFromClassLoader) {
            // TODO: consider implementing deduplication
            PackageFragmentProviderExtension.registerExtension(
                environment.project,
                PackageFragmentFromClassLoaderProviderExtension(
                    dependency.classLoaderGetter, scriptCompilationConfiguration, environment.configuration
                )
            )
        }
    }
}