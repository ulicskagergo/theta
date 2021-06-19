package hu.bme.mit.theta.xsts.analysis.argmetrics;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.LitExpr;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataVariableInfo {

    private final List<LitExpr<?>> values;
    private final List<VarDecl<?>> predicates;

    public DataVariableInfo() {
        values = new ArrayList<>();
        predicates = new ArrayList<>();
    }

    public DataVariableInfo addValue(LitExpr<?> value) {
        values.add(value);
        return this;
    }

    public DataVariableInfo addPredicate(VarDecl<?> predicate) {
        predicates.add(predicate);
        return this;
    }

    @Override
    public String toString() {
        return "DataVariableInfo{" +
                "values=" + values +
                ", predicates=" + predicates +
                '}';
    }

    public JsonObjectBuilder toJson() {
        return Json.createObjectBuilder()
                .add("explicit_values", "[" + values.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]")
                .add("predicates", "[" + predicates.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]");
    }
}
