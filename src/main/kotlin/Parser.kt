package logiviz

object DatalogParser {
    fun parseRule(input: String): Rule {
        val parts = input.split(":-")
        val head = parseAtom(parts[0])
        val body = parts[1].split(Regex("(?<=\\)),")).map { parseAtom(it) }
        return Rule(head, body)
    }

    fun parseAtom(input: String): Atom {
        val name = input.substringBefore("(").trim()
        val terms = input.substringAfter("(").substringBefore(")")
            .split(",").map { it.trim() }
        return Atom(name, terms)
    }
}