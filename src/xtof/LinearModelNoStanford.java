package xtof;

import java.util.Random;

import edu.stanford.nlp.classify.GeneralDataset;

public class LinearModelNoStanford {
	float[] w;
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

	public float test(GeneralDataset data) {
		final int[][] feats = data.getDataArray();
		int[] refs = data.getLabelsArray();
		
		int nok=0;
		for (int i=0;i<feats.length;i++) {
			double sc=0;
			for (int j=0;j<feats[i].length;j++) {
				sc+=w[feats[i][j]];
			}
			int rec=0;
			if (sc<0) rec=1;
			if (rec==refs[i]) nok++;
		}
		float acc = (float)nok/(float)feats.length;
		return acc;
	}
}
