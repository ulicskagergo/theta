package hu.bme.mit.theta.xsts.analysis.argmetrics;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.XstsState;

import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

public class XstsArgMetrics {

	private final ARG<? extends State, ? extends Action> arg;
	private final List<VarDecl<?>> ctrlVars;
	private final List<VarDecl<?>> notCtrlVars;
	private final List<ControlValuationInfo> metrics;
	private final Map<Valuation, DataInfo> metricsMap;
	private final String file;

	public XstsArgMetrics(ARG<? extends State, ? extends Action> arg, XSTS xsts, String file){
		this.arg = arg;
		this.ctrlVars = new ArrayList<>(xsts.getCtrlVars());
		this.notCtrlVars = new ArrayList<>(xsts.getVars());
		notCtrlVars.removeAll(ctrlVars);
		this.metrics = new ArrayList<>();
		this.metricsMap = new HashMap<>();
		this.file = file;
	}

	public void getMetrics(){
		final List<ArgNode<? extends State, ? extends Action>> argNodes = arg.getNodes().collect(Collectors.toList());

		for (ArgNode<? extends State, ? extends Action> argNode : argNodes) {
			if (argNode.getState() instanceof XstsState<?>) {
				XstsState<?> xstsState = (XstsState<?>) argNode.getState();
				if (!xstsState.lastActionWasEnv()) {
					ControlValuationInfo act = new ControlValuationInfo(xstsState, ctrlVars, notCtrlVars);
					metricsMap.putIfAbsent(act.getKey(), act.getValue());
				}
			}
		}
		for (Map.Entry<Valuation, DataInfo> entry : metricsMap.entrySet()) {
			metrics.add(new ControlValuationInfo(entry.getKey(), entry.getValue()));
		}
		toJson();
	}

	private void toJson() {
		try {
			FileWriter fileWriter = new FileWriter(file);
			for (ControlValuationInfo controlValuationInfo : metrics) {
				System.out.println(controlValuationInfo.toString());
				fileWriter.write(controlValuationInfo + "\n");
			}
			fileWriter.close();

			fileWriter = new FileWriter("metrics.json");
			JsonWriter jsonWriter = Json.createWriter(fileWriter);
			JsonObjectBuilder builder = Json.createObjectBuilder();
			for (ControlValuationInfo controlValuationInfo : metrics) {
				builder.add(controlValuationInfo.getKey().toString().replaceAll("\\s+", " "), controlValuationInfo.getValue().toJson());
			}
			JsonObject jsonObject = builder.build();
			jsonWriter.write(jsonObject);
			fileWriter.close();
		} catch (IOException ioe){
			ioe.getStackTrace();
		}
	}
}
