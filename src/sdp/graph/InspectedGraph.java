/*
 * See the file "LICENSE" for the full license governing this code.
 */
package sdp.graph;

/**
 * Inspect graph-theoretic properties.
 *
 * @author Marco Kuhlmann <marco.kuhlmann@liu.se>
 */
public class InspectedGraph {

    /**
     * The analyzed graph.
     */
    private final Graph graph;
    /**
     * The index of the run during which this node was (first) visited.
     */
    private final int[] run;
    /**
     * The number of runs.
     */
    private int nRuns;
    /**
     * The preorder timestamps of each node.
     */
    private final int[] enter;
    /**
     * The postorder timestamps of each node.
     */
    private final int[] leave;
    /**
     * Flags indicating whether a node is a singleton.
     */
    private final boolean[] isSingleton;
    /**
     * The number of singleton nodes in this graph.
     */
    private final int nSingletons;

    /**
     * Construct a new inspector for the specified graph.
     *
     * @param graph the graph to be analyzed
     */
    public InspectedGraph(Graph graph) {
	this.graph = graph;

	int nNodes = graph.getNNodes();

	this.run = new int[nNodes];

	this.enter = new int[nNodes];
	this.leave = new int[nNodes];
	computeTimestamps();

	this.isSingleton = new boolean[nNodes];
	this.nSingletons = computeSingletons();
    }

    /**
     * Computes the preorder and postorder timestamps for the inspected graph.
     */
    private void computeTimestamps() {
	Timer timer = new Timer();
	for (Node node : graph.getNodes()) {
	    if (enter[node.id] == 0) {
		computeTimestamps(node, timer);
		nRuns++;
	    }
	}
    }

    /**
     * Computes the preorder and postorder timestamps for the subgraph starting
     * at the specified node.
     *
     * @param node the entry point for the subgraph
     * @param timer the global timer
     */
    private void computeTimestamps(Node node, Timer timer) {
	run[node.id] = nRuns;
	enter[node.id] = timer.tick();
	for (Edge outgoingEdge : node.getOutgoingEdges()) {
	    // Only visit nodes that have not been visited before.
	    if (enter[outgoingEdge.target] == 0) {
		computeTimestamps(graph.getNode(outgoingEdge.target), timer);
	    }
	}
	leave[node.id] = timer.tick();
    }

    /**
     * Timer used in depth-first search.
     */
    private static final class Timer {

	/**
	 * The current time.
	 */
	private int time;

	/**
	 * Returns the current time, then increments it.
	 *
	 * @return the current time
	 */
	public int tick() {
	    return time++;
	}
    }

    /**
     * Tests whether the inspected graph contains a cycle.
     *
     * @return {@code true} if and only if the inspected graph contains a cycle
     */
    public boolean isCyclic() {
	for (Edge edge : graph.getEdges()) {
	    // Check whether the current edge is a self-loop or a back edge.
	    if (edge.target == edge.source || enter[edge.target] < enter[edge.source] && leave[edge.source] < leave[edge.target]) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Computes flags indicating whether a node is a singleton.
     */
    private int computeSingletons() {
	int n = 0;
	for (Node node : graph.getNodes()) {
	    if (node.id != 0 && !node.hasIncomingEdges() && !node.hasOutgoingEdges() && !node.isTop) {
		isSingleton[node.id] = true;
		n++;
	    }
	}
	return n;
    }

    /**
     * Tests whether the specified node is a singleton.
     *
     * @param id a node id
     * @return {@code true} if the specified node is a singleton
     */
    public boolean isSingleton(int id) {
	return isSingleton[id];
    }

    /**
     * Returns the number of singleton nodes of this graph. A node is a
     * singleton if it has no neighbors and is not a top node.
     */
    public int getNSingletons() {
	return nSingletons;
    }

    /**
     * Computes the maximal indegree of the non-trivial nodes in the inspected
     * graph.
     *
     * @return the maximal indegree of the non-trivial nodes in the inspected
     * graph
     */
    public int getMaximalIndegree() {
	int max = 0;
	for (Node node : graph.getNodes()) {
	    if (!isSingleton[node.id]) {
		max = Math.max(max, node.getNIncomingEdges());
	    }
	}
	return max;
    }

    /**
     * Computes the maximal outdegree of the non-trivial nodes in the inspected
     * graph.
     *
     * @return the maximal outdegree of the non-trivial nodes in the inspected
     * graph
     */
    public int getMaximalOutdegree() {
	int max = 0;
	for (Node node : graph.getNodes()) {
	    if (!isSingleton[node.id]) {
		max = Math.max(max, node.getNOutgoingEdges());
	    }
	}
	return max;
    }

    /**
     * Returns the number of non-trivial root nodes in the inspected graph. A
     * <em>root node</em> is a node without incoming edges.
     *
     * @return the number of non-trivial root nodes in the inspected graph
     */
    public int getNRootNodes() {
	int nRootNodes = 0;
	for (Node node : graph.getNodes()) {
	    if (node.id != 0 && !isSingleton[node.id]) {
		nRootNodes += node.hasIncomingEdges() ? 0 : 1;
	    }
	}
	return nRootNodes;
    }

    /**
     * Returns the number of non-trivial leaf nodes in the inspected graph. A
     * <em>leaf node</em> is a node without outgoing edges.
     *
     * @return the number of non-trivial leaf nodes in the inspected graph
     */
    public int getNLeafNodes() {
	int nLeafNodes = 0;
	for (Node node : graph.getNodes()) {
	    if (node.id != 0 && !isSingleton[node.id]) {
		nLeafNodes += node.hasOutgoingEdges() ? 0 : 1;
	    }
	}
	return nLeafNodes;
    }

    /**
     * Tests whether the inspected graph is a forest. A forest is an acyclic
     * graph in which every node has at most one incoming edge.
     *
     * @return {@code true} if and only if the inspected graph is a forest
     */
    public boolean isForest() {
	return !isCyclic() && getMaximalIndegree() <= 1;
    }

    /**
     * Tests whether the inspected graph is a tree. A tree is a forest with
     * exactly one root node.
     *
     * @return {@code true} if and only if the inspected graph is a tree
     */
    public boolean isTree() {
	return isForest() && getNRootNodes() == 1;
    }

    /**
     * Tests whether the inspected graph is projective. A graph is projective if
     * there are no overlapping edges, and no edge covers some root node.
     *
     * @return {@code true} if and only if the inspected graph is projective
     */
    public boolean isProjective() {
	int nNodes = graph.getNNodes();
	boolean[] hasIncomingEdge = new boolean[nNodes];
	boolean[] isCovered = new boolean[nNodes];
	for (Edge edge1 : graph.getEdges()) {
	    int min1 = Math.min(edge1.source, edge1.target);
	    int max1 = Math.max(edge1.source, edge1.target);
	    for (Edge edge2 : graph.getEdges()) {
		int min2 = Math.min(edge2.source, edge2.target);
		int max2 = Math.max(edge2.source, edge2.target);
		if (overlap(min1, max1, min2, max2)) {
		    return false;
		}
	    }
	    hasIncomingEdge[edge1.target] = true;
	    for (int i = min1 + 1; i < max1; i++) {
		isCovered[i] = true;
	    }
	}
	for (int node = 0; node < nNodes; node++) {
	    if (!hasIncomingEdge[node] && isCovered[node] && !isSingleton[node]) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Tests whether the specified edges overlap (cross).
     *
     * @param min1 the position of the left node of the first edge
     * @param max1 the position of the right node of the first edge
     * @param min2 the position of the left node of the second edge
     * @param max2 the position of the right node of the second edge
     * @return {@code true} if and only if the specified edges overlap
     */
    private static boolean overlap(int min1, int max1, int min2, int max2) {
	return min1 < min2 && min2 < max1 && max1 < max2 || min2 < min1 && min1 < max2 && max2 < max1;
    }

    /**
     * Returns the number of weakly connected components of the inspected graph.
     *
     * @return The number of weakly connected components of the inspected graph
     */
    public int getNComponents() {
	int[] component = new int[nRuns];
	for (int i = 0; i < nRuns; i++) {
	    component[i] = i;
	}
	int nComponents = nRuns;
	for (Node node : graph.getNodes()) {
	    nComponents -= isSingleton[node.id] ? 1 : 0;
	}
	for (Edge edge : graph.getEdges()) {
	    if (component[run[edge.source]] != component[run[edge.target]]) {
		component[run[edge.source]] = component[run[edge.target]];
		nComponents--;
	    }
	}
	assert nComponents >= 0;
	return nComponents;
    }
}
