package xtof;

import java.util.Random;

import xtof.LinearModel.TestResult;

import edu.stanford.nlp.classify.GeneralDataset;

public class LinearModelNoStanford {
	public float[] w;
	UnlabCorpus corp;
	
	public void setRandomWeights() {
		Random r = new Random();
		for (int i=0;i<w.length;i++) w[i]=r.nextFloat()-0.5f;
	}
	
	public LinearModelNoStanford(UnlabCorpus c) {
		corp=c;
		w = new float[c.featureSpaceSize];
		setRandomWeights();
	}
	
	public float[] computeAllScores() {
		float[] sc = new float[corp.feats.length];
		for (int i=0;i<sc.length;i++) {
			sc[i]=0;
			for (int j=0;j<corp.feats[i].length;j++) sc[i]+=w[corp.feats[i][j]];
		}
		return sc;
	}

	public TestResult test(GeneralDataset data) {
		final int[][] feats = data.getDataArray();
		int[] refs = data.getLabelsArray();
		
		TestResult res = new TestResult();
		for (int i=0;i<feats.length;i++) {
			double sc=0;
			for (int j=0;j<feats[i].length;j++) {
				sc+=w[feats[i][j]];
			}
			int rec=0;
			if (sc<0) rec=1;
			res.addRec(refs[i], rec);
		}
		return res;
	}
	public int[] predict(GeneralDataset data) {
		final int[][] feats = data.getDataArray();
		int[] rec = new int[feats.length];
		
		for (int i=0;i<feats.length;i++) {
			double sc=0;
			for (int j=0;j<feats[i].length;j++) {
				sc+=w[feats[i][j]];
			}
			rec[i]=0;
			if (sc<0) rec[i]=1;
		}
		return rec;
	}
	
	/**
	 * compute risk from scratch: retrain a GMM with EM
	 * 
	 * @return
	 */
	public float computeRisk() {
		double[] priors = {0.2,0.8};
		RiskMachine risk = new RiskMachine(priors);
		float[] sc = computeAllScores();
		RiskMachine.GMMDiag gmm = new RiskMachine.GMMDiag();
		double[] post = gmm.train(sc);
		float r0 = risk.computeRisk(gmm);
		return r0;
	}
	public void trainGMMnoinit(RiskMachine.GMMDiag gmm) {
		double[] priors = {0.2,0.8};
		RiskMachine risk = new RiskMachine(priors);
		float[] sc = computeAllScores();
		gmm.trainEM(sc);
	}
	
	public RiskMachine.GMMDiag optimizeRisk() {
		Random rand = new Random();
		double[] priors = {0.2,0.8};
		RiskMachine risk = new RiskMachine(priors);
		float[] sc = computeAllScores();
		RiskMachine.GMMDiag gmm = new RiskMachine.GMMDiag();
		double[] post = gmm.train(sc);
		System.out.println("post "+post[0]+" "+post[1]);
		System.out.println("means "+gmm.mean0+" "+gmm.mean1);
		
		float r0 = risk.computeRisk(gmm);
		System.out.println("riskiter -1 "+r0);
		for (int i=0;i<Parms.nitersRiskOptim;i++) {
			int feat = rand.nextInt(corp.featureSpaceSize);
			w[feat] += Parms.finiteDiffDelta;
			float m0=gmm.mean0,m1=gmm.mean1,v0=gmm.var0,v1=gmm.var1,g0=gmm.gconst0,g1=gmm.gconst1;
			RiskMachine.updateGMMAfterRiskGradientStep(gmm, corp.feats.length,
					corp.feat2obs[feat], Parms.finiteDiffDelta);
			float r1=risk.computeRisk(gmm);
			gmm.mean0=m0; gmm.mean1=m1; gmm.var0=v0; gmm.var1=v1; gmm.gconst0=g0; gmm.gconst1=g1;
			float grad = (r1-r0)/Parms.finiteDiffDelta;
			float delta = -grad*Parms.gradientStep;
			RiskMachine.updateGMMAfterRiskGradientStep(gmm, corp.feats.length,
					corp.feat2obs[feat], delta);
			r0=risk.computeRisk(gmm);
			System.out.println("riskiter "+i+" "+r0);
		}
		return gmm;
	}
}
