/**
 * 
 */
package xtof.tests;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Arrays;

import xtof.Coresets;
import xtof.RiskMachine;

/**
 * @author xtof
 *
 */
public class CoresetsTest {

	/**
	 * Test method for {@link xtof.Coresets#buildcoreset(float[], int)}.
	 */
	@Test
	public void testBuildcoreset() {
		float[] sc = new float[10000];
		Random rand = new Random();
		for (int i=0;i<sc.length;i++) sc[i]=rand.nextFloat()-0.5f;
		RiskMachine.GMMDiag gmm = new RiskMachine.GMMDiag();
		gmm.train(sc);
		float lfull = gmm.getLoglike(sc);
		Coresets cs = new Coresets();
		float[] sc2 = Arrays.copyOf(sc, sc.length);
		float[][] c = cs.buildcoreset(sc2, 20);
		Coresets.GMMDiag gmm2 = new Coresets.GMMDiag(c[1]);
		gmm2.train(c[0]);
		float lcore = gmm2.getLoglike(sc);
		System.out.println("loglikes "+lfull+" "+lcore+" "+sc.length);
		lcore-=lfull;
		// check that the difference in loglikes is smaller than 10
		assertTrue(lcore>-10&&lcore<10);
	}

}
