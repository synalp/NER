package ester2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.ponctuation.UttSegmenter;

import edu.stanford.nlp.ie.crf.CRFClassifier;

/**
 * Utiliser les classes de HBC ne permet pas de desambiguiser les mots en fonction
 * de leur contexte: ex: "Gaza" est classe comme "loc" alors qu'il est sujet de "crie" !
 * Il vaut donc mieux recuperer les classes de HBC calculees directement sur le test
 * 
 * TODO:
 * - relire reviews pour Pavel
 * - tester d'abord avec 1 nouveau modele pour org et les classes a la place des POStags
 *   FAIT: ne marche pas mieux, car les verbes d'un certain type sont rarement précédés d'une EN, mais souvent d'un pronom, ou d'un GN simple
 *   Un autre probleme est que le CRF n'apprend pas a utiliser les classes pour discriminer
 *   entre plusieurs ENs possibles, mais seulement entre une EN et NOEN.
 *   Il faut tester avec un seul CRF et tous les ENs possibles !
 *   
 * @author cerisara
 *
 */
public class JCommands {
	final static String[] allens = {"pers","fonc","org","loc","prod","time","amount"};
	final static String[] allens4merge = allens;

	// false pour avoir la baseline
	final static String HBCopt = "false";
	
	public static void main(String[] args) {
		if (args.length==0) {
/*
			makeTABSforCRF_v2(false);
			insertClasses_v2(false);
			trainCRF_v2();
			makeTABSforCRF_v2(true);
			insertClasses_v2(true);
			testCRF_v2();
*/
			merge_v2();
			eval();
			return;
		}
			
		if (args[0].equals("makeTabsTrain")) makeTABSforCRF(false);
		else if (args[0].equals("makeTabsTest")) makeTABSforCRF(true);
		else if (args[0].equals("insertClassesTrain")) insertClasses(false);
		else if (args[0].equals("insertClassesTest")) insertClasses(true);
		else if (args[0].equals("trainCRF")) trainCRF();
		else if (args[0].equals("testCRF")) testCRF();
		else if (args[0].equals("eval")) eval();
	}

	public static void eval() {
		System.out.println("running EVAL");
		GraphIO gio = new GraphIO(null);
		int nok=0, nerr=0;
		DetGraph[] res1000 = new DetGraph[1000];
		int idx1000 = 0;
		try {
			BufferedReader f=new BufferedReader(new FileReader("test/trs2xml.list"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String[] st = s.split(" ");
				String trs = st[0];
				String xml = st[1].replace(".xml", ".xml.merged.xml");
				File ff = File.createTempFile("stmnetmp", "trsl");
				PrintWriter ffp = new PrintWriter(ff);
				ffp.println(trs);
				ffp.close();
				ESTER2EN e = new ESTER2EN();
				e.loadTRS(ff.getAbsolutePath());
//				List<DetGraph> ref = STMNEParser.loadTRS(ff.getAbsolutePath());
				List<DetGraph> ref = e.toGraphs();
				ff.delete();
				
				UttSegmenter.segmente(ref);
				
				List<DetGraph> rec = gio.loadAllGraphs(xml);
				assert ref.size()==rec.size();
				System.out.println("ref/rec "+ref.size()+" "+rec.size());
				for (int i=0;i<ref.size();i++) {
					DetGraph gref = ref.get(i);
					DetGraph grec = rec.get(i);
					assert gref.getNbMots()==grec.getNbMots();
					boolean iserr=false;
					HashSet<Integer> errs = new HashSet<Integer>();
					for (int j=0;j<gref.getNbMots();j++) {
						int[] groupsrefi = gref.getGroups(j);
						if (groupsrefi==null) groupsrefi = new int[0];
						String[] groupsref = new String[groupsrefi.length];
						for (int k=0;k<groupsrefi.length;k++)
							groupsref[k]=gref.groupnoms.get(groupsrefi[k]);
						int[] groupsreci = grec.getGroups(j);
						if (groupsreci==null) groupsreci = new int[0];
						String[] groupsrec = new String[groupsreci.length];
						for (int k=0;k<groupsreci.length;k++)
							groupsrec[k]=grec.groupnoms.get(groupsreci[k]);
						
						HashSet<Integer> matched = new HashSet<Integer>();
						for (int k=0;k<groupsref.length;k++) {
							int l=0;
							for (;l<groupsrec.length;l++)
								if (groupsref[k].startsWith(groupsrec[l])) {
									nok++;
									matched.add(l);
									break;
								}
							if (l>=groupsrec.length) {
								iserr=true;
								nerr++;
								errs.add(groupsrefi[k]);
							}
						}
						for (int k=0;k<groupsrec.length;k++) {
							if (!matched.contains(k)) {
								iserr=true;
								nerr++;
								errs.add(-groupsreci[k]-1);
							}
						}
					}
					for (int gg : errs) {
						if (gg>=0) gref.groupnoms.set(gg, gref.groupnoms.get(gg)+"R");
						else grec.groupnoms.set(-gg-1, grec.groupnoms.get(-gg-1)+"R");
					}
					if (iserr && idx1000<999) {
						res1000[idx1000++]=gref;
						res1000[idx1000++]=grec;
					}
				}
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		float acc = (float)nok/(float)(nok+nerr);
		System.out.println("eval nok "+nok+" nerr "+nerr +" ntot "+(nok+nerr)+" acc="+acc);
		if (idx1000>0) {
			gio.save(Arrays.asList(res1000), "res1000.xml");
			JSafran.viewGraph(res1000);
		}
	}
	
	public static void insertClasses(boolean isTest) {
		String suff = ".train";
		String xmll = "train.xmll";
		if (isTest) {
			suff=".test";
			xmll="test.xmll";
		}

		System.out.println("insert HBC classes into TABS");
		try {
			for (String en: allens) {
				final String[] args = {"-inserttab","groups."+en+".tab"+suff,"verbs2class.txt","words2class.txt","0","0",xmll};
				Unsup.main(args);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void insertClasses_v2(boolean isTest) {
		String suff = ".train";
		String xmll = "train.xmll";
		if (isTest) {
			suff=".test";
			xmll="test.xmll";
		}

		System.out.println("insert HBC classes into TABS");
		try {
			final String[] args = {"-inserttab","groups.all.tab"+suff,"verbs2class.txt","words2class.txt","0","0",xmll};
			Unsup.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void trainCRF() {
		System.out.println("train CRF");
		try {
			for (String en: allens) {
				final String[] args = {"-map","word=0,tag=1,synt1=2,synt2=3,answer=4",
						"-useWord","true","-useTags","true",
						"-usePrev","true","-useNext","true",
						"-useSynt1",HBCopt,"-useSynt2",HBCopt,
						"-backgroundSymbol","NO",
						"-tolerance","0.01",
						"-trainFile","groups."+en+".tab.train.out",
						"-serializeTo","en"+en+".mods"
				};
				CRFClassifier.main(args);
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void trainCRF_v2() {
		System.out.println("train CRF");
		try {
			final String[] args = {"-map","word=0,tag=1,synt1=2,synt2=3,answer=4",
					"-useWord","true","-useTags","true",
					"-usePrev","true","-useNext","true",
					"-useSynt1",HBCopt,"-useSynt2",HBCopt,
					"-backgroundSymbol","NO",
					"-tolerance","0.01",
					"-trainFile","groups.all.tab.train.out",
					"-serializeTo","enall.mods"
			};
			CRFClassifier.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void merge() {
		String[] args = new String[allens4merge.length+2];
		args[0]="-mergeens";
		args[1]="test.xmll";
		int i=2;
		for (String en : allens4merge) args[i++]=en;
		ESTER2EN.main(args);
	}
	public static void merge_v2() {
		String[] args = new String[3];
		args[0]="-mergeens";
		args[1]="test.xmll";
		args[2]="all";
		ESTER2EN.main(args);
	}
	
	// cette evaluaation ne marche pas sous windows
	// je prefere utiliser la methode "eval()"
	public static void mergeAndEval() {
		String[] args = new String[allens.length+2];
		args[0]="-mergeen";
		args[1]="test.xmll";
		int i=2;
		for (String en : allens) args[i++]=en;
		ESTER2EN.main(args);

		System.out.println("convert merged xmls into stm-ne");
		try {
			BufferedReader f=new BufferedReader(new FileReader("test/trs2xml.list"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String[] st = s.split(" ");
				String trs = st[0];
				String xml = st[1].replace(".xml", ".xml.merged.xml");
				String out = st[1].replace(".xml",".stm-ne");
				final String[] xargs = {"-project2stmne",xml,trs,out};
				STMNEParser.main(xargs);
			}
			f.close();

			System.out.println("eval score");
			Runtime rt = Runtime.getRuntime();

			final String dest2 = "/cygdrive/c/xtof/corpus/ESTER2ftp/package_scoring_ESTER2-v1.7/information_extraction_task/";

			final String[] env = {"PATH=C:/cygwin/bin/;"+dest2+"/tools"};
			String cmd = "perl.exe "+dest2+"/tools/score-ne -v -rd "+dest2+"../../EN/test -cfg "+
			dest2+"example/ref/NE-ESTER2.cfg -dic "+
			dest2+"tools/ESTER1-dictionnary-v1.9.1.dic test/*.stm-ne";
			final Process p = rt.exec(cmd,env);
			Thread t = new Thread(new Runnable() {
				BufferedReader ierr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				@Override
				public void run() {
					try {
						for (;;) {
							String s = ierr.readLine();
							if (s==null) break;
							System.out.println("[ERR] "+s);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			t.run();
			Thread tt = new Thread(new Runnable() {
				BufferedReader ierr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				@Override
				public void run() {
					try {
						for (;;) {
							String s = ierr.readLine();
							if (s==null) break;
							System.out.println("[OUT] "+s);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			tt.run();
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void testCRF() {
		System.out.println("test CRF");
		try {
			for (String en: allens) {
				final String[] args = {"-map","word=0,tag=1,synt1=2,synt2=3,answer=4",
						"-useWord","true","-useTags","true",
						"-usePrev","true","-useNext","true",
						"-useSynt1",HBCopt,"-useSynt2",HBCopt,
						"-backgroundSymbol","NO",
						"-testFile","groups."+en+".tab.test.out",
						"-loadClassifier","en"+en+".mods"
				};
				PrintStream sout = System.out;
				PrintStream fout = new PrintStream("test."+en+".log");
				System.setOut(fout);
				CRFClassifier.main(args);
				fout.close();
				System.setOut(sout);
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void testCRF_v2() {
		System.out.println("test CRF");
		try {
			final String[] args = {"-map","word=0,tag=1,synt1=2,synt2=3,answer=4",
					"-useWord","true","-useTags","true",
					"-usePrev","true","-useNext","true",
					"-useSynt1",HBCopt,"-useSynt2",HBCopt,
					"-backgroundSymbol","NO",
					"-testFile","groups.all.tab.test.out",
					"-loadClassifier","enall.mods"
			};
			PrintStream sout = System.out;
			PrintStream fout = new PrintStream("test.all.log");
			System.setOut(fout);
			CRFClassifier.main(args);
			fout.close();
			System.setOut(sout);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void makeTABSforCRF(boolean isTest) {
		System.out.println("create TAB files for Stanford CRF");

		String suff = ".train";
		String xmll = "train.xmll";
		if (isTest) {
			suff=".test";
			xmll="test.xmll";
		}

		for (String en: allens) {
			final String[] args = {"-saveNER",xmll,en};
			ESTER2EN.main(args);
			File f = new File("groups."+en+".tab");
			File ff = new File("groups."+en+".tab"+suff);
			f.renameTo(ff);
		}
	}
	public static void makeTABSforCRF_v2(boolean isTest) {
		makeTABSforCRF(isTest);
		// puis on merge tous les fichiers TABS en un seul
		String suff = ".train";
		if (isTest) suff =".test";
		try {
			PrintWriter fout = FileUtils.writeFileUTF("groups.all.tab"+suff);
			BufferedReader fs[] = new BufferedReader[allens.length];
			for (int i=0;i<allens.length;i++)
				fs[i] = FileUtils.openFileUTF("groups."+allens[i]+".tab"+suff);
			String en="NO";
			for (;;) {
				en="NO";
				String s = fs[0].readLine();
				if (s==null) break;
				if (s.trim().length()==0) {
					for (int i=1;i<allens.length;i++)
						fs[i].readLine();
					continue;
				}
				String[] ss = s.split("\t");
				if (en.equals("NO")&&!ss[ss.length-1].equals("NO")) {
					en=ss[ss.length-1];
				}
				for (int i=1;i<allens.length;i++) {
					String sx = fs[i].readLine();
					String[] ssx = sx.split("\t");
					if (en.equals("NO")&&!ssx[ssx.length-1].equals("NO")) {
						en=ssx[ssx.length-1];
					}
				}
				for (int i=0;i<ss.length-1;i++)
					fout.print(ss[i]+"\t");
				fout.println(en);
			}
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
