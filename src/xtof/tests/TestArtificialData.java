package xtof.tests;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;

import tools.CNConstants;
import xtof.Corpus;
import xtof.LinearModel;

public class TestArtificialData {

	public static void main(String[] args) {
		TestArtificialData m = new TestArtificialData();
		m.genArtificialDataLC("artdat.train", 1000, 0.2f);
		m.genArtificialDataLC("artdat.test", 500, 0.2f);
		Corpus c = new Corpus("artdat.train", null, null, "artdat.test");
		
		LinearModel mod=LinearModel.train(c.columnDataClassifier, c.trainData);
		float acc = mod.test(c.columnDataClassifier, c.testData);
		System.out.println("trained acc "+acc);
		if (acc<1) throw new Error("trained acc not 100% "+acc);
		
		mod.randomizeWeights();
		acc = mod.test(c.columnDataClassifier, c.testData);
		System.out.println("random acc "+acc);
		if (acc==1||acc==0) throw new Error("random acc weird "+acc);
	}

	void genArtificialDataLC(String outfile, int nex, float priorPN) {
		try {
			OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream(outfile),CNConstants.UTF8_ENCODING);
			Random r = new Random();
			for (int i=0;i<nex;i++) {
				// very basic random generation with only 2 features
				if (r.nextFloat()<priorPN) {
					outFile.append("pn\t");
					outFile.append("W1\t");
					outFile.append("W1\t");
					outFile.append("W1\t");
					outFile.append("C1\t");
					outFile.append("W1");
				} else {
					outFile.append("O\t");
					outFile.append("W2\t");
					outFile.append("W2\t");
					outFile.append("W2\t");
					outFile.append("C2\t");
					outFile.append("W2");
				}
				outFile.append('\n');
			}
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
