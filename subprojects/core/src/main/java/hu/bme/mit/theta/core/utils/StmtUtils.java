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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Type;

/**
 * Utility functions related to statements.
 */
public final class StmtUtils {

	private StmtUtils() {
	}

	/**
	 * Get variables appearing in a statement
	 *
	 * @param stmt Statement
	 * @return Variables
	 */
	public static Set<VarDecl<? extends Type>> getVars(final Stmt stmt) {
		final Set<VarDecl<?>> vars = new HashSet<>();
		stmt.accept(VarCollectorStmtVisitor.getInstance(), vars);
		return vars;
	}

	/**
	 * Get variables appearing in statements
	 *
	 * @param stmts Statements
	 * @return Variables
	 */
	public static Set<VarDecl<? extends Type>> getVars(final Iterable<? extends Stmt> stmts) {
		final Set<VarDecl<?>> vars = new HashSet<>();
		stmts.forEach(s -> s.accept(VarCollectorStmtVisitor.getInstance(), vars));
		return vars;
	}

	public static Set<VarDecl<? extends Type>> getWrittenVars(final Stmt stmt) {
		final Set<VarDecl<?>> vars = new HashSet<>();
		stmt.accept(WrittenVarCollectorStmtVisitor.getInstance(), vars);
		return vars;
	}

	/**
	 * Unfold a statement into expressions with a given indexing
	 *
	 * @param stmt     Statement
	 * @param indexing Indexing
	 * @return Expressions and new indexing
	 */
	public static StmtUnfoldResult toExpr(final Stmt stmt, final VarIndexing indexing) {
		return StmtToExprTransformer.toExpr(stmt, indexing);
	}

	/**
	 * Unfold statements into expressions with a given indexing
	 *
	 * @param stmts    Statements
	 * @param indexing Indexing
	 * @return Expressions and new indexing
	 */
	public static StmtUnfoldResult toExpr(final List<? extends Stmt> stmts, final VarIndexing indexing) {
		return StmtToExprTransformer.toExpr(stmts, indexing);
	}

}