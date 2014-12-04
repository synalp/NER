package xtof;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import xtof.LinearModel.TestResult;
import xtof.RiskMachine.GMMDiag;

import edu.stanford.nlp.classify.GeneralDataset;

public class LinearModelNoStanford {
	public float[] w;
	UnlabCorpus corp;
	Random rand = new Random();
	float lastRiskValue;
	
	public LinearModelNoStanford(UnlabCorpus c) {
		corp=c;
		w = new float[c.featureSpaceSize];
		setRandomWeights();
	}

	private void addInUnlabCorpus(int[][] fts) {
		int off=0;
		if (corp.feats!=null) {
			off=corp.feats.length;
			corp.feats=(int[][])Arrays.copyOf(corp.feats, corp.feats.length+fts.length);
		} else corp.feats = new int[fts.length][];
		for (int i=0;i<fts.length;i++) {
			corp.feats[off+i]=fts[i];
			for (int j=0;j<fts[i].length;j++) {
				if (fts[i][j]>=corp.featureSpaceSize) corp.featureSpaceSize=fts[i][j]+1;
			}
		}
	}
	public LinearModelNoStanford(Corpus c) {
		corp = new UnlabCorpus();
		corp.featureSpaceSize=0;
		if (c.trainData!=null) addInUnlabCorpus(c.getTrainFeats());
		if (c.testData!=null) addInUnlabCorpus(c.getTestFeats());
		if (c.unlabData!=null) addInUnlabCorpus(c.getUnlabFeats());
		if (c.devData!=null) addInUnlabCorpus(c.getDevFeats());
		corp.buildFeat2ExampleIndex();
		w=new float[corp.featureSpaceSize];
	}

	/**
	 * warning: this requires that all corpora have been loaded in the same order, and with the same size (except for the last one that can be bigger)
	 * @param lc
	 */
	public void projectTrainingWeights(LinearModel lc) {
		Arrays.fill(w, 0);
		for (int i=0;i<lc.getWeights().length;i++) {
			w[i]=(float)lc.getWeights()[i][0];
		}
	}
	
	public void setRandomWeights() {
		Random r = new Random();
		for (int i=0;i<w.length;i++) w[i]=r.nextFloat()-0.5f;
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
		float[] sc = computeAllScores();
		RiskMachine.GMMDiag gmm = new RiskMachine.GMMDiag();
		double[] post = gmm.train(sc);
		return getRiskFromGMM(gmm);
	}
	public void trainGMMnoinit(RiskMachine.GMMDiag gmm) {
		float[] sc = computeAllScores();
		gmm.trainEM(sc);
	}
	
	public float getRiskFromGMM(GMMDiag gmm) {
		double[] priors = {0.2,0.8};
		RiskMachine risk = new RiskMachine(priors);
		float r0 = risk.computeRisk(gmm);
		return r0;
	}
	public RiskMachine.GMMDiag initializeGMM() {
		float[] sc = computeAllScores();
		RiskMachine.GMMDiag gmm = new RiskMachine.GMMDiag();
		double[] post = gmm.train(sc);
		System.out.println("post "+post[0]+" "+post[1]);
		System.out.println("means "+gmm.mean0+" "+gmm.mean1);
		return gmm;
	}
	
	public interface ExecutedAtEachOptimIter {
		public void execute(LinearModelNoStanford mod);
	}
	public ArrayList<ExecutedAtEachOptimIter> executors = new ArrayList<LinearModelNoStanford.ExecutedAtEachOptimIter>();
	
	public RiskMachine.GMMDiag optimizeRiskWithApprox() {
		RiskMachine.GMMDiag gmm = initializeGMM();
		long initTime = System.currentTimeMillis();
		for (int i=0;i<Parms.nitersRiskOptimGlobal;i++) {
			// this is always the true risk, not the approximate one; so we can print it
			lastRiskValue = getRiskFromGMM(gmm);
			long curTime = System.currentTimeMillis();
			curTime-=initTime;
			System.out.println("riskiter "+i+" "+curTime+" "+lastRiskValue);
			optimizeRiskApproxLoop(Parms.nitersRiskOptimApprox*i,gmm);
			trainGMMnoinit(gmm);
			for (ExecutedAtEachOptimIter ex : executors) ex.execute(this);
		}
		return gmm;
	}
	public RiskMachine.GMMDiag optimizeRiskWithoutApprox() {
		RiskMachine.GMMDiag gmm = initializeGMM();
		long initTime = System.currentTimeMillis();
		for (int i=0;i<Parms.nitersRiskOptim;i++) {
			lastRiskValue = getRiskFromGMM(gmm);
			long curTime = System.currentTimeMillis();
			curTime-=initTime;
			System.out.println("riskiter "+i+" "+curTime+" "+lastRiskValue);
			int feat = rand.nextInt(corp.featureSpaceSize);
			w[feat] += Parms.finiteDiffDelta;
			trainGMMnoinit(gmm);
			float r1=getRiskFromGMM(gmm);
			float grad = (r1-lastRiskValue)/Parms.finiteDiffDelta;
			float delta = -grad*Parms.gradientStep;
			w[feat] += delta-Parms.finiteDiffDelta;
			trainGMMnoinit(gmm);
		}
		return gmm;
	}
	public RiskMachine.GMMDiag optimizeRiskApproxLoop(int iteroffset, RiskMachine.GMMDiag gmm) {
		for (int i=0;i<Parms.nitersRiskOptimApprox;i++) {
			int feat = rand.nextInt(corp.featureSpaceSize);
			w[feat] += Parms.finiteDiffDelta;
			float m0=gmm.mean0,m1=gmm.mean1,v0=gmm.var0,v1=gmm.var1,g0=gmm.gconst0,g1=gmm.gconst1;
			RiskMachine.updateGMMAfterRiskGradientStep(gmm, corp.feats.length, corp.feat2obs[feat], Parms.finiteDiffDelta);
			float r1=getRiskFromGMM(gmm);
			gmm.mean0=m0; gmm.mean1=m1; gmm.var0=v0; gmm.var1=v1; gmm.gconst0=g0; gmm.gconst1=g1;
			float grad = (r1-lastRiskValue)/Parms.finiteDiffDelta;
			float delta = -grad*Parms.gradientStep;
			w[feat] += delta-Parms.finiteDiffDelta;
			RiskMachine.updateGMMAfterRiskGradientStep(gmm, corp.feats.length, corp.feat2obs[feat], delta);
			// warning: this risk comes from an approximated value, so its best not to print it
			lastRiskValue=getRiskFromGMM(gmm);
		}
		return gmm;
	}
	public void save(String f) {
		try {
			ObjectOutputStream fo = new ObjectOutputStream(new FileOutputStream(f));
			fo.writeObject(w);
			fo.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
