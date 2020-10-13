package hu.bme.mit.theta.mcm.graph;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.Process;

import java.util.HashSet;
import java.util.Set;

public class GraphOrNodeSet {
    private boolean changed;
    private final boolean isGraph;
    private final Graph graph;
    private final Set<MemoryAccess> nodeSet;
    private VarDecl<?> varDecl;
    private Process process;
    private MemoryAccess memoryAccess;

    private GraphOrNodeSet(Graph graph) {
        isGraph = true;
        this.graph = graph;
        this.nodeSet = null;
        this.changed = true;
    }

    private GraphOrNodeSet(Set<MemoryAccess> nodeSet) {
        isGraph = false;
        this.graph = null;
        this.nodeSet = nodeSet;
        this.changed = true;
    }

    public static  GraphOrNodeSet of(Graph graph) {
        return new GraphOrNodeSet(graph);
    }

    public static  GraphOrNodeSet of(Set<MemoryAccess> nodeSet) {
        return new GraphOrNodeSet(nodeSet);
    }

    public boolean isGraph() {
        return isGraph;
    }

    public Set<MemoryAccess> getNodeSet() {
        return nodeSet;
    }

    public Graph getGraph() {
        return graph;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public GraphOrNodeSet duplicate() {
        GraphOrNodeSet ret;
        if(isGraph) {
            ret = GraphOrNodeSet.of(graph.duplicate());
        } else {
            ret = GraphOrNodeSet.of(new HashSet<>(nodeSet));
        }
        ret.setChanged(isChanged());
        return ret;
    }

    public VarDecl<?> getVarDecl() {
        return varDecl;
    }

    public void setVarDecl(VarDecl<?> varDecl) {
        this.varDecl = varDecl;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public MemoryAccess getMemoryAccess() {
        return memoryAccess;
    }

    public void setMemoryAccess(MemoryAccess memoryAccess) {
        this.memoryAccess = memoryAccess;
    }
}
