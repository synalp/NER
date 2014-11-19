package test;

import tools.CNConstants;
import tools.GeneralConfig;
import conll03.CoNLL03Ner;

public class AutoTests {
	CoNLL03Ner conll;
	public static float initR=0, finalR=0;
	private static boolean autoTestOn=false;
	
	public static void main(String args[]) throws Exception {
//		throw new Exception("Just an example of test that fails");
//		System.out.println("example of test that succeeds");
		autoTestOn=true;
		AutoTests m = new AutoTests();
		m.conll = new CoNLL03Ner();
		if (args.length>0) {
			String xmstanford = args[0];
			GeneralConfig.forceXmxStanford=xmstanford;
		}
		
		m.testCRFquick();
		m.testGigaquick();
                //test the gmm with generated data
                TestingGMM.TestingGMMWithGeneratedData();
                //test gmm with ester gaussians
                //TestingGMM.TestingGMMWithClassifierWeights();
                //test gmm with conllmulticlass gaussians
               //TestingGMM.TestingGMMCoNLLData();
	}
	
	/**
	 * Quickly test on a small dataset that the baseline CRF is learning correctly.
	 * Also check that adding a new feature column (in this case, an "oracle" feature) is correctly taken into account
	 */
	void testCRFquick() throws Exception {
        conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true,20,CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.ALL, "test", true,CNConstants.CHAR_NULL);
    	float f1=conll.trainStanfordCRF(CNConstants.ALL, false, false,false);
    	// check that the CRF gives reasonnable F1
    	if (f1<40f||f1>100f) throw new Exception("CRF F1 with 20 training utts is too low "+f1);
    	
    	// Test by training a CRF on a small training corpus with an extra-column-feature that contains oracle class
    	// and check that the F1 of the CRF is close to 100%
    	conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true, 20, "autotest_oracle");
    	conll.generatingStanfordInputFiles(CNConstants.ALL, "test", true,"autotest_oracle");
    	float f1_oracle=conll.trainStanfordCRF(CNConstants.ALL, false, true,false);
    	System.out.println("testCRFF1s "+f1+" "+f1_oracle);
    	if (f1_oracle<=f1) throw new Exception("ORACLE F1 is not better than F1");
    	if (f1_oracle<70) throw new Exception("ORACLE F1 is too low "+f1_oracle);
	}
	
	/**
	 * - Why do "macro-averaged F1" alternate between 48 and 87% ?? ==> because tests are run on train and on test
	 * - How are the priors estimated ? Is it fair ? ==> we don't care, the plan is to tune the priors as another parameter
	 * - Why do the nb of examples in test set vary: 921, 1400, 921... ==> because tests are run on train and on test
	 * 
	 * @throws Exception
	 */
	void testGigaquick() throws Exception {
        GeneralConfig.corpusGigaDir="res/";
        GeneralConfig.corpusGigaTrain="giga1000.conll03";
        conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, 20, CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "gigaw", false,CNConstants.CHAR_NULL);
        conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,false,Integer.MAX_VALUE,Integer.MAX_VALUE,10);
        if (finalR-initR>=0) throw new Exception("WeakSup R does not decrease: "+initR+" "+finalR);
	}
	
	/**
	 * TODO: Pretty weird: it looks like posteriors and priors are inter-exchanged ???
	 * 
	 * @return true if test is a success
	 */
	public static void checkPosteriors(double[] post, float[] priors) {
		if (!autoTestOn) return;
		double nex = post[0]+post[1];
		double realpost0 = post[0]/nex;
		double realpost1 = post[1]/nex;
		double err = Math.abs(realpost0-priors[0])+Math.abs(realpost1-priors[1]);
		System.out.println("checkpost "+realpost0+" "+realpost1+" .. "+priors[0]+" "+priors[1]+" "+err);
		if (err>0.4) {
			throw new Error("TEST ERROR: posteriors differ from priors ");
		}
	}
}
