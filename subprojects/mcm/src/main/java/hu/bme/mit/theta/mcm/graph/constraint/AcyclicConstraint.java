package hu.bme.mit.theta.mcm.graph.constraint;

import hu.bme.mit.theta.mcm.Result;
import hu.bme.mit.theta.mcm.graph.Graph;
import hu.bme.mit.theta.mcm.graph.GraphOrNodeSet;
import hu.bme.mit.theta.mcm.graph.filter.Filter;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.Process;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class AcyclicConstraint extends Constraint {
    private Graph last;
    public AcyclicConstraint(Filter filter, boolean not, String name) {
        super(filter, not, name);
    }

    public AcyclicConstraint(Filter duplicate, boolean not, String name, Graph last) {
        super(duplicate, not, name);
        this.last = last;
    }

    public Result checkMk(MemoryAccess source, MemoryAccess target, String label, boolean isFinal) {
        Set<GraphOrNodeSet> resultSet = filter.filterMk(source, target, label, isFinal);
        Collection<Process> violators = new HashSet<>();
        for (GraphOrNodeSet result : resultSet) {
            if(result.isGraph()) {
                last = result.getGraph();
                if(not && result.getGraph().isAcyclic()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
                else if(!not && !result.getGraph().isAcyclic()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
            }
            else {
                throw new UnsupportedOperationException("Cannot check for acyclicity in a set!");
            }
        }
        return violators.isEmpty() ? Result.ok() : Result.violation(violators);
    }
    public Result checkRm(MemoryAccess source, MemoryAccess target, String label) {
        Set<GraphOrNodeSet> resultSet = filter.filterRm(source, target, label);
        Collection<Process> violators = new HashSet<>();
        for (GraphOrNodeSet result : resultSet) {
            if(result.isGraph()) {
                last = result.getGraph();
                if(not && result.getGraph().isAcyclic()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
                else if(!not && !result.getGraph().isAcyclic()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
            }
            else {
                throw new UnsupportedOperationException("Cannot check for acyclicity in a set!");
            }
        }
        return violators.isEmpty() ? Result.ok() : Result.violation(violators);
    }

    @Override
    public Constraint duplicate() {
        return new AcyclicConstraint(filter.duplicate(new Stack<>(), new Stack<>(), new Stack<>()), not, name, last);
    }
}
