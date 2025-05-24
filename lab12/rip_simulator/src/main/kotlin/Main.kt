package rip_simulator

import java.io.File
import kotlin.concurrent.thread
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.random.Random

const val MAX_HOPS = 15

@Serializable
data class NetworkConfig(val routers: List<String>, val edges: List<List<String>>)

class NetworkNode(val ip: String, network: Network) {
    private val edges = mutableListOf<Edge>()
    private var table = RoutingTable(this, network)
    private var simulationCycles = 0

    fun addEdge(edge: Edge) = edges.add(edge)
    fun getEdges(): List<Edge> = edges
    fun setSimulationCycles(cycles: Int) {
        simulationCycles = cycles
    }

    fun simulate() {
        for (cycle in 0 until simulationCycles) {
            broadcast()
            Thread.sleep(200)
        }
    }

    fun printTable() = table.print()

    private fun broadcast() {
        for (edge in edges) {
            val neighbor = if (edge.left == this) edge.right else edge.left
            neighbor.receive(this, table)
        }
    }

    private fun receive(from: NetworkNode, receivedTable: RoutingTable) {
        synchronized(this) {
            val statesCopy = ArrayList(receivedTable.states)

            for (incoming in statesCopy) {
                if (incoming.nextHop.ip == "0.0.0.0") continue
                if (incoming.destination.ip == this.ip) continue

                val current = table.states.find { it.destination.ip == incoming.destination.ip }
                val newMetric = (incoming.metric + 1).coerceAtMost(MAX_HOPS + 1)

                if (current != null && (newMetric < current.metric || from.ip == current.nextHop.ip)) {
                    current.metric = newMetric
                    current.nextHop = from

                    if (newMetric > MAX_HOPS) {
                        current.metric = MAX_HOPS + 1
                    }
                } else if (current == null && newMetric <= MAX_HOPS) {
                    table.states.add(
                        RoutingState(
                            destination = incoming.destination,
                            nextHop = from,
                            metric = newMetric
                        )
                    )
                }
            }
        }
    }
}

class RoutingTable(private val owner: NetworkNode, private val network: Network) {
    val states = mutableListOf<RoutingState>()

    init {
        initializeStates()
    }

    private fun initializeStates() {
        states.clear()
        states.add(RoutingState(destination = owner, nextHop = owner, metric = 0))

        for (node in network.nodes) {
            if (node == owner) continue

            val directEdge = owner.getEdges().firstOrNull { it.left == node || it.right == node }

            if (directEdge != null) {
                val nextHop = if (directEdge.left == owner) directEdge.right else directEdge.left
                states.add(RoutingState(destination = node, nextHop = nextHop, metric = 1))
            } else {
                val dummyNode = NetworkNode("0.0.0.0", network)
                states.add(RoutingState(destination = node, nextHop = dummyNode, metric = MAX_HOPS + 1))
            }
        }
    }

    fun print() {
        var maxIpLength = 15
        synchronized(states) {
            for (s in states) {
                maxIpLength = maxOf(maxIpLength, owner.ip.length, s.destination.ip.length, s.nextHop.ip.length)
            }
        }

        maxIpLength += 2

        val sourceFormat = "%-${maxIpLength}s"
        val destFormat = "%-${maxIpLength}s"
        val nextHopFormat = "%-${maxIpLength}s"
        val metricFormat = "%-7s"

        val separator =
            "+${"-".repeat(maxIpLength + 2)}+${"-".repeat(maxIpLength + 2)}+${"-".repeat(maxIpLength + 2)}+${
                "-".repeat(9)
            }+"

        println("\n┌${"─".repeat(separator.length - 2)}┐")
        println("│ Final state of router ${owner.ip} routing table ${" ".repeat(separator.length - 43 - owner.ip.length)}   │")
        println(separator)
        println(
            "│ ${"Source IP".padEnd(maxIpLength)} │ ${"Destination IP".padEnd(maxIpLength)} │ ${
                "Next Hop".padEnd(
                    maxIpLength
                )
            } │ Metric  │"
        )
        println(separator)

        synchronized(states) {
            for (s in states) {
                val metricStr = if (s.metric > MAX_HOPS) "inf" else s.metric.toString()
                println(
                    "│ ${String.format(sourceFormat, owner.ip)} │ ${
                        String.format(
                            destFormat,
                            s.destination.ip
                        )
                    }│ ${String.format(nextHopFormat, s.nextHop.ip)}  │ ${String.format(metricFormat, metricStr)} │"
                )
            }
        }

        println("└${"─".repeat(separator.length - 2)}┘\n")
    }
}

data class RoutingState(
    val destination: NetworkNode,
    var nextHop: NetworkNode,
    var metric: Int
)

data class Edge(val left: NetworkNode, val right: NetworkNode)

class Network {
    val nodes = mutableListOf<NetworkNode>()
    private val rand = Random.Default

    fun generate(n: Int) {
        nodes.clear()
        val ips = mutableSetOf<String>()
        while (ips.size < n) {
            val ip = "${rand.nextInt(1, 254)}.${rand.nextInt(0, 254)}.${rand.nextInt(0, 254)}.${rand.nextInt(0, 254)}"
            ips.add(ip)
        }
        val nodeList = ips.map { NetworkNode(it, this) }
        nodes.addAll(nodeList)

        for (i in 1 until nodeList.size) {
            val a = nodeList[i]
            val b = nodeList[rand.nextInt(0, i)]
            val edge = Edge(a, b)
            a.addEdge(edge)
            b.addEdge(edge)
        }

        repeat(n) {
            val a = nodeList.random()
            val b = nodeList.random()
            if (a != b && !a.getEdges().any { it.left == b || it.right == b }) {
                val edge = Edge(a, b)
                a.addEdge(edge)
                b.addEdge(edge)
            }
        }
    }

    fun loadFromJson(path: String) {
        nodes.clear()
        val config = Json.decodeFromString<NetworkConfig>(File(path).readText())
        val nodeMap = config.routers.associateWith { NetworkNode(it, this) }
        nodes.addAll(nodeMap.values)

        for ((a, b) in config.edges) {
            val nodeA = nodeMap[a] ?: continue
            val nodeB = nodeMap[b] ?: continue
            val edge = Edge(nodeA, nodeB)
            nodeA.addEdge(edge)
            nodeB.addEdge(edge)
        }
    }


    fun simulate(cycles: Int) {
        for (node in nodes) {
            node.setSimulationCycles(cycles)
        }
        val threads = nodes.map { node ->
            thread { node.simulate() }
        }
        for (thread in threads) {
            thread.join()
        }
        for (node in nodes) {
            node.printTable()
        }
    }
}

fun printNetwork(network: Network) {
    println("\nList of routers:")
    for ((index, node) in network.nodes.withIndex()) {
        val connections = node.getEdges().joinToString(", ") {
            if (it.left == node) it.right.ip else it.left.ip
        }
        println("${index + 1}. ${node.ip} connected to: $connections")
    }
}

fun main(args: Array<String>) {
    val network = Network()

    if (args.isEmpty()) {
        println("Usage: ")
        println("  generate <num_of_routers> <num_of_simulation_cycles>")
        println("  from_file <config_json_path> <num_of_simulation_cycles>")
        return
    }

    when (args[0]) {
        "generate" -> {
            if (args.size < 3) {
                println("ERROR: invalid number of arguments")
                println("Usage: generate <number_of_routers> <simulation_cycles>")
                return
            }

            val n = args[1].toIntOrNull() ?: 5
            network.generate(n)
            println("\n NETWORK GENERATED:")
            printNetwork(network)

            val cycles = args[2].toIntOrNull() ?: 5
            println("\nRIP SIMULATION STARTED:")
            network.simulate(cycles)
        }

        "from_file" -> {
            if (args.size < 3) {
                println("ERROR: invalid number of arguments")
                println("Usage: from_file <config_json_path> <num_of_simulation_cycles>")
                return
            }

            val path = args[1]
            try {
                network.loadFromJson(path)
                println("\nNETWORK LOADED FROM FILE")
                printNetwork(network)

                val cycles = args[2].toIntOrNull() ?: 5
                println("\nRIP SIMULATION STARTED:")
                network.simulate(cycles)
            } catch (e: Exception) {
                println("ERROR: ${e.message}")
            }
        }

        else -> {
            println("ERROR: invalid command '${args[0]}'")
            println("Usage: ")
            println("  generate <number_of_routers> <simulation_cycles>")
            println("  from_file <config_file_path> <simulation_cycles>")
        }
    }
}
