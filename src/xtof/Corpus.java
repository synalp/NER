package xtof;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.util.Pair;

/**
 * Because everytime we use ColumnDataClass.read(), a new HashSet for features is created, there is always a risk that
 * features index may differ from training to weak sup training to testing...
 * 
 * So in order to limit this risk, the idea here is to read ALL the data (train, dev, unsup, test) *only once*
 * and then access part of it using this class.
 * 
 * 
 * @author xtof
 *
 */
public class Corpus {
	// all corpora are read together, so these offsets just tell where does each sub-corpus start
	private int trainDeb=-1, unlabDeb=-1, devDeb=-1, testDeb=-1;
	private int trainEnd=-1, unlabEnd=-1, devEnd=-1, testEnd=-1;
	public GeneralDataset trainData=null, unlabData=null, devData=null, testData=null;
	public ColumnDataClassifier columnDataClassifier;
	
	public Corpus (String train, String unlab, String dev, String test) {
		String mergedFile = mergeAllFiles(train,unlab,dev,test);
		loadAll(mergedFile);
	}
	
	public int[] getTrainLabs() {
		return trainData.getLabelsArray();
	}
	public int[] getTestLabs() {
		return testData.getLabelsArray();
	}
	public int[][] getTrainFeats() {
		return trainData.getDataArray();
	}
	public int[][] getTestFeats() {
		return testData.getDataArray();
	}
	
	private String mergeAllFiles(String train, String unlab, String dev, String test) {
		final String resFile = "all.dat";
		try {
			PrintWriter f = new PrintWriter(new FileWriter(resFile));
			int n=0;
			if (train!=null) {
				trainDeb=n;
				BufferedReader g = new BufferedReader(new FileReader(train));
				for (;;n++) {
					String s=g.readLine();
					if (s==null) break;
					f.println(s);
				}
				g.close();
				trainEnd=n;
			}
			if (unlab!=null) {
				unlabDeb=n;
				BufferedReader g = new BufferedReader(new FileReader(unlab));
				for (;;n++) {
					String s=g.readLine();
					if (s==null) break;
					f.println(s);
				}
				g.close();
				unlabEnd=n;
			}
			if (dev!=null) {
				devDeb=n;
				BufferedReader g = new BufferedReader(new FileReader(dev));
				for (;;n++) {
					String s=g.readLine();
					if (s==null) break;
					f.println(s);
				}
				g.close();
				devEnd=n;
			}
			if (test!=null) {
				testDeb=n;
				BufferedReader g = new BufferedReader(new FileReader(test));
				for (;;n++) {
					String s=g.readLine();
					if (s==null) break;
					f.println(s);
				}
				g.close();
				testEnd=n;
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resFile;
	}
	private void loadAll(String mergedFile) {
		columnDataClassifier = new ColumnDataClassifier("etc/slinearclassifier.props");
		GeneralDataset alldata = columnDataClassifier.readTrainingExamples(mergedFile);
		if (trainDeb>=0) {
			Pair<GeneralDataset, GeneralDataset> splitData = alldata.split(trainDeb,trainEnd);
			trainData=splitData.second;
		}
		if (unlabDeb>=0) {
			Pair<GeneralDataset, GeneralDataset> splitData = alldata.split(unlabDeb,unlabEnd);
			unlabData=splitData.second;
		}
		if (devDeb>=0) {
			Pair<GeneralDataset, GeneralDataset> splitData = alldata.split(devDeb,devEnd);
			devData=splitData.second;
		}
		if (testDeb>=0) {
			Pair<GeneralDataset, GeneralDataset> splitData = alldata.split(testDeb,testEnd);
			testData=splitData.second;
		}
	}
}
