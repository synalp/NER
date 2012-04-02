package etape.unsup;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * 
 * @author cerisara
 *
 */
public class SparseRules {

	NECorpus corp;
	public SparseRules(NECorpus corp) {
		this.corp=corp;
	}
	
	void deterministic() {
		for (;;) {
			List<String> toks = corp.getNextSentence();
			if (toks==null) break;
			applyTime1(toks);
		}
	}
	
	public static void main(String args[]) throws Exception {
		NECorpus corp = new NECorpus();
		corp.load("/home/xtof/corpus/ETAPE2/Dom/Etape/quaero-ne-normalized/19990617_1900_1920_inter_fm_dga.ne");
		corp.annots.clear();
		SparseRules m = new SparseRules(corp);
		m.deterministic();
		corp.save("rec.ne");
	}
	
	// =======================================
	/**
	 * 
	 * @param pat
	 * @param annot
	 * @param annotdeb
	 * @param toks
	 * @param tokdeb
	 * 
	 * @return position of next (untreated) token, or -1 if no match
	 */
	int parse(String pat, Annotations annot, int annotdeb, List<String> toks, int tokdeb) {
		boolean doesMatch = true;
		{
			int curtok=tokdeb;
			StringTokenizer patelts = new StringTokenizer(pat);
			while (patelts.hasMoreTokens()) {
				String patelt = patelts.nextToken();
				if (patelt.charAt(0)=='<') {
					// dans un premier temps, on regarde juste si ca matche
				} else {
					String ps = patelt.replace('_', ' ');
					Pattern p = Pattern.compile(ps);
					int maxlen=0;
					StringBuilder scand = new StringBuilder();
					scand.append(toks.get(curtok));
					for (int len=1;curtok+len<toks.size();len++) {
						boolean m = p.matcher(scand.toString()).matches();
						if (!m) break;
						scand.append(' ');
						scand.append(toks.get(curtok+len));
if (scand.toString().startsWith("il y a"))
	System.out.println("debug "+toks);
						maxlen=len;
					}
					if (maxlen>0) {
						curtok+=maxlen;
					} else {
						doesMatch=false; break;
					}
				}
			}
		}
		if (doesMatch) {
			int curtok=tokdeb;
			StringTokenizer patelts = new StringTokenizer(pat);
			while (patelts.hasMoreTokens()) {
				String patelt = patelts.nextToken();
				if (patelt.charAt(0)=='<') {
					if (patelt.charAt(1)=='/') {
						String en = patelt.substring(2,patelt.length()-1);
						corp.annots.closePreviousEN(corp.curSentDeb+curtok-1, en);
					} else {
						String en = patelt.substring(1,patelt.length()-1);
						corp.annots.startNewEN(corp.curSentDeb+curtok, en);
					}
				} else {
					Pattern p = Pattern.compile(patelt);
					int maxlen=0;
					StringBuilder scand = new StringBuilder();
					scand.append(toks.get(curtok));
					for (int len=1;curtok+len<toks.size();len++) {
						boolean m = p.matcher(scand.toString()).matches();
						if (!m) break;
						scand.append(' ');
						scand.append(toks.get(curtok+len));
						maxlen=len;
					}
					if (maxlen>0) {
						curtok+=maxlen;
					} else {
						doesMatch=false; break;
					}
				}
			}
			return curtok;
		} else return -1;
	}
	
	// =======================================
	static void debug() {
		String s="il y a 3 ans";
		Pattern p = Pattern.compile("(pendant|durant|il y a) (\\d+)+ (heure(s)?|mois|an(s)?|semaine(s)?)");
		boolean m = p.matcher(s).matches();
		System.out.println(m);
		System.exit(1);
	}
	
	List<Integer> applyTime1(List<String> s) {
		final String[] pats = {
				"<time.hour.rel> <time-modifier> (pendant|durant|il_y_a) </time-modifier> <val> (\\d+)+ </val> <unit> (heure(s)?|mois|an(s)?|semaine(s)?) </unit> </time.hour.rel>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> (\\d+)+ </val> <unit> minute(s)? </unit>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> et <val> (\\d+)+ </val> <unit> minute(s)? </unit>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> (\\d+)+ </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> et demi(e)? </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> et quart </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> trois quart(s)? </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs>",
		};
		
		ArrayList<Integer> posApplied = new ArrayList<Integer>();
		for (int i=0;i<s.size();i++) {
			for (int pi=0;pi<pats.length;pi++) {
				String p = pats[pi];
				int m = parse(p,corp.annots,corp.curSentDeb,s,i);
				if (m>=0) {
					posApplied.add(i);
					i=m-1;
					// on applique les patterns dans l'ordre defini: si un pattent est applique, on n'applique pas les suivants !
					break;
				}
			}
		}
		return posApplied;
	}
}
