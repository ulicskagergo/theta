/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.core.utils;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.anytype.Exprs.Prime;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.*;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;

import java.util.*;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntType;

final class StmtToExprTransformer {

	private StmtToExprTransformer() {
	}

	static StmtUnfoldResult toExpr(final Stmt stmt, final VarIndexing indexing) {
		return stmt.accept(StmtToExprVisitor.INSTANCE, indexing);
	}

	static StmtUnfoldResult toExpr(final List<? extends Stmt> stmts, final VarIndexing indexing) {
		final Collection<Expr<BoolType>> resultExprs = new ArrayList<>();
		VarIndexing resultIndexing = indexing;

		for (final Stmt stmt : stmts) {
			final StmtUnfoldResult subResult = toExpr(stmt, resultIndexing);
			resultExprs.addAll(subResult.exprs);
			resultIndexing = subResult.indexing;
		}

		return StmtUnfoldResult.of(resultExprs, resultIndexing);
	}

	////////

	private static class StmtToExprVisitor implements StmtVisitor<VarIndexing, StmtUnfoldResult> {
		private static final StmtToExprVisitor INSTANCE = new StmtToExprVisitor();

		private StmtToExprVisitor() {
		}

		@Override
		public StmtUnfoldResult visit(final SkipStmt stmt, final VarIndexing indexing) {
			return StmtUnfoldResult.of(ImmutableList.of(True()), indexing);
		}

		@Override
		public StmtUnfoldResult visit(final AssumeStmt stmt, final VarIndexing indexing) {
			final Expr<BoolType> cond = stmt.getCond();
			final Expr<BoolType> expr = ExprUtils.applyPrimes(cond, indexing);
			return StmtUnfoldResult.of(ImmutableList.of(expr), indexing);
		}

		@Override
		public <DeclType extends Type> StmtUnfoldResult visit(final HavocStmt<DeclType> stmt,
															  final VarIndexing indexing) {
			final VarDecl<?> varDecl = stmt.getVarDecl();
			final VarIndexing newIndexing = indexing.inc(varDecl);
			return StmtUnfoldResult.of(ImmutableList.of(True()), newIndexing);
		}

		@Override
		public <DeclType extends Type> StmtUnfoldResult visit(final AssignStmt<DeclType> stmt,
															  final VarIndexing indexing) {
			final VarDecl<DeclType> varDecl = stmt.getVarDecl();
			final VarIndexing newIndexing = indexing.inc(varDecl);
			final Expr<DeclType> rhs = ExprUtils.applyPrimes(stmt.getExpr(), indexing);
			final Expr<DeclType> lhs = ExprUtils.applyPrimes(varDecl.getRef(), newIndexing);

			final Expr<BoolType> expr = Eq(lhs, rhs);
			return StmtUnfoldResult.of(ImmutableList.of(expr), newIndexing);
		}

		@Override
		public StmtUnfoldResult visit(SequenceStmt sequenceStmt, VarIndexing indexing) {
			StmtUnfoldResult result = toExpr(sequenceStmt.getStmts(), indexing);
			return StmtUnfoldResult.of(ImmutableList.of(And(result.getExprs())), result.getIndexing());
		}

		@Override
		public StmtUnfoldResult visit(NonDetStmt nonDetStmt, VarIndexing indexing) {

			List<Expr<BoolType>> choices = new ArrayList<>();
			List<VarIndexing> indexings = new ArrayList<>();
			VarIndexing jointIndexing = indexing;
			int count = 0;
			VarDecl<IntType> tempVar = VarPoolUtil.requestInt();
			for (Stmt stmt : nonDetStmt.getStmts()) {
				Expr<BoolType> tempExpr = Eq(ExprUtils.applyPrimes(tempVar.getRef(), indexing), Int(count++));
				StmtUnfoldResult result = toExpr(stmt, indexing.inc(tempVar));
				choices.add(And(tempExpr, And(result.exprs)));
				indexings.add(result.indexing);
				jointIndexing = jointIndexing.join(result.indexing);
			}
			Set<VarDecl<?>> vars = ExprUtils.getVars(choices);
			List<Expr<BoolType>> branchExprs = new ArrayList<>();
			for (int i = 0; i < choices.size(); i++) {
				List<Expr<BoolType>> exprs = new ArrayList<>();
				exprs.add(choices.get(i));
				for (VarDecl<?> decl : vars) {
					int currentBranchIndex = indexings.get(i).get(decl);
					int jointIndex = jointIndexing.get(decl);
					if (currentBranchIndex < jointIndex) {
						if (currentBranchIndex > 0)
							exprs.add(Eq(Prime(decl.getRef(), currentBranchIndex), Prime(decl.getRef(), jointIndex)));
						else exprs.add(Eq(decl.getRef(), Prime(decl.getRef(), jointIndex)));
					}
				}
				branchExprs.add(And(exprs));
			}
			final Expr<BoolType> expr = Or(branchExprs);
			VarPoolUtil.returnInt(tempVar);
			return StmtUnfoldResult.of(ImmutableList.of(expr), jointIndexing);
		}

		@Override
		public StmtUnfoldResult visit(OrtStmt ortStmt, VarIndexing indexing) {
			throw new UnsupportedOperationException();
		}

		@Override
		public StmtUnfoldResult visit(LoopStmt stmt, VarIndexing indexing) {
			throw new UnsupportedOperationException(String.format("Loop statement %s was not unrolled",stmt));
		}
	}
}
