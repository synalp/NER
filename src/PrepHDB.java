import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import utils.ErrorsReporting;
import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;

/**
 * prepare les data en entree du program HDB de Hal Daum√©
 * 
 * @author xtof
 *
 */
public class PrepHDB {
	final static int nclassesMax=100;

	public static void main(String args[]) throws Exception {
		if (args[0].equals("-train")) {
			train(args[1]);
		} else if (args[0].equals("-test")) {
			test(args[1]);
		} else if (args[0].equals("-meanres")) {
			meanres();
		} else if (args[0].equals("-putclass")) {
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs(args[1]);
			saveGroups(gs, args[2]);
		}
	}

	public static void meanres() {
		try {
			BufferedReader f = new BufferedReader(new FileReader("verbsuj.classes"));
			HashMap<String, float[]> verb2classes = new HashMap<String, float[]>();
			int nclasses = 0;
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				if (ss.length<3) continue;
				int classe = Integer.parseInt(ss[1]);
				if (classe>nclasses) nclasses = classe;
			}
			nclasses++;
			f.close();
			f = new BufferedReader(new FileReader("verbsuj.classes"));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				if (ss.length<3) continue;
				int classe = Integer.parseInt(ss[1]);
				for (int i=3;i<ss.length;i++) {
					int j=ss[i].indexOf('(');
					String v = ss[i].substring(0, j).trim();
					// PB: les classes peuvent etre interchangeables !!					
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveGroups(java.util.List<DetGraph> gs, String logWithClasses) {
		try {
			HashMap<String, Integer> lemme2classe = new HashMap<String, Integer>();
			// lecture des classes
			BufferedReader f = new BufferedReader(new FileReader(logWithClasses));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("classe ")) {
					String[] st = s.split(" ");
					int clid = Integer.parseInt(st[1]);
					for (int j=3;j<st.length;j++) {
						int i=st[j].indexOf('(');
						while (st[j].charAt(i+1)=='(') i++;
						int k=st[j].lastIndexOf(')');
						float p = Float.parseFloat(st[j].substring(i+1,k));
						if (true||p>0.7) {
							String verbe = st[j].substring(0, i).trim();
							lemme2classe.put(verbe, clid);
						}
					}
				}
			}
			f.close();

			// nb de groupes differents
			HashSet<String> groups = new HashSet<String>();
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				if (g.groupnoms!=null)
					groups.addAll(g.groupnoms);
			}

			for (String gr : groups) {
				PrintWriter fout = FileUtils.writeFileUTF("groups."+gr+".tab");
				for (int i=0;i<gs.size();i++) {
					DetGraph g = gs.get(i);
					boolean isInGroup = false;
					for (int j=0;j<g.getNbMots();j++) {
						String lab = "NO";
						int[] grps = g.getGroups(j);
						boolean grfound = false;
						if (grps!=null&&grps.length>0) {
							for (int k=0;k<grps.length;k++)
								if (g.groupnoms.get(grps[k]).equals(gr)) {
									grfound=true;
									break;
								}
						}
						String classesuj = "NOCL";
						if (g.getMot(j).getPOS().charAt(0)=='N'){
							int d = g.getDep(j);
							if (d>=0) {
								//								if (d>=0 && g.getDepLabel(d).toLowerCase().equals("suj")) {
								String verb = g.getMot(g.getHead(d)).getLemme();
								Integer vcl = lemme2classe.get(verb);
								if (vcl!=null) classesuj = "CLSUJ"+vcl;
							}
						}
						if (!isInGroup&&grfound) lab=gr+"B";
						else if (isInGroup&&grfound) lab=gr+"I";
						isInGroup=grfound;
						fout.println(g.getMot(j).getForme()+"\t"+g.getMot(j).getPOS()+"\t"+classesuj+"\t"+lab);
					}
					fout.println();
				}
				fout.close();
			}
			ErrorsReporting.report("groups saved in groups.*.tab");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void printClasseRepresentatives(int[][] nOcc, String[] vocinv) {
		// mot le plus frequent de chaque classe: on veut W* = argmax_W P(W|E)
		// W* = argmax_W P(E|W)P(W)
		// avec P(E|W) = #(w,e)/#(w,*)
		// et   P(W)   = #w/#*
		final int nbest = 6;
		int[][] bestmot4class = new int[nbest][nclassesMax];
		float[][] pwe = new float[nbest][nclassesMax];
		for (int i=0;i<nbest;i++) Arrays.fill(pwe[i], 0);
		int ntotw=0;
		for (int word=0;word<nOcc.length;word++) {
			for (int cl=0;cl<nOcc[word].length;cl++) {
				ntotw+=nOcc[word][cl];
			}			
		}
		for (int word=0;word<nOcc.length;word++) {
			if (vocinv[word].charAt(0)=='@') continue;
			int nw=0;
			for (int cl=0;cl<nOcc[word].length;cl++) {
				nw += nOcc[word][cl];
			}
			float pw = (float)nw/(float)ntotw;
//			pw=1;
			for (int cl=0;cl<nOcc[word].length;cl++) {
				float pe_w = (float)nOcc[word][cl]/(float)nw;
				float pw_e = pe_w * pw;
				for (int i=0;i<nbest;i++) {
					if (pw_e>pwe[i][cl]) {
						// decale
						for (int j=nbest-1;j>i;j--) {
							pwe[j][cl]=pwe[j-1][cl];
							bestmot4class[j][cl]=bestmot4class[j-1][cl];
						}
						pwe[i][cl]=pw_e;
						bestmot4class[i][cl]=word;
						break;
					}
				}
			}
		}
		for (int cl=0;cl<nclassesMax;cl++) {
			if (pwe[0][cl]>0) {
				System.out.print("\t classe "+cl+" : ");
				for (int i=0;i<nbest;i++) {
					System.out.print(vocinv[bestmot4class[i][cl]]+" ");
//					System.out.print(vocinv[bestmot4class[i][cl]]+"("+pwe[i][cl]+") ");
				}
				System.out.println();
			}
		}
	}

	/**
	 * parse la sortie de HBC et recupere les samples de la variable Z ou E:
	 * - si on recupere vocV et enV, on a alors Z = type du head
	 * - si on recupere vocO et enO, on a alors E = EN du gouvernÈ
	 * 
	 * TODO: recuperer plutot les "EN" affectees aux instances de O...
	 * 
	 * @param logfile
	 * @param nclasses
	 * @throws Exception
	 */
	public static void test(String logfile) throws Exception {

		// on veut pour chaque verbe sa distribution des classes:
		// new: a la place des verbes, un head quelconque

		// il y a 2 variables: V=VERB=HEAD et O=GOV
		HashMap<String, Integer> voc = new HashMap<String, Integer>();
		{
			// lecture des indices des verbes
			BufferedReader f = new BufferedReader(new FileReader("vocO"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.lastIndexOf(' ');
				int idx = Integer.parseInt(s.substring(i+1));
				String verbe = s.substring(0,i);
				voc.put(verbe, idx);
			}
			f.close();
			System.out.println("verb voc read "+voc.size());
		}
		// pour afficher, on doit construire le vocabulaire inverse
		String[] vocinv = new String[voc.size()];
		for (String x : voc.keySet()) vocinv[voc.get(x)]=x;

		int[] wordAtInstance;
		{
			// lecture de quel verbe se trouve ‡ l'instance t
			BufferedReader f = new BufferedReader(new FileReader("enO"));
			ArrayList<Integer> seq = new ArrayList<Integer>();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int idx = Integer.parseInt(s);
				seq.add(idx);
			}
			f.close();
			wordAtInstance = new int[seq.size()];
			for (int i=0;i<seq.size();i++) wordAtInstance[i]=seq.get(i)-1;
			System.out.println("words identified for ninstances = "+wordAtInstance.length);
		}

		int nclasses=0;
		int[][] nOccs = new int[voc.size()][nclassesMax];
		{
			// lecture et cumul des classes par verbe
			for (int[] x : nOccs) Arrays.fill(x, 0);

			BufferedReader f = new BufferedReader(new FileReader(logfile));
			long ntot=0;
			for (int iter=0;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("e = ")) {
					// nouvelle iteration
					// toutes les instances sont sur une ligne
					String[] ss = s.substring(4).split(" ");
					assert ss.length==wordAtInstance.length;
					for (int i=0;i<ss.length;i++) {
						int classe = Integer.parseInt(ss[i]);
						if (classe+1>nclasses) nclasses=classe+1;
						nOccs[wordAtInstance[i]][classe-1]++;
						ntot++;
					}
					if (++iter%10==0) {
						System.out.println("iter "+iter);
						printClasseRepresentatives(nOccs,vocinv);
					}
				}
			}
			f.close();
		}

		if (false) {
			// a partir de la, on ne travaille plus avec les instances

			// affichage des classes de verbes
			class VerbClassPdf implements Comparable<VerbClassPdf> {
				int verbe;
				float[] pdf;
				int bestclass;
				public VerbClassPdf(int verb, int[] noccs, int sum) {
					verbe=verb;
					pdf = new float[noccs.length];
					bestclass=0;
					for (int i=0;i<pdf.length;i++) {
						pdf[i]=(float)noccs[i]/(float)sum;
						if (pdf[i]>pdf[bestclass]) bestclass=i;
					}
				}
				@Override
				public int compareTo(VerbClassPdf p) {
					if (pdf[bestclass]>p.pdf[p.bestclass]) return -1;
					else if (pdf[bestclass]<p.pdf[p.bestclass]) return 1;
					else return 0;
				}
			}
			// calcule la PDF + la classe de chaque verbe du dico
			VerbClassPdf[] verbspdf = new VerbClassPdf[nOccs.length];
			int nv=0;
			for (int i=0;i<nOccs.length;i++) {
				int sum=0; for (int j=0;j<nOccs[i].length;j++) sum+=nOccs[i][j];
				// on s'interesse aux verbes qui apparaissent au moins 5 fois
				if (sum>5) {
					verbspdf[i] = new VerbClassPdf(i,nOccs[i],sum);
					nv++;
				}
			}
			System.out.println("nverbs "+nv);
			// calcule la liste ordonnee des verbes representatifs d'une classe
			// TODO: ordonner en tenant compte du nombre d'occurrences ?
			TreeSet<VerbClassPdf>[] verbsPerClass = new TreeSet[nclassesMax];
			for (int i=0;i<nclassesMax;i++) verbsPerClass[i] = new TreeSet<VerbClassPdf>();
			for (int i=0;i<verbspdf.length;i++) {
				if (verbspdf[i]!=null)
					verbsPerClass[verbspdf[i].bestclass].add(verbspdf[i]);
			}

			for (int i=0;i<nclasses;i++) {
				System.out.print("classe "+i+" : ");
				for (VerbClassPdf v : verbsPerClass[i]) {
					System.out.print(vocinv[v.verbe]+"("+v.pdf[v.bestclass]+") ");
				}
				System.out.println();
			}
		}
	}

	// cree le fichier d'exemples qui passera dans le programme en.hier -> en.c
	public static void train(String corp) throws Exception {
		//		final static String corp = "../../git/jsafran/train2011.xml";
		//		final static String corp = "../../git/jsafran/c0b.conll";

		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(corp);
		PrintWriter fv = new PrintWriter(new FileWriter("enV.0"));
		PrintWriter fo = new PrintWriter(new FileWriter("enO.0"));
		HashMap<String, Integer> vocV = new HashMap<String, Integer>();
		HashMap<String, Integer> vocO = new HashMap<String, Integer>();
		HashMap<Integer, Integer> nocc = new HashMap<Integer, Integer>();

		for (DetGraph g :gs) {
			for (int i=0;i<g.getNbMots();i++) {
				if (!g.getMot(i).getPOS().startsWith("N")) continue;
				int d = g.getDep(i);
				if (d>=0) {
					String deplab = g.getDepLabel(d);
					// j'accepte tous les HEADs, qqsoit le deplabel
					// mais seulement les HEADs qui sont des NOMS ou des VERBES
					if (true||deplab.equals("suj")) {
						String o = g.getMot(i).getLemme();
						Mot head = g.getMot(g.getHead(d));
						if (head.getPOS().startsWith("P")) {
							// preposition: on va chercher un head plus haut
							int h = g.getHead(d);
							d=g.getDep(h);
							if (d>=0) {
								head = g.getMot(g.getHead(d));
								if (head.getPOS().startsWith("N")) {
									// nom = OK
								} else if (head.getPOS().startsWith("V")) {
									// verb = OK
								} else continue;
							} else continue;
						} else if (head.getPOS().startsWith("N")) {
							// nom = OK
						} else if (head.getPOS().startsWith("V")) {
							// verb = OK
						} else continue;

						String v = head.getLemme();
						Integer oi = vocO.get(o);
						if (oi==null) {
							oi=vocO.size();
							vocO.put(o,oi);
						}
						Integer vi = vocV.get(v);
						if (vi==null) {
							vi=vocV.size();
							vocV.put(v,vi);
						}
						fo.println((oi+1));
						fv.println((vi+1));
						Integer noc = nocc.get(vi);
						if (noc==null) noc=1;
						else noc++;
						nocc.put(vi, noc);
					}
				}
			}
		}
		fv.close();
		fo.close();
		saveVoc(vocV,"vocV");
		saveVoc(vocO,"vocO");

		// supprime les obs avec head trop peu frequentes
		final int MINOCC = 10;

		PrintWriter f = new PrintWriter(new FileWriter("enV"));
		PrintWriter g = new PrintWriter(new FileWriter("enO"));
		BufferedReader f0 = new BufferedReader(new FileReader("enV.0"));
		BufferedReader g0 = new BufferedReader(new FileReader("enO.0"));
		for (;;) {
			String s = f0.readLine();
			if (s==null) break;
			int v = Integer.parseInt(s);
			Integer noc = nocc.get(v-1);
			if (noc>MINOCC) {
				f.println(v);
				g.println(g0.readLine());
			}
		}
		g0.close();
		f0.close();
		f.close();
		g.close();
	}

	private static void saveVoc(Map<String, Integer> voc, String fn) {
		try {
			PrintWriter f = new PrintWriter(new FileWriter(fn));
			for (String s : voc.keySet()) {
				f.println(s+" "+voc.get(s));
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
