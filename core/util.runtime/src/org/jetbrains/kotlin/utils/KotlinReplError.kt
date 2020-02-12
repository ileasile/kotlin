/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils;

data class KotlinReplError(val loc: Location?, val message: String = "", val severity: Severity) {

    constructor(startLine: Int, startCol: Int, endLine: Int, endCol: Int, message: String, severity: String) : this(
        Location(Pos(startLine, startCol), Pos(endLine, endCol)), message, Severity.valueOf(severity)
    )

    data class Pos(val line: Int, val col: Int)
    data class Location(val start: Pos, val end: Pos?)
    enum class Severity {
        ERROR, FATAL, WARNING
    }
}
