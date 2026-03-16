package logiviz

import java.util.Scanner

fun main() {
    val engine = InferenceEngine()
    val scanner = Scanner(System.`in`)

    println("""
        Rules:  head(X,Y) :- body(X,Y).
        Facts:  father(bob, alice).
        Query:  ? grandfather(bob, X).
        Tools:  'visualize' (Cloud API) | 'exit'
    """.trimIndent())

    while (true) {
        print("\nlogiviz > ")
        val input = scanner.nextLine().trim()

        when {
            input.lowercase() == "exit" -> break
            input.lowercase() == "visualize" -> {
                println("Sending graph to Kroki API...")
                openGraphInBrowser(engine.toMermaid())
            }
            input.startsWith("?") -> {
                val queryAtom = DatalogParser.parseAtom(input.substring(1))
                val results = engine.query(queryAtom)
                if (results.isEmpty()) println("No matches found.")
                else results.forEach { println("Match: $it") }
            }
            input.contains(":-") -> {
                val rule = DatalogParser.parseRule(input)
                engine.addRule(rule)
                println("Rule Added.")
            }
            input.isNotBlank() -> {
                val fact = Fact(DatalogParser.parseAtom(input.removeSuffix(".")))
                engine.addFact(fact)
                println("Fact Added.")
            }
        }
    }
}