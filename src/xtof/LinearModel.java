package xtof;

import java.util.Random;

import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;

public class LinearModel {
	LinearClassifier model;
	
	public double[][] getWeights() {
		return model.weights();
	}
	
	public static LinearModel train(ColumnDataClassifier cdc, GeneralDataset data) {
		LinearModel m = new LinearModel();
        m.model = (LinearClassifier) cdc.makeClassifier(data);
        return m;
	}
	
	public float getSCore(int[] feats) {
		double[][] w = model.weights();
		double sc=0;
		for (int j=0;j<feats.length;j++) {
			sc+=w[feats[j]][0];
		}
		return (float)sc;
	}
	
	public void randomizeWeights() {
		double[][] w = model.weights();
		Random r = new Random();
		for (int i=0;i<w.length;i++)
			for (int j=0;j<w[i].length;j++)
				w[i][j]=r.nextDouble();
	}
	
	public float test(GeneralDataset data) {
		final int[][] feats = data.getDataArray();
		int[] refs = data.getLabelsArray();
		double[][] w = model.weights();
		
		int nok=0;
		for (int i=0;i<feats.length;i++) {
			double sc=0;
			for (int j=0;j<feats[i].length;j++) {
				sc+=w[feats[i][j]][0];
			}
			int rec=0;
			if (sc<0) rec=1;
			if (rec==refs[i]) nok++;
		}
		float acc = (float)nok/(float)feats.length;
		return acc;
	}
	
	public void optimizeRisk(GeneralDataset data) {
		Random r = new Random();
		final int[][] feats = data.getDataArray();
		double[] priors = {0.2,0.8};
		float[] sc = new float[feats.length];
		double[][] w = model.weights();
		float prevRisk;
		{
			for (int i=0;i<sc.length;i++) sc[i]=getSCore(feats[i]);
			RiskMachine rr = new RiskMachine(priors);
			prevRisk=rr.computeRisk(sc,new RiskMachine.GMMDiag());
		}
		float acc = test(data);
		System.out.println("SCD iter -1 "+prevRisk+" acc "+acc);
		for (int i=0;i<Parms.nitersRiskOptim;i++) {
			int wi=r.nextInt(w.length);
			w[wi][0]+=Parms.finiteDiffDelta;
			w[wi][1]-=Parms.finiteDiffDelta;
			float newRisk;
			{
				for (int ii=0;ii<sc.length;ii++) sc[ii]=getSCore(feats[ii]);
				RiskMachine rr = new RiskMachine(priors);
				newRisk=rr.computeRisk(sc,new RiskMachine.GMMDiag());
			}
			float grad=(newRisk-prevRisk)/Parms.finiteDiffDelta;
			if (grad!=0) {
				w[wi][0]-=grad*Parms.gradientStep;
				w[wi][1]+=grad*Parms.gradientStep;
				{
					for (int ii=0;ii<sc.length;ii++) sc[ii]=getSCore(feats[ii]);
					RiskMachine rr = new RiskMachine(priors);
					prevRisk=rr.computeRisk(sc,new RiskMachine.GMMDiag());
				}
				acc = test(data);
				System.out.println("SCD iter "+i+" "+prevRisk+" acc "+acc);
			}
		}
	}
}
