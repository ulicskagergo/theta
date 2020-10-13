package hu.bme.mit.theta.xcfa.analysis.stateless.executor;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.stmt.xcfa.*;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.ExecutionGraph;

public class XcfaStmtExecutionVisitor
        implements  StmtVisitor<Tuple2<XCFA.Process, ExecutionGraph>, Void>,
                    XcfaStmtVisitor<Tuple2< XCFA.Process, ExecutionGraph>, Void>{
    @Override
    public Void visit(SkipStmt stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        /* Intentionally left empty. */
        return null;
    }

    @Override
    public Void visit(AssumeStmt stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        /* Intentionally left empty. */
        return null;
    }

    @Override
    public <DeclType extends Type> Void visit(AssignStmt<DeclType> stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        param.get2().getXcfaExecutor().putValuation(param.get1(), stmt.getVarDecl(), stmt.getExpr().eval(param.get2().getXcfaExecutor().getMutablePartitionedValuation()));
        return null;
    }

    @Override
    public <DeclType extends Type> Void visit(HavocStmt<DeclType> stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        param.get2().getXcfaExecutor().getMutablePartitionedValuation().remove(stmt.getVarDecl());
        return null;
    }

    @Override
    public Void visit(XcfaStmt xcfaStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        return xcfaStmt.accept(this, param);
    }

    @Override
    public Void visit(SequenceStmt stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(NonDetStmt stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(OrtStmt stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(XcfaCallStmt stmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(StoreStmt storeStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        param.get2().addWrite(param.get1(), storeStmt.getLhs(), storeStmt.getRhs());
        return null;
    }

    @Override
    public Void visit(LoadStmt loadStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        param.get2().addRead(param.get1(), loadStmt.getLhs(), loadStmt.getRhs());
        return null;
    }

    @Override
    public Void visit(FenceStmt fenceStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        param.get2().addFence(param.get1(), fenceStmt.getType());
        return null;
    }

    @Override
    public Void visit(AtomicBeginStmt atomicBeginStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        param.get2().getXcfaExecutor().setCurrentlyAtomic(param.get1());
        return null;
    }

    @Override
    public Void visit(AtomicEndStmt atomicEndStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        param.get2().getXcfaExecutor().setCurrentlyAtomic(null);
        return null;
    }

    @Override
    public Void visit(NotifyAllStmt notifyAllStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(NotifyStmt notifyStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(WaitStmt waitStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(MtxLockStmt lockStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(MtxUnlockStmt unlockStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(ExitWaitStmt exitWaitStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(EnterWaitStmt enterWaitStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Void visit(XcfaInternalNotifyStmt enterWaitStmt, Tuple2<XCFA.Process, ExecutionGraph> param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
