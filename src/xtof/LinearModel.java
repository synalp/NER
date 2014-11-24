package xtof;

import java.util.Random;

import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;

public class LinearModel {
	LinearClassifier model;
	
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
	
	public float test(ColumnDataClassifier cdc, GeneralDataset data) {
		int[][] feats = data.getDataArray();
		int[] refs = data.getLabelsArray();
		double[][] w = model.weights();
		System.out.println("debug w "+w.length+" "+w[0].length);
		
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
}
