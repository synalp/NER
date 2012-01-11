import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import jsafran.GroupManager;
import jsafran.JSafran;
import jsafran.Mot;

/**
 * prepare les data en entree du program HDB de Hal Daumé
 * 
 * @author xtof
 *
 */
public class PrepHDB {
	final static int nclassesMax=100;

	public static void main(String args[]) throws Exception {
		if (args.length==0) {
			debug();
			return;
		}
		if (args[0].equals("-train")) {
			train(args[1],args[2],args[3]);
		} else if (args[0].equals("-test")) {
			test(args[1]);
		} else if (args[0].equals("-debug")) {
			debug();
		} else if (args[0].equals("-meanres")) {
			meanres();
		} else if (args[0].equals("-putclass")) {
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs(args[1]);
			saveGroups(gs, args[2], Integer.parseInt(args[3]),args[4]);
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

	public static void saveGroups(java.util.List<DetGraph> gs, String logen, int trainOrTest, String en) {

		try {
			// lecture des indexes
			DataInputStream fin = new DataInputStream(new FileInputStream("indexes.hdb"));
			// ces 3 indexes font reference a la "big list", en considerant toutes les instances possibles
			long idxTrain = fin.readLong();
			long idxTest  = fin.readLong();
			long idxEnd   = fin.readLong();
			long idxTrainkept = fin.readLong();
			long idxTestkept  = fin.readLong();
			long idxEndkept   = fin.readLong();
			int nlist = fin.readInt();
			// idem pour ce tableau d'index
			long[] indexeskept = new long[nlist];
			for (int i=0;i<nlist;i++) {
				indexeskept[i]=fin.readLong();
			}
			fin.close();

			long idxdebInBigList=idxTrain;
			long idxendInBigList=idxTest;
			long idxdebInKeptList=idxTrainkept;
			long idxendInKeptList=idxTestkept;
			if (trainOrTest==1) {
				idxdebInBigList=idxTest;
				idxendInBigList=idxEnd;
				idxdebInKeptList=idxTestkept;
				idxendInKeptList=idxEndkept;
			}
			System.out.println("indexes      "+idxTrain+" "+idxTest+" "+idxEnd+" "+indexeskept.length);
			System.out.println("indexes kept "+idxTrainkept+" "+idxTestkept+" "+idxEndkept+" "+indexeskept.length);
			
			// calcul du nb de classes
			// les tokens sur la ligne e correspondent aux instances de la "keptList" (et non de la BigList)
			int nclasses = 0, ninst=0;
			BufferedReader f = new BufferedReader(new FileReader(logen));
			for (int iter=0;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("e = ")) {
					// cette ligne contient toutes les instances des 3 corpus: unlab + train + test
					int p1=4;
					// on "saute" toutes les instances de unlab, et eventuellement du train
					for (int j=0;j<idxdebInKeptList;j++) p1=s.indexOf(' ',p1)+1;
					ninst=0;
					for (int inst=(int)idxdebInKeptList, k=0;inst<idxendInKeptList;inst++,k++) {
						int l=s.indexOf(' ',p1);
						int ent=Integer.parseInt(s.substring(p1,l));
						System.out.println("debugcl "+inst+" "+k+" "+ens[ent-1]);
						ninst++;
						if (ent+1>nclasses) nclasses=ent+1;
						p1=l+1;
					}
					iter++;
				}
			}
			f.close();
			System.out.println("ninst read "+ninst+" "+(idxendInKeptList-idxdebInKeptList));
			assert ninst==idxendInKeptList-idxdebInKeptList;
			System.out.println("nclasses "+nclasses+" "+ninst);
			
			// lecture des classes samplees: pour chaque instance, on a un sample de E par iter. qui suit posterior P(E|sample)
			// on conserve la distribution empirique P(E|inst) = #(E=e)/#(E=*)
			int[][] counts = new int[ninst][nclasses];
			for (int i=0;i<ninst;i++) Arrays.fill(counts[i], 0);
			f = new BufferedReader(new FileReader(logen));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("e = ")) {
					int i=4;
					for (int j=0;j<idxdebInKeptList;j++) i=s.indexOf(' ',i)+1;
					int inst=0;
					for (int j=(int)idxdebInKeptList;j<idxendInKeptList;j++) {
						int l=s.indexOf(' ',i);
						int classe=Integer.parseInt(s.substring(i,l));
						counts[inst][classe]++;
						inst++;
						i=l+1;
					}
				}
			}
			f.close();
			
			// pour chaque instance, on a P(E|inst); on calcule la valeur max de P(E|inst)
			int[] obs2classe = new int[(int)(idxendInKeptList-idxdebInKeptList)];
			System.out.println("create obs2classe "+obs2classe.length);
			for (int j=(int)idxdebInKeptList, k=0;j<idxendInKeptList;j++,k++) {
				int cmax=0;
				for (int l=1;l<counts[k].length;l++)
					if (counts[k][l]>counts[k][cmax]) cmax=l;
				obs2classe[k]=cmax;
			}

			// lecture des obs
			PrintWriter fout = FileUtils.writeFileUTF("groups."+en+".tab");
			
			// widx=indexe de l'instance dans les graphes
			// il faut donc positionner nextinstinlist a idxdeb
			long curInstInBigList=idxdebInBigList;
			int idxkept=(int)idxdebInKeptList;
			// indexe de la prochaine instance dans le tableau obs2classe
			int obs2classeidx=0;
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				int nexinutt=0;
				for (int j=0;j<g.getNbMots();j++) {
					nexinutt++;
					
					// calcul du label
					String lab = "NO";
					int[] groups = g.getGroups(j);
					if (groups!=null)
						for (int gr : groups) {
							if (g.groupnoms.get(gr).equals(en)) {
								int debdugroupe = g.groups.get(gr).get(0).getIndexInUtt()-1;
								if (debdugroupe==j) lab = en+"B";
								else lab = en+"I";
								break;
							}
						}
					
					String BayesFeat = "CLUNK";
					if (!isAnExemple(g, j)) {
						fout.println(g.getMot(j).getForme()+"\t"+g.getMot(j).getPOS()+"\t"+BayesFeat+"\t"+lab);
						continue;
					}

					// calcul des features
//					if (obs2classeidx>=obs2classe.length&&idxkept<idxendInKeptList)
//						System.out.println("ERROR too many words "+obs2classeidx+" "+obs2classe.length);
//					else {
//						System.out.println("devyf "+obs2classeidx+" "+idxdebInBigList+" "+(curInstInBigList)+" next="+indexeskept[idxkept]+" "+(idxkept-idxendInKeptList));
						// normalement, ne peut pas etre >
						if (idxkept<indexeskept.length&&curInstInBigList>=indexeskept[idxkept]) {
							idxkept++;
							BayesFeat = "CL"+obs2classe[obs2classeidx];
							obs2classeidx++;
						} else {
							// c'est un exemple qui n'a pas été pris en compte (trop rare)
						}
						fout.println(g.getMot(j).getForme()+"\t"+g.getMot(j).getPOS()+"\t"+BayesFeat+"\t"+lab);
//					}
					curInstInBigList++;
				}
				if (nexinutt>0)
					fout.println();
			}
			fout.close();
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
//			if (vocinv[word].charAt(0)=='@') continue;
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
	 * parse la sortie de HBC et recupere les samples de la variable E:
	 * 
	 * @param logfile
	 * @param nclasses
	 * @throws Exception
	 */
	public static void test(String logfile) throws Exception {

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
			// lecture de quel verbe se trouve � l'instance t
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
		// distribution empirique #(W,E)
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
	}

	private static boolean isAnExemple(DetGraph g, int w) {
		if (g.getMot(w).getPOS().startsWith("NUM")) return false;
		if (!g.getMot(w).getPOS().startsWith("N")) return false;
		return true;
	}
	
	static HashMap<String, Integer> vocV = new HashMap<String, Integer>();
	static HashMap<String, Integer> vocO = new HashMap<String, Integer>();
	static HashMap<Integer, Integer> noccV = new HashMap<Integer, Integer>();
	static HashMap<Integer, Integer> noccO = new HashMap<Integer, Integer>();
	private static long saveObs(List<DetGraph> gs, PrintWriter fv, PrintWriter fo) {
		long nobs=0;
		for (DetGraph g :gs) {
			for (int i=0;i<g.getNbMots();i++) {
				if (!isAnExemple(g,i)) continue;
				String headword = "NO_HEAD";
				String govword = g.getMot(i).getLemme();
				int d = g.getDep(i);
				if (d>=0) {
					// j'accepte tous les HEADs, qqsoit le deplabel
					// mais seulement les HEADs qui sont des NOMS ou des VERBES
					Mot head = g.getMot(g.getHead(d));
					if (head.getPOS().startsWith("P")) {
						// preposition: on va chercher un head plus haut
						int h = g.getHead(d);
						d=g.getDep(h);
						if (d>=0) {
							head = g.getMot(g.getHead(d));
							if (head.getPOS().startsWith("N")) {
								// nom = OK
								headword = head.getLemme();
							} else if (head.getPOS().startsWith("V")) {
								// verb = OK
								headword = head.getLemme();
							}
						}
					} else if (head.getPOS().startsWith("N")) {
						// nom = OK
						headword = head.getLemme();
					} else if (head.getPOS().startsWith("V")) {
						// verb = OK
						headword = head.getLemme();
					}
				}

				Integer oi = vocO.get(govword);
				if (oi==null) {
					oi=vocO.size();
					vocO.put(govword,oi);
				}
				Integer vi = vocV.get(headword);
				if (vi==null) {
					vi=vocV.size();
					vocV.put(headword,vi);
				}
				fo.println((oi+1));
				fv.println((vi+1));
				nobs++;
						
				{
					Integer noc = noccV.get(vi);
					if (noc==null) noc=1;
					else noc++;
					noccV.put(vi, noc);
				}
				{
					Integer noc = noccO.get(oi);
					if (noc==null) noc=1;
					else noc++;
					noccO.put(oi, noc);
				}
			}
		}
		return nobs;
	}

	// liste des ens que l'on garde
	final static String[] ens = {"none","loc","org","pers"};
	
	// tous les graphes donnent des instances dont les index apparaissent dans la liste indexeskept.
	// mais les graphes ne commencent pas � 0, ils commencent � offdeb, qui est l'index absolu du 1er elt des graphes
	private static int[] getGoldClass(List<DetGraph> gs, long offdeb, long offend, List<Long> indexeskept) {
		
		// calcul du nombre d'elements de indexeskept qui font partie de ces graphes
		int ninst = 0;
		{
			int i;
			for (i=0;i<indexeskept.size();i++) if (indexeskept.get(i)>=offdeb) break;
			int deb=i;
			for (;i<indexeskept.size();i++) if (indexeskept.get(i)>=offend) break;
			int end=i;
			ninst = end-deb;
			System.out.println("gold class: found n="+ninst);
		}
		
		int[] gold = new int[ninst];
		int goldidx=0;
		long curidx=offdeb;
		int ninstings=0;
		for (int xidx=0;xidx<gs.size();xidx++) {
			DetGraph g = gs.get(xidx);
			for (int i=0;i<g.getNbMots();i++) {
				if (!isAnExemple(g,i)) continue;
				ninstings++;
				if (!indexeskept.contains(curidx++)) continue;

				int[] grps = g.getGroups(i);
				if (grps==null||grps.length==0) {
					gold[goldidx++]=0;
					continue;
				}
				// quels sont les groupes qui nous interessent qui apparaissent sur ce mot ?
				ArrayList<Integer> enidxfound = new ArrayList<Integer>();
				for (int gr : grps) {
					String grpnom = g.groupnoms.get(gr);
					int gidx=0;
					for (int j=1;j<ens.length;j++)
						if (grpnom.startsWith(ens[j])) {gidx=j; break;}
					if (gidx!=0) enidxfound.add(gr);
				}
				if (enidxfound.isEmpty()) {
					gold[goldidx++]=0;
					continue;
				}
				// il y a des groupes: je prends le plus "petit" = le plus proche du mot
				int smallest=0;
				int smallestLen = g.groups.get(enidxfound.get(0)).get(g.groups.get(enidxfound.get(0)).size()-1).getIndexInUtt()-g.groups.get(enidxfound.get(0)).get(0).getIndexInUtt();
				for (int j=1;j<enidxfound.size();j++) {
					int len = g.groups.get(enidxfound.get(j)).get(g.groups.get(enidxfound.get(j)).size()-1).getIndexInUtt()-g.groups.get(enidxfound.get(j)).get(0).getIndexInUtt();
					if (len<smallestLen) {
						smallestLen=len; smallest=j;
					}
				}
				String grpnom = g.groupnoms.get(enidxfound.get(smallest));
				for (int j=1;j<ens.length;j++)
					if (grpnom.startsWith(ens[j])) {
						System.out.println("debugEN "+ens[j]+" "+g.getMot(i)+" "+goldidx+" "+xidx+" "+i);
						gold[goldidx++]=j; break;
					}
			}
		}
		System.out.println("out of gold "+curidx+" "+goldidx+" "+offdeb+" "+offend+" "+indexeskept.size()+" ninstings="+ninstings);
		assert goldidx==gold.length;
		return gold;
	}

	// cree le fichier d'exemples qui passera dans le programme en.hier -> en.c
	// 1 exemple = tous les mots de type N*
	public static void train(String unlabeled, String train, String test) throws Exception {
		//		final static String corp = "../../git/jsafran/train2011.xml";
		//		final static String corp = "../../git/jsafran/c0b.conll";

		PrintWriter fv = new PrintWriter(new FileWriter("enV.0"));
		PrintWriter fo = new PrintWriter(new FileWriter("enO.0"));
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(unlabeled);
		System.out.println("unlab "+gs.size());
		long idxTrain = saveObs(gs, fv, fo);
		gs = gio.loadAllGraphs(train);
		System.out.println("train "+gs.size());
		long idxTest  = idxTrain+saveObs(gs, fv, fo);
		gs = gio.loadAllGraphs(test);
		System.out.println("test  "+gs.size());
		long idxEnd   = idxTest+saveObs(gs, fv, fo);
		fv.close();
		fo.close();
		
		System.out.println("indexs "+idxTrain+" "+idxTest+" "+idxEnd);
		
		saveVoc(vocV,"vocV");
		saveVoc(vocO,"vocO");

		// supprime les obs trop peu frequentes
		final int MINOCC = 100;

		ArrayList<Long> instkept = new ArrayList<Long>();
		PrintWriter f = new PrintWriter(new FileWriter("enV"));
		PrintWriter g = new PrintWriter(new FileWriter("enO"));
		BufferedReader f0 = new BufferedReader(new FileReader("enV.0"));
		BufferedReader g0 = new BufferedReader(new FileReader("enO.0"));
		long nUnlabDel = 0;
		long nTrainDel = 0;
		long nTestDel = 0;
		for (long idx=0;;idx++) {
			String s = f0.readLine();
			if (s==null) break;
			int v = Integer.parseInt(s);
			s = g0.readLine();
			int o = Integer.parseInt(s);
			Integer noco = noccO.get(o-1);
			Integer nocv = noccV.get(v-1);
			if (nocv>MINOCC&&noco>MINOCC) {
				f.println(v);
				g.println(o);
				instkept.add(idx);
			} else {
				// supprime une obs
				if (idx<idxTrain) nUnlabDel++;
				else if (idx<idxTest) nTrainDel++;
				else nTestDel++;
			}
		}
		g0.close();
		f0.close();
		f.close();
		g.close();
		
		// calcule les classes "gold" pour le train seulement
		gs = gio.loadAllGraphs(train);
		int[] golds = getGoldClass(gs, idxTrain, idxTest, instkept);
		
		// sauve les index des mots gardes
		DataOutputStream ff = new DataOutputStream(new FileOutputStream("indexes.hdb"));
		ff.writeLong(idxTrain);
		ff.writeLong(idxTest);
		ff.writeLong(idxEnd);
		long idxTrain2=idxTrain-nUnlabDel;
		long idxTest2=idxTest-nUnlabDel-nTrainDel;
		long idxEnd2=idxEnd-nUnlabDel-nTrainDel-nTestDel;
		ff.writeLong(idxTrain2);
		ff.writeLong(idxTest2);
		ff.writeLong(idxEnd2);
		ff.writeInt(instkept.size());
		for (int i=0;i<instkept.size();i++)
			ff.writeLong(instkept.get(i));
		
		System.out.println("indexes: "+idxTrain2+" "+idxTest2+" "+idxEnd2+" "+instkept.size());
		ff.close();

		// save les golds pour le programme en.out
		{
			PrintWriter fg = new PrintWriter(new FileWriter("tmpgolds.txt"));
			int i=0;
			while (instkept.get(i)<idxTrain) {
				fg.println("-1"); i++;
			}
			for (int j=0;j<golds.length;j++,i++) {
				fg.println(golds[j]);
			}
			for (;i<idxEnd2;i++) {
				fg.println("-1");
			}				
			fg.close();
			golds=null;
		}
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
	
	public static void debug() {
		try {
			ArrayList<Integer> heads = new ArrayList<Integer>();
			ArrayList<Integer> govs  = new ArrayList<Integer>();
			BufferedReader f0 = new BufferedReader(new FileReader("enV"));
			BufferedReader g0 = new BufferedReader(new FileReader("enO"));
			for (long idx=0;;idx++) {
				String s = f0.readLine();
				if (s==null) break;
				int v = Integer.parseInt(s);
				heads.add(v-1);
				s = g0.readLine();
				int o = Integer.parseInt(s);
				govs.add(o-1);
			}
			g0.close();
			f0.close();
			System.out.println("instances: "+heads.size()+" "+govs.size());
			
			HashMap<Integer, String> vocV = new HashMap<Integer, String>();
			{
				BufferedReader f = new BufferedReader(new FileReader("vocV"));
				for (;;) {
					String s=f.readLine();
					if (s==null) break;
					int i=s.lastIndexOf(' ');
					int j=Integer.parseInt(s.substring(i+1));
					vocV.put(j, s.substring(0,i).trim());
				}
				f.close();
			}
			System.out.println("vocv: "+vocV.size());
			HashMap<Integer, String> vocO = new HashMap<Integer, String>();
			{
				BufferedReader f = new BufferedReader(new FileReader("vocO"));
				for (;;) {
					String s=f.readLine();
					if (s==null) break;
					int i=s.lastIndexOf(' ');
					int j=Integer.parseInt(s.substring(i+1));
					vocO.put(j, s.substring(0,i).trim());
				}
				f.close();
			}
			System.out.println("voco: "+vocO.size());
			
			BufferedReader f = new BufferedReader(new FileReader("tmpgolds.txt"));
			for (int i=0;;i++) {
				String s=f.readLine();
				if (s==null) break;
				int cl = Integer.parseInt(s);
				if (cl>0) {
					System.out.println("goldclass "+ens[cl]+" "+vocO.get(govs.get(i))+" "+vocV.get(heads.get(i)));
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
