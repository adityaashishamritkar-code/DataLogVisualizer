package logiviz

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater

class InferenceEngine {
    private val baseFacts = mutableSetOf<Atom>()
    private val rules = mutableListOf<Rule>()
    private var derivedFacts = mutableSetOf<Atom>()

    private val MAX_RECURSION_DEPTH = 50

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
            val newDiscoveries = mutableSetOf<Atom>()

            for (rule in rules) {
                solveBody(rule.body, emptyMap()).forEach { bindings ->
                    val newFact = applyBinding(rule.head, bindings)
                    newDiscoveries.add(newFact)
                }
            }

            derivedFacts.addAll(newDiscoveries)
            changed = derivedFacts.size > sizeBefore
        }
    }

    private fun solveBody(
        body: List<Atom>,
        bindings: Map<String, String>,
        depth: Int = 0
    ): Sequence<Map<String, String>> {
        if (depth > MAX_RECURSION_DEPTH) return emptySequence()
        if (body.isEmpty()) return sequenceOf(bindings)

        val first = body.first()
        val rest = body.drop(1)

        return if (first.isNegated) {
            val groundCheck = applyBinding(first.copy(isNegated = false), bindings)
            val hasMatch = query(groundCheck).any()
            if (!hasMatch) solveBody(rest, bindings, depth + 1) else emptySequence()
        } else {
            derivedFacts.asSequence()
                .filter { it.name == first.name }
                .mapNotNull { fact -> unify(first, fact, bindings) }
                .flatMap { newMatch ->
                    solveBody(rest, bindings + newMatch, depth + 1)
                }
        }
    }

    private fun unify(query: Atom, fact: Atom, existing: Map<String, String>): Map<String, String>? {
        if (query.name != fact.name || query.terms.size != fact.terms.size) return null
        val newBindings = mutableMapOf<String, String>()

        for (i in query.terms.indices) {
            val qTerm = query.terms[i]
            val fTerm = fact.terms[i]

            val factVal = when(fTerm) {
                is Term.Constant -> fTerm.value.trim()
                is Term.Variable -> fTerm.name.trim()
            }

            when (qTerm) {
                is Term.Variable -> {
                    val boundValue = existing[qTerm.name] ?: newBindings[qTerm.name]
                    if (boundValue != null) {
                        if (boundValue != factVal) return null
                    } else {
                        newBindings[qTerm.name] = factVal
                    }
                }
                is Term.Constant -> {
                    if (qTerm.value.trim() != factVal) return null
                }
            }
        }
        return newBindings
    }

    private fun applyBinding(atom: Atom, bindings: Map<String, String>): Atom {
        val newTerms = atom.terms.map { term ->
            if (term is Term.Variable) {
                val value = bindings[term.name]
                if (value != null) Term.Constant(value) else term
            } else term
        }
        return atom.copy(terms = newTerms)
    }

    fun query(q: Atom): List<Map<String, String>> {
        return derivedFacts
            .filter { it.name == q.name }
            .mapNotNull { fact -> unify(q, fact, emptyMap()) }
            .distinct()
    }

    fun getCloudGraphUrl(): String {
        val mermaidCode = toMermaid()
        return try {
            val input = mermaidCode.toByteArray(Charsets.UTF_8)
            val output = ByteArrayOutputStream()
            val deflater = Deflater(Deflater.BEST_COMPRESSION)
            deflater.setInput(input)
            deflater.finish()
            val buffer = ByteArray(1024)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                output.write(buffer, 0, count)
            }
            deflater.end()
            val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray())
            "https://kroki.io/mermaid/svg/$encoded"
        } catch (e: Exception) {
            "Error"
        }
    }

    fun toMermaid(): String {
        val sb = StringBuilder("graph LR\n")

        rules.forEach { rule ->
            rule.body.forEach { bodyAtom ->
                sb.append("  ${bodyAtom.name} --> ${rule.head.name}\n")
            }
        }

        val allPredicates = (rules.map { it.head.name } + baseFacts.map { it.name }).distinct()

        allPredicates.forEach { pred ->
            val isDerived = rules.any { it.head.name == pred }
            val isBaseOnly = baseFacts.any { it.name == pred } && !isDerived

            if (isBaseOnly) {
                sb.append("  $pred([$pred])\n")
                sb.append("  style $pred fill:#f9f,stroke:#333,stroke-width:2px\n")
            } else {
                sb.append("  $pred{{$pred}}\n")
                sb.append("  style $pred fill:#bbf,stroke:#333,stroke-width:2px\n")
            }
        }

        return sb.toString()
    }
}