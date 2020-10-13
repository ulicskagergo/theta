package hu.bme.mit.theta.xcfa.analysis.stateless.executor;

import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.xcfa.XCFA;

public class StackFrame {
    private final XCFA.Process.Procedure.Edge edge;
    private Stmt stmt;
    private boolean lastStmt;

    public StackFrame(XCFA.Process.Procedure.Edge edge, Stmt stmt) {
        this.edge = edge;
        this.stmt = stmt;
        this.lastStmt = false;
    }

    public XCFA.Process.Procedure.Edge getEdge() {
        return edge;
    }

    public Stmt getStmt() {
        return stmt;
    }

    public boolean isLastStmt() {
        return lastStmt;
    }

    public void setLastStmt() {
        this.lastStmt = true;
    }

    public void setStmt(Stmt stmt) {
        this.stmt = stmt;
    }

    public StackFrame duplicate() {
        return new StackFrame(edge, stmt);
    }
}
