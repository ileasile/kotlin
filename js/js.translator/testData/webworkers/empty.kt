// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS_IR

import kotlin.js.worker.*
import kotlin.js.Promise

fun box(): Promise<String> {
    worker<Unit> {
        // Just a smoke test: no exception should be thrown
    }
    return Promise<String> { resolve, _ -> resolve("OK") }
}