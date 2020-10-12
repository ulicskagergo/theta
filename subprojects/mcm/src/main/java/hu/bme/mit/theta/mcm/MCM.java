package hu.bme.mit.theta.mcm;

import hu.bme.mit.theta.mcm.graph.constraint.Constraint;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;

import java.util.HashMap;
import java.util.Map;

public class MCM {
    private final Map<Constraint, Result> constraints;

    public MCM() {
        constraints = new HashMap<>();
    }

    public MCM(Map<Constraint, Result> constraints) {
        this.constraints = new HashMap<>();
        for (Map.Entry<Constraint, Result> constraint : constraints.entrySet()) {
            Constraint duplicated = constraint.getKey().duplicate();
            this.constraints.put(duplicated, constraint.getValue());
        }
    }

    public void addConstraint(Constraint g) {
        constraints.put(g, Result.ok());
    }

    public void checkMk(MemoryAccess source, MemoryAccess target, String label, boolean isFinal) {
        constraints.replaceAll((c, v) -> c.checkMk(source, target, label, isFinal));
    }

    public void checkRm(MemoryAccess source, MemoryAccess target, String label) {
        constraints.replaceAll((c, v) -> c.checkRm(source, target, label));
    }

    public MCM duplicate() {
        return new MCM(constraints);
    }

    public Map<Constraint, Result> getConstraints() {
        return constraints;
    }
}
