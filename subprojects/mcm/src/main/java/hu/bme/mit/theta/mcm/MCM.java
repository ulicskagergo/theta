package hu.bme.mit.theta.mcm;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.mcm.graph.Graph;
import hu.bme.mit.theta.mcm.graph.GraphOrNodeSet;
import hu.bme.mit.theta.mcm.graph.constraint.Constraint;
import hu.bme.mit.theta.mcm.graph.filter.Filter;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;

import java.util.*;

public class MCM {
    private final Map<Constraint, Result> constraints;
    private final Map<String, Tuple2<Filter, Set<GraphOrNodeSet>>> filters;
    private final Graph graph;

    public MCM() {
        constraints = new HashMap<>();
        filters = new HashMap<>();
        graph = Graph.empty();
    }

    private MCM(MCM toCopy) {
        this.graph = toCopy.graph.duplicate();
        this.constraints = new HashMap<>();
        this.filters = new HashMap<>();
        for (Map.Entry<Constraint, Result> constraint : toCopy.constraints.entrySet()) {
            Constraint duplicated = constraint.getKey().duplicate();
            this.constraints.put(duplicated, constraint.getValue());
        }
        for (Map.Entry<String, Tuple2<Filter, Set<GraphOrNodeSet>>> filter : toCopy.filters.entrySet()) {
            Filter duplicated = filter.getValue().get1().duplicate(new Stack<>(), new Stack<>(), new Stack<>());
            this.filters.put(filter.getKey(), Tuple2.of(duplicated, filter.getValue().get2()));
        }
    }


    public void mkEdge(MemoryAccess source, MemoryAccess target, String label, boolean isFinal) {
        constraints.replaceAll((c, v) -> c.checkMk(source, target, label, isFinal));
        filters.replaceAll((c, v) -> Tuple2.of(v.get1(), v.get1().filterMk(source, target, label, isFinal)));
        graph.addEdge(source, target, isFinal);
    }

    public void rmEdge(MemoryAccess source, MemoryAccess target, String label) {
        constraints.replaceAll((c, v) -> c.checkRm(source, target, label));
        filters.replaceAll((c, v) -> Tuple2.of(v.get1(), v.get1().filterRm(source, target, label)));
        graph.removeEdge(source, target);
    }

    public MCM duplicate() {
        return new MCM(this);
    }


    public void addConstraint(Constraint g) {
        constraints.put(g, Result.ok());
    }

    public Map<Constraint, Result> getConstraints() {
        return constraints;
    }

    public Set<GraphOrNodeSet> getFilterResult(String key) {
        return filters.get(key).get2();
    }

    public void addFilter(String key, Filter value) {
        filters.put(key, Tuple2.of(value, new HashSet<>()));
    }

    public Graph getGraph() {
        return graph;
    }
}
