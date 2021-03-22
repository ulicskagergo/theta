package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.xsts.XSTS;

import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

public class XstsArgMetrics {

	private final ARG<? extends State, ? extends Action> arg;
	private final XSTS xsts;
	private final Map<String, Map<String, String>> metrics;
	private Integer stateNum;

	public XstsArgMetrics(ARG<? extends State, ? extends Action> arg, XSTS xsts){
		this.arg = arg;
		this.xsts = xsts;
		this.metrics = new HashMap<>();
		this.stateNum = 0;
	}

	public void getMetrics(){

		final List<ArgNode<? extends State, ? extends Action>> argNodes = arg.getNodes().collect(Collectors.toList());
		final List<XstsState<?>> xstsStates = new ArrayList<>();

		for (ArgNode<? extends State, ? extends Action> argNode : argNodes) {
			if (argNode.getState() instanceof XstsState<?>) {
				XstsState<?> xstsState = (XstsState<?>) argNode.getState();
				if (!xstsState.lastActionWasEnv()) {
					xstsStates.add(xstsState);
				}
			}
		}

		for (XstsState xstsState : xstsStates)
			System.out.println(xstsState.toString());

		//serializer();
	}

	private void addStat(boolean init, String stateName, String variable, String value) {
		Map<String, String> tmp = new HashMap<>();
		tmp.put(variable, value);
		String state = (init ? "post_init " : "pre_init ") + stateName + " " + stateNum;
		metrics.put(state, tmp);
		stateNum++;
		System.out.println("DEBUG: Added stat with parameters" +
				"\n * state:\t" + state +
				"\n * variable:\t" + variable +
				"\n * value:\t" + value);
	}

	private void serializer(){
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for(Map.Entry<String, Map<String, String>> entry : metrics.entrySet()){
			JsonObjectBuilder inside = Json.createObjectBuilder();
			for(Map.Entry<String, String> insideentry : entry.getValue().entrySet())
				inside.add(insideentry.getKey(), insideentry.getValue());
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
			System.out.println(ioe);
		}
	}
}
