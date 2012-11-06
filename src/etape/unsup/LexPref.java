package etape.unsup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;

/**
 * represents a syntactic frame, or another more general unsupervised criterion.
 * It keeps counts of occurrences of a given variable and applies multinomial-dirichlet
 * 
 * @author xtof
 *
 */
public class LexPref {

	final double alpha = 0.001;
	public HashMap<String, int[]> dep2frameCounts = new HashMap<String, int[]>();
	ArrayList<String> forms2keep = new ArrayList<String>();
	ArrayList<String> postag2keep = new ArrayList<String>();
	int nFramesDiff = 0;
	String group;

	public LexPref(List<DetGraph> gs, String group) {
		this.group=group;
		{
			final int nFormsMin = 0;
			// uses FORMS for the most frequent words
			HashMap<String, Integer> vocForms =new HashMap<String, Integer>();
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					String w = g.getMot(i).getForme();
					Integer n = vocForms.get(w);
					if (n==null) n=0;
					n++;
					vocForms.put(w, n);
				}
			}
			for (String w : vocForms.keySet()) {
				if (vocForms.get(w)>=nFormsMin) forms2keep.add(w);
			}
			Collections.sort(forms2keep);
			System.out.println("keeping nFORMS= "+forms2keep.size());
			// si, au test, la forme n'est pas dans cette liste, alors on utilisera le POStag
		}
		{
			final int nPOSMin = 50;
			// uses FORMS for the most frequent words
			HashMap<String, Integer> vocPOS =new HashMap<String, Integer>();
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					String postag = g.getMot(i).getPOS();
					Integer n = vocPOS.get(postag);
					if (n==null) n=0;
					n++;
					vocPOS.put(postag, n);
				}
			}
			for (String postag : vocPOS.keySet()) {
				if (vocPOS.get(postag)>=nPOSMin) postag2keep.add(postag);
			}
			// si, au test, le POStag n'est pas dans cette liste, alors c'est:
			postag2keep.add("UNK");
			// on doit aussi considerer le ROOT de la phrase:
			postag2keep.add("ROOT");
			Collections.sort(postag2keep);
			System.out.println("keeping nPOS= "+postag2keep.size());
		}
		nFramesDiff=forms2keep.size()+postag2keep.size();
		System.out.println("nFrames = "+nFramesDiff);
	}
	// returns the "keys" on which the frame is conditioned
	// ici, c'est YES ou NONE, car on veut calculer la pdf pour chaque groupe
	public String getKey(DetGraph g, int w) {
		String grp="NONE";
		int[] grps = g.getGroups(w);
		if (grps!=null)
			for (int gr : grps) {
				String obsgrp = g.groupnoms.get(gr);
				if (obsgrp.charAt(0)=='R') obsgrp=obsgrp.substring(1);
				if (obsgrp.equals(group)) {
					grp="YES";
					break;
				}
			}
		return grp;
	}
	public void decreaseFrameCounts(DetGraph g) {
		for (int w=0;w<g.getNbMots();w++) {
			int fridx = getFrameIndex(g, w);
			String key=getKey(g, w);
			int[] frcounts = dep2frameCounts.get(key);
			frcounts[fridx]--;
			if (frcounts[fridx]<0) {
				System.out.println("ERROR fr counts "+frcounts[fridx]);
			}
		}
	}
	public void increaseFrameCounts(DetGraph g) {
		for (int w=0;w<g.getNbMots();w++) {
			int fridx = getFrameIndex(g, w);
			String key=getKey(g, w);
			int[] frcounts = dep2frameCounts.get(key);
			if (frcounts==null) {
				frcounts = new int[nFramesDiff];
				dep2frameCounts.put(key, frcounts);
			}
			if (fridx<0||fridx>=frcounts.length) {
				System.out.println("ERROR frcounts "+key+" "+w+" "+fridx+" "+nFramesDiff);
				JSafran.viewGraph(g,w);
			}
			frcounts[fridx]++;
		}
	}
	// retourne le word ID
	public int getFrameIndex(DetGraph g, int w) {
		int widx;
		int i=Collections.binarySearch(forms2keep, g.getMot(w).getForme());
		if (i>=0) widx = i;
		else {
			i=Collections.binarySearch(postag2keep, g.getMot(w).getPOS());
			if (i>=0) widx=forms2keep.size()+i;
			else widx=forms2keep.size()+Collections.binarySearch(postag2keep, "UNK");
		}
		return widx;
	}
	public double getLogDirMult(DetGraph g, int w) {
		// recupere l'ID de la frame du mot i
		int fridx = getFrameIndex(g,w);
		// calcule le posterior smoothed (avec les counts)
		String key=getKey(g, w);

		double anneal = 1;
		//		if (anneal>1) anneal=1;
		int[] frcounts = dep2frameCounts.get(key);
		if (frcounts==null) {
			double num = Math.log(alpha);
			double sum = nFramesDiff*alpha;
			sum = Math.log(sum);
			num-=sum;
			return num;
		}
		double num = Math.log(alpha+anneal*frcounts[fridx]);
		double sum = 0;
		for (int co : frcounts) sum+=alpha+anneal*co;
		sum = Math.log(sum);
//				System.out.println("dbug logpost "+fridx+"/"+frcounts.length+" "+num+" "+sum);
		num-=sum;
		return num;
	}
	public void saveCounts(String countsfile) {
		try {
			PrintWriter f = new PrintWriter(new FileWriter(countsfile));
			for (String pos : dep2frameCounts.keySet()) {
				f.println(pos+" "+Arrays.toString(dep2frameCounts.get(pos)));
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void loadCounts(String countsfile) {
		System.out.println("nframesdiff "+nFramesDiff);
		dep2frameCounts.clear();
		try {
			BufferedReader f = new BufferedReader(new FileReader(countsfile));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				int i=s.indexOf(' ');
				String pos = s.substring(0,i);
				StringTokenizer st = new StringTokenizer(s.substring(i+1), " [],");
				int[] co = new int[nFramesDiff];
				dep2frameCounts.put(pos, co);
				for (i=0;i<nFramesDiff;i++) {
					String x = null;
					for (;;) {
						x = st.nextToken();
						if (x.length()>0) break;
					}
					co[i]=Integer.parseInt(x);
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
