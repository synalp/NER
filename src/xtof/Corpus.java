package xtof;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;

import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.util.Index;
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
	private GeneralDataset alldata=null;
	public ColumnDataClassifier columnDataClassifier;
	
	public Corpus() {
	}
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
	// needs the train file to initialize a dataset
	public static Corpus buildFeatureFile(String train, String unlabFile) {
		Corpus c = new Corpus();
		c.columnDataClassifier = new ColumnDataClassifier("etc/slinearclassifier.props");
		GeneralDataset trainset = c.columnDataClassifier.readTrainingExamples(train);
		// will add in this feature index all the new features coming from the unlabeled dataset
		Index<String> featureIndex = trainset.featureIndex;
		try {
			DataOutputStream g = new DataOutputStream(new FileOutputStream("unlabfeats.dat"));
			BufferedReader f = new BufferedReader(new FileReader(unlabFile));
			for (int line=0;;line++) {
				if (line%1000==0) System.out.print("nex "+line+"\r");
				String s=f.readLine();
				if (s==null) break;
				Datum d=c.columnDataClassifier.makeDatumFromLine(s, line);
				Collection<String> features = d.asFeatures();
			    int[] intFeatures = new int[features.size()];
			    int j = 0;
			    for (String feature : features) {
			      featureIndex.add(feature);
			      int index = featureIndex.indexOf(feature);
			      if (index >= 0) {
			        intFeatures[j] = featureIndex.indexOf(feature);
			        j++;
			      }
			    }
			    g.writeInt(j);
			    for (int i=0;i<j;i++)
			    	g.writeInt(intFeatures[i]);
			}
			f.close();
			// save also the feature index
			g.writeInt(featureIndex.size());
			for (int i=0;i<featureIndex.size();i++)
				g.writeUTF(featureIndex.get(i));
			g.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	private void loadAll(String mergedFile) {
		columnDataClassifier = new ColumnDataClassifier("etc/slinearclassifier.props");
		alldata = columnDataClassifier.readTrainingExamples(mergedFile);
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
		System.out.println("nfeats "+alldata.numFeatures()+" "+alldata.size());
	}
	private void saveUnlabFeatsPerInstance() {
		System.out.println("saving unlab feats...");
		int[][] feats = unlabData.getDataArray();
		try {
			DataOutputStream f = new DataOutputStream(new FileOutputStream("unlabfeats.dat"));
			f.writeInt(feats.length);
			for (int i=0;i<feats.length;i++) {
				f.writeInt(feats[i].length);
				for (int j=0;j<feats[i].length;j++) {
					f.writeInt(feats[i][j]);
				}
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
