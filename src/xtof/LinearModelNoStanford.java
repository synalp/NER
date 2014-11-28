package xtof;

import java.util.Random;

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
}
