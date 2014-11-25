package xtof.tests;

import conll03.CoNLL03Ner;
import tools.CNConstants;
import tools.GeneralConfig;
import xtof.Corpus;
import xtof.LinearModel;
import xtof.Parms;

public class TestGigaword {
	public static void main(String[] args) {
		CoNLL03Ner conll = new CoNLL03Ner();
        GeneralConfig.corpusGigaDir="res/";
        GeneralConfig.corpusGigaTrain="giga1000.conll03";
        conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "train", false, 20, CNConstants.CHAR_NULL);
        conll.generatingStanfordInputFiles(CNConstants.PRNOUN, "gigaw", false,CNConstants.CHAR_NULL);

		Corpus c = new Corpus("conll.pn.tab.LC.train", "gw.pn.tab.LC.test", null, null);
		Parms.nitersRiskOptim=1000;
		LinearModel mod=LinearModel.train(c.columnDataClassifier, c.trainData);
		mod.optimizeRisk(c.unlabData);
	}
}
