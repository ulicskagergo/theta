package hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph.nodes;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.xcfa.XCFA;

public class Write extends MemoryAccess implements hu.bme.mit.theta.mcm.graph.filter.interfaces.Write {
    private static int cnt;

    static {
        cnt = 0;
    }

    private final int id;
    private final LitExpr<?> value;

    public Write(VarDecl<?> globalVar, LitExpr<?> value, XCFA.Process parentProcess) {
        super(globalVar, parentProcess);
        this.value = value;
        id = cnt++;
    }

    public LitExpr<?> getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "\"W(" + getGlobalVariable().getName() + ", " + getValue() + ")_" + id + "\"";
    }
}
