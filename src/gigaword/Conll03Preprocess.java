package gigaword;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;

/**
 * Preprocessing of gigaword EN to get Conll03-like data files
 * 
 * @author xtof
 *
 */
public class Conll03Preprocess {
	
	public static void main(String[] args) {
		Conll03Preprocess m = new Conll03Preprocess();
		GigawordEn c = new GigawordEn();
		List<String> utts = m.sentDetect(c.getChunk(0));
	}
	
	List<String> sentDetect(List<String> txt) {
		InputStream modelIn = null;
		ArrayList<String> res = new ArrayList<String>();
		try {
			modelIn = new FileInputStream("res/en-sent.bin");
			SentenceModel model = new SentenceModel(modelIn);
			SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
			for (int i=0;i<txt.size();i++) {
				String s=txt.get(i);
				String sentences[] = sentenceDetector.sentDetect(s);
				for (int j=0;j<sentences.length;j++) {
					String tt = sentences[j].trim();
					if (tt.length()>0) res.add(tt);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {}
			}
		}
		System.out.println("sentence detection: "+txt.size()+" "+res.size());
		return res;
	}
	
	void tokenize() {
		InputStream modelIn=null;
		try {
			modelIn = new FileInputStream("res/en-token.bin");
			TokenizerModel model = new TokenizerModel(modelIn);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {}
			}
		}
	}
}
