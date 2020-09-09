package hu.bme.mit.theta.xcfa.analysis.statelessold.graph;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple4;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutableValuation;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.analysis.statelessold.State;
import hu.bme.mit.theta.xcfa.analysis.statelessold.XcfaStmtExecutionVisitor;
import hu.bme.mit.theta.xcfa.analysis.statelessold.graph.node.Node;
import hu.bme.mit.theta.xcfa.analysis.statelessold.graph.node.Read;
import hu.bme.mit.theta.xcfa.analysis.statelessold.graph.node.Write;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class ExecutionGraph {
    private final Set<Write> initialWrites;
    private final Map<VarDecl<?>, List<Read>> revisitableReads;
    private final Map<VarDecl<?>, List<Write>> revisitableWrites;
    private final Map<XCFA.Process, List<Node>> nodes;
    private final State currentState;
    private final int id;
    private static int cnt = 0;
    private final Map<Node, Node> copyLut;
    private Tuple2<hu.bme.mit.theta.xcfa.XCFA.Process, XCFA.Process.Procedure.Edge> processingEdge;
    private int firstStmt;
    
    private static final Queue<ExecutionGraph> toRun = new ConcurrentLinkedQueue<>();

    public static void start() {
        ExecutionGraph executionGraph;
        while((executionGraph = toRun.poll()) != null) {
            executionGraph.execute();
        }
    }

    public ExecutionGraph(XCFA xcfa) {
        this.copyLut = null;
        this.currentState = new State(xcfa, this);
        this.revisitableReads = new HashMap<>();
        nodes = new HashMap<>();
        initialWrites = new HashSet<>();
        revisitableWrites = new HashMap<>();

        id = cnt++;
        processingEdge = null;
        toRun.add(this);
    }

    private ExecutionGraph(
            Set<Write> initialWrites,
            Map<VarDecl<?>, List<Read>> revisitableReads,
            Map<VarDecl<?>, List<Write>> revisitableWrites,
            Map<XCFA.Process, List<Node>> nodes,
            State currentState,
            Tuple2<hu.bme.mit.theta.xcfa.XCFA.Process, XCFA.Process.Procedure.Edge> processingEdge) {
        this.processingEdge = processingEdge;
        this.revisitableReads = new HashMap<>();
        this.revisitableWrites = new HashMap<>();
        this.nodes = new HashMap<>();
        this.initialWrites = new HashSet<>();

        copyLut = new HashMap<>();
        initialWrites.forEach(write -> {
            Node dup = write.duplicate();
            copyLut.put(write, dup);
            this.initialWrites.add((Write)dup);
        });
        nodes.forEach((process, nodeList) -> {
            this.nodes.put(process, new ArrayList<>());
            nodeList.forEach(node -> {
                Node dup = node.duplicate();
                copyLut.put(node, dup);
                this.nodes.get(process).add(dup);
            });
        });

        initialWrites.forEach(write -> {
            write.getOutgoingEdges().forEach(edge -> {
                Node source, target;
                Edge newEdge = new Edge(source = copyLut.get(edge.getSource()), target = copyLut.get(edge.getTarget()), edge.getLabel());
                source.addOutgoingEdge(newEdge);
                target.addIncomingEdge(newEdge);
            });
        });
        nodes.forEach((process, nodes1) -> nodes1.forEach(node -> {
            node.getOutgoingEdges().forEach(edge -> {
                Node source, target;
                Edge newEdge = new Edge(source = copyLut.get(edge.getSource()), target = copyLut.get(edge.getTarget()), edge.getLabel());
                source.addOutgoingEdge(newEdge);
                target.addIncomingEdge(newEdge);
            });
        }));

        revisitableReads.forEach((varDecl, reads) -> {
            List<Read> list = new ArrayList<>();
            reads.forEach(read -> list.add((Read)copyLut.get(read)));
            this.revisitableReads.put(varDecl, list);
        });
        revisitableWrites.forEach((varDecl, writes) -> {
            List<Write> list = new ArrayList<>();
            writes.forEach(write -> list.add((Write)copyLut.get(write)));
            this.revisitableWrites.put(varDecl, list);
        });
        initialWrites.forEach(write -> this.initialWrites.add((Write)copyLut.get(write)));

        this.currentState = State.copyOf(currentState, this);
        id = cnt++;
    }

    public static ExecutionGraph copyOf(ExecutionGraph executionGraph) {
        return new ExecutionGraph(
                executionGraph.initialWrites,
                executionGraph.revisitableReads,
                executionGraph.revisitableWrites,
                executionGraph.nodes,
                executionGraph.currentState,
                executionGraph.processingEdge);
    }

    private int step = 0;
    private void execute() {
        Tuple2<XCFA.Process, XCFA.Process.Procedure.Edge> edge;

        while((edge = (processingEdge == null ? currentState.getOneStep() : processingEdge)) != null) {

            currentState.getCurrentLocs().put(edge.get1(), edge.get2().getTarget());

            if(edge.get2().getTarget().isErrorLoc()) {
                System.out.println("Error location reachable!");
            }

            processingEdge = edge;
            firstStmt = currentState.getFirstStmt().getOrDefault(edge.get2(), 0);
            for(;firstStmt < edge.get2().getStmts().size();++firstStmt) {
                currentState.getFirstStmt().put(edge.get2(), firstStmt+1);
                Stmt stmt = edge.get2().getStmts().get(firstStmt);
                //printGraph(step++);
                stmt.accept(new XcfaStmtExecutionVisitor(), Tuple4.of(currentState.getMutablePartitionedValuation(), currentState, edge.get1(), this));
            }
            currentState.getFirstStmt().remove(edge.get2());
            processingEdge = null;


        }
        printGraph(step);
    }

    public void printGraph(int step) {
        System.out.println("subgraph cluster_" + id + "_" + step + " {");
        System.out.println("label=cluster_" + id + "_" + step);
        revisitableReads.forEach((varDecl, reads) -> reads.forEach(read -> System.out.println("\"" + read + "_" + step  + "\" [style=filled]")));
        initialWrites.forEach(write -> write.getOutgoingEdges().forEach(edge1 -> System.out.println("\"" + write + "_" + step  + "\"" + " -> " + "\"" + edge1.getTarget() + "_" + step  + "\"" + (edge1.getLabel().equals("po") ? "" : "[label=" + edge1.getLabel() + ",constraint=false,color=green,fontcolor=green,style=dashed]"))));
        nodes.forEach((process, nodes1) -> nodes1.forEach(node -> node.getOutgoingEdges().forEach(edge1 -> System.out.println("\"" + node + "_" + step  + "\"" + " -> " + "\"" + edge1.getTarget() + "_" + step  + "\"" + (edge1.getLabel().equals("po") ? "" : "[label=" + edge1.getLabel() + ",constraint=false,color=green,fontcolor=green,style=dashed]")))));
        System.out.println("}");
    }

    private void addNode(XCFA.Process proc, Node node) {
        if(nodes.containsKey(proc)) {
            Node last = nodes.get(proc).get(nodes.get(proc).size()-1);
            Edge edge = new Edge(last, node, "po");
            last.addOutgoingEdge(edge);
            node.addIncomingEdge(edge);
        }
        else {
            nodes.put(proc, new ArrayList<>());
            for(Write w : initialWrites) {
                Edge edge = new Edge(w, node, "po");
                w.addOutgoingEdge(edge);
                node.addIncomingEdge(edge);
            }
        }
        nodes.get(proc).add(node);
    }

    public void addRead(XCFA.Process proc, VarDecl<?> local, VarDecl<?> global) {
        Read read;
        if(nodes.containsKey(proc)) {
            read = new Read(currentState.getMutablePartitionedValuation().getValuation(currentState.getPartitionId(proc)), global, local, processingEdge, firstStmt);
        }
        else {
            read = new Read(new MutableValuation(), global, local, processingEdge, firstStmt);
        }
        addNode(proc, read);

        List<Write> writes = revisitableWrites.get(global);
        if(writes == null) {
            throw new UnsupportedOperationException("Reading before writing is not yet supported");
        }

        for(int i = 0; i < writes.size(); ++i) {
            Write write = writes.get(i);
            ExecutionGraph executionGraph;
            if(i == writes.size()-1) {
                executionGraph = this;
                if(!revisitableReads.containsKey(global)) {
                    revisitableReads.put(global, new ArrayList<>());
                }
                revisitableReads.get(global).add(read);
                executionGraph.currentState.getMutablePartitionedValuation().put(executionGraph.currentState.getPartitionId(proc), local, write.getValue());
                Edge rf = new Edge(write, read, "rf");
                write.addOutgoingEdge(rf);
                read.addIncomingEdge(rf);
            }
            else {
                executionGraph = ExecutionGraph.copyOf(this);
                //System.out.println("subgraph cluster_start_" + executionGraph.id + "{start_" + executionGraph.id + "}");
                executionGraph.currentState.getMutablePartitionedValuation().put(executionGraph.currentState.getPartitionId(proc), local, write.getValue());
                Edge rf = new Edge(executionGraph.copyLut.get(write), executionGraph.copyLut.get(read), "rf");
                executionGraph.copyLut.get(write).addOutgoingEdge(rf);
                executionGraph.copyLut.get(read).addIncomingEdge(rf);
                toRun.add(executionGraph);
            }
        }

    }

    public void addWrite(XCFA.Process proc, VarDecl<?> local, VarDecl<?> global) {
        Write write = new Write(global, currentState.getMutablePartitionedValuation().eval(local).get(), proc);
        addNode(proc, write);
        if(!revisitableWrites.containsKey(global)) {
            revisitableWrites.put(global, new ArrayList<>());
        }
        revisitableWrites.get(global).add(write);
        List<List<Read>> revisitSets = getRevisitSets(global);
        for (int i = 0; i < revisitSets.size(); i++) {
            List<Read> reads = revisitSets.get(i);
            ExecutionGraph executionGraph;
            if (i < revisitSets.size()-1) {
                executionGraph = ExecutionGraph.copyOf(this);
                //System.out.println("subgraph cluster_start_" + executionGraph.id + "{start_" + executionGraph.id + "}");
                for (Read read : reads) {
                    Write newWrite = (Write) executionGraph.copyLut.get(write);
                    Read newRead = (Read) executionGraph.copyLut.get(read);
                    executionGraph.removeRevisit(newRead);
                    newRead.invalidate(executionGraph.currentState);
                    executionGraph.currentState.getMutablePartitionedValuation().put(executionGraph.currentState.getPartitionId(read.getParentProcess()), newRead.getLocal(), newWrite.getValue());
                    removeRfEdges(newWrite, newRead);
                }
                toRun.add(executionGraph);
            } else {
                executionGraph = this;
                for (Read read : reads) {
                    executionGraph.removeRevisit(read);
                    read.invalidate(executionGraph.currentState);
                    executionGraph.currentState.getMutablePartitionedValuation().put(executionGraph.currentState.getPartitionId(read.getParentProcess()), read.getLocal(), write.getValue());
                    removeRfEdges(write, read);
                }
            }
        }
    }

    private void removeRevisit(Node node) {
        if(node instanceof Read) {
            revisitableReads.get(((Read) node).getGlobal()).remove(node);
        }
        node.getIncomingEdges().forEach(edge -> removeRevisit(edge.getSource()));
    }

    private void removeRfEdges(Write write, Read read) {
        for(Edge e : read.getIncomingEdges().stream().filter(edge -> edge.getLabel().equals("rf")).collect(Collectors.toList())) {
            e.getSource().getOutgoingEdges().remove(e);
            e.getTarget().getIncomingEdges().remove(e);
        }
        Edge rf = new Edge(write, read, "rf");
        write.addOutgoingEdge(rf);
        read.addIncomingEdge(rf);
    }

    private List<List<Read>> getRevisitSets(VarDecl<?> global) {
return null;
    }

    public void addInitialWrite(VarDecl<?> global, LitExpr<?> value) {

    }

    public void removeNode(Node target) {
        nodes.get(target.getParentProcess()).remove(target);
        if(target instanceof Write) {
            revisitableWrites.get(((Write) target).getGlobalVar()).remove(target);
        }
        else if(target instanceof Read) {
            revisitableReads.get(((Read) target).getGlobal()).remove(target);
        }
    }
}