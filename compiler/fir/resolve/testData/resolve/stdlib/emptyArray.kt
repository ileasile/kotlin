val x: Array<String> = <!AMBIGUITY!>emptyArray<!>()

val y: Array<String>
    get() = <!AMBIGUITY!>emptyArray<!>()

interface My

val z: Array<out My>
    get() = <!AMBIGUITY!>emptyArray<!>()