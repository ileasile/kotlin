// FILE: Descriptor.java
// FULL_JDK

public interface Descriptor

// FILE: ResolvedCall.java

import org.jetbrains.annotations.NotNull;

public interface ResolvedCall<D extends Descriptor> {
    @NotNull
    D getResultingDescriptor();
}

// FILE: test.kt

val Descriptor.name = "123"

interface Call<D : Descriptor> {
    val resultingDescriptor: D
}

fun <D> test(call: Call<D>, resolvedCall: ResolvedCall<D>) {
    call.resultingDescriptor.<!INAPPLICABLE_CANDIDATE!>name<!>
    resolvedCall.resultingDescriptor.<!INAPPLICABLE_CANDIDATE!>name<!>
}

fun otherTest(call: Call<*>, resolvedCall: ResolvedCall<*>) {
    call.resultingDescriptor.name
    resolvedCall.resultingDescriptor.<!INAPPLICABLE_CANDIDATE!>name<!>
}