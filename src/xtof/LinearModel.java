package xtof;

import java.util.HashMap;
import java.util.Random;

import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.util.Pair;

public class LinearModel {
	LinearClassifier model;
	
	public double[][] getWeights() {
		return model.weights();
	}
	
	/** used to do 10-fold cross-validation
	 * 
	 */
	public static GeneralDataset[] getTrainDev(int part, GeneralDataset data) {
		GeneralDataset[] res = {null,null};
		if (part>9) throw new Error("retrainPart only support 10-fold cross-validation for now");
		int n = data.size();
		int nPerPart = n/10;
		int testdeb=nPerPart*part;
		int testend=nPerPart*(part+1);
		if (part==9) testend=n;
		Pair<GeneralDataset, GeneralDataset> splitData = data.split(testdeb,testend);
		GeneralDataset trainData=splitData.first;
		GeneralDataset devData=splitData.second;
		res[0]=trainData;
		res[1]=devData;
		return res;
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
	
	public TestResult test(GeneralDataset data) {
		final int[][] feats = data.getDataArray();
		int[] refs = data.getLabelsArray();
		double[][] w = model.weights();
		
		TestResult res = new TestResult();
		for (int i=0;i<feats.length;i++) {
			double sc=0;
			for (int j=0;j<feats[i].length;j++) {
				sc+=w[feats[i][j]][0];
			}
			int rec=0;
			if (sc<0) rec=1;
			res.addRec(refs[i], rec);
		}
		return res;
	}
	
	public static class TestResult {
		int ref0rec0=0, ref0rec1=0, ref1rec0=0, ref1rec1=0;
		float tp,fp,fn,tn;
		public void addRec(int ref, int rec) {
			if (ref==0&&rec==0) ref0rec0++;
			else if (ref==0&&rec==1) ref0rec1++;
			else if (ref==1&&rec==0) ref1rec0++;
			else if (ref==1&&rec==1) ref1rec1++;
		}
		public float getAcc() {
			calcStats();
			float acc = (tp+tn)/(tp+fp+tn+fn);
			return acc;
		}
		private void calcStats() {
			// which class is "negative" ?
			int nref0=ref0rec0+ref0rec1;
			int nref1=ref1rec0+ref1rec1;
			if (nref0>nref1) {
				// neg is 0
				tp=ref1rec1; fp=ref0rec1; fn=ref1rec0; tn=ref0rec0;
			} else {
				// neg is 1
				tp=ref0rec0; fp=ref1rec0; fn=ref0rec1; tn=ref1rec1;
			}
		}
		public float getF1() {
			calcStats();
			float prec=tp/(tp+fp);
			float reca=tp/(tp+fn);
			float f1=(tp+tp)/(tp+tp+fp+fn);
			return f1;
		}
		public String toString() {return "F1= "+getF1()+" n= "+(tp+fp+tn+fn);}
		public boolean isSimilar(TestResult r) {
			float diff = getF1()-r.getF1();
			if (diff<-0.02 || diff>0.02) return false;
			return true;
		}
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
		TestResult acc = test(data);
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
