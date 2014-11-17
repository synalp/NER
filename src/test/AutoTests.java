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
	}
	
	void testCRFquick() throws Exception {
        conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true,20,CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.ALL, "test", true,CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.ALL, "dev", true,CNConstants.CHAR_NULL);
    	float f1=conll.trainStanfordCRF(CNConstants.ALL, false, false,false);
    	if (f1<43f) throw new Exception("CRF F1 with 20 training utts is "+f1);
	}
	
	/**
	 * - Why do "macro-averaged F1" alternate between 48 and 87% ??
	 * - How are the priors estimated ? Is it fair ?
	 * - Why do the nb of examples in test set vary: 921, 1400, 921...
	 * - TODO: check that posteriors match priors
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
	
	public static void checkPosteriors(double[] post, float[] priors) {
		if (!autoTestOn) return;
		double nex = post[0]+post[1];
		double realpost0 = post[0]/nex;
		double realpost1 = post[1]/nex;
		System.out.println("checkpost "+realpost0+" "+realpost1+" .. "+priors[0]+" "+priors[1]);
	}
}
