package etape.unsup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
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
	
	String deterministic() {
		String fc = corp.fullcorp;
		
		fc = applyTitle(fc);

		/*
		fc = applyProd1(fc);
		fc = applyTime1(fc);
		fc = applyPers(fc);
		 */
		return fc;
	}
	
	public static void main(String args[]) throws Exception {
		NECorpus corp = new NECorpus();
		corp.load("/home/xtof/corpus/ETAPE2/Dom/Etape/quaero-ne-normalized/19990617_1900_1920_inter_fm_dga.ne");
		corp.annots.clear();
		SparseRules m = new SparseRules(corp);
		String fullcorp = m.deterministic();
		corp.fullcorp=fullcorp;
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
	@Deprecated
	int parse(String pat, Annotations annot, int annotdeb, List<String> toks, int tokdeb) {

		// supprime tous les capturing groups
		String pat2 = Pattern.compile("\\(([^\\?])").matcher(pat).replaceAll("(?$1");
		
		if (true) {
			// debug: supprime les balises a ajouter
			String pat3 = Pattern.compile("<[^>]+>").matcher(pat).replaceAll("");
			pat3 = pat3.replaceAll("  +", " ");
			System.out.println("debug "+pat3);
			
		}
		
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
					int maxlen = ps.split(" ").length;
					int matchlen=0;
					StringBuilder scand = new StringBuilder();
					scand.append(toks.get(curtok));
					// cherche quelle longueur peut matcher le pattern-elt
					for (int len=1;len<=maxlen&&curtok+len<toks.size();len++) {
						boolean m = p.matcher(scand.toString()).matches();
						if (m) {
							matchlen=len;
							break;
						}
						scand.append(' ');
						scand.append(toks.get(curtok+len));
					}
					if (matchlen>0) {
						curtok+=matchlen;
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
	
	final int[] zone0={};
	int[] debZoneInterdite = zone0;
	int[] finZoneInterdite = zone0;
	void clearZonesInterdites() {
		debZoneInterdite=finZoneInterdite=zone0;
	}
	boolean isInZoneInterdite(int off) {
		int i=Arrays.binarySearch(debZoneInterdite, off);
		if (i<0) i=-i-2;
		if (i<0) return false;
		// off se trouve apres la zone dont le debut est indexe par i
		return (off<finZoneInterdite[i]);
	}

	String parse(String pat, String ruleName, String fullcorp) {
		String pat2 = Pattern.compile("\\(([^\\?])").matcher(pat).replaceAll("(?:$1");
		
		StringBuilder sbm=new StringBuilder();
		StringBuilder sbr=new StringBuilder();
		
		for (int i=1, x=0;x<pat2.length();) {
			int y=pat2.indexOf('<',x);
			if (y>x) {
				sbm.append(' ');
				sbm.append('(');
				sbm.append(pat2.substring(x, y-1));
				sbm.append(')');
				sbr.append(' ');
				sbr.append("$"+i++);
			}
			if (y>=0) {
				x=pat2.indexOf('>',y)+1;
				sbr.append(' ');
				sbr.append(pat2.substring(y, x++));
			} else {
				sbr.append(pat2.substring(x));
				break;
			}
		}
		String mats = sbm.toString().trim();
		String reps = sbr.toString().trim();
		String reps0="";
		{
			// pour les zones interdites:
			int i=reps.lastIndexOf('$')+1;
			assert i>=0;
			int j=reps.indexOf(' ',i);
			int ngroups = Integer.parseInt(reps.substring(i, j));
			StringBuilder sb = new StringBuilder();
			for (i=1;i<=ngroups;i++) {
				sb.append(" $"+i);
			}
			reps0 = sb.toString().trim();
		}
		System.out.println("dbugm ["+sbm+"]");
		System.out.println("dbugr "+sbr);
		
		Matcher mat = Pattern.compile(mats).matcher(fullcorp);
		mat.reset();
		StringBuffer sb = new StringBuffer();
		while (mat.find()) {
			System.out.println("dbg matfound "+mat.start()+" "+mat.group()+" --- "+reps);
			if (isInZoneInterdite(mat.start())) {
				mat.appendReplacement(sb, reps0);
			} else {
				mat.appendReplacement(sb, reps);
			}
		}
		mat.appendTail(sb);
		
		String res = sb.toString();
		
		System.out.println("debug res: "+res.substring(0, 100));
		return res;
	}
	
	void calcZonesInterdites(String corp, String[] balises) {
		ArrayList<Integer> debs = new ArrayList<Integer>();
		ArrayList<Integer> ends = new ArrayList<Integer>();
		for (String balise : balises) {
			final String debbal = "<"+balise;
			final String endbal = "</"+balise;
			int i=0;
			for (;;) {
				int j=corp.indexOf(debbal,i);
				if (j<0) break;
				int k=corp.indexOf(endbal,j);
				i=corp.indexOf('>',k)+1;
				debs.add(j);
				ends.add(i);
			}
		}
		debZoneInterdite = new int[debs.size()];
		finZoneInterdite = new int[debs.size()];
		for (int i=0;i<debs.size();i++) {
			debZoneInterdite[i]=debs.get(i);
			finZoneInterdite[i]=ends.get(i);
		}
	}
	
	// chaque fonction est non-ambigue, mais l'application d'une fonction ou d'une autre est ambigue !
	
	String applyProd1(String fullcorp) {
		// TODO: charger ces gazettes depuis un fichier de listes
		final String[] medias = {"France-Inter","France Inter","France-Culture","France Culture","France-Info","France Info","France2",
				"France 2","France 3", "TF1", "Radio France","France Soir","France-Soir"
		};
		final String[] pats = {
				"<prod.media> <kind> journal </kind> <name> %M </name> </prod.media>",
				"<prod.media> <kind> radio </kind> <name> %M </name> </prod.media>",
				"<prod.media> <kind> chaîne de télévision </kind> <name> %M </name> </prod.media>",
				"<prod.media> <name> %M </name> </prod.media>",
		};
		
		for (int pi=0;pi<pats.length;pi++) {
			for (int mi=0;mi<medias.length;mi++) {
				// lorsqu'une zone est matchee, ne plus la matcher ensuite dans la meme function
				String[] forbids = {"prod.media"};
				calcZonesInterdites(fullcorp,forbids);
				String m=medias[mi];
				String p = pats[pi].replaceAll("%M", m);
				fullcorp = parse(p,"RProd"+pi,fullcorp);
			}
		}
		return fullcorp;
	}
	
	String applyTime1(String fullcorp) {
		final String[] pats = {
				"<time.date.abs> <time-modifier> en </time-modifier> l' <kind> an </kind> <year> [12][0-9][0-9][0-9] </year> </time.date.abs>",
				"<time.date.abs> <kind> an </kind> <year> [12][0-9][0-9][0-9] </year> </time.date.abs>",
// TODO: autre strategie: elargir une EN, ou annoter en sous-niveaux d'abord (year - firstname - ...) puis extrapoler les ENs à partir de ces annots
				"<time.date.abs> <kind> année </kind> <year> [12][0-9][0-9][0-9] </year> </time.date.abs>",
				
				"<time.hour.rel> <time-modifier> il y a </time-modifier> <amount> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </amount> </time.hour.rel>",
				"<amount> <qualifier> (pendant|durant) </qualifier> <val> (\\d+)+ </val> <unit> (heure(s)?|mois|an(s)?|semaine(s)?) </unit> </amount>",
				"<time.date.rel> <time-modifier> il y a </time-modifier> <amount> <val> (\\d+)+ </val> <unit> (mois|an(s)?|semaine(s)?) </unit> </amount> </time.date.rel>",

// TODO: tous les suivants doivent etre ambigus avec <amount>
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> (\\d+)+ </val> <unit> minute(s)? </unit>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> et <val> (\\d+)+ </val> <unit> minute(s)? </unit> </time.hour.abs>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> (\\d+)+ </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> et demi(e)? </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> et quart </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs> <val> trois quart(s)? </val>",
				"<time.hour.abs> <val> (\\d+)+ </val> <unit> heure(s)? </unit> </time.hour.abs>",
		};
		
		for (int pi=0;pi<pats.length;pi++) {
			String p = pats[pi];
			fullcorp = parse(p,"RTime"+pi,fullcorp);
		}
		return fullcorp;
	}
	
	String applyPers(String corp) {
		final String[] pats = {
				// TODO: should be ambiguous with loc, ...
				"<pers.ind> <name.first> [A-Z][a-zéèëêàâôöûùüï]+ </name.first> <name.last> [A-Z][a-zéèëêàâôöûùüï]+ </name.last> </pers.ind>",
		};		
		for (int pi=0;pi<pats.length;pi++) {
			String[] forbids = {"prod.media"};
			calcZonesInterdites(corp,forbids);
			String p = pats[pi];
			corp = parse(p,"RPers"+pi,corp);
		}
		return corp;
	}
	
	String applyTitle(String corp) {
		final String[] titres = {"Dr.","dr.","Pr.","pr.","docteur","professeur","commandant","académicien","général","caporal","lieutenant","lieutenant de marine",
				"sa majesté le roi", "Cheick",
		};

		final String[] pats = {
				"<title> %T </title>",
		};		
		for (int pi=0;pi<pats.length;pi++) {
			for (int mi=0;mi<titres.length;mi++) {
				String[] forbids = {"title"};
				calcZonesInterdites(corp,forbids);
				String m=titres[mi];
				String p = pats[pi].replaceAll("%T", m);
				corp = parse(p,"RTitres"+pi,corp);
			}
		}
		return corp;
	}
}
