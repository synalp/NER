package xtof.tests;

import java.util.Arrays;

import tools.Histoplot;
import xtof.Corpus;
import xtof.LinearModel;
import xtof.LinearModelNoStanford;
import xtof.Parms;
import xtof.RiskMachine;
import xtof.UnlabCorpus;

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
		double[] priors = {0.2,0.8};
		RiskMachine r = new RiskMachine(priors);
	
		// First, train the LC on the train and computes its risk: this is our objective,
		// we want that after optimization the final risk gets close to this one
		// No ! because robj is computed on the train, while the optimized risk is computed on the unlab
		Corpus c = new Corpus("conll.pn.tab.LC.train", null, null, null);
		LinearModel mod0=LinearModel.train(c.columnDataClassifier, c.trainData);
		float robj;
		{
			float[] scores = new float[c.trainData.size()];
			for (int i=0;i<scores.length;i++) {
				int[] fs = c.getTrainFeats()[i];
				scores[i]=mod0.getSCore(fs);
			}
			robj = r.computeRisk(scores,new RiskMachine.GMMDiag());
		}
		mod0.randomizeWeights(0.7f);
		int nfeats = mod0.getWeights().length;
		System.out.println("nfeats "+nfeats);
		
		UnlabCorpus.nmax=500;
		UnlabCorpus m = UnlabCorpus.loadFeatureFile();
		m.buildFeat2ExampleIndex();
		LinearModelNoStanford mod1 = new LinearModelNoStanford(m);
		Arrays.fill(mod1.w, 0);
		for (int i=0;i<mod0.getWeights().length&&i<mod1.w.length;i++) {
			mod1.w[i]=(float)mod0.getWeights()[i][0];
		}
		float[] sc = mod1.computeAllScores();
		float r0 = r.computeRisk(sc,new RiskMachine.GMMDiag());
		System.out.println("risk "+r0);
		
		Parms.nitersRiskOptim=100000;
		mod1.optimizeRiskWithApprox();
		sc = mod1.computeAllScores();
		float r1 = r.computeRisk(sc,new RiskMachine.GMMDiag());
		
		System.out.println("risk gain "+r0+" "+r1+" "+robj);
		
//		float[] scores = new float[c.trainData.size()];
//		for (int i=0;i<scores.length;i++) {
//			int[] fs = c.getTrainFeats()[i];
//			scores[i]=mod0.getSCore(fs);
//		}
//		Parms.nitersRiskOptim=50;
//		Parms.finiteDiffDelta=0.01f;
//		Parms.gradientStep=10f;
//		float gain = mod0.optimizeRisk(c.trainData);
//		double[] priors = {0.2,0.8};
//		RiskMachine r = new RiskMachine(priors);
//		float r0 = r.computeRisk(scores,new RiskMachine.GMMDiag());
//		System.out.println("risk "+r0+" gain "+gain);
//		Histoplot.showit(scores);
	}
}
