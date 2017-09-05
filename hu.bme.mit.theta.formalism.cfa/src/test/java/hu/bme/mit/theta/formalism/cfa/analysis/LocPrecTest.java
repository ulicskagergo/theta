package hu.bme.mit.theta.formalism.cfa.analysis;

import org.junit.Assert;
import org.junit.Test;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.formalism.cfa.CFA;
import hu.bme.mit.theta.formalism.cfa.CFA.Builder;
import hu.bme.mit.theta.formalism.cfa.CFA.Loc;
import hu.bme.mit.theta.formalism.cfa.analysis.prec.GlobalCfaPrec;
import hu.bme.mit.theta.formalism.cfa.analysis.prec.LocalCfaPrec;

public class LocPrecTest {

	public static class PrecStub implements Prec {

	}

	private final PrecStub p0 = new PrecStub();
	private final PrecStub p1 = new PrecStub();
	private final PrecStub p2 = new PrecStub();

	@Test
	public void testConstLocPrec() {
		final GlobalCfaPrec<PrecStub> cp = GlobalCfaPrec.create(p1);
		final GlobalCfaPrec<PrecStub> r1 = cp.refine(p1);
		final GlobalCfaPrec<PrecStub> r2 = cp.refine(p2);
		final GlobalCfaPrec<PrecStub> r3 = r2.refine(p2);

		Assert.assertTrue(cp == r1);
		Assert.assertTrue(r1 != r2);
		Assert.assertTrue(r2 == r3);
	}

	@Test
	public void testConstLocPrecEquals() {
		final GlobalCfaPrec<PrecStub> cp1 = GlobalCfaPrec.create(p1);
		final GlobalCfaPrec<PrecStub> cp2 = GlobalCfaPrec.create(p1);
		final GlobalCfaPrec<PrecStub> cp3 = GlobalCfaPrec.create(p2);

		Assert.assertEquals(cp1, cp2);
		Assert.assertNotEquals(cp1, cp3);
		Assert.assertNotEquals(cp2, cp3);
	}

	@Test
	public void testGenericLocPrec() {
		final LocalCfaPrec<PrecStub> gp = LocalCfaPrec.create(p0);
		final Builder builder = CFA.builder();
		final Loc l1 = builder.createLoc("L1");
		final Loc l2 = builder.createLoc("L2");

		Assert.assertEquals(p0, gp.getPrec(l1));
		Assert.assertEquals(p0, gp.getPrec(l2));

		final LocalCfaPrec<PrecStub> r1 = gp.refine(l1, p1);

		Assert.assertEquals(p1, r1.getPrec(l1));
		Assert.assertEquals(p0, r1.getPrec(l2));

		final LocalCfaPrec<PrecStub> r2 = r1.refine(l2, p2);

		Assert.assertEquals(p1, r2.getPrec(l1));
		Assert.assertEquals(p2, r2.getPrec(l2));
	}

	@Test
	public void testGenericLocPrecEquals() {
		final LocalCfaPrec<PrecStub> gp0 = LocalCfaPrec.create(p0);
		final LocalCfaPrec<PrecStub> gp1 = LocalCfaPrec.create(p0);
		final LocalCfaPrec<PrecStub> gp2 = LocalCfaPrec.create(p1);

		Assert.assertEquals(gp0, gp1);
		Assert.assertNotEquals(gp0, gp2);
		Assert.assertNotEquals(gp1, gp2);

		final Builder builder = CFA.builder();
		final Loc l1 = builder.createLoc("L1");
		final Loc l2 = builder.createLoc("L2");

		final LocalCfaPrec<PrecStub> gp0r0 = gp0.refine(l1, p1);
		final LocalCfaPrec<PrecStub> gp1r0 = gp1.refine(l1, p1);

		Assert.assertEquals(gp0r0, gp1r0);

		final LocalCfaPrec<PrecStub> gp0r1 = gp0r0.refine(l2, p1);
		final LocalCfaPrec<PrecStub> gp1r1 = gp1r0.refine(l2, p2);

		Assert.assertNotEquals(gp0r1, gp1r1);
	}

	@Test
	public void testGenericLocPrecEquals2() {
		final Builder builder = CFA.builder();
		final Loc l1 = builder.createLoc("L1");

		final LocalCfaPrec<PrecStub> original = LocalCfaPrec.create(p0);
		final LocalCfaPrec<PrecStub> refined = original.refine(l1, p1);
		final LocalCfaPrec<PrecStub> refinedBack = refined.refine(l1, p0);

		Assert.assertEquals(original, refinedBack);

	}
}
