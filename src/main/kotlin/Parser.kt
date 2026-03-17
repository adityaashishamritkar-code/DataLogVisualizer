package logiviz

object DatalogParser {

    /**
     * Splits "ancestor(X, Y) :- parent(X, Z), ancestor(Z, Y)"
     * into a Rule(head, List<Atom>)
     */
    fun parseRule(input: String): Rule {
        val clean = input.trim().removeSuffix(".")
        if (!clean.contains(":-")) throw IllegalArgumentException("Missing ':-'")

        val parts = clean.split(":-")
        if (parts.size < 2 || parts[1].isBlank()) {
            throw IllegalArgumentException("Rule body cannot be empty")
        }

        val head = parseAtom(parts[0].trim())

        // Safety Split: Uses a lookahead to avoid splitting inside parent(X, Y)
        val bodyParts = parts[1].trim().split(Regex(",(?![^\\(]*\\))"))
        val body = bodyParts.filter { it.isNotBlank() }.map { parseAtom(it.trim()) }

        return Rule(head, body)
    }

    fun parseAtom(input: String): Atom {
        val trimmed = input.trim()
        val isNegated = trimmed.startsWith("!")
        val cleanAtom = if (isNegated) trimmed.substring(1) else trimmed

        val name = cleanAtom.substringBefore("(").trim()
        val termsRaw = cleanAtom.substringAfter("(").substringBefore(")")

        val terms = if (termsRaw.isBlank()) {
            emptyList()
        } else {
            termsRaw.split(",").map {
                val t = it.trim()
                if (t[0].isUpperCase()) Term.Variable(t) else Term.Constant(t)
            }
        }
        return Atom(name, terms, isNegated)
    }
}