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
	
	@Deprecated
	List<String> tokens = new ArrayList<String>();
	List<String> sentences = new ArrayList<String>();

	public Annotations annots = new Annotations();
	int nextSentDeb = 0;

	@Deprecated
	public int curSentDeb = -1;
	
	public NECorpus() {}

	@Deprecated
	public List<String> getNextSentence0() {
		if (nextSentDeb>=tokens.size()) return null;
		curSentDeb = nextSentDeb;
		for (int i=curSentDeb;;i++) {
			if (tokens.get(i)==EOL) {
				nextSentDeb=i+1;
				return tokens.subList(curSentDeb, i);
			}
		}
	}
	
	@Deprecated
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
	
	@Deprecated
	public void save0(String outfile) {
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
	public void save(String outfile) {
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outfile),Charset.forName("ISO-8859-1")));
			f.println(fullcorp);
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Deprecated
	public void load0(String nefile) {
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
	// je concatene tout le corpus car les sorties de reco n'ont pas de ponct et ne sont pas segmentees !
	public String fullcorp = null;
	public void load(String nefile) {
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(nefile), Charset.forName("ISO-8859-1")));
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				// remove previous annotations
				s=s.replaceAll("<[^>]*>", "");
				sb.append(s);
				sb.append(' ');
			}
			f.close();
			String s=sb.toString().replaceAll("  +", " ");
			s=s.trim();
			fullcorp=s;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
