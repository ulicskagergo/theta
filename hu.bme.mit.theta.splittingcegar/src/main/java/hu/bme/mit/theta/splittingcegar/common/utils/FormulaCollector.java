package hu.bme.mit.theta.splittingcegar.common.utils;

import java.util.ArrayList;
import java.util.Collection;

import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.utils.impl.ExprUtils;
import hu.bme.mit.theta.formalism.sts.STS;

public class FormulaCollector {

	private FormulaCollector() {
	}

	public static void collectAtomsFromTransitionConditions(final STS sts,
			final Collection<Expr<? extends BoolType>> collectTo) {
		final Collection<Expr<? extends BoolType>> conditions = new ArrayList<>();
		final ITECondCollectorVisitor collector = new ITECondCollectorVisitor();
		for (final Expr<? extends BoolType> tran : sts.getTrans())
			tran.accept(collector, conditions);
		for (final Expr<? extends BoolType> cond : conditions)
			ExprUtils.collectAtoms(cond, collectTo);
	}

	public static void collectAtomsFromExpression(final Expr<? extends BoolType> expr,
			final Collection<Expr<? extends BoolType>> collectTo) {
		ExprUtils.collectAtoms(expr, collectTo);
	}
}