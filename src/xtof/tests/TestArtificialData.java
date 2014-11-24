package xtof.tests;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;

import edu.emory.mathcs.backport.java.util.Arrays;

import tools.CNConstants;
import tools.Histoplot;
import xtof.Corpus;
import xtof.LinearModel;
import xtof.Parms;
import xtof.RiskMachine;

public class TestArtificialData {

	public static void main(String[] args) {
		TestArtificialData m = new TestArtificialData();
		m.genArtificialDataLC("artdat.train", 1000, 0.2f);
		m.genArtificialDataLC("artdat.test", 500, 0.2f);
		Corpus c = new Corpus("artdat.train", null, null, "artdat.test");
		
		// check that training of linear model gives 100% acc on this simple data
		LinearModel mod=LinearModel.train(c.columnDataClassifier, c.trainData);
		float acc = mod.test(c.testData);
		System.out.println("trained acc "+acc);
		if (acc<1) throw new Error("trained acc not 100% "+acc);

		float riskTrain=-1;
		{
			int[][] feats = c.getTrainFeats();
			float[] sc = new float[feats.length];
			for (int i=0;i<sc.length;i++) sc[i]=mod.getSCore(feats[i]);
			double[] priors = {0.2,0.8};
			RiskMachine rr = new RiskMachine(priors);
			riskTrain=rr.computeRisk(sc);
		}
		
		// check that random weights give less than 100% of acc
		mod.randomizeWeights();
		acc = mod.test(c.testData);
		System.out.println("random acc "+acc);
		if (acc==1||acc==0) throw new Error("random acc weird "+acc);
		
		// check that the risk with random weights is higher than the risk with optimal weights
		float riskRand=-1;
		{
			int[][] feats = c.getTrainFeats();
			float[] sc = new float[feats.length];
			for (int i=0;i<sc.length;i++) sc[i]=mod.getSCore(feats[i]);
			double[] priors = {0.2,0.8};
			RiskMachine rr = new RiskMachine(priors);
			riskRand=rr.computeRisk(sc);
		}
		System.out.println("train-rand risks "+riskTrain+" "+riskRand);
		if (riskTrain>=riskRand) throw new Error("error risks");
		
		// check that the risk gives the same values when we invert priors
		int[][] feats = c.getTrainFeats();
		float[] sc = new float[feats.length];
		for (int i=0;i<sc.length;i++) sc[i]=mod.getSCore(feats[i]);
		double[] priors = {0.2,0.8};
		RiskMachine rr = new RiskMachine(priors);
		float risk1=rr.computeRisk(sc);
		priors[0]=0.8; priors[1]=0.2;
		RiskMachine rr2 = new RiskMachine(priors);
		float risk2=rr2.computeRisk(sc);
		System.out.println("RISK "+risk1+" "+risk2);
		if (risk1!=risk2) throw new Error("risk sensible to priors order "+risk1+" "+risk2);
		
		// visualize means and histogram of scores
//		float[] means = rr.getMeans();
//		double[] post = rr.getPosteriors();
//		System.out.println("means "+Arrays.toString(means)+" "+Arrays.toString(post));
//		Histoplot.showit(sc);
		
		// check that risk optimization makes the risk decrease
		Parms.nitersRiskOptim=100;
		mod.optimizeRisk(c.trainData);
		RiskMachine rr3 = new RiskMachine(priors);
		for (int i=0;i<sc.length;i++) sc[i]=mod.getSCore(feats[i]);
		float risk3=rr3.computeRisk(sc);
		if (risk3>=risk2) throw new Error("optim risk does not make risk decrease "+risk2+" "+risk3);
		acc = mod.test(c.testData);
		System.out.println("final acc "+acc);
		if (acc>0.1&&acc<0.9) throw new Error("Acc after risk optim too bad");
	}

	void genArtificialDataLC(String outfile, int nex, float priorPN) {
		try {
			OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream(outfile),CNConstants.UTF8_ENCODING);
			Random r = new Random();
			for (int i=0;i<nex;i++) {
				// very basic random generation with only 2 features
				if (r.nextFloat()<priorPN) {
					outFile.append("pn\t");
					outFile.append("W1\t");
					outFile.append("W1\t");
					outFile.append("W1\t");
					outFile.append("C1\t");
					outFile.append("W1");
				} else {
					outFile.append("O\t");
					outFile.append("W2\t");
					outFile.append("W2\t");
					outFile.append("W2\t");
					outFile.append("C2\t");
					outFile.append("W2");
				}
				outFile.append('\n');
			}
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
