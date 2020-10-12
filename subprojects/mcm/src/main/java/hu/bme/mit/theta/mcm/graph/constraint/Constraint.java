package hu.bme.mit.theta.mcm.graph.constraint;

import hu.bme.mit.theta.mcm.Result;
import hu.bme.mit.theta.mcm.graph.filter.Filter;
import hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess;

public abstract class Constraint {
    protected final Filter filter;
    protected final boolean not;
    protected final String name;

    public Constraint(Filter filter, boolean not, String name) {
        this.filter = filter;
        this.not = not;
        this.name = name;
    }

    public abstract Result checkMk(MemoryAccess source, MemoryAccess target, String label, boolean isFinal);

    public abstract Result checkRm(MemoryAccess source, MemoryAccess target, String label);

    public abstract Constraint duplicate();

    public String getName() {
        return name;
    }
}
