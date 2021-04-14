package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
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
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.dsl.XstsTypeDeclSymbol;

import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

public class XstsArgMetrics {

	private final boolean DEBUG_MODE = true;

	private final ARG<? extends State, ? extends Action> arg;
	final XSTS xsts;
	final List<VarDecl<?>> ctrlVars;
	final List<VarDecl<?>> notCrtlVars;
	private final Map<String, Map<String, String>> metrics;

	public XstsArgMetrics(ARG<? extends State, ? extends Action> arg, XSTS xsts){
		this.arg = arg;
		this.xsts = xsts;
		this.ctrlVars = new ArrayList<>(xsts.getCtrlVars());
		this.notCrtlVars = new ArrayList<>(xsts.getVars());
		notCrtlVars.removeAll(ctrlVars);
		this.metrics = new HashMap<>();
	}

	public void getMetrics(){
		final List<ArgNode<? extends State, ? extends Action>> argNodes = arg.getNodes().collect(Collectors.toList());

		for (ArgNode<? extends State, ? extends Action> argNode : argNodes) {
			if (argNode.getState() instanceof XstsState<?>) {
				XstsState<?> xstsState = (XstsState<?>) argNode.getState();
				if (!xstsState.lastActionWasEnv()) {
					Valuation ctrlValuation = extractCtrlValuation(xstsState);
					Valuation wholeValuation = extractWholeValuation(xstsState);
					getVariableValues(ctrlValuation, wholeValuation);
				}
			}
		}
		serializer();
	}

	private void getVariableValues(Valuation ctrlValuation, Valuation wholeValuation) {
		for (VarDecl<?> var : notCrtlVars) {
			Optional<? extends LitExpr<?>> eval = wholeValuation.eval(var);
			eval.ifPresent(litExpr -> addStat(ctrlValuation, var.getName(), litExpr.toString()));
		}
	}

	private Valuation extractWholeValuation(XstsState<?> xstsState) {
		final ImmutableValuation.Builder builder = ImmutableValuation.builder();	//kulcs
		ExprState innerState = xstsState.getState();

		if (innerState instanceof PredState) {
			System.out.println("[ ERROR ] We cannot work with predicate abstraction.");
		} else if (innerState instanceof ExplState) {
			ExplState explState = (ExplState) innerState;
			buildWholeValuation(explState, builder);
		} else if (innerState instanceof Prod2State) {
			if (((Prod2State<?, ?>) innerState).getState1() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod2State<?, ?>) innerState).getState1();
				buildWholeValuation(explState, builder);
			} else if (((Prod2State<?, ?>) innerState).getState2() instanceof ExplState){
				ExplState explState = (ExplState) ((Prod2State<?, ?>) innerState).getState2();
				buildWholeValuation(explState, builder);
			}
		} else if (innerState instanceof Prod3State) {
			if(((Prod3State<?, ?, ?>) innerState).getState1() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod3State<?, ?, ?>) innerState).getState1();
				buildWholeValuation(explState, builder);
			} else if (((Prod3State<?, ?, ?>) innerState).getState2() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod3State<?, ?, ?>) innerState).getState2();
				buildWholeValuation(explState, builder);
			} else if (((Prod3State<?, ?, ?>) innerState).getState3() instanceof ExplState){
				ExplState explState = (ExplState) ((Prod3State<?, ?, ?>) innerState).getState3();
				buildWholeValuation(explState, builder);
			}
		} else if (innerState instanceof Prod4State) {
			if (((Prod4State<?, ?, ?, ?>) innerState).getState1() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState1();
				buildWholeValuation(explState, builder);
			} else if (((Prod4State<?, ?, ?, ?>) innerState).getState2() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState2();
				buildWholeValuation(explState, builder);
			} else if (((Prod4State<?, ?, ?, ?>) innerState).getState3() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState3();
				buildWholeValuation(explState, builder);
			} else if (((Prod4State<?, ?, ?, ?>) innerState).getState4() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState4();
				buildWholeValuation(explState, builder);
			}
		}

		return builder.build();
	}

	private void buildWholeValuation(ExplState explState, ImmutableValuation.Builder builder) {
		for (VarDecl<?> ctrlVar : notCrtlVars) {
			Optional<? extends LitExpr<?>> eval = explState.eval(ctrlVar);
			eval.ifPresent(litExpr -> builder.put(ctrlVar, litExpr));
		}
	}

	private Valuation extractCtrlValuation(XstsState<?> xstsState) {
		final ImmutableValuation.Builder builder = ImmutableValuation.builder();	//kulcs
		ExprState innerState = xstsState.getState();

		if (innerState instanceof PredState) {
			System.out.println("[ ERROR ] We cannot work with predicate abstraction.");
		} else if (innerState instanceof ExplState) {
			ExplState explState = (ExplState) innerState;
			buildCtrlValuation(explState, builder);
		} else if (innerState instanceof Prod2State) {
			if (((Prod2State<?, ?>) innerState).getState1() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod2State<?, ?>) innerState).getState1();
				buildCtrlValuation(explState, builder);
			} else if (((Prod2State<?, ?>) innerState).getState2() instanceof ExplState){
				ExplState explState = (ExplState) ((Prod2State<?, ?>) innerState).getState2();
				buildCtrlValuation(explState, builder);
			}
		} else if (innerState instanceof Prod3State) {
			if(((Prod3State<?, ?, ?>) innerState).getState1() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod3State<?, ?, ?>) innerState).getState1();
				buildCtrlValuation(explState, builder);
			} else if (((Prod3State<?, ?, ?>) innerState).getState2() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod3State<?, ?, ?>) innerState).getState2();
				buildCtrlValuation(explState, builder);
			} else if (((Prod3State<?, ?, ?>) innerState).getState3() instanceof ExplState){
				ExplState explState = (ExplState) ((Prod3State<?, ?, ?>) innerState).getState3();
				buildCtrlValuation(explState, builder);
			}
		} else if (innerState instanceof Prod4State) {
			if (((Prod4State<?, ?, ?, ?>) innerState).getState1() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState1();
				buildCtrlValuation(explState, builder);
			} else if (((Prod4State<?, ?, ?, ?>) innerState).getState2() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState2();
				buildCtrlValuation(explState, builder);
			} else if (((Prod4State<?, ?, ?, ?>) innerState).getState3() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState3();
				buildCtrlValuation(explState, builder);
			} else if (((Prod4State<?, ?, ?, ?>) innerState).getState4() instanceof ExplState) {
				ExplState explState = (ExplState) ((Prod4State<?, ?, ?, ?>) innerState).getState4();
				buildCtrlValuation(explState, builder);
			}
		}

		return builder.build();
	}

	private void buildCtrlValuation(ExplState explState, ImmutableValuation.Builder builder) {
		for (VarDecl<?> ctrlVar : ctrlVars) {
			Optional<? extends LitExpr<?>> eval = explState.eval(ctrlVar);
			eval.ifPresent(litExpr -> builder.put(ctrlVar, litExpr));
		}
	}

	private void addStat(Valuation ctrlStateVector, String variable, String value) {
		Map<String, String> varVal = new HashMap<>();
		String keyVector = vectorToString(ctrlStateVector);

		if (metrics.containsKey(keyVector)) {												// vector already defined
			varVal = metrics.get(keyVector);
			if (varVal.containsKey(variable) && !varVal.get(variable).contains(value)) {	// variable already defined & there is a new value
				String oldValue = varVal.get(variable);
				String newValue = oldValue + ", " + value;
				varVal.put(variable, newValue);
				varVal.remove(value, oldValue);
			} else {
				varVal.put(variable, value);
			}
		}
		metrics.put(keyVector, varVal);

		if (DEBUG_MODE){
			System.out.println("[ DEBUG ] Added stat with parameters" +
					"\n\t * state:\t" + keyVector +
					"\n\t * variable:\t" + variable +
					"\n\t * value:\t" + value);
		}
	}

	private String vectorToString(Valuation ctrlStateVector) {
		for (VarDecl<?> ctrlVar : ctrlVars) {
			Optional<? extends LitExpr<?>> eval = ctrlStateVector.eval(ctrlVar);
			if (eval.isPresent()) {
				if (xsts.getVarToType().containsKey(ctrlVar)) {
					XstsTypeDeclSymbol type = xsts.getVarToType().get(ctrlVar);
					IntLitExpr intValue = (IntLitExpr) eval.get();
					var optSymbol = type.getLiterals().stream()
							.filter(symbol -> symbol.getIntValue().equals(intValue.getValue()))
							.findFirst();
					assert optSymbol.isPresent();
					return String.format("(%s %s)", ctrlVar.getName(), optSymbol.get().getName());
				} else {
					return String.format("(%s %s)", ctrlVar.getName(), eval.get());
				}
			}
		}
		throw new RuntimeException("[ Error ] Vector changed in meanwhile");
	}

	private void serializer(){
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for(Map.Entry<String, Map<String, String>> entry : metrics.entrySet()){
			JsonObjectBuilder inside = Json.createObjectBuilder();
			for(Map.Entry<String, String> insideEntry : entry.getValue().entrySet())
				inside.add(insideEntry.getKey(), insideEntry.getValue());
			builder.add(entry.getKey(), inside);
		}
		JsonObject o = builder.build();

		try {
			FileWriter sw = new FileWriter("metrics.json");
			JsonWriter jw = Json.createWriter(sw);
			jw.writeObject(o);
			jw.close();
			sw.close();
		} catch (IOException ioe){
			ioe.getStackTrace();
		}
	}
}
