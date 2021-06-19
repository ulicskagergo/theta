package hu.bme.mit.theta.xsts.analysis.argmetrics;

import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.prod3.Prod3State;
import hu.bme.mit.theta.analysis.prod4.Prod4State;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.xsts.analysis.XstsState;

import java.util.List;
import java.util.Optional;

public class ControlValuationInfo {

    private final Valuation key;
    private final DataInfo value;
    private XstsState<?> xstsState;
    private List<VarDecl<?>> ctrlVars;
    private ImmutableValuation.Builder builder;

    public ControlValuationInfo(XstsState<?> xstsState, List<VarDecl<?>> ctrlVars, List<VarDecl<?>> notCtrlVars) {
        this.ctrlVars = ctrlVars;
        this.xstsState = xstsState;
        builder = ImmutableValuation.builder();
        value = new DataInfo(xstsState, notCtrlVars);
        key = extractValuation();
    }

    public ControlValuationInfo(Valuation key, DataInfo value) {
        this.key = key;
        this.value = value;
    }

    private Valuation extractValuation() {
        ExprState innerState = xstsState.getState();
        if (innerState instanceof PredState) {
            System.out.println("[ ERROR ] We cannot work with predicate abstraction.");
        } else if (innerState instanceof ExplState) {
            buildValuation((ExplState) innerState);
        } else if (innerState instanceof Prod2State) {
            if (((Prod2State<?, ?>) innerState).getState1() instanceof ExplState) {
                buildValuation((ExplState) ((Prod2State<?, ?>) innerState).getState1());
            } else if (((Prod2State<?, ?>) innerState).getState2() instanceof ExplState){
                buildValuation((ExplState) ((Prod2State<?, ?>) innerState).getState2());
            }
        } else if (innerState instanceof Prod3State) {
            if(((Prod3State<?, ?, ?>) innerState).getState1() instanceof ExplState) {
                buildValuation((ExplState) ((Prod3State<?, ?, ?>) innerState).getState1());
            } else if (((Prod3State<?, ?, ?>) innerState).getState2() instanceof ExplState) {
                buildValuation((ExplState) ((Prod3State<?, ?, ?>) innerState).getState2());
            } else if (((Prod3State<?, ?, ?>) innerState).getState3() instanceof ExplState){
                buildValuation((ExplState) ((Prod3State<?, ?, ?>) innerState).getState3());
            }
        } else if (innerState instanceof Prod4State) {
            if (((Prod4State<?, ?, ?, ?>) innerState).getState1() instanceof ExplState) {
                buildValuation((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState1());
            } else if (((Prod4State<?, ?, ?, ?>) innerState).getState2() instanceof ExplState) {
                buildValuation((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState2());
            } else if (((Prod4State<?, ?, ?, ?>) innerState).getState3() instanceof ExplState) {
                buildValuation((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState3());
            } else if (((Prod4State<?, ?, ?, ?>) innerState).getState4() instanceof ExplState) {
                buildValuation((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState4());
            }
        }

        return builder.build();
    }

    private void buildValuation(ExplState explState) {
        for (VarDecl<?> ctrlVar : ctrlVars) {
            Optional<? extends LitExpr<?>> eval = explState.eval(ctrlVar);
            eval.ifPresent(litExpr -> builder.put(ctrlVar, litExpr));
        }
    }

    public Valuation getKey() {
        return key;
    }

    public DataInfo getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "KeyValuePair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
