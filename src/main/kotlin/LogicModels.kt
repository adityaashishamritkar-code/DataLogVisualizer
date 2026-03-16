package logiviz

data class Atom(val name: String, val terms: List<String>) {
    fun isConstant(term: String) = term[0].isLowerCase()
    fun isVariable(term: String) = term[0].isUpperCase()
}

data class Rule(val head: Atom, val body: List<Atom>)

data class Fact(val atom: Atom)