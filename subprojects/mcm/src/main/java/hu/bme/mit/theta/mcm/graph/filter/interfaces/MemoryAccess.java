package hu.bme.mit.theta.mcm.graph.filter.interfaces;

import hu.bme.mit.theta.core.decl.VarDecl;

public interface MemoryAccess {
    Process getProcess();
    VarDecl<?> getGlobalVariable();
}
