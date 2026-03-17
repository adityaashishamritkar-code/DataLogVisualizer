package logiviz

sealed class Term {
    data class Variable(val name: String) : Term()
    data class Constant(val value: String) : Term()
    override fun toString(): String = when(this) {
        is Variable -> name
        is Constant -> value
    }
}

data class Atom(val name: String, val terms: List<Term>, val isNegated: Boolean = false)

data class Rule(val head: Atom, val body: List<Atom>)

data class Fact(val atom: Atom)