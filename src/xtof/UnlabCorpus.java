package xtof;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.Arrays;

import conll03.CoNLL03Ner;

import tools.CNConstants;
import tools.Histoplot;
import xtof.Coresets.GMMDiag;

/**
 * contains only the features from the gigaword
 * 
 * @author xtof
 *
 */
public class UnlabCorpus {
	public int featureSpaceSize;
	public int[][] feats;

	public static void main(String args[]) {
		// first train the classifier to get good initial weights
		CoNLL03Ner conll = new CoNLL03Ner();
		// warning: its impossible to change 20 to another number of sentences
		// because the features loaded next in loadFeatureFile() have been computed
		// with a "train" pre-corpus of only 20 sentences.
		// So, the feature indexes will differ...
        conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, 20, CNConstants.CHAR_NULL);
		Corpus ctrain = new Corpus("conll.pn.tab.LC.train", null, null, null);
		LinearModel mod=LinearModel.train(ctrain.columnDataClassifier, ctrain.trainData);
		float acc = mod.test(ctrain.trainData);
		System.out.println("acc LC train "+acc+" "+mod.getWeights().length);
		
		UnlabCorpus m = loadFeatureFile();
		LinearModelNoStanford c = new LinearModelNoStanford(m);
		// project training weights onto these weigths
		// the feats index are the same, because both train and unlab have been created in the same order
		for (int i=0;i<mod.getWeights().length;i++) {
			c.w[i]=(float)mod.getWeights()[i][0];
		}
		float acc2 = c.test(ctrain.trainData);
		System.out.println("acc2 LC train "+acc2+" "+c.w.length);
		
		float[] sc = c.computeAllScores();
		RiskMachine.GMMDiag gmm = new RiskMachine.GMMDiag();
		double[] post = gmm.train(sc);
		System.out.println("post "+post[0]+" "+post[1]);
		System.out.println("means "+gmm.mean0+" "+gmm.mean1);
		
		/*
		 * les post estimes ici sont completement faux: on trouve du presque 50%
		 * alors que les priors calcules sur le train de 20 phrases donnent 14%
		 */
		
		Histoplot.showit(sc);
		
		
//		Coresets cs = new Coresets();
//		cs.buildcoreset(sc,100);
	}

	public static UnlabCorpus loadFeatureFile() {
		System.out.println("loading gigaword features...");
		UnlabCorpus c = new UnlabCorpus();
		int nread = 0;
		c.featureSpaceSize=0;
		try {
			// this file has been saved in TestGigaword
			DataInputStream g = new DataInputStream(new FileInputStream("unlabfeats.dat"));
			final int nmax = 800;
			c.feats = new int[nmax][];
			for (int i=0;i<nmax;i++) {
				int nf = g.readInt();
				nread++;
				c.feats[i]=new int[nf];
				for (int j=0;j<nf;j++) {
					c.feats[i][j]=g.readInt();
					if (c.feats[i][j]>=c.featureSpaceSize) c.featureSpaceSize=c.feats[i][j]+1;
				}
			}
			g.close();
		} catch (EOFException e) {
			c.feats=Arrays.copyOf(c.feats, nread);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("loading done "+nread);
		return c;
	}
}
