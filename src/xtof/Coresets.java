package xtof;

import gmm.LogMath;

import java.util.Arrays;
import java.util.Random;

public class Coresets {
	static Random rand = new Random();
	final float delta=0.1f, epsilon=0.1f; 
	final int beta = 20;

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
	public float[][] buildcoreset(float[] d, int nsets) {
		LogMath logMath = new LogMath();
		int sizeDprim=d.length, sizeB=0;
		final float lnd = logMath.linearToLog(1/delta);
		System.out.println("lnd "+lnd);
		float[] m = new float[d.length];

		{
			float[] b = new float[d.length/5]; // b is smaller than d; but how much ?
			float[] s = new float[beta];
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

			float zz = 0;
			for (float x : m) zz+=x;
			for (int i=0;i<m.length;i++) m[i]/=zz;
			System.out.println("Importance weights "+m[0]+" "+m[1]+" ...");

			// we can now sample from this multinomial
			//		int npts = (int)(20*(double)(sizeB*sizeB)*lnd/(epsilon*epsilon));
			int npts = nsets;
			System.out.println("npts to sample "+npts+" "+d.length+" "+sizeB);
			if (npts>d.length/10) {
				System.out.println("ERROR: coresets too large");
				return null;
			}

			float[][] res = new float[2][nsets];
			for (int ii=0;ii<npts;ii++) {
				int i=sample_Mult(m);
				res[0][ii]=d[i];
				float gamma = zz/(float)npts/m[i];
				res[1][ii]=gamma;
			}
			return res;
		}
	}

	static int sample_Mult(float[] th) {
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
	
	/**
	 * This class is an adapted version of GMMDiag that does
	 * a *weighted* EM training
	 * 
	 * @author xtof
	 *
	 */
	public static class GMMDiag extends RiskMachine.GMMDiag {
		// mean0 always represents the mode with the highest score
		private float[] exw;
		
		public GMMDiag(float[] weights) {
			super();
			exw = weights;
		}
		
		public double[] train(float[] x) {
			initSample(x);
			return trainEM(x);
		}
		private void initSample(float[] x) {
			int i=sample_Mult(exw);
			mean0=x[i]; var0=1; gconst0=calcGconst(var0); logw0=logMath.linearToLog(0.5);
			i=sample_Mult(exw);
			mean1=x[i]; var1=1; gconst1=calcGconst(var1); logw1=logMath.linearToLog(0.5);
		}
		public double[] trainEM(float[] xs) {
			double[] post = {0,0};
			for (int i=0;i<Parms.nitersGMMtraining;i++) {
				Arrays.fill(post, 0);
				float sumx0=0, sumxx0=0;
				float sumx1=0, sumxx1=0;
				for (int j=0;j<xs.length;j++) {
					float x=xs[j];
					float l0=logw0+getLoglike(mean0,var0,gconst0,x);
					float l1=logw1+getLoglike(mean1,var1,gconst1,x);
					float normConst = logMath.addAsLinear(l0, l1);
					double post0=logMath.logToLinear(l0-normConst);
					double post1=1-post0;
					post0 *= exw[j];
					post1 *= exw[j];
					post[0]+=post0;
					post[1]+=post1;
					sumx0+=post0*x; sumxx0+=post0*x*x;
					sumx1+=post1*x; sumxx1+=post1*x*x;
				}
				mean0=sumx0/(float)post[0];
				var0=sumxx0/(float)post[0]-mean0*mean0;
				if (var0<Parms.minvarGMM) var0=Parms.minvarGMM;
				gconst0=calcGconst(var0);
				logw0=logMath.linearToLog(post[0]);
				mean1=sumx1/(float)post[1];
				var1=sumxx1/(float)post[1]-mean1*mean1;
				if (var1<Parms.minvarGMM) var1=Parms.minvarGMM;
				gconst1=calcGconst(var1);
				logw1=logMath.linearToLog(post[1]);
			}
			return post;
		}
	}

}
