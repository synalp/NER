package xtof.tests;

import java.util.Arrays;

import tools.CNConstants;
import tools.Histoplot;
import utils.Wait;
import xtof.Corpus;
import xtof.LinearModel;
import xtof.LinearModelNoStanford;
import xtof.Parms;
import xtof.RiskMachine;
import xtof.RiskMachine.GMMDiag;
import xtof.UnlabCorpus;
import xtof.LinearModel.TestResult;
import conll03.CoNLL03Ner;
import edu.stanford.nlp.classify.GeneralDataset;

public class ConllXP {
	static final int nuttsLCtraining = 20;
	static final int nuttsCRFtraining = 20;
	
	public static void main(String[] args) {
//		xpfull();
		xpLConly();
	}
	
	public static void xpLConly() {
		// starting from weights trained on 20 utts, study the decrease of the risk using optimization on the train+test
		CoNLL03Ner conll = new CoNLL03Ner();
		String trainfile = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, 20, CNConstants.CHAR_NULL);
        String testfile  = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "test", false, Integer.MAX_VALUE, CNConstants.CHAR_NULL);
		Corpus ctrain = new Corpus(trainfile, null, null, testfile);
		LinearModel lcmod=LinearModel.train(ctrain.columnDataClassifier, ctrain.trainData);
		LinearModelNoStanford lcbig = new LinearModelNoStanford(ctrain);
		lcbig.projectTrainingWeights(lcmod);
		Parms.nitersRiskOptimApprox=1;
		lcbig.optimizeRiskWithApprox();
//		lcbig.optimizeRiskWithoutApprox();
	}
	public static void curveSupervisedRisk() {
		// curve of the risk as a function of nb of training utts
		CoNLL03Ner conll = new CoNLL03Ner();
		double[] priors = {0.2,0.8};
		RiskMachine r = new RiskMachine(priors);
		final int[] nutts = {20,50,100,250,500,1000,2000,5000};
		for (int i=0;i<nutts.length;i++) {
	        String trainfile = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, nutts[i], CNConstants.CHAR_NULL);
	        String testfile  = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "test", false, Integer.MAX_VALUE, CNConstants.CHAR_NULL);
			Corpus ctrain = new Corpus(trainfile, null, null, testfile);
			LinearModel lcmod=LinearModel.train(ctrain.columnDataClassifier, ctrain.trainData);
			float[] scores = new float[ctrain.testData.size()];
			for (int ii=0;ii<scores.length;ii++) {
				int[] fs = ctrain.getTestFeats()[ii];
				scores[ii]=lcmod.getSCore(fs);
			}
			float risk = r.computeRisk(scores,new RiskMachine.GMMDiag());
			System.out.println("trainriskcurve "+nutts[i]+" "+risk);
		}
	}
	
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
	public static void xpfull() {
		// first train the classifier to get good initial weights
		CoNLL03Ner conll = new CoNLL03Ner();
		// warning: its impossible to change 20 to another number of sentences
		// because the features loaded next in loadFeatureFile() have been computed
		// with a "train" pre-corpus of only 20 sentences.
		// So, the feature indexes will differ...
        String trainfile = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, nuttsLCtraining, CNConstants.CHAR_NULL);
        String testfile  = conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "test", false, Integer.MAX_VALUE, CNConstants.CHAR_NULL);
		Corpus ctrain = new Corpus(trainfile, null, null, testfile);
		System.out.println("corpus loaded "+trainfile+" "+testfile);
		// load unlab gigaword corpus
		UnlabCorpus m = UnlabCorpus.loadFeatureFile();
		m.buildFeat2ExampleIndex();
		// create the "big" LC
		LinearModelNoStanford c = new LinearModelNoStanford(m);
		
		// reparse the train + test to add the predicted class, then train the CRF and evaluate it
        conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true, nuttsCRFtraining, CNConstants.CHAR_NULL);
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
				
				// repeat this approximate optim to recompute a full EM 100 times
				Parms.nitersRiskOptim=10000;
				GMMDiag gmm = c.optimizeRiskWithApprox();
				for (int j=0;j<100;j++) {
					// Optimize the risk using the assumption that the posterior stays constant
					c.trainGMMnoinit(gmm);
					c.optimizeRiskApproxLoop(0,gmm);
				}
				System.out.println("risk xval "+xval+" "+c.computeRisk());
				
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
	        String enhancedTrainfile = conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true, nuttsCRFtraining, CNConstants.TABLE_IN_UNLABCORPUS);
	        System.out.println("enhanced corpus for CRF "+enhancedTrainfile+" "+enhancedTestfile);
	        
	        // retrain the CRF
	    	f1=conll.trainStanfordCRF(CNConstants.ALL, false, true,false);
	    	System.out.println("enhancedCRF "+f1);
		}
	}


}
