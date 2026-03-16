package logiviz

class InferenceEngine {
    private val baseFacts = mutableSetOf<Atom>()
    private val rules = mutableListOf<Rule>()
    private var derivedFacts = mutableSetOf<Atom>()

    fun addFact(fact: Fact) {
        baseFacts.add(fact.atom)
        recompute()
    }

    fun addRule(rule: Rule) {
        rules.add(rule)
        recompute()
    }

    private fun recompute() {
        derivedFacts = baseFacts.toMutableSet()
        var changed = true

        while (changed) {
            val sizeBefore = derivedFacts.size

            for (rule in rules) {
                val bindings = solveBody(rule.body, mapOf())
                for (binding in bindings) {
                    val newFact = applyBinding(rule.head, binding)
                    derivedFacts.add(newFact)
                }
            }
            changed = derivedFacts.size > sizeBefore
        }
    }

    private fun solveBody(body: List<Atom>, currentBindings: Map<String, String>): List<Map<String, String>> {
        if (body.isEmpty()) return listOf(currentBindings)

        val first = body.first()
        val rest = body.drop(1)
        val results = mutableListOf<Map<String, String>>()

        for (fact in derivedFacts) {
            val match = unify(first, fact, currentBindings)
            if (match != null) {
                results.addAll(solveBody(rest, currentBindings + match))
            }
        }
        return results
    }

    private fun unify(query: Atom, fact: Atom, existing: Map<String, String>): Map<String, String>? {
        if (query.name != fact.name || query.terms.size != fact.terms.size) return null
        val newBindings = mutableMapOf<String, String>()

        for (i in query.terms.indices) {
            val qTerm = query.terms[i]
            val fTerm = fact.terms[i]

            val boundValue = existing[qTerm] ?: newBindings[qTerm]

            when {
                query.isVariable(qTerm) -> {
                    if (boundValue != null && boundValue != fTerm) return null
                    newBindings[qTerm] = fTerm
                }
                qTerm != fTerm -> return null
            }
        }
        return newBindings
    }

    private fun applyBinding(atom: Atom, bindings: Map<String, String>): Atom {
        val newTerms = atom.terms.map { bindings[it] ?: it }
        return Atom(atom.name, newTerms)
    }

    fun query(q: Atom): List<Map<String, String>> {
        return derivedFacts.filter { it.name == q.name }
            .mapNotNull { unify(q, it, emptyMap()) }
            .distinct()
    }

    fun toMermaid(): String {
        val sb = StringBuilder("graph TD\n")

        // 1. Draw the Logic Flow (Dependencies)
        rules.forEach { rule ->
            rule.body.forEach { bodyAtom ->
                // body atom "flows into" the head atom
                sb.append("  ${bodyAtom.name} --> ${rule.head.name}\n")
            }
        }

        // 2. Styling based on Knowledge Base
        val allPredicates = rules.flatMap { it.body.map { b -> b.name } + it.head.name }.toSet() +
                baseFacts.map { it.name }.toSet()

        allPredicates.distinct().forEach { pred ->
            val isBase = baseFacts.any { it.name == pred } && rules.none { it.head.name == pred }
            val isRecursive = rules.any { r -> r.head.name == pred && r.body.any { b -> b.name == pred } }

            if (isBase) {
                sb.append("  style $pred fill:#d4edda,stroke:#28a745\n") // Green for Facts
            } else if (isRecursive) {
                sb.append("  style $pred fill:#f8d7da,stroke:#dc3545\n") // Red for Recursion
            } else {
                sb.append("  style $pred fill:#e1f5fe,stroke:#01579b\n") // Blue for Rules
            }
        }
        return sb.toString()
    }
}