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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import utils.JDiff;
import utils.SuiteDeMots;

//import com.sun.org.apache.xml.internal.security.utils.ElementCheckerImpl.FullChecker;

import jsafran.DetGraph;

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
	
	public static void save(List<DetGraph> gs, String inputNEfile, String outfile) {
		// align les graphes avec les inputNEfiles pour conserver la meme toknisation
		NECorpus nec = new NECorpus();
		nec.load(inputNEfile);
		String[] sne = nec.fullcorp.split(" ");
		ArrayList<String> lgs = new ArrayList<>();
		for (DetGraph g : gs) {
			for (int i=0;i<g.getNbMots();i++) {
				lgs.add(g.getMot(i).getForme());
			}
		}
		String[] sgs = lgs.toArray(new String[lgs.size()]);
		
		SuiteDeMots sune = new SuiteDeMots(sne);
		SuiteDeMots sugs = new SuiteDeMots(sgs);
		sugs.align(sune);
		// pour chaque EN
		// definit les TAGS EN_i
		// projette les TAGS
		// sauve les tags pour les cumuler
		HashSet<String> nes = new HashSet<String>();
		HashMap<String, List<Integer>> en2widx = new HashMap<String,List<Integer>>();
		for (DetGraph g : gs)
			if (g.groups!=null)
				for (String gr : g.groupnoms) nes.add(gr);
		for (String ne : nes) {
			int gwidx=0;
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++,gwidx++) {
					int[] grps = g.getGroups(i);
					for (int gr : grps) {
						if (g.groupnoms.get(gr).equals(ne)) sugs.setTag(gwidx, 1);
						else sugs.setTag(gwidx,0);
					}
				}
			}
			sugs.projectTagsToLinkedSuite();
			ArrayList<Integer> widx = new ArrayList<Integer>();
			en2widx.put(ne, widx);
			for (int w=0;w<sune.getNmots();w++) {
				if (sune.getThisTag(w)==1) widx.add(w);
			}
		}
		
		try {
			PrintWriter fout = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outfile),Charset.forName("ISO-8859-1")));
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(inputNEfile), Charset.forName("ISO-8859-1")));
			
			for (int si=0;;) {
				String s=f.readLine();
				if (s==null) break;
				// remove previous annotations
				s=s.replaceAll("<[^>]*>", "");
				s=s.replaceAll("  +", " ");
				s=s.trim();
				if (s.length()==0) continue;
				String[] sa = s.split(" "); // meme tokenisation que sur le corpus complet !!
				// pour cette phrase:
				int sdeb=si;
				int sfin=si+sa.length;
				
			}
			f.close();
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		NECorpus m = new NECorpus();
		m.load("/home/xtof/corpus/ETAPE2/Dom/Etape/quaero-ne-normalized/19990621_1900_1920_inter_fm_dga.ne");
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream("corp.txt"),Charset.forName("UTF-8")));
			f.println(m.fullcorp);
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
