package hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutablePartitionedValuation;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.booltype.BoolLitExpr;
import hu.bme.mit.theta.mcm.MCM;
import hu.bme.mit.theta.mcm.Result;
import hu.bme.mit.theta.mcm.graph.constraint.Constraint;
import hu.bme.mit.theta.mcm.graph.filter.ForEachNode;
import hu.bme.mit.theta.mcm.graph.filter.ForEachVar;
import hu.bme.mit.theta.mcm.graph.filter.NamedEdge;
import hu.bme.mit.theta.mcm.graph.filter.NamedNode;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.nodes.Fence;
import hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.nodes.MemoryAccess;
import hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.nodes.Read;
import hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.nodes.Write;
import hu.bme.mit.theta.xcfa.analysis.stateless.executor.StackFrame;
import hu.bme.mit.theta.xcfa.analysis.stateless.executor.XCFAExecutor;
import hu.bme.mit.theta.xcfa.analysis.stateless.executor.XcfaStmtExecutionVisitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class ExecutionGraph implements Runnable{
    private static final XcfaStmtExecutionVisitor xcfaStmtExecutionVisitor;
    private ThreadPoolExecutor threadPool;

    private final MCM mcm;                                                    //deep
    private final XCFA xcfa;                                                  //shallow
    private final XCFAExecutor xcfaExecutor;                                  //deep
    private final Set<Write> initialWrites;                                   //shallow
    private final Map<VarDecl<?>, List<Read>> revisitableReads;               //deep

    private final List<Integer> path;                                         //deep
    private int step;                                                         //shallow


    //CONSTRUCTORS

    private ExecutionGraph(XCFA xcfa, MCM mcm)
    {
        this.mcm = mcm;
        initFilters();
        initialWrites = new HashSet<>();
        revisitableReads = new HashMap<>();
        this.xcfa = xcfa;
        this.path = new ArrayList<>();
        this.xcfaExecutor = new XCFAExecutor(xcfa);

        xcfa.getGlobalVars().forEach(varDecl -> {
            revisitableReads.put(varDecl, new ArrayList<>());
            LitExpr<?> litExpr;
            if((litExpr = xcfa.getInitValue(varDecl)) != null) {
                addInititalWrite(varDecl, litExpr);
            }
        });
        this.step = 0;
    }

    private void initFilters() {
        ForEachVar forEachVar = new ForEachVar(xcfa.getGlobalVars());
        this.mcm.addFilter("writes", new NamedNode(Write.class));
        this.mcm.addFilter("mo", new NamedEdge("mo"));
    }

    private ExecutionGraph(ExecutionGraph executionGraph, int no) {
        this.mcm = executionGraph.mcm.duplicate();
        this.initialWrites = executionGraph.initialWrites;
        this.revisitableReads = new HashMap<>();
        executionGraph.revisitableReads.forEach((varDecl, reads) -> this.revisitableReads.put(varDecl, new ArrayList<>(reads)));
        this.xcfa = executionGraph.xcfa;
        this.path = new ArrayList<>(executionGraph.path);
        this.path.add(executionGraph.step);
        this.path.add(no);
        this.xcfaExecutor = executionGraph.xcfaExecutor.duplicate();
        this.step = 0;
    }

    // STATIC METHODS

    static {
        xcfaStmtExecutionVisitor = new XcfaStmtExecutionVisitor();
    }

    /*
     * Create a new ExecutionGraph and return it
     */
    public static ExecutionGraph create(XCFA xcfa, MCM mcm) {
        return new ExecutionGraph(xcfa, mcm);
    }





    // PUBLIC METHODS
    public void execute(int threads) {
        threadPool = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        threadPool.execute(this);
        try {
            if(!threadPool.awaitTermination(600, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     * Run the algorithm on the current ExecutionGraph
     */
    @Override
    public void run() {
        if(threadPool.getCompletedTaskCount() % 1000 == 0) {
            System.out.println("Active: " + threadPool.getActiveCount() + ", Queue: " + threadPool.getQueue().size() + ", Finished: "+ threadPool.getCompletedTaskCount());
        }
        while(xcfaExecutor.executeNextStmt(getProcesses(), this)) {
            step++;
        }
        try {
            printGraph(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        testQueue();
    }

    public XCFAExecutor getXcfaExecutor() {
        return xcfaExecutor;
    }

    // PACKAGE-PRIVATE METHODS

    /*
     * Add a read node
     */
    public void addRead(XCFA.Process proc, VarDecl<?> local, VarDecl<?> global) {
        Read read = new Read(
                global,
                local,
                xcfaExecutor.getValuation(proc),
                xcfaExecutor.getStackFrame(proc),
                proc,
                xcfaExecutor.getCurrentlyAtomic() == proc);
        addNode(read);

        Map<VarDecl<?>, List<Write>> writes = new HashMap<>(); // TODO


        int size = writes.get(global).size();
        for(int i = 0; i < size; ++i) {
            Write write = writes.get(global).get(i);
            Tuple2<MemoryAccess, String> edge = Tuple2.of(read, "rf");
            if(i < size - 1) {
                ExecutionGraph executionGraph = duplicate(i);
                executionGraph.mcm.mkEdge(write, read, "rf", true);
                executionGraph.xcfaExecutor.putValuation(proc,global,write.getValue());
                threadPool.execute(executionGraph);
            }
            else {
                mcm.mkEdge(write, read, "rf", false);
                xcfaExecutor.putValuation(proc,global,write.getValue());
                revisitableReads.get(global).add(read);
            }
        }

    }

    /*
     * Add a fence node
     */
    public void addFence(XCFA.Process proc, String type) {
        Fence fence = new Fence(null, proc, type);
        addNode(fence);
    }

    /*
     * Add a write node
     */
    public void addWrite(XCFA.Process proc, VarDecl<?> local, VarDecl<?> global) {
        @SuppressWarnings("OptionalGetWithoutIsPresent") Write write = new Write(global, xcfaExecutor.eval(local).get(), proc);
        addNode(write);
        List<List<Read>> revisitSets = getRevisitSets(global);
        int childCnt = 0;

        Map<VarDecl<?>, List<Write>> mo = new HashMap<>(); // TODO

        int size = mo.get(global).size();
        for(int j = -1; j < size; ++j) {
            for(int i = 0; i < revisitSets.size(); ++i) {
                List<Read> reads = revisitSets.get(i);
                ExecutionGraph executionGraph;
                if(i < revisitSets.size() - 1 || j < size - 1) {
                    executionGraph = this.duplicate(childCnt++);
                }
                else {
                    executionGraph = this;
                }

                if(j == -1) {
                    if(size > 0) {
                        executionGraph.mcm.mkEdge(write, mo.get(global).get(j+1), "mo", false);
                    }
                }
                else if(j < size-1) {
                    executionGraph.mcm.rmEdge(mo.get(global).get(j), mo.get(global).get(j+1), "mo");
                    executionGraph.mcm.mkEdge(write, mo.get(global).get(j+1), "mo", false);
                    executionGraph.mcm.mkEdge(mo.get(global).get(j), write, "mo", false);
                }
                else {
                    executionGraph.mcm.mkEdge(mo.get(global).get(j), write, "mo", false);
                }

                for(Read read : reads) {
                    executionGraph.revisitRead(read);
                    executionGraph.mcm.mkEdge(write, read, "rf", true);
                    executionGraph.xcfaExecutor.putValuation(proc,global,write.getValue());
                }

                if(i < revisitSets.size() - 1 || j < size - 1) {
                    threadPool.execute(executionGraph);
                }
            }

        }

    }

    /*
     * Add an initial write node
     */
    void addInititalWrite(VarDecl<?> global, LitExpr<?> value) {
        Write write = new Write(global, value, null);
        initialWrites.add(write);
    }



    //PRIVATE METHODS

    public void invalidateFuture(Read read) {
        Map<XCFA.Process, Boolean> atomic = new HashMap<>();
        invalidateFuture(read, atomic, true);

        boolean foundOne = false;
        for (Map.Entry<XCFA.Process, Boolean> entry : atomic.entrySet()) {
            XCFA.Process process = entry.getKey();
            Boolean atomicity = entry.getValue();
            if (atomicity) {
                checkState(!foundOne, "Multiple processes cannot be concurrently atomic!");
                foundOne = true;
                xcfaExecutor.setCurrentlyAtomic(process);
            }
        }
    }

    private void invalidateFuture(MemoryAccess memoryAccess, Map<XCFA.Process, Boolean> atomic, boolean first) {
        Map<MemoryAccess, Set<Tuple2<MemoryAccess, String>>> edges = new HashMap<>(); // TODO

        if(memoryAccess instanceof Read) {
            //TODO remove all incoming rf edges
            revisitableReads.get(memoryAccess.getGlobalVariable()).remove(memoryAccess);
        }
        else if(memoryAccess instanceof Write) {
            //TODO remove from mo
        }
        for(Tuple2<MemoryAccess, String> edge : edges.get(memoryAccess)) {
            invalidateFuture(edge.get1(), atomic, false);
        }
        atomic.put(memoryAccess.getProcess(), memoryAccess.revert(xcfaExecutor.getStackFrames(), xcfaExecutor.getMutablePartitionedValuation(), xcfaExecutor.getPartitionId(memoryAccess.getProcess())));
        if(first) {
            edges.put(memoryAccess, new HashSet<>());
        }
        else {
            edges.remove(memoryAccess);
        }
    }

    /*
     * Returns a duplicate of the current ExecutionGraph
     */
    private ExecutionGraph duplicate(int i) {
        return new ExecutionGraph(this, i);
    }

    /*
     * Returns the current revisit (sub)sets of variable 'global'
     */
    private List<List<Read>> getRevisitSets(VarDecl<?> global) {
        List<List<Read>> ret = new ArrayList<>();
        if(revisitableReads.get(global) == null) return ret;
        for(int i = 0; i < (1<<revisitableReads.get(global).size()); ++i) {
            List<Read> list = new ArrayList<>();
            for(int j = 0; j < revisitableReads.get(global).size(); ++j) {
                if((i & (1<<j)) != 0) {
                    list.add(revisitableReads.get(global).get(j));
                }
            }
            ret.add(list);
        }
        return ret.stream().filter(reads -> {
            Set<XCFA.Process> processes = new HashSet<>();
            for(Read r : reads) {
                processes.add(r.getProcess());
            }
            return processes.size() == reads.size();
        }).collect(Collectors.toList());
    }

    private Collection<XCFA.Process> getProcesses() {
        if(xcfaExecutor.getCurrentlyAtomic() != null) return Collections.singleton(xcfaExecutor.getCurrentlyAtomic());
        else {
            return xcfa.getProcesses();
            // TODO
        }
    }



    public boolean handleNewEdge(XCFA.Process process, XCFA.Process.Procedure.Location newSource, Map<XCFA.Process, List<StackFrame>> stackFrames) {
        for(XCFA.Process.Procedure.Edge edge : newSource.getOutgoingEdges()) {
            boolean canExecute = true;
            for(Stmt stmt : edge.getStmts()) {
                if (stmt instanceof AssumeStmt) {
                    canExecute = ((BoolLitExpr) ((AssumeStmt) stmt).getCond().eval(xcfaExecutor.getMutablePartitionedValuation())).getValue();
                }
            }
            if(canExecute) {
                for(Stmt stmt : edge.getStmts()) {
                    List<StackFrame> stackFrameList = stackFrames.get(process);
                    StackFrame stackFrame;
                    if(stackFrameList.size() > 0 && (stackFrame = stackFrameList.get(stackFrameList.size() - 1)).isLastStmt()) {
                        stackFrameList.remove(stackFrame);
                    }
                    stackFrameList.add(new StackFrame(edge, stmt));
                    stmt.accept(xcfaStmtExecutionVisitor, Tuple2.of(process, this));
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleCurrentEdge(XCFA.Process process, StackFrame stackFrame, Map<XCFA.Process, List<StackFrame>> stackFrames) {
        Stmt nextStmt = null;
        boolean found = false;
        for (Stmt stmt : stackFrame.getEdge().getStmts()) {
            if (stmt == stackFrame.getStmt()){
                found = true;
            }
            else if(found) {
                nextStmt = stmt;
                break;
            }
        }
        if(nextStmt != null) {
            stackFrame.setStmt(nextStmt);
            nextStmt.accept(xcfaStmtExecutionVisitor, Tuple2.of(process, this));
            return true;
        }
        else {
            stackFrame.setLastStmt();
            return handleNewEdge(process, stackFrame.getEdge().getTarget(), stackFrames);
        }
    }


    private void addNode(MemoryAccess memoryAccess) {
        initialWrites.forEach(write -> {
            mcm.mkEdge(write, memoryAccess, "po", true);
        });
    }


    private void revisitRead(Read read) {
        for(Read r : read.getPrecedingReads()) { //TODO
            revisitableReads.get(r.getGlobalVariable()).remove(r);
        }
        invalidateFuture(read);
    }


    private synchronized void testQueue() {
        if(threadPool.getQueue().size() == 0 && threadPool.getActiveCount() == 1) {
            threadPool.shutdown();
            System.out.println("Traces: " + threadPool.getCompletedTaskCount());
        }
    }

    /*
     * Prints the graph as a graphviz cluster
     */
    private void printGraph(boolean isFinal) throws IOException {
        File outFile;
        if(!isFinal) {
            StringBuilder path = new StringBuilder("out").append(File.separator).append("steps").append(File.separator);
            for (Integer integer : this.path) {
                path.append(integer).append(File.separator);
            }
            outFile = new File(path.append(step).append("graph.dot").toString());
        }
        else {
            StringBuilder path = new StringBuilder("out").append(File.separator).append("final").append(File.separator);
            for (Integer integer : this.path) {
                path.append(integer);
            }
            outFile = new File(path.append("graph.dot").toString());
        }
        if (outFile.getParentFile().exists() || outFile.getParentFile().mkdirs()) {
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile))) {
                bufferedWriter.write("digraph G {");
                bufferedWriter.newLine();
                for (Write initialWrite : initialWrites) {
                    bufferedWriter.write(initialWrite.toString());
                    bufferedWriter.newLine();
                }
                for (XCFA.Process process : xcfa.getProcesses()) {
                    bufferedWriter.write("subgraph cluster_" + process.getName() + "{");
                    bufferedWriter.newLine();
                    for (MemoryAccess memoryAccess : edges.keySet()) {
                        if (memoryAccess.getProcess() == process) {
                            bufferedWriter.write(memoryAccess.toString());
                            if (memoryAccess instanceof Read && revisitableReads.get(memoryAccess.getGlobalVariable()).contains(memoryAccess)) {
                                bufferedWriter.write(" [style=filled]");
                            }
                            bufferedWriter.newLine();
                        }
                    }
                    bufferedWriter.write("}");
                    bufferedWriter.newLine();
                }
                for (Map.Entry<MemoryAccess, Set<Tuple2<MemoryAccess, String>>> entry : edges.entrySet()) {
                    MemoryAccess memoryAccess = entry.getKey();
                    Set<Tuple2<MemoryAccess, String>> tuple2s = entry.getValue();
                    for (Tuple2<MemoryAccess, String> tuple2 : tuple2s) {
                        bufferedWriter.write(memoryAccess + " -> " + tuple2.get1());
                        switch (tuple2.get2()) {
                            case "po":
                                break;
                            case "rf":
                                bufferedWriter.write(" [constraint=false,color=green,fontcolor=green,style=dashed,label=rf]");
                                break;
                            case "mo":
                                bufferedWriter.write(" [constraint=false,color=purple,fontcolor=purple,style=dashed,label=mo]");
                                break;
                            default:
                                bufferedWriter.write(" [constraint=false,color=grey,style=dashed,label=" + tuple2.get2() + "]");
                                break;
                        }
                        bufferedWriter.newLine();
                    }
                }
                bufferedWriter.write("fontcolor=red");
                bufferedWriter.newLine();
                bufferedWriter.write("label=\"");
                for (Map.Entry<Constraint, Result> entry : mcm.getConstraints().entrySet()) {
                    Constraint constraint = entry.getKey();
                    Result result = entry.getValue();
                    if (result.getResult() != Result.ResultType.OK) {
                        bufferedWriter.write(constraint.getName() + System.lineSeparator());
                    }
                }
                bufferedWriter.write("\"");
                bufferedWriter.newLine();
                bufferedWriter.write("}");
            }
        }
    }
}
