package etape.unsup;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;

public class TwoRules {
	final String[] ens = {
			"amount",
			"event",
			"func.coll","func.ind",
			"loc.add.elec","loc.add.phys","loc.adm.nat","loc.adm.reg","loc.adm.sup","loc.adm.town","loc.gac","loc.oro","loc.other","loc.phys.astro","loc.phys.geo","loc.phys.hydro",
			"org.adm","org.ent",
			"pers.coll","pers.ind",
			"prod.art","prod.award","prod.doctr","prod.fin","prod.media","prod.object","prod.other","prod.rule","prod.serv","prod.soft",
			"time.date.abs","time.date.rel","time.hour.abs","time.hour.rel",
	};

	String[] prenoms, prods;
	LexPref[] lexprefs;
	final ArrayList<Rule> allrules = new ArrayList<TwoRules.Rule>();
	
	void loadGazettes() {
		try {
			{
				ArrayList<String> ll = new ArrayList<String>();
				BufferedReader f = FileUtils.openFileUTF("prenoms.txt");
				for (;;) {
					String s=f.readLine();
					if (s==null) break;
					s=s.trim();
					ll.add(s);
				}
				f.close();
				Collections.sort(ll);
				prenoms = ll.toArray(new String[ll.size()]);
			}
			{
				ArrayList<String> ll = new ArrayList<String>();
				BufferedReader f = FileUtils.openFileUTF("prods.txt");
				for (;;) {
					String s=f.readLine();
					if (s==null) break;
					s=s.trim();
					ll.add(s);
				}
				f.close();
				prods = ll.toArray(new String[ll.size()]);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public TwoRules() {
		allrules.add(ruleName1);
		allrules.add(ruleProd1);
		allrules.add(ruleNameDico);
		allrules.add(ruleProdDico);
	}
	
	interface Rule {
		boolean apply(DetGraph g, int w);
	}

	// ces 2 rules sont concurrentes
	// pour les departager, c'est le critere unsup sur les prefs. lex. et les 2 rules suivantes
	Rule ruleName1 = new Rule() {
		@Override
		public boolean apply(DetGraph g, int w) {
			String ws = g.getMot(w).getForme();
			if (w+1>=g.getNbMots()) return false;
			if (Character.isUpperCase(ws.charAt(0))&&Character.isUpperCase(g.getMot(w+1).getForme().charAt(0))) {
				g.addgroup(w, w+1, "Rpers.ind");
				g.addgroup(w, w, "Rname.first");
				g.addgroup(w+1, w+1, "Rname.last");
				return true;
			}
			return false;
		}
		public String toString() {return "RName";}
	};
	Rule ruleProd1 = new Rule() {
		@Override
		public boolean apply(DetGraph g, int w) {
			String ws = g.getMot(w).getForme();
			if (w+1>=g.getNbMots()) return false;
			if (Character.isUpperCase(ws.charAt(0))&&Character.isUpperCase(g.getMot(w+1).getForme().charAt(0))) {
				g.addgroup(w, w+1, "Rprod.media");
				g.addgroup(w, w+1, "Rname");
				return true;
			}
			return false;
		}
		public String toString() {return "RProd";}
	};
	Rule ruleNameDico = new Rule() {
		@Override
		public boolean apply(DetGraph g, int w) {
			String ws = g.getMot(w).getForme();
			if (Arrays.binarySearch(prenoms, ws)>=0) {
				g.addgroup(w, w, "Rname.first");
				return true;
			}
			return false;
		}
		public String toString() {return "RNameDic";}
	};
	Rule ruleProdDico = new Rule() {
		@Override
		public boolean apply(DetGraph g, int w) {
			for (String s : prods) {
				String[] st = s.split(" ");
				if (w+st.length>g.getNbMots()) continue;
				boolean found=true;
				for (int i=0;i<st.length;i++) {
					if (!g.getMot(w+i).getForme().equals(st[i])) {found=false; break;}
				}
				if (found) {
if (g==gdebug) System.out.println("debug rproddico "+s);
					g.addgroup(w, w+st.length-1, "Rprod.media");
					return true;
				}
			}
			return false;
		}
		public String toString() {return "RProdDic";}
	};

	DetGraph gdebug;
	
	// =============================================

	void saveNE(List<DetGraph> gs) {
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream("corp.ne"),Charset.forName("ISO-8859-1")));
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					int[] grps = g.getGroupsThatStartHere(i);
					if (grps!=null) {
						// first, the ENs, ordered by len
						ArrayList<Integer> len = new ArrayList<Integer>();
						ArrayList<String> en = new ArrayList<String>();
						ArrayList<Integer> grpsleft = new ArrayList<Integer>();
						for (int gr : grps) {
							String e = g.groupnoms.get(gr);
							if (Arrays.binarySearch(ens, e)<0) {
								grpsleft.add(gr);
								continue;
							}
							int l = g.groups.size();
							int p=0;
							for (p=0;p<len.size();i++) {
								if (l<len.get(p)) break;
							}
							len.add(p, l);
							en.add(p,e);
						}
						for (String e : en) {
							f.print("<"+e+"> ");
						}
						// second, the components
						for (int gr : grpsleft) {
							String e = g.groupnoms.get(gr);
							int l = g.groups.size();
							int p=0;
							for (p=0;p<len.size();i++) {
								if (l<len.get(p)) break;
							}
							len.add(p, l);
							en.add(p,e);
						}
						for (String e : en) {
							f.print("<"+e+"> ");
						}
					}

					f.print(g.getMot(i).getForme()+" ");

					grps = g.getGroupsThatEndHere(i);
					if (grps!=null) {
						// first, the components
						ArrayList<Integer> len = new ArrayList<Integer>();
						ArrayList<String> en = new ArrayList<String>();
						ArrayList<Integer> grpsleft = new ArrayList<Integer>();
						for (int gr : grps) {
							String e = g.groupnoms.get(gr);
							if (Arrays.binarySearch(ens, e)>=0) {
								grpsleft.add(gr);
								continue;
							}
							int l = g.groups.size();
							int p=0;
							for (p=0;p<len.size();i++) {
								if (l<len.get(p)) break;
							}
							len.add(p, l);
							en.add(p,e);
						}
						for (String e : en) {
							f.print("<"+e+"> ");
						}
						// second, the ENs
						for (int gr : grpsleft) {
							String e = g.groupnoms.get(gr);
							int l = g.groups.size();
							int p=0;
							for (p=0;p<len.size();i++) {
								if (l<len.get(p)) break;
							}
							len.add(p, l);
							en.add(p,e);
						}
						for (String e : en) {
							f.print("<"+e+"> ");
						}
					}
				}
				f.println();
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void determ(List<DetGraph> gs) {
		for (DetGraph g : gs) {
			System.out.println(g);
			for (int i=0;i<g.getNbMots();i++) {
				ruleName1.apply(g, i);
			}
		}
	}
	/**
	 * - il ne faut pas ajouter la meme EN ou composant 2x sur le meme mot
	 * - il ne faut pas avoir 2 ENs de meme taille au meme endroit
	 * 
	 * @param g
	 * @return
	 */
	boolean checkNotDuplicateEns(DetGraph g) {
		for (int i=0;i<g.getNbMots();i++) {
			int[] grps = g.getGroups(i);
			if (grps!=null&&grps.length>1) {
				HashSet<String> enOuComp = new HashSet<String>();
				for (int gr : grps) {
					String gn = g.groupnoms.get(gr);
					if (gn.charAt(0)=='R') gn=gn.substring(1);
					enOuComp.add(gn);
				}
				if (enOuComp.size()!=grps.length) return false;
			}
		}
		for (int i=0;i<g.getNbMots();i++) {
			int[] grps = g.getGroupsThatStartHere(i);
			if (grps!=null&&grps.length>1) {
				ArrayList<Integer> ensx = new ArrayList<Integer>();
				for (int gr : grps) {
					String gn = g.groupnoms.get(gr);
					if (gn.charAt(0)=='R') gn=gn.substring(1);
					if (Arrays.binarySearch(ens, gn)>=0) {
						ensx.add(gr);
					}
				}
				HashSet<Integer> lens = new HashSet<Integer>();
				for (int gr : ensx) {
					lens.add(g.groups.get(gr).size());
				}
				if (lens.size()!=ensx.size()) return false;
			}
		}
		return true;
	}
	void unsup(List<DetGraph> gs) {
		gdebug=gs.get(0);
		
		final String[] ensused = {"pers.ind","prod.media","name","name.first","name.last"};
		lexprefs = new LexPref[ensused.length];
		for (int i=0;i<ensused.length;i++) {
			lexprefs[i]=new LexPref(gs, ensused[i]);
		}
		for (LexPref lp : lexprefs)
			for (DetGraph g : gs)
				lp.increaseFrameCounts(g);
				
		final Rule rnull = new Rule() {
			@Override
			public boolean apply(DetGraph g, int w) {
				return true;
			}
			public String toString() {return "Rnull";}
		};
		final Rule rdel = new Rule() {
			@Override
			public boolean apply(DetGraph g, int gr) {
				g.groupnoms.remove(gr);
				g.groups.remove(gr);
				return true;
			}
			public String toString() {return "RDel";}
		};
		
		int niters=100;
		for (int iter=0;iter<niters;iter++) {
			double logtot = 0;
			for (DetGraph g : gs) {
				for (LexPref lp : lexprefs) lp.decreaseFrameCounts(g);
				if (g.groups!=null) {
					// TODO: qu'est-ce qui est mieux ?
					// - supprimer tous les groupes a chaque fois et sampler les regles qui les ajoutent => PB: on ne peut pas appliquer plusieurs regles sur le meme mot !
					// - ou laisser les groupes mais samples les rules qui ajoutent et qui suppriment ? ==> je prends cette sol
					// g.groupnoms.clear(); g.groups.clear();
				}
				ArrayList<Rule> sampleCandidates = new ArrayList<TwoRules.Rule>();
				ArrayList<Integer> posCandidate = new ArrayList<Integer>();
				ArrayList<Double> sampleProb = new ArrayList<Double>();
				for (int i=0;i<g.getNbMots();i++) {
					for (Rule r : allrules) {
						if (r.apply(g, i)) {
							if (checkNotDuplicateEns(g)) {
								double p = getLogPost(g);
								sampleCandidates.add(r);
								posCandidate.add(i);
								sampleProb.add(p);
							}
							deleteTmpGroups(g);
						}
					}
				}
				// regle = on supprime un des groupes
				if (g.groups!=null) {
					for (int i=0;i<g.groupnoms.size();i++) {
						String delgrnom = g.groupnoms.remove(i);
						ArrayList<Mot> delgrmots = g.groups.remove(i);
						double p = getLogPost(g);
						sampleCandidates.add(rdel);
						posCandidate.add(i);
						sampleProb.add(p);
						g.groupnoms.add(i, delgrnom);
						g.groups.add(i,delgrmots);
					}
				}
				// regle = on ne fait rien
				sampleCandidates.add(rnull);
				posCandidate.add(0);
				double p = getLogPost(g);
				sampleProb.add(p);

				normalizeLog(sampleProb);
					
				int r = sampleFrom(sampleProb);
				
				if (false) {
					if (g==gs.get(0)) {
						int fridx = lexprefs[0].getFrameIndex(gs.get(0), 0);
						System.out.println("fridx "+fridx+" sampled "+r);
						{
							int[] counts = lexprefs[0].dep2frameCounts.get("YES");
							if (counts!=null) System.out.println("debug France pers.ind "+counts[fridx]);
						}
						{
							int[] counts = lexprefs[0].dep2frameCounts.get("NONE");
							if (counts!=null) System.out.println("debug France nopers   "+counts[fridx]);
						}
						if (sampleProb.size()>1) System.out.println("sampled "+r+" "+sampleProb);
						String[] xlabs = new String[sampleProb.size()];
						for (int ii=0;ii<xlabs.length;ii++) xlabs[ii]=sampleCandidates.get(ii).toString()+"-"+posCandidate.get(ii);
						System.out.println(g);
						System.out.println(g.printGroups());
						SimpleBarChart.drawChart(sampleProb, xlabs);
					}
				}

				logtot+=sampleProb.get(r); // TODO: calculer plutot le log-like 
				if (sampleCandidates.get(r)!=rnull) {
					sampleCandidates.get(r).apply(g, posCandidate.get(r));
					fixTmpGroups(g);
				}
					
				for (LexPref lp : lexprefs) lp.increaseFrameCounts(g);
			}
			System.out.println("iter "+iter+" "+logtot);
		}
	}
	
	
	double addLog(double x, double y) {
		if (x==-Double.MAX_VALUE) { return y; }
		if (y==-Double.MAX_VALUE) { return x; }

		if (x-y > 16) { return x; }
		else if (x > y) { return x + Math.log(1 + Math.exp(y-x)); }
		else if (y-x > 16) { return y; }
		else { return y + Math.log(1 + Math.exp(x-y)); }
	}
	void normalizeLog(double[] x) {
		double s;
		int i;
		s = -Double.MAX_VALUE;
		for (i=0; i<x.length; i++) s = addLog(s, x[i]);
		for (i=0; i<x.length; i++) x[i] = Math.exp(x[i] - s);
	}
	void normalizeLog(List<Double> x) {
		double s;
		int i;
		s = -Double.MAX_VALUE;
		for (i=0; i<x.size(); i++) s = addLog(s, x.get(i));
		for (i=0; i<x.size(); i++) x.set(i, Math.exp(x.get(i) - s));
	}

	Random rand = new Random();
	int sample_Mult(double[] th) {
		double s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= rand.nextDouble();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}
	int sampleFrom(List<Double> th) {
		double s=0;
		for (int i=0;i<th.size();i++) s+=th.get(i);
		s *= rand.nextDouble();
		for (int i=0;i<th.size();i++) {
			s-=th.get(i);
			if (s<0) return i;
		}
		return 0;
	}
	
	// un lexpref par type d'EN
	double getLogPost(DetGraph g) {
		double logp=0;
		for (LexPref lp : lexprefs) {
			for (int i=0;i<g.getNbMots();i++)
				logp+=lp.getLogDirMult(g, i);
		}
		return logp;
	}
	
	void deleteTmpGroups(DetGraph g) {
		if (g.groups==null) return;
		for (int i=g.groupnoms.size()-1;i>=0;i--) {
			if (g.groupnoms.get(i).charAt(0)=='R') {
				g.groupnoms.remove(i);
				g.groups.remove(i);
			}
		}
	}
	void fixTmpGroups(DetGraph g) {
		if (g.groups==null) return;
		for (int i=g.groupnoms.size()-1;i>=0;i--) {
			if (g.groupnoms.get(i).charAt(0)=='R') {
				g.groupnoms.set(i, g.groupnoms.get(i).substring(1));
			}
		}
	}

	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("corp.xml");
		TwoRules m = new TwoRules();
		m.loadGazettes();
		m.unsup(gs);
//		m.determ(gs);
		//		m.saveNE(gs);
		JSafran.viewGraph(gs.toArray(new DetGraph[gs.size()]));
	}
}
