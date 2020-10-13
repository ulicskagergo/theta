package hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.nodes;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutablePartitionedValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.analysis.stateless.executor.StackFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Read extends MemoryAccess implements hu.bme.mit.theta.mcm.graph.filter.interfaces.Read {
    private static int cnt;

    static {
        cnt = 0;
    }

    private final int id;
    private final VarDecl<?> localVar;

    private final Valuation savedState;
    private final List<StackFrame> savedStack;
    private final boolean savedAtomicity;

    public Read(VarDecl<?> globalVar, VarDecl<?> localVar, Valuation savedState, List<StackFrame> savedStack, XCFA.Process parentProcess, boolean atomic) {
        super(globalVar, parentProcess);
        this.localVar = localVar;
        this.savedStack = new ArrayList<>();
        savedStack.forEach(stackFrame -> this.savedStack.add(stackFrame.duplicate()));
        this.savedState = savedState;
        this.savedAtomicity = atomic;
        id = cnt++;
    }

    VarDecl<?> getLocalVar() {
        return localVar;
    }

    @Override
    public boolean revert(Map<XCFA.Process, List<StackFrame>> stackFrames, MutablePartitionedValuation mutablePartitionedValuation, int partitionId) {
        super.revert(stackFrames, mutablePartitionedValuation, partitionId);
        ArrayList<StackFrame> stackCopy = new ArrayList<>();
        savedStack.forEach(stackFrame -> stackCopy.add(stackFrame.duplicate()));
        stackFrames.put(getProcess(), stackCopy);
        mutablePartitionedValuation.clear(partitionId);
        mutablePartitionedValuation.putAll(partitionId, savedState);
        return savedAtomicity;
    }

    @Override
    public String toString() {
        return "\"R(" + getGlobalVariable().getName() + ")_" + id + "\"";
    }
}
