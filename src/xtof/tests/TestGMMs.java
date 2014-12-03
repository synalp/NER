package xtof.tests;

import tools.Histoplot;
import xtof.Corpus;
import xtof.LinearModel;
import xtof.LinearModelNoStanford;
import xtof.Parms;
import xtof.RiskMachine;

/**
 * Test to understand why, when the risk decreases, this does not lead to a good F1.
 * 
 * With random weights:
 * [java] compR 1.2070829 -0.17998087 3.0196252 2.229154
   [java] risk 0.10138864

   With training:
   [java] compR 3.387907 -4.2259774 0.74917126 0.6993141
   [java] risk 1.72454E-8
 * 
 * 
 * @author xtof
 *
 */
public class TestGMMs {
	public static void main(String args[]) {
		Corpus c = new Corpus("conll.pn.tab.LC.train", null, null, null);
		LinearModel mod0=LinearModel.train(c.columnDataClassifier, c.trainData);
		int nfeats = mod0.getWeights().length;
		System.out.println("nfeats "+nfeats);
		mod0.randomizeWeights(0.5f);
		float[] scores = new float[c.trainData.size()];
		for (int i=0;i<scores.length;i++) {
			int[] fs = c.getTrainFeats()[i];
			scores[i]=mod0.getSCore(fs);
		}
		Parms.nitersRiskOptim=50;
		Parms.finiteDiffDelta=0.01f;
		Parms.gradientStep=10f;
		float gain = mod0.optimizeRisk(c.trainData);
		double[] priors = {0.2,0.8};
		RiskMachine r = new RiskMachine(priors);
		float r0 = r.computeRisk(scores,new RiskMachine.GMMDiag());
		System.out.println("risk "+r0+" gain "+gain);
		Histoplot.showit(scores);
	}
}
