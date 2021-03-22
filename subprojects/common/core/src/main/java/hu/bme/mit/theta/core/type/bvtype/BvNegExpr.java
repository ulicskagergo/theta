package hu.bme.mit.theta.core.type.bvtype;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.UnaryExpr;

import static hu.bme.mit.theta.core.utils.TypeUtils.castBv;

public final class BvNegExpr extends UnaryExpr<BvType, BvType> {

    private static final int HASH_SEED = 8325;
    private static final String OPERATOR_LABEL = "bvneg";

    private BvNegExpr(final Expr<BvType> op) {
        super(op);
    }

    public static BvNegExpr of(final Expr<BvType> op) {
        return new BvNegExpr(op);
    }

    public static BvNegExpr create(final Expr<?> op) {
        final Expr<BvType> newOp = castBv(op);
        return BvNegExpr.of(newOp);
    }

    @Override
    public BvType getType() {
        return getOp().getType();
    }

    @Override
    public BvLitExpr eval(final Valuation val) {
        final BvLitExpr opVal = (BvLitExpr) getOp().eval(val);
        return opVal.neg();
    }

    @Override
    public BvNegExpr with(final Expr<BvType> op) {
        if (op == getOp()) {
            return this;
        } else {
            return BvNegExpr.of(op);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof BvNegExpr) {
            final BvNegExpr that = (BvNegExpr) obj;
            return this.getOp().equals(that.getOp());
        } else {
            return false;
        }
    }

    @Override
    protected int getHashSeed() {
        return HASH_SEED;
    }

    @Override
    public String getOperatorLabel() {
        return OPERATOR_LABEL;
    }

}
