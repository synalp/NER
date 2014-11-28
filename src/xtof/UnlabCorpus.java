package xtof;

import gmm.LogMath;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

/**
 * contains only the features from the gigaword
 * 
 * @author xtof
 *
 */
public class UnlabCorpus {
	int featureSpaceSize;
	int[][] feats;
	Random rand = new Random();

	public static void main(String args[]) {
		UnlabCorpus m = loadFeatureFile();
		LinearModelNoStanford c = new LinearModelNoStanford(m);
		float[] sc = c.computeAllScores();
		final float delta=0.2f, epsilon=0.2f; 
		m.buildcoreset(sc, delta, epsilon);
	}

	public int closestidx;
	// s must be sorted
	private float getDist(float x, float[] s) {
		int i=Arrays.binarySearch(s, x);
		if (i>=0) {
			closestidx=i;
			return 0;
		}
		int idx=-i-1;
		if (idx==0) { // idx = first element greater than x = where x would be inserted
			closestidx=0;
			return s[0]-x;
		} else if (idx==s.length) {
			closestidx=s.length-1;
			return x-s[s.length-1];
		} else {
			float d1=s[idx]-x;
			float d2=x-s[idx-1];
			if (d1<d2) {
				closestidx=idx; return d1;
			} else {
				closestidx=idx-1; return d2;
			}
		}
	}

	// delta gives the probability that the result is outside the bounds
	public void buildcoreset(float[] d, final float delta, final float epsilon) {
		LogMath logMath = new LogMath();
		int sizeDprim=d.length, sizeB=0;
		final float lnd = logMath.linearToLog(1/delta);
		System.out.println("lnd "+lnd);
		float[] m = new float[d.length];

		{
			float[] b = new float[d.length/5]; // b is smaller than d; but how much ?
			float[] s = new float[(int)(20*lnd)];
			while (sizeDprim>s.length) {
				int nEx2remove = sizeDprim/2 - s.length;
System.out.println("nex to remove "+nEx2remove+" "+s.length);
				// sample S
				for (int i=0;i<s.length;i++) {
					int j=rand.nextInt(sizeDprim);
					s[i]=d[j]; // D' is actually contained in D
					// immediately removes this point from D'
					// actually just swap it, because we need the full D again later on
					float dtmp=d[j]; d[j]=d[--sizeDprim]; d[sizeDprim]=dtmp;
				}
				if (sizeDprim>0) {
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
					for (int i=0;i<nEx2remove;i++) {
						float dtmp=d[distidx[i]]; d[distidx[i]]=d[--sizeDprim]; d[sizeDprim]=dtmp;
					}
				}
				// add S in B
				for (int i=0;i<s.length;i++) b[sizeB++]=s[i];
			}
			// add D' in B
			for (int i=0;i<sizeDprim;i++) b[sizeB++]=d[i];
			
			b=Arrays.copyOf(b, sizeB);
			System.gc();
			Arrays.sort(b);
			System.out.println("size B "+sizeB+" D' "+sizeDprim);

			// Db contains the points that are represented by b
			// Let z = sum_{x \in D} dist(x,B)^2
			// TODO: we might not need to store the closestB of all data points... ?
			int[] closestB = new int[d.length];
			float z=0;
			for (int i=0;i<d.length;i++) {
				float dist = getDist(d[i], b);
				closestB[i]=closestidx;
				z+=dist*dist;
			}
			System.out.println("z "+z);

			// compute size of Db
			int[] sizeDb = new int[sizeB];
			Arrays.fill(sizeDb, 0);
			for (int i=0;i<d.length;i++) sizeDb[closestB[i]]++;
			System.out.println("Db size "+sizeDb[0]+" "+sizeDb[1]+" ...");

			// now we can compute the "importance weights" m of every point in D
			for (int i=0;i<d.length;i++) {
				float dist = Math.abs(b[closestB[i]]-d[i]);
				m[i]=5/sizeDb[closestB[i]]+dist*dist/z;
			}
			System.gc();
		}
		float z = 0;
		for (float x : m) z+=x;
		for (int i=0;i<m.length;i++) m[i]/=z;
		System.out.println("Importance weights "+m[0]+" "+m[1]+" ...");
		
		// we can now sample from this multinomial
		int npts = (int)(20*(double)(sizeB*sizeB)*lnd/(epsilon*epsilon));
		System.out.println("npts to sample "+npts+" "+d.length+" "+sizeB);
		if (npts>d.length/10) {
			System.out.println("ERROR: coresets too large");
			return;
		}
		HashSet<Integer> cidx = new HashSet<Integer>();
		while (cidx.size()<npts) {
			int i=sample_Mult(m);
			cidx.add(i);
		}
		System.out.println("sample done "+cidx.size());
	}

	private int sample_Mult(float[] th) {
		float s = 0;
		for (int i = 0; i < th.length; i++)
			s += th[i];
		s *= rand.nextFloat();
		for (int i = 0; i < th.length; i++) {
			s -= th[i];
			if (s < 0)
				return i;
		}
		return 0;
	}
	
	public static UnlabCorpus loadFeatureFile() {
		System.out.println("loading gigaword features...");
		UnlabCorpus c = new UnlabCorpus();
		int nread = 0;
		c.featureSpaceSize=0;
		try {
			DataInputStream g = new DataInputStream(new FileInputStream("unlabfeats.dat"));
			final int nmax = 10000;
			c.feats = new int[nmax][];
			for (int i=0;i<nmax;i++) {
				int nf = g.readInt();
				nread++;
				c.feats[i]=new int[nf];
				for (int j=0;j<nf;j++) {
					c.feats[i][j]=g.readInt();
					if (c.feats[i][j]>=c.featureSpaceSize) c.featureSpaceSize=c.feats[i][j]+1;
				}
			}
			g.close();
		} catch (EOFException e) {
			c.feats=Arrays.copyOf(c.feats, nread);
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("loading done "+nread);
		return c;
	}
}
