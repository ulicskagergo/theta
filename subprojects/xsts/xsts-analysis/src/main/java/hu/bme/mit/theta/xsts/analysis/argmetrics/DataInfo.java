package hu.bme.mit.theta.xsts.analysis.argmetrics;

import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.prod3.Prod3State;
import hu.bme.mit.theta.analysis.prod4.Prod4State;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xsts.analysis.XstsState;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.*;

public class DataInfo {

    private final List<VarDecl<?>> notCtrlVars;
    private final XstsState<?> xstsState;
    private final Map<VarDecl<?>, DataVariableInfo> variablePairs;
    private final List<VarDecl<?>> globalPredicates;

    public DataInfo(XstsState<?> xstsState, List<VarDecl<?>> notCtrlVars) {
        this.xstsState = xstsState;
        this.notCtrlVars = notCtrlVars;
        this.variablePairs = new HashMap<>();
        this.globalPredicates = new ArrayList<>();
        extractCtrlValuation();
    }

    private void extractCtrlValuation() {
        ExprState innerState = xstsState.getState();
        if (innerState instanceof PredState) {
            addPredicate((PredState) innerState);
        } else if (innerState instanceof ExplState) {
            addVariablePair((ExplState) innerState);
        } else if (innerState instanceof Prod2State) {
            if (((Prod2State<?, ?>) innerState).getState1() instanceof ExplState) {
                addVariablePair((ExplState) ((Prod2State<?, ?>) innerState).getState1());
            } else if (((Prod2State<?, ?>) innerState).getState2() instanceof ExplState){
                addVariablePair((ExplState) ((Prod2State<?, ?>) innerState).getState2());
            }
        } else if (innerState instanceof Prod3State) {
            if(((Prod3State<?, ?, ?>) innerState).getState1() instanceof ExplState) {
                addVariablePair((ExplState) ((Prod3State<?, ?, ?>) innerState).getState1());
            } else if (((Prod3State<?, ?, ?>) innerState).getState2() instanceof ExplState) {
                addVariablePair((ExplState) ((Prod3State<?, ?, ?>) innerState).getState2());
            } else if (((Prod3State<?, ?, ?>) innerState).getState3() instanceof ExplState){
                addVariablePair((ExplState) ((Prod3State<?, ?, ?>) innerState).getState3());
            }
        } else if (innerState instanceof Prod4State) {
            if (((Prod4State<?, ?, ?, ?>) innerState).getState1() instanceof ExplState) {
                addVariablePair((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState1());
            } else if (((Prod4State<?, ?, ?, ?>) innerState).getState2() instanceof ExplState) {
                addVariablePair((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState2());
            } else if (((Prod4State<?, ?, ?, ?>) innerState).getState3() instanceof ExplState) {
                addVariablePair((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState3());
            } else if (((Prod4State<?, ?, ?, ?>) innerState).getState4() instanceof ExplState) {
                addVariablePair((ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState4());
            }
        }
    }

    private void addVariablePair(ExplState explState) {
        for (VarDecl<?> notCtrlVar : notCtrlVars) {
            Optional<? extends LitExpr<?>> eval = explState.eval(notCtrlVar);
            if (eval.isPresent()) {
                DataVariableInfo dataVariableInfo = new DataVariableInfo();
                if (variablePairs.containsKey(notCtrlVar)) {
                    dataVariableInfo = variablePairs.get(notCtrlVar);
                }
                variablePairs.put(notCtrlVar, dataVariableInfo.addValue(eval.get()));
            }
        }
    }

    private void addPredicate(PredState predState) {
        for (VarDecl<?> pred : ExprUtils.getVars(predState.getPreds())) {
            boolean varPair = false;
            for (VarDecl<?> notCtrlVar : notCtrlVars) {
                assert pred != null;
                if (notCtrlVar.getName().equals(pred.getName())) {
                    DataVariableInfo dataVariableInfo = new DataVariableInfo();
                    if (variablePairs.containsKey(notCtrlVar)) {
                        dataVariableInfo = variablePairs.get(notCtrlVar);
                    }
                    variablePairs.put(notCtrlVar, dataVariableInfo.addPredicate(pred));
                    varPair = true;
                    break;
                }
            }
            if (!varPair) {
                globalPredicates.add(pred);
            }
        }
    }

    @Override
    public String toString() {
        return "DataInfo{" +
                "variablePairs=" + variablePairs +
                ", globalPredicates=" + globalPredicates +
                '}';
    }

    public JsonObjectBuilder toJson() {
        JsonObjectBuilder builder =  Json.createObjectBuilder();
        for (Map.Entry<VarDecl<?>, DataVariableInfo> entry : variablePairs.entrySet()) {
            builder.add(entry.getKey().getName(), entry.getValue().toJson());
        }
        for (VarDecl<?> predicate : globalPredicates) {
            System.out.println("\t[ DEBUG ] " + predicate.toString());
            builder.add("global_predicate", predicate.toString());
        }
        return builder;
    }
}
