package hu.bme.mit.theta.frontend.benchmark;

import java.io.FileNotFoundException;
import java.io.IOException;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Precision;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.impl.ConsoleLogger;
import hu.bme.mit.theta.formalism.sts.STS;
import hu.bme.mit.theta.frontend.aiger.impl.AigerParserSimple;
import hu.bme.mit.theta.frontend.benchmark.StsConfigurationBuilder.Domain;
import hu.bme.mit.theta.frontend.benchmark.StsConfigurationBuilder.InitPrecision;
import hu.bme.mit.theta.frontend.benchmark.StsConfigurationBuilder.Refinement;
import hu.bme.mit.theta.frontend.benchmark.StsConfigurationBuilder.Search;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

public class SandBox {
	public static void main(final String[] args) throws FileNotFoundException, IOException {
		// final StsSpec spec =
		// StsDslManager.parse("src/test/resources/simple/multipleinitial.system",
		// Collections.emptyList());
		// STS sts = spec.getAllSts().iterator().next();
		// sts = new StsIteTransformation().transform(sts);

		System.out.println("Press a key to start");
		System.in.read();

		final STS sts = new AigerParserSimple().parse("src/test/resources/hw/power2sum32.aag");

		System.out.println(sts);

		final Logger logger = new ConsoleLogger(3);

		final Configuration<? extends State, ? extends Action, ? extends Precision> configuration = new StsConfigurationBuilder(
				Domain.EXPL, Refinement.SEQ_ITP).initPrecision(InitPrecision.PROP).logger(logger).search(Search.BFS)
						.solverFactory(Z3SolverFactory.getInstace()).build(sts);

		configuration.check();
	}
}