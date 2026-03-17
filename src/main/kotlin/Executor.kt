package logiviz

import java.util.Scanner
import java.awt.Desktop
import java.net.URI

fun main() {
    val engine = InferenceEngine()
    val scanner = Scanner(System.`in`)

    println("""
        ===========================================
        LogiViz: Datalog Inference Engine 
        ===========================================
        Commands:
          - fact(args).          : Add a base fact
          - head :- body.       : Add a recursive rule
          - ? query(args).       : Query the engine
          - visualize            : Open cloud-based graph
          - help                 : Show this menu
          - exit                 : Quit program
        ===========================================
    """.trimIndent())

    while (true) {
        print("\nlogiviz > ")
        val input = scanner.nextLine()?.trim() ?: break

        if (input.isEmpty()) continue
        if (input.lowercase() == "exit") break
        if (input.lowercase() == "help") continue

        try {
            when {
                input.lowercase() == "visualize" -> {
                    val url = engine.getCloudGraphUrl()
                    println("Generating visualization...")
                    println("URL: $url")

                    if (Desktop.isDesktopSupported()) {
                        println("Opening in your default browser...")
                        Desktop.getDesktop().browse(URI(url))
                    } else {
                        println("Desktop not supported. Please copy the URL above.")
                    }
                }

                input.startsWith("?") -> {
                    val queryAtom = DatalogParser.parseAtom(input.substring(1))
                    val results = engine.query(queryAtom)

                    if (results.isEmpty()) {
                        println("No matches found.")
                    } else {
                        println("Found ${results.size} matches:")
                        results.forEach { binding ->
                            if (binding.isEmpty()) {
                                println("   -> true")
                            } else {
                                val output = binding.entries.joinToString(", ") { "${it.key} = ${it.value}" }
                                println("   -> $output")
                            }
                        }
                    }
                }

                input.contains(":-") -> {
                    val rule = DatalogParser.parseRule(input)
                    engine.addRule(rule)
                    println("Rule added and knowledge base recomputed.")
                }

                else -> {
                    val atom = DatalogParser.parseAtom(input)
                    engine.addFact(Fact(atom))
                    println("Fact added.")
                }
            }
        } catch (e: IllegalArgumentException) {
            println("Syntax Error: ${e.message}")
        } catch (e: Exception) {
            println("Unexpected Error: ${e.javaClass.simpleName}")
            e.printStackTrace()
        }
    }
}