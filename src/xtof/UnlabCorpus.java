package xtof;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.Arrays;

import conll03.CoNLL03Ner;
import edu.stanford.nlp.classify.GeneralDataset;

import tools.CNConstants;
import tools.Histoplot;
import xtof.LinearModel.TestResult;

/**
 * contains only the features from the gigaword
 * 
 * @author xtof
 *
 */
public class UnlabCorpus {
	public static int nmax = 100000;
	public static int[] LCrec;

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
	 * TODO: train LC on 500 sentences;
	 * add LC classes then train CRF on 20 sentences: is F1 improved ? risk ? 
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
        String trainfile = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, 20, CNConstants.CHAR_NULL);
        String testfile  = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "test", false, Integer.MAX_VALUE, CNConstants.CHAR_NULL);
		Corpus ctrain = new Corpus(trainfile, null, null, testfile);
		System.out.println("corpus loaded "+trainfile+" "+testfile);
		// load unlab gigaword corpus
		UnlabCorpus m = loadFeatureFile();
		m.buildFeat2ExampleIndex();
		// create the "big" LC
		LinearModelNoStanford c = new LinearModelNoStanford(m);
		
		// reparse the train + test to add the predicted class, then train the CRF and evaluate it
        conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true, 20, CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.ALL, "test", true, Integer.MAX_VALUE, CNConstants.CHAR_NULL);
    	float f1=conll.trainStanfordCRF(CNConstants.ALL, false, false,false);
    	System.out.println("baselineCRF "+f1);
    	
		int[] predsontrain = new int[ctrain.trainData.size()];
		int predsontrainidx=0;
		{
			float totf1ondev=0, totf1ontest=0;
			for (int xval=0;xval<10;xval++) {
				GeneralDataset[] tmpds = LinearModel.getTrainDev(xval, ctrain.trainData);
				GeneralDataset xvaltrain = tmpds[0];
				GeneralDataset xvaldev = tmpds[1];
				// train LC on train
				LinearModel tmpLC=LinearModel.train(ctrain.columnDataClassifier, xvaltrain);
				TestResult acc = tmpLC.test(xvaldev);
				totf1ondev+=acc.getF1();
				acc = tmpLC.test(ctrain.testData);
				totf1ontest+=acc.getF1();
				
				// project training weights onto these weigths
				// the feats index are the same, because both train and unlab have been created in the same order
				Arrays.fill(c.w, 0);
				for (int i=0;i<tmpLC.getWeights().length;i++) {
					c.w[i]=(float)tmpLC.getWeights()[i][0];
				}
				// check the accuracy after projection
				TestResult acc2 = c.test(ctrain.testData);
				System.out.println("check projectweights "+acc+" "+acc2+" "+ctrain.testData.size());
				if (!acc.isSimilar(acc2)) throw new Error("ERROR: projecting weights give difference acc");
				
				// Optimize the risk using the assumption that the posterior stays constant
				c.optimizeRiskWithApprox();
				
				// infer the predicted class on the dev
				int[] rec = c.predict(xvaldev);
				for (int i=0;i<rec.length;i++) predsontrain[predsontrainidx++]=rec[i];
			}
			totf1ondev/=10f;
			totf1ontest/=10f;
			// warning: there is quite a large diff between dev and test
			System.out.println("acc LC trainxvaldev "+totf1ondev);
			System.out.println("acc LC trainxvaltest "+totf1ontest);
		}
		
		{
			// retrain a LC on the full train to predict classes on the test
			LinearModel tmpLC=LinearModel.train(ctrain.columnDataClassifier,ctrain.trainData);
			Arrays.fill(c.w, 0);
			for (int i=0;i<tmpLC.getWeights().length;i++) {
				c.w[i]=(float)tmpLC.getWeights()[i][0];
			}
			TestResult acc = c.test(ctrain.testData);
			System.out.println("acc LC trainfulltest "+acc);
			// complete train + test corpus for the CRF with weaksup classes
			int[] rec = c.predict(ctrain.testData);
			UnlabCorpus.LCrec = rec;
	        String enhancedTestfile = conll.generatingStanfordInputFiles(CNConstants.ALL, "test", true, Integer.MAX_VALUE, CNConstants.TABLE_IN_UNLABCORPUS);
			UnlabCorpus.LCrec = predsontrain;
	        String enhancedTrainfile = conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true, 20,CNConstants.TABLE_IN_UNLABCORPUS);
	        System.out.println("enhanced corpus for CRF "+enhancedTrainfile+" "+enhancedTestfile);
	    	f1=conll.trainStanfordCRF(CNConstants.ALL, false, true,false);
	    	System.out.println("enhancedCRF "+f1);
		}
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
