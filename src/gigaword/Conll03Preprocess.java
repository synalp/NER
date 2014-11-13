package gigaword;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
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
		
	}
	public static void tagGigaword(String[] args) {
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
		List<String[]> tags = m.postagger(toks);
		m.saveConll03("giga.conll03",toks,tags);
	}
	
	/**
	 * retag the original Conll03 corpus with the openNLP tagger
	 * For now, reuse the original Conll03 tokenization: if it's not good, then we may want to resegment/retokenize as well !
	 */
	public static void retagConll03() {
		Conll03Preprocess m = new Conll03Preprocess();
		final String[] conllfiles = {"corpus/CoNLL-2003/eng.testa","corpus/CoNLL-2003/eng.testb","corpus/CoNLL-2003/eng.train"};
		for (String cofile : conllfiles) {
			try {
				String prefix;
				ArrayList<String[]> wordsAndTags = new ArrayList<String[]>();
				ArrayList<String[]> justwords = new ArrayList<String[]>();
				ArrayList<String> oneutt = new ArrayList<String>();
				BufferedReader f = new BufferedReader(new FileReader(cofile));
				{
					String s=f.readLine(); // DOCSTART
					prefix=s;
					s=f.readLine(); // empty line
				}
				for (;;) {
					String s=f.readLine();
					if (s==null) break;
					s=s.trim();
					if (s.length()==0) {
						String[] lines = oneutt.toArray(new String[oneutt.size()]);
						wordsAndTags.add(lines);
						String[] words = new String[lines.length];
						justwords.add(words);
						for (int i=0;i<lines.length;i++) {
							int k=lines[i].indexOf(' ');
							words[i]=lines[i].substring(0, k);
						}
						oneutt.clear();
					} else {
						oneutt.add(s);
					}
				}
				f.close();
				List<String[]> newtags = m.postagger(justwords);
				PrintWriter g = new PrintWriter(new FileWriter(cofile+".openNLP"));
				g.println(prefix);
				g.println();
				for (int i=0;i<wordsAndTags.size();i++) {
					String[] lines=wordsAndTags.get(i);
					for (int w=0;w<lines.length;w++) {
						int k=lines[w].indexOf(' ')+1;
						int l=lines[w].indexOf(' ', k);
						g.println(justwords.get(i)[w]+" "+newtags.get(i)[w]+lines[w].substring(l));
					}
					g.println();
				}
				g.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	void saveConll03(String file, List<String[]> toks, List<String[]> tags) {
		assert toks.size()==tags.size();
		try {
			PrintWriter f = new PrintWriter(new FileWriter(file));
			for (int i=0;i<toks.size();i++) {
				String[] words = toks.get(i);
				String[] pos = tags.get(i);
				assert words.length==pos.length;
				for (int j=0;j<words.length;j++)
					f.println(words[j]+"\t"+pos[j]);
				f.println();
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
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
	
	List<String[]> postagger(List<String[]> words) {
		InputStream modelIn=null;
		ArrayList<String[]> res = new ArrayList<String[]>();
		HashSet<String> pos = new HashSet<String>();
		try {
			modelIn = new FileInputStream("res/en-pos-maxent.bin");
			POSModel model = new POSModel(modelIn);
			POSTaggerME tagger = new POSTaggerME(model);
			for (int i=0;i<words.size();i++) {
				String tags[] = tagger.tag(words.get(i));
				for (String t : tags) pos.add(t);
				res.add(tags);
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
		System.out.println("postagging "+res.size()+" "+pos);
		return res;
	}
}
