package ester2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.GraphIO;

/**
 * unsup clustering sur Gigaword.
 * Approche progressive: on commence avec des exemples "linguistiquement" tres contraints (ex: NOM/NAM juste devant verbe)
 * puis on enleve les contraintes lorsque le modele commence a bien apprendre
 * 
 * @author xtof
 *
 */
public class Unsup {
	static String fn = "voc.serialized";
	static HashMap<String, Integer> vocV = new HashMap<String, Integer>();
	static HashMap<String, Integer> vocW = new HashMap<String, Integer>();
	static HashMap<String, Integer> vocD = new HashMap<String, Integer>();
	static int getVocID(HashMap<String, Integer> voc, String mot) {
		Integer i = voc.get(mot);
		if (i==null) {
			i=voc.size();
			voc.put(mot, i);
		}
		return i;
	}
	private static void saveVoc() {
		try {
			PrintWriter f = new PrintWriter(new FileWriter(fn+".v"));
			for (String s : vocV.keySet()) {
				f.println(s+" "+vocV.get(s));
			}
			f.close();
			f = new PrintWriter(new FileWriter(fn+".w"));
			for (String s : vocW.keySet()) {
				f.println(s+" "+vocW.get(s));
			}
			f.close();
			f = new PrintWriter(new FileWriter(fn+".d"));
			for (String s : vocD.keySet()) {
				f.println(s+" "+vocD.get(s));
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {
		if (args[0].equals("-creeObs")) creeObsFile(args[1],args[2],args[3]);
		else if (args[0].equals("-analyse")) analyse(args[1]);
		else if (args[0].equals("-dbg")) debg();
		else if (args[0].equals("-inserttab")) {
			int i=0;
			String enlog = args[++i];
			String tabtrain = args[++i];	// tabs du train; peut etre null
			String tabtest  = args[++i];	// tabs du test; peut etre null
			String unlabxmll = args[++i];	// contient les 3 parties: unlab, train et test
			String trainxmll = args[++i];
			String testxmll = args[++i];
			insertInTab(enlog,unlabxmll,trainxmll,testxmll,tabtrain,tabtest);
		}

	}

	// permet de centraliser en un seul endroit l'extraction des instances pour HBC depuis les graphes
	interface InstanceHandler {
		public void nextWord(DetGraph g, int wordInGraph, int head, int depidx, boolean isInstance) throws Exception;
	}
	static class SelectInstances {
		static InstanceHandler ih = null;
		static void parseCorpus(String xmll) {
			try {
				GraphIO gio = new GraphIO(null);
				BufferedReader flist = new BufferedReader(new FileReader(xmll));
				int nfiles=0, ngraphs=0, nwords=0, nhbc=0;
				for (;;) {
					String s = flist.readLine();
					if (s==null) break;
					nfiles++;
					List<DetGraph> gs = gio.loadAllGraphs(s);
					ngraphs+=gs.size();
					for (DetGraph g : gs) {
						nwords+=g.getNbMots();
						for (int i=0;i<g.getNbMots();i++) {
							int d = g.getDep(i);
							if (d>=0) {
								int h = g.getHead(d);
								if (g.getMot(h).getPOS().startsWith("P")) {
									// cas particulier "il a réuni à Paris"
									int dh = g.getDep(h);
									if (dh>=0) h = g.getHead(dh);
								}
								ih.nextWord(g, i, h, d, true);
								nhbc++;
							} else {
								ih.nextWord(g, i, -1, -1, true);
								nhbc++;
							}
						}
					}
				}
				flist.close();
				System.out.println("nfiles "+nfiles+" ngraphs "+ngraphs+" nwords "+nwords+" nhbc "+nhbc);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static void testdebug(String trainxmll) {
		try {
			final int[] counts = {0,0};
			SelectInstances.ih = new InstanceHandler() {
				@Override
				public void nextWord(DetGraph g, int wordInGraph, int head, int d, boolean isInstance) {
					if (isInstance)
						counts[1]++;
					counts[0]++;
				}
			};
			SelectInstances.parseCorpus(trainxmll);
			System.out.println("nwords in train graphs "+counts[0]+" nhbc "+counts[1]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}
	static void insertInTab(String enlog, String unlabxmll, String trainxmll, String testxmll, String tabtrain, String tabtest) {

		final boolean baseline = false;

		// repere le debut du train et du test dans les samples
		final int[] hbcidx = {0};
		SelectInstances.ih = new InstanceHandler() {
			@Override
			public void nextWord(DetGraph g, int wordInGraph, int head, int d, boolean isInstance) {
				if (isInstance) {
					hbcidx[0]++;
				}
			}
		};
		SelectInstances.parseCorpus(unlabxmll);
		int debTrainInhHbc = hbcidx[0];
		
		try {
			// nombre de samples ?
			int nsamp=0;
			{
				String s;
				BufferedReader f = FileUtils.openFileUTF(enlog);
				for (;;) {
					s=f.readLine();
					if (s==null) break;
					if (s.indexOf("e = ")>=0) break;
				}
				f.close();
				StringTokenizer st = new StringTokenizer(s," ");
				st.nextToken(); // e
				st.nextToken(); // =
				nsamp=st.countTokens();
			}
			
			// calcul de la moyenne des samples
			final int[] samp2Vcl = new int[nsamp];
			final int[] samp2Wcl = new int[nsamp];
			{
				final int nVclasses = 100;
				final int nWclasses = 40;
				int[][] samp2Vclasse = new int[nsamp][nVclasses];
				int[][] samp2Wclasse = new int[nsamp][nWclasses];
				
				String s;
				BufferedReader f = FileUtils.openFileUTF(enlog);
				for (;;) {
					s=f.readLine();
					if (s==null) break;
					if (s.indexOf("e = ")>=0) {
						StringTokenizer st = new StringTokenizer(s," ");
						st.nextToken(); // e
						st.nextToken(); // =
						for (int i=0;i<nsamp;i++) {
							samp2Wclasse[i][Integer.parseInt(st.nextToken())-1]++;
						}
					} else if (s.indexOf("c = ")>=0) {
						StringTokenizer st = new StringTokenizer(s," ");
						st.nextToken(); // c
						st.nextToken(); // =
						for (int i=0;i<nsamp;i++) {
							samp2Vclasse[i][Integer.parseInt(st.nextToken())-1]++;
						}
					}
				}
				f.close();
				for (int i=0;i<nsamp;i++) {
					samp2Vcl[i]=0;
					for (int j=1;j<nVclasses;j++)
						if (samp2Vclasse[i][j]>samp2Vclasse[i][samp2Vcl[i]]) samp2Vcl[i]=j;
					samp2Wcl[i]=0;
					for (int j=1;j<nWclasses;j++)
						if (samp2Wclasse[i][j]>samp2Wclasse[i][samp2Wcl[i]]) samp2Wcl[i]=j;
				}
			}
			
			final int[] sampidx = {0};
			{
				// train
				final Object[] ftabs = {null,null};
				SelectInstances.ih = new InstanceHandler() {
					@Override
					public void nextWord(DetGraph g, int wordInGraph, int head, int d, boolean isInstance) throws Exception {
						String tabline=null;
						if (ftabs[0]!=null) {
							for (;;) {
								tabline = ((BufferedReader)ftabs[0]).readLine();
								tabline = tabline.trim();
								if (tabline.length()>0) break;
							}
						}
						String taboutline = tabline;
						if (isInstance) {
							// je n'utilise pour le moment QUE la classe W
							int cl = samp2Wcl[sampidx[0]++];
							if (ftabs[0]!=null) {
								String[] stt = tabline.split("\t");
								if (stt!=null&&stt.length>=3) {
									String wcl="CLW"+cl;
									taboutline = stt[0]+"\t"+stt[1]+"\t"+wcl+"\t"+stt[3];
								}
							}
							hbcidx[0]++;
						}
						if (baseline)
							((PrintWriter)ftabs[1]).println(tabline);
						else
							((PrintWriter)ftabs[1]).println(taboutline);
					}
				};
				if (tabtrain!=null) {
					ftabs[0]= FileUtils.openFileUTF(tabtrain);
					ftabs[1]= FileUtils.writeFileUTF(tabtrain+".out");
				}
				SelectInstances.parseCorpus(trainxmll);
				if (tabtrain!=null) {
					((BufferedReader)ftabs[0]).close();
					((PrintWriter)ftabs[1]).close();
				}
			}
			{
				// test
				final Object[] ftabs = {null,null};
				SelectInstances.ih = new InstanceHandler() {
					@Override
					public void nextWord(DetGraph g, int wordInGraph, int head, int d, boolean isInstance) throws Exception {
						String tabline=null;
						if (ftabs[0]!=null) {
							for (;;) {
								tabline = ((BufferedReader)ftabs[0]).readLine();
								tabline = tabline.trim();
								if (tabline.length()>0) break;
							}
						}
						String taboutline = tabline;
						if (isInstance) {
							// je n'utilise pour le moment QUE la classe W
							int cl = samp2Wcl[sampidx[0]++];
							if (ftabs[0]!=null) {
								String[] stt = tabline.split("\t");
								if (stt!=null&&stt.length>=3) {
									String wcl="CLW"+cl;
									taboutline = stt[0]+"\t"+stt[1]+"\t"+wcl+"\t"+stt[3];
								}
							}
							hbcidx[0]++;
						}
						if (baseline)
							((PrintWriter)ftabs[1]).println(tabline);
						else
							((PrintWriter)ftabs[1]).println(taboutline);
					}
				};
				if (tabtest!=null) {
					ftabs[0]= FileUtils.openFileUTF(tabtest);
					ftabs[1]= FileUtils.writeFileUTF(tabtest+".out");
				}
				SelectInstances.parseCorpus(testxmll);
				if (tabtest!=null) {
					((BufferedReader)ftabs[0]).close();
					((PrintWriter)ftabs[1]).close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("hbcidx "+hbcidx);
		}
	}

	// je suppose la classe C avant E
	static void analyse(String logfile) throws Exception {
		{
			// lecture des indices des verbes
			BufferedReader f = new BufferedReader(new FileReader(fn+".v"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.lastIndexOf(' ');
				int idx = Integer.parseInt(s.substring(i+1));
				String verbe = s.substring(0,i);
				vocV.put(verbe, idx);
			}
			f.close();
			System.out.println("verb voc read "+vocV.size());
		}
		{
			// lecture des indices des words
			BufferedReader f = new BufferedReader(new FileReader(fn+".w"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.lastIndexOf(' ');
				int idx = Integer.parseInt(s.substring(i+1));
				String word = s.substring(0,i);
				vocW.put(word, idx);
			}
			f.close();
			System.out.println("verb voc read "+vocW.size());
		}
		// pour afficher, on doit construire le vocabulaire inverse
		String[] vocinv = new String[vocV.size()];
		for (String x : vocV.keySet()) vocinv[vocV.get(x)]=x;

		ArrayList<Integer> wordsinst = new ArrayList<Integer>();
		{
			BufferedReader f = new BufferedReader(new FileReader("enO"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int idx = Integer.parseInt(s);
				// les idx commencent a 1
				wordsinst.add(idx-1);
			}
			f.close();
			System.out.println("instances read "+wordsinst.size());
		}
		ArrayList<Integer> verbsinst = new ArrayList<Integer>();
		{
			BufferedReader f = new BufferedReader(new FileReader("enV"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int idx = Integer.parseInt(s);
				// les idx commencent a 1
				verbsinst.add(idx-1);
			}
			f.close();
			System.out.println("instances read "+verbsinst.size());
		}

		class Word implements Comparable<Word> {
			int w;
			int co=0;
			HashMap<Integer,Integer> type = new HashMap<Integer, Integer>();
			public int addType(int t) {
				Integer n = type.get(t);
				if (n==null) n=1; else n++;
				type.put(t, n);
				co++;
				return n;
			}
			@Override
			public int compareTo(Word o) {
				if (o.co<co) return -1;
				else if (o.co>co) return 1;
				else return 0;
			}
			public int[] getBestType(int n) {
				int[] hs = new int[n];
				Arrays.fill(hs, -1);
				for (int h : type.keySet()) {
					for (int i=0;i<hs.length;i++) {
						if (hs[i]<0||type.get(h)>type.get(hs[i])) {
							for (int j=hs.length-1;j>i;j--) hs[j]=hs[j-1];
							hs[i]=h;
							break;
						}
					}
				}
				return hs;
			}
		}

		Word[] words = new Word[vocV.size()];
		for (int i=0;i<words.length;i++) {
			words[i] = new Word();
			words[i].w=i;
		}
		System.out.println("words alloc OK "+words.length);

		int nVerbsClasses = 0;
		BufferedReader f = new BufferedReader(new FileReader(logfile));
		for (;;) {
			String s=f.readLine();
			if (s==null) break;
			if (s.startsWith("c = ")) {
				String[] sse = s.split(" ");
				for (int i=2;i<sse.length;i++) {
					int w = verbsinst.get(i-2);
					int verbclasse =Integer.parseInt(sse[i]);
					if (verbclasse>nVerbsClasses) nVerbsClasses=verbclasse;
					words[w].addType(verbclasse);
				}
			}
		}
		f.close();
		Arrays.sort(words);

		{
			// save all verbs with their class in a file
			PrintWriter ff = new PrintWriter(new FileWriter("verbs2class.txt"));
			for (int i=0;i<words.length;i++) {
				ff.println(vocinv[words[i].w]+"\t"+words[i].getBestType(1)[0]);
			}
			ff.close();
		}

		System.out.println("best words");
		for (int i=0;i<10;i++) {
			System.out.println(vocinv[words[i].w]+" : "+words[i].co+" .. "+words[i].getBestType(1)[0]);
		}

		System.out.println("words per class");
		HashMap<Integer, List<Word>> cl2words = new HashMap<Integer, List<Word>>();
		for (Word w : words) {
			int cl = w.getBestType(1)[0];
			List<Word> ws = cl2words.get(cl);
			if (ws==null) {
				ws = new ArrayList<Word>();
				cl2words.put(cl, ws);
			}
			ws.add(w);
		}
		for (Integer cl : cl2words.keySet()) {
			List<Word> ws = cl2words.get(cl);
			Collections.sort(ws);
			StringBuilder sb = new StringBuilder();
			for (int i=0;i<10&&i<ws.size();i++) {
				sb.append(vocinv[ws.get(i).w]+"-"+ws.get(i).type.get(cl)+" ");
			}
			System.out.println("class "+cl+" : "+sb.toString());
		}

		vocinv = new String[vocW.size()];
		for (String x : vocW.keySet()) vocinv[vocW.get(x)]=x;
		words = new Word[vocW.size()];
		for (int i=0;i<words.length;i++) {
			words[i] = new Word();
			words[i].w=i;
		}
		System.out.println("words alloc OK "+words.length);

		int nWordsClasses=0;
		f = new BufferedReader(new FileReader(logfile));
		for (;;) {
			String s=f.readLine();
			if (s==null) break;
			if (s.startsWith("e = ")) {
				String[] sse = s.split(" ");
				for (int i=2;i<sse.length;i++) {
					int w = wordsinst.get(i-2);
					int wordClasse =Integer.parseInt(sse[i]); 
					if (wordClasse>nWordsClasses) nWordsClasses=wordClasse;
					words[w].addType(wordClasse);
				}
			}
		}
		f.close();

		{
			// save all words with their class in a file
			PrintWriter ff = new PrintWriter(new FileWriter("words2class.txt"));
			for (int i=0;i<words.length;i++) {
				ff.println(vocinv[words[i].w]+"\t"+words[i].getBestType(1)[0]);
			}
			ff.close();
		}

		System.out.println("words per class");
		cl2words = new HashMap<Integer, List<Word>>();
		for (Word w : words) {
			int cl = w.getBestType(1)[0];
			List<Word> ws = cl2words.get(cl);
			if (ws==null) {
				ws = new ArrayList<Word>();
				cl2words.put(cl, ws);
			}
			ws.add(w);
		}
		for (Integer cl : cl2words.keySet()) {
			List<Word> ws = cl2words.get(cl);
			Collections.sort(ws);
			StringBuilder sb = new StringBuilder();
			for (int i=0;i<10&&i<ws.size();i++) {
				sb.append(vocinv[ws.get(i).w]+"-"+ws.get(i).type.get(cl)+" ");
			}
			System.out.println("class "+cl+" : "+sb.toString());
		}

		// association classes de verbe - classes de sujets
		System.out.println("nverbs "+nVerbsClasses+" "+nWordsClasses);
		int[][] verbs2words = new int[nVerbsClasses][nWordsClasses];
		f = new BufferedReader(new FileReader(logfile));
		ArrayList<Integer> verbsClasses = new ArrayList<Integer>();
		for (;;) {
			String s=f.readLine();
			if (s==null) break;
			if (s.startsWith("c = ")) {
				String[] sse = s.split(" ");
				verbsClasses.clear();
				for (int i=2;i<sse.length;i++) {
					verbsClasses.add(Integer.parseInt(sse[i])-1);
				}
			}
			if (s.startsWith("e = ")) {
				String[] sse = s.split(" ");
				for (int i=2;i<sse.length;i++) {
					int wordClasse = Integer.parseInt(sse[i])-1;
					verbs2words[verbsClasses.get(i-2)][wordClasse]++;
				}
				verbsClasses.clear();
			}
		}
		f.close();
		for (int i=0;i<nVerbsClasses;i++) {
			for (int j=0;j<nWordsClasses;j++) {
				System.out.println("VERBWORDCL "+i+" "+j+" "+verbs2words[i][j]);
			}
		}
	}

	// pour le stanford CRF
	public static void creeObsFile(String unlabxmls, String trainxmls, String testxmls) throws Exception {
		final PrintWriter fv = new PrintWriter(new FileWriter("enV"));
		final PrintWriter fw = new PrintWriter(new FileWriter("enO"));
		final PrintWriter fd = new PrintWriter(new FileWriter("enD"));
		
		// dans cette nouvelle version, TOUT mot est affecte a une classe, qui depend du mot qui est son HEAD, quelque soit la relation
		
		final long[] nobs = {0,0}; // inHBC, inTAB
		final String[] tmps = {unlabxmls,trainxmls,testxmls};
		for (String tmpx : tmps) {
			SelectInstances.ih = new InstanceHandler() {
				@Override
				public void nextWord(DetGraph g, int wordInGraph, int h, int d, boolean isInstance) {
					if (isInstance) {
						int v = getVocID(vocV, "ROOT");
						if (h>=0) v = getVocID(vocV, g.getMot(h).getLemme());
						int w = getVocID(vocW, g.getMot(wordInGraph).getForme());
						int dl = getVocID(vocD,"ROOT");
						if (h>=0) dl = getVocID(vocD, g.getDepLabel(d));
						++v; ++w; ++dl;
						fv.println(v);
						fw.println(w);
						fd.println(dl);
						nobs[0]++;
					}
					nobs[1]++;
				}
			};
			SelectInstances.parseCorpus(tmpx);

			// sauve l'indice des obs dans le fichier HBC des 3 parties
			System.out.println("indexobsHBC "+nobs[0]+" "+tmpx);
			System.out.println("indexobsTAB "+nobs[1]+" "+tmpx);
		}
		fd.close();
		fv.close();
		fw.close();
		saveVoc();
	}

	static void debg() {
		try {
			// lecture des indices des words
			BufferedReader f = new BufferedReader(new FileReader(fn+".w"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.lastIndexOf(' ');
				int idx = Integer.parseInt(s.substring(i+1));
				String word = s.substring(0,i);
				vocW.put(word, idx);
			}
			f.close();
			System.out.println("word voc read "+vocW.size());
			
			int Breteauidx = vocW.get("Breteau");
			
			String bests = null;
			f = new BufferedReader(new FileReader("en.log"));
			for (;;) {
				String s= f.readLine();
				if (s==null) break;
				if (s.indexOf("e = ")>=0) bests=s;
			}
			f.close();
			StringTokenizer st = new StringTokenizer(bests);
			st.nextToken();
			st.nextToken();
			
			f = new BufferedReader(new FileReader("enO"));
			for (int i=0;;i++) {
				String s = f.readLine();
				if (s==null) break;
				int w = Integer.parseInt(s);
				String en = st.nextToken();
				if (i<449960) continue;
				if (w==Breteauidx) {
					System.out.println(en);
				}
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
