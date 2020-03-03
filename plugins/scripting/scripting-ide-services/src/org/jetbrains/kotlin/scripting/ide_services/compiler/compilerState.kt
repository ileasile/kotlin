/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import java.io.Serializable
import java.util.ArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode


class JvmReplCompilerStageHistory(state: JvmCompilerState) :
    BasicCompilerHistory<ScriptDescriptor>(state.lock)

class JvmCompilerState(
    private val compiler: KJvmReplCompilerImpl,
    override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : IReplStageState<ScriptDescriptor> {

    override val history = JvmReplCompilerStageHistory(this)

    override fun dispose() {
        lock.write {
            _compilation?.disposable?.let {
                Disposer.dispose(it)
            }
            _compilation = null
            super.dispose()
        }
    }

    fun getCompilationState(scriptCompilationConfiguration: ScriptCompilationConfiguration): Compilation = lock.write {
        if (_compilation == null) {
            initializeCompilation(scriptCompilationConfiguration)
        }
        _compilation!!
    }

    private var _compilation: Compilation? = null

    private fun initializeCompilation(scriptCompilationConfiguration: ScriptCompilationConfiguration) {
        if (_compilation != null) throw IllegalStateException("Compilation state is already initialized")
        _compilation = compiler.createReplCompilationState(scriptCompilationConfiguration)
    }

    interface Compilation {
        val disposable: Disposable?
        val baseScriptCompilationConfiguration: ScriptCompilationConfiguration
        val environment: KotlinCoreEnvironment
        val analyzerEngine: CodeAnalyzer
    }
}

data class LineId(override val no: Int, private val codeHash: Int) : ILineId, Serializable {

    constructor(no: Int, codeLine: SourceCode) : this(no, codeLine.hashCode())

    override fun compareTo(other: ILineId): Int = (other as? LineId)?.let { lineId ->
        no.compareTo(lineId.no).takeIf { no -> no != 0 }
            ?: codeHash.compareTo(lineId.codeHash)
    } ?: -1 // TODO: check if it doesn't break something

    companion object {
        private const val serialVersionUID: Long = 8328354000L
    }
}

open class BasicCompilerHistory<T>(override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : IReplStageHistory<T>,
    ArrayList<ReplHistoryRecord<T>>() {

    override fun push(id: ILineId, item: T) {
        lock.write {
            add(ReplHistoryRecord(id, item))
        }
    }
}


interface ILineId : Comparable<ILineId> {
    val no: Int
}

data class ReplHistoryRecord<out T>(val id: ILineId, val item: T)

interface IReplStageHistory<T> : List<ReplHistoryRecord<T>> {
    val lock: ReentrantReadWriteLock
    fun peek(): ReplHistoryRecord<T>? = lock.read { lastOrNull() }
    fun push(id: ILineId, item: T)
}

interface IReplStageState<T> {
    val history: IReplStageHistory<T>

    val lock: ReentrantReadWriteLock

    fun getNextLineNo(): Int =
        history.peek()?.id?.no?.let { it + 1 } ?: REPL_CODE_LINE_FIRST_NO // TODO: it should be more robust downstream (e.g. use atomic)

    @Suppress("UNCHECKED_CAST")
    fun <StateT : IReplStageState<*>> asState(target: Class<out StateT>): StateT =
        if (target.isAssignableFrom(this::class.java)) this as StateT
        else throw IllegalArgumentException("$this is not an expected instance of IReplStageState")

    fun dispose() {
    }
}
