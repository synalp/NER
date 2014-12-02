package xtof;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.Arrays;

import conll03.CoNLL03Ner;

import tools.CNConstants;
import tools.Histoplot;

/**
 * contains only the features from the gigaword
 * 
 * @author xtof
 *
 */
public class UnlabCorpus {
	final static int nmax = 1000000;

	public int featureSpaceSize;
	public int[][] feats;
	public int[][] feat2obs;
	public int[] feat2obsLengths;

	/**
	 * This is where the real job is done:
	 * - train a Stanford linear classifier on 20 training utts
	 * - load the features from the gigaword, create a linear model with these features
	 * - project the Stanford LC weights to initizalize this "big" LC, check that the accuracy is preserved
	 * - Compute the scores, train an initial GMM with classical EM
	 * - Build the mapping feature -> instances in the gigaword corpus
	 * - Optimize the risk in "fast mode", without knowing the scores
	 * 
	 * @param args
	 */
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
		
		// Optimize the risk using the assumption that the posterior stays constant
		m.buildFeat2ExampleIndex();
		c.optimizeRisk();
		
		// TODO: reparse the train + test to add the predicted class, then train the CRF and evaluate it
		
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
	
	public void buildFeat2ExampleIndex() {
		feat2obsLengths = new int[featureSpaceSize];
		Arrays.fill(feat2obsLengths, 0);
		feat2obs = new int[featureSpaceSize][100];
		for (int ex=0;ex<feats.length;ex++) {
			for (int f : feats[ex]) {
				int n=feat2obsLengths[f];
				if (n>=feat2obs[f].length) feat2obs[f] = Arrays.copyOf(feat2obs[f], feat2obs[f].length+100);
				feat2obs[f][n]=ex;
				feat2obsLengths[f]++;
			}
		}
		System.out.println("feat2obs index built");
	}
}
