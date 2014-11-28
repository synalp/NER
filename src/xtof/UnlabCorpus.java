package xtof;

import gmm.LogMath;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * contains only the features from the gigaword
 * 
 * @author xtof
 *
 */
public class UnlabCorpus {
	int[][] feats;
	
	public static void main(String args[]) {
		loadFeatureFile();
	}
	
	// s must be sorted
	private float getDist(float x, float[] s) {
		int i=Arrays.binarySearch(s, x);
		if (i>=0) return 0;
		int idx=-i-1;
		if (idx==0) { // idx = first element greater than x = where x would be inserted
			return s[0]-x;
		} else if (idx==s.length) {
			return x-s[s.length-1];
		} else {
			float d1=s[idx]-x;
			float d2=x-s[idx-1];
			return d1>d2?d2:d1;
		}
	}
	
	// delta gives the probability that the result is outside the bounds
	public void buildcoreset(float[] d, final float delta) {
		Random rand = new Random();
		LogMath logMath = new LogMath();
		int sizeDprim=d.length, sizeB=0;
		final float lnd = logMath.linearToLog(1/delta);
		float[] b = new float[d.length/5]; // b is smaller than d; but how much ?
		float[] s = new float[(int)(20*lnd)];
		int sizeS=0;
		while (sizeDprim>s.length) {
			int nEx2remove = sizeDprim/2 - s.length;
			// sample S
			for (int i=0;i<s.length;i++) {
				int j=rand.nextInt(sizeDprim);
				s[sizeS++]=d[j]; // D' is actually contained in D
				// immediately removes this point from D'
				d[j]=d[sizeDprim--];
			}
			// remove closest points
			Arrays.sort(s);
			float[] dist = new float[sizeDprim];
			int[] distidx = new int[dist.length];
			for (int i=0;i<dist.length;i++) distidx[i]=i;
			for (int i=0;i<sizeDprim;i++) {
				dist[i]=getDist(d[i],s);
			}
			QuickSort qs = new QuickSort();
			qs.sort(dist, distidx);
			for (int i=0;i<nEx2remove;i++) d[distidx[i]]=d[sizeDprim--];
			// add S in B
			for (int i=0;i<s.length;i++) b[sizeB++]=s[i];
		}
		
	}
	
	public static UnlabCorpus loadFeatureFile() {
		System.out.println("loading gigaword features...");
		UnlabCorpus c = new UnlabCorpus();
		try {
			DataInputStream g = new DataInputStream(new FileInputStream("unlabfeats.dat"));
			int nfeats = g.readInt();
			c.feats = new int[nfeats][];
			for (int i=0;i<nfeats;i++) {
				int nf = g.readInt();
				c.feats[i]=new int[nf];
				for (int j=0;j<nf;j++) {
					c.feats[i][j]=g.readInt();
				}
			}
			g.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("loading done "+c.feats.length);
		return c;
	}
}
