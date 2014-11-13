package gigaword;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
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
		List<String[]> toks = m.tokenize(utts);
		{
			for (int i=0;i<5;i++) {
				for (int j=0;j<toks.get(i).length;j++)
					System.out.print(toks.get(i)[j]+' ');
				System.out.println();
			}
		}
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
	
	List<String[]> tokenize(List<String> txt) {
		InputStream modelIn=null;
		ArrayList<String[]> res = new ArrayList<String[]>();
		long nw=0;
		try {
			modelIn = new FileInputStream("res/en-token.bin");
			TokenizerModel model = new TokenizerModel(modelIn);
			Tokenizer tokenizer = new TokenizerME(model);
			for (int i=0;i<txt.size();i++) {
				String s=txt.get(i);
				String tokens[] = tokenizer.tokenize(s);
				nw+=tokens.length;
				res.add(tokens);
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
		System.out.println("tokenization "+res.size()+" "+nw);
		return res;
	}
}
