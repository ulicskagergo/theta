package hu.bme.mit.theta.mcm;

import hu.bme.mit.theta.mcm.graph.filter.interfaces.Process;

import java.util.Collection;
import java.util.HashSet;

public class Result {
    private final Collection<Process> processes;
    private final ResultType result;

    public static Result ok() {
        return new Result(new HashSet<>(), ResultType.OK);
    }

    public static Result violation(Collection<Process> processes) {
        return new Result(new HashSet<>(processes), ResultType.VIOLATION);
    }

    public static Result finalViolation() {
        return new Result(new HashSet<>(), ResultType.FINAL_VIOLATION);
    }

    private Result(Collection<Process> processes, ResultType result) {
        this.processes = processes;
        this.result = result;
    }

    public Collection<Process> getProcesses() {
        return processes;
    }

    public ResultType getResult() {
        return result;
    }

    public enum ResultType {
        OK,
        VIOLATION,
        FINAL_VIOLATION
    }
}
