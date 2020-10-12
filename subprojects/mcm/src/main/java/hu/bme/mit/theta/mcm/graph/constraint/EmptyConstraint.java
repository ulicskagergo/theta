package hu.bme.mit.theta.mcm.graph.constraint;

import hu.bme.mit.theta.mcm.Result;
import hu.bme.mit.theta.mcm.graph.GraphOrNodeSet;
import hu.bme.mit.theta.mcm.graph.filter.Filter;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.Process;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class EmptyConstraint extends Constraint {
    private GraphOrNodeSet last;
    private final Collection<MemoryAccess> finalNodes;

    public EmptyConstraint(Filter filter, boolean not, String name) {
        super(filter, not, name);
        finalNodes = new HashSet<>();
    }

    private EmptyConstraint(Filter duplicate, boolean not, String name, GraphOrNodeSet last, Collection<MemoryAccess> finalNodes) {
        super(duplicate, not, name);
        this.last = last;
        this.finalNodes = new HashSet<>(finalNodes);
    }

    public Result checkMk(MemoryAccess source, MemoryAccess target, String label, boolean isFinal) {
        if(isFinal) {
            finalNodes.add(source);
            finalNodes.add(target);
        }
        Set<GraphOrNodeSet> resultSet = filter.filterMk(source, target, label, isFinal);
        Collection<Process> violators = new HashSet<>();
        for (GraphOrNodeSet result : resultSet) {
            last = result;
            if(result.isGraph()) {
                if(not && result.getGraph().isEmpty()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
                else if(!not && !result.getGraph().isEmpty()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
            }
            else {
                if (not && result.getNodeSet().isEmpty()) {
                    if (finalNodes.containsAll(result.getNodeSet())) return Result.finalViolation();
                    else result.getNodeSet().forEach(memoryAccess -> violators.add(memoryAccess.getProcess()));
                } else if (!not && !result.getNodeSet().isEmpty()) {
                    if (finalNodes.containsAll(result.getNodeSet())) return Result.finalViolation();
                    else result.getNodeSet().forEach(memoryAccess -> violators.add(memoryAccess.getProcess()));
                }
            }
        }
        return violators.isEmpty() ? Result.ok() : Result.violation(violators);
    }

    public Result checkRm(MemoryAccess source, MemoryAccess target, String label) {
        Set<GraphOrNodeSet> resultSet = filter.filterRm(source, target, label);
        Collection<Process> violators = new HashSet<>();
        for (GraphOrNodeSet result : resultSet) {
            last = result;
            if(result.isGraph()) {
                if(not && result.getGraph().isEmpty()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
                else if(!not && !result.getGraph().isEmpty()) {
                    if(result.getGraph().isFinal()) return Result.finalViolation();
                    else violators.addAll(result.getGraph().getProcesses());
                }
            }
            else {
                if (not && result.getNodeSet().isEmpty()) {
                    if (finalNodes.containsAll(result.getNodeSet())) return Result.finalViolation();
                    else result.getNodeSet().forEach(memoryAccess -> violators.add(memoryAccess.getProcess()));
                } else if (!not && !result.getNodeSet().isEmpty()) {
                    if (finalNodes.containsAll(result.getNodeSet())) return Result.finalViolation();
                    else result.getNodeSet().forEach(memoryAccess -> violators.add(memoryAccess.getProcess()));
                }
            }
        }
        return violators.isEmpty() ? Result.ok() : Result.violation(violators);
    }

    @Override
    public Constraint duplicate() {
        return new EmptyConstraint(filter.duplicate(new Stack<>(), new Stack<>(), new Stack<>()), not, name, last, finalNodes);
    }
}
