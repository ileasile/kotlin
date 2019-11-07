abstract class Base {
    class BaseNested
}

class Derived : Base() {
    class DerivedNested

    companion object {
        val b: BaseNested = <!UNRESOLVED_REFERENCE!>BaseNested<!>()

        val d: DerivedNested = DerivedNested()

        fun foo() {
            val bb: BaseNested = <!UNRESOLVED_REFERENCE!>BaseNested<!>()
            val dd: DerivedNested = DerivedNested()
        }
    }
}