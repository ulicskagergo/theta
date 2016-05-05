package hu.bme.mit.inf.ttmc.cegar.visiblecegar.data;

import java.util.HashSet;
import java.util.Set;

import hu.bme.mit.inf.ttmc.cegar.common.data.AbstractSystemBase;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.formalism.sts.STS;

/**
 * Represents the visibility-based abstract system.
 */
public class VisibleAbstractSystem extends AbstractSystemBase {
	private final Set<VarDecl<? extends Type>> visibleVars;
	private final Set<VarDecl<? extends Type>> invisibleVars;
	private final Set<VarDecl<? extends Type>> cnfVars;

	public VisibleAbstractSystem(final STS system) {
		super(system);
		visibleVars = new HashSet<>();
		invisibleVars = new HashSet<>();
		cnfVars = new HashSet<>();
	}

	public Set<VarDecl<? extends Type>> getVisibleVars() {
		return visibleVars;
	}

	public Set<VarDecl<? extends Type>> getInvisibleVars() {
		return invisibleVars;
	}

	public Set<VarDecl<? extends Type>> getCNFVars() {
		return this.cnfVars;
	}

	public void makeVisible(final VarDecl<? extends Type> var) {
		if (invisibleVars.remove(var)) {
			visibleVars.add(var);
		} else {
			throw new RuntimeException("Variable " + var + " could not be made visible because it was not found.");
		}
	}
}