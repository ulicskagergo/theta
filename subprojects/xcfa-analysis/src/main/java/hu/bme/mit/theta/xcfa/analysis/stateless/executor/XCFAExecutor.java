package hu.bme.mit.theta.xcfa.analysis.stateless.executor;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutablePartitionedValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.ExecutionGraph;

import java.util.*;

public class XCFAExecutor {
    private final Map<XCFA.Process, List<StackFrame>> stackFrames;            //deep
    private final MutablePartitionedValuation mutablePartitionedValuation;    //deep
    private XCFA.Process currentlyAtomic;                                     //deep
    private final Map<XCFA.Process, Integer> partitions;                      //shallow


    public XCFAExecutor(XCFA xcfa) {
        this.stackFrames = new HashMap<>();
        this.mutablePartitionedValuation = new MutablePartitionedValuation();
        this.partitions = new HashMap<>();

        xcfa.getProcesses().forEach(process -> {
            stackFrames.put(process, new ArrayList<>());
            partitions.put(process, mutablePartitionedValuation.createPartition());
        });
    }

    private XCFAExecutor(XCFAExecutor xcfaExecutor) {
        this.stackFrames = new HashMap<>();
        xcfaExecutor.stackFrames.forEach((process, stackFrames1) -> this.stackFrames.put(process, new ArrayList<>(stackFrames1)));
        this.stackFrames.forEach((process, stackFrameList) -> {
            int lastId = stackFrameList.size() - 1;
            if(lastId != -1) {
                StackFrame stackFrame;
                if (!(stackFrame = stackFrameList.get(lastId)).isLastStmt()) {
                    stackFrameList.remove(lastId);
                    stackFrameList.add(stackFrame.duplicate());
                }
            }
        });
        this.partitions = xcfaExecutor.partitions;
        this.mutablePartitionedValuation = MutablePartitionedValuation.copyOf(xcfaExecutor.mutablePartitionedValuation);
        this.currentlyAtomic = xcfaExecutor.currentlyAtomic;
    }

    public XCFAExecutor duplicate() {
        return new XCFAExecutor(this);
    }

    public void setCurrentlyAtomic(XCFA.Process currentlyAtomic) {
        this.currentlyAtomic = currentlyAtomic;
    }

    public int getPartitionId(XCFA.Process process) {
        return partitions.get(process);
    }

    public boolean executeNextStmt(Collection<XCFA.Process> processes, ExecutionGraph executionGraph) {
        for(XCFA.Process process : processes) {
            StackFrame stackFrame;
            if(stackFrames.get(process).size() == 0) {
                if (executionGraph.handleNewEdge(process, process.getMainProcedure().getInitLoc(), stackFrames)) {
                    return true;
                }
            }
            else if((stackFrame = stackFrames.get(process).get(stackFrames.get(process).size()-1)).isLastStmt()) {
                if (executionGraph.handleNewEdge(process, stackFrame.getEdge().getTarget(), stackFrames)) {
                    return true;
                }
            }
            else {
                if (executionGraph.handleCurrentEdge(process, stackFrame, stackFrames)) {
                    return true;
                }
            }
        }
        return false;
    }


    public XCFA.Process getCurrentlyAtomic() {
        return currentlyAtomic;
    }

    public Valuation getValuation(XCFA.Process proc) {
        return mutablePartitionedValuation.getValuation(getPartitionId(proc));
    }

    public List<StackFrame> getStackFrame(XCFA.Process proc) {
        return stackFrames.get(proc);
    }

    public void putValuation(XCFA.Process proc, VarDecl<?> global, LitExpr<?> value) {
        mutablePartitionedValuation.put(getPartitionId(proc), global, value);
    }

    public <T extends Type> Optional<LitExpr<T>> eval(VarDecl<T> local) {
        return mutablePartitionedValuation.eval(local);
    }

    public MutablePartitionedValuation getMutablePartitionedValuation() {
        return mutablePartitionedValuation;
    }

    public Map<XCFA.Process, List<StackFrame>> getStackFrames() {
        return stackFrames;
    }
}
