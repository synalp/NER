package etape.unsup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class NECorpus {
	final String EOL = "__EOL__";
	
	List<String> tokens = new ArrayList<String>();
	public Annotations annots = new Annotations();
	int nextSentDeb = 0;
	public int curSentDeb = -1;
	
	public NECorpus() {}

	public List<String> getNextSentence() {
		if (nextSentDeb>=tokens.size()) return null;
		curSentDeb = nextSentDeb;
		for (int i=curSentDeb;;i++) {
			if (tokens.get(i)==EOL) {
				nextSentDeb=i+1;
				return tokens.subList(curSentDeb, i);
			}
		}
	}
	
	private void parseString(String s) {
		s=s.trim();
		s=s.replaceAll("<", " <");
		s=s.replaceAll(">", "> ");
		s=s.replaceAll("  *", " ");
		s=s.replaceAll(" +>", ">");
		s=s.replaceAll("< +", "<");
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			if (tok.charAt(0)=='<') {
				if (tok.charAt(1)=='/') {
					String enst = tok.substring(2, tok.length()-1);
					annots.closePreviousEN(tokens.size()-1, enst);
				} else {
					String enst = tok.substring(1, tok.length()-1);
					annots.startNewEN(tokens.size(), enst);
				}
			} else {
				tokens.add(tok);
			}
		}
		tokens.add(EOL);
//		if (curendebs.size()>0)
//			System.out.println("WARNING: utt ended with ENs open !");
	}
	
	public void save(String outfile) {
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outfile),Charset.forName("ISO-8859-1")));
			for (int i=0;i<tokens.size();i++) {
				String tok = tokens.get(i);
				if (tok==EOL) f.println();
				else {
					String[] tys = annots.getTypDeb(i);
					for (int j=0;j<tys.length;j++) {
						f.print("<"+tys[j]+"> ");
					}
					f.print(tok+" ");
					tys = annots.getTypEnd(i);
					for (int j=0;j<tys.length;j++) {
						f.print("</"+tys[j]+"> ");
					}
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void load(String nefile) {
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(nefile), Charset.forName("ISO-8859-1")));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				parseString(s);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
