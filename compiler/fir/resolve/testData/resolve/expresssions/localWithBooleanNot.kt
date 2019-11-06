// FILE: Node.java

public interface Node<R> {
    R result();
}

// FILE: foo.kt

fun foo(): Boolean {
    object : Node<Boolean> {
        private var result = false

        fun bar(): Boolean {
            return <!UNRESOLVED_REFERENCE!>!<!><!INAPPLICABLE_CANDIDATE!>result<!>
        }

        // Cannot see private member of local class / anonymous object
        override fun result() = <!INAPPLICABLE_CANDIDATE!>result<!>
    }

    val some = true
    return !some
}