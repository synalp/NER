package test;

import tools.CNConstants;
import tools.GeneralConfig;
import conll03.CoNLL03Ner;

public class AutoTests {
	public static void main(String args[]) throws Exception {
//		throw new Exception("Just an example of test that fails");
//		System.out.println("example of test that succeeds");
		
//		testCRFquick();
		testGigaquick();
	}
	
	static void testCRFquick() throws Exception {
        CoNLL03Ner conll = new CoNLL03Ner();
        conll.generatingStanfordInputFiles(CNConstants.ALL, "train", true,20,CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.ALL, "test", true,CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.ALL, "dev", true,CNConstants.CHAR_NULL);
    	float f1=conll.trainStanfordCRF(CNConstants.ALL, false, false,false);
    	if (f1!=43.38f) throw new Exception("CRF F1 with 20 training utts is "+f1);
	}
	
	static void testGigaquick() throws Exception {
        CoNLL03Ner conll = new CoNLL03Ner();
        GeneralConfig.corpusGigaDir="res/";
        GeneralConfig.corpusGigaTrain="giga1000.conll03";
        conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, 20, CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "gigaw", false,CNConstants.CHAR_NULL);
        conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN,false);
	}
}
