package hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.nodes;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutablePartitionedValuation;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.analysis.stateless.executor.StackFrame;

import java.util.List;
import java.util.Map;

public abstract class MemoryAccess implements hu.bme.mit.theta.mcm.graph.filter.interfaces.MemoryAccess {
    protected final VarDecl<?> globalVar;
    private final XCFA.Process parentProcess;

    MemoryAccess(VarDecl<?> globalVar, XCFA.Process parentProcess) {
        this.globalVar = globalVar;
        this.parentProcess = parentProcess;
    }

    public VarDecl<?> getGlobalVariable() {
        return globalVar;
    }

    public XCFA.Process getProcess() {
        return parentProcess;
    }

    public boolean revert(Map<XCFA.Process, List<StackFrame>> stackFrames, MutablePartitionedValuation mutablePartitionedValuation, int partitionId) {
        return false;
    }
}
