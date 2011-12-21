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

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

/**
 * prepare les data en entree du program HDB de Hal Daumé
 * 
 * @author xtof
 *
 */
public class PrepHDB {
	final static String corp = "../../git/jsafran/train2011.xml";
	
	public static void main(String args[]) throws Exception {
//		train();
		test();
	}

	// je suppose qu'on a obtenu un resultat avec coord.c en affichant dans le fichier log les samples des Wl et Wr
	public static void test() throws Exception {
		String logfile = "/home/xtof/softs/hbc_v0_7_linux/log";
		BufferedReader f = new BufferedReader(new FileReader(logfile));
		ArrayList<Integer> zs = new ArrayList<Integer>();
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			if (s.startsWith("z = ")) {
				String[] ss = s.substring(4).split(" ");
				for (String x : ss) {
					zs.add(Integer.parseInt(x));
				}
				break;
			}
		}
		f.close();
		System.out.println("classes trouvees:");
		HashSet<Integer> cls = new HashSet<Integer>();
		cls.addAll(zs);
		Integer[] clst = cls.toArray(new Integer[cls.size()]);
		Arrays.sort(clst);
		System.out.println(Arrays.toString(clst));

		List<String> verbclasses[] = new List[clst[clst.length-1]+1];
		for (int i=0;i<verbclasses.length;i++) verbclasses[i]=new ArrayList<String>();
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(corp);
		int t=0;
		for (int j=0;j<gs.size();j++) {
			DetGraph g = gs.get(j);
			for (int i=0;i<g.getNbMots();i++) {
				if (!g.getMot(i).getPOS().startsWith("N")) continue;
				int d = g.getDep(i);
				if (d>=0) {
					String deplab = g.getDepLabel(d);
					if (deplab.equals("OBJ")) {
						String o = g.getMot(i).getLemme();
						String v = g.getMot(g.getHead(d)).getLemme();
						verbclasses[zs.get(t++)].add(v+"-"+o);
					}
				}
			}
		}
		System.out.println("verb classes:");
		for (List<String> vs : verbclasses) {
			if (vs.size()>0) {
				System.out.println(vs);
			}
		}
		
		/*
		System.out.println("markage des classes 0 "+zs);
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(corp);
		ArrayList<int[]> marks = new ArrayList<int[]>();

		int t=-0;
		for (int j=0;j<gs.size();j++) {
			DetGraph g = gs.get(j);
			for (int i=0;i<g.getNbMots();i++) {
				String w= g.getMot(i).getForme();
				if (w.equals("et")||w.equals("ou")) {
					if (zs.get(t++)==4) {
						int[] m = {0,j,i};
						marks.add(m);
					}
				}
			}
		}
		ArrayList<List<DetGraph>> gss = new ArrayList<List<DetGraph>>();
		gss.add(gs);
		System.out.println("saving "+marks.size());
		JSafran.save(gss, "tmp.xml", null, marks);
		*/
	}

	// cree le fichier d'exemples qui passera dans le programme en.hier -> en.c
	public static void train() throws Exception {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(corp);
		PrintWriter fv = new PrintWriter(new FileWriter("enV"));
		PrintWriter fo = new PrintWriter(new FileWriter("enO"));
		HashMap<String, Integer> vocV = new HashMap<String, Integer>();
		HashMap<String, Integer> vocO = new HashMap<String, Integer>();

		for (DetGraph g :gs) {
			for (int i=0;i<g.getNbMots();i++) {
				if (!g.getMot(i).getPOS().startsWith("N")) continue;
				int d = g.getDep(i);
				if (d>=0) {
					String deplab = g.getDepLabel(d);
					if (deplab.equals("OBJ")) {
						String o = g.getMot(i).getLemme();
						String v = g.getMot(g.getHead(d)).getLemme();
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
					}
				}
			}
		}
		fv.close();
		fo.close();
		saveVoc(vocV,"vocV");
		saveVoc(vocO,"vocO");
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
