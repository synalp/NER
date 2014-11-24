package xtof;

import edu.emory.mathcs.backport.java.util.Arrays;
import gmm.LogMath;

/**
 * Computes the risk and maps automatically LC classes with priors classes.
 * We don't need to pre-map the LC to priors classes, because the LC may represent both classes equivalently, just by negating weights.
 * So we just dont care about what the mapping really is;
 * 
 * @author xtof
 *
 */
public class RiskMachine {
	public float computeRisk(float[] scores) {
		// first, I find the 2 modes of the scores, without using any prior, just by looking at the data:
		GMMDiag gmm = new GMMDiag();
	  	double[] post=gmm.train(scores);
	  	// then 
	  	return 0;
	}
	
	private class GMMDiag {
		// mean0 always represents the mode with the highest score
		public float mean0, var0, gconst0, logw0;
		public float mean1, var1, gconst1, logw1;
		private LogMath logMath = new LogMath();
		
		public double[] train(float[] x) {
			train1gauss(x);
			split();
			return trainEM(x);
		}
		public void train1gauss(float[] xs) {
			float sumx=0, sumxx=0;
			for (float x:xs) {sumx+=x; sumxx+=x*x;}
			mean0=sumx/(float)xs.length;
			var0=sumxx/(float)xs.length-mean0*mean0;
			if (var0<Parms.minvarGMM) var0=Parms.minvarGMM;
			gconst0=calcGconst(var0);
			logw0=0;
		}
		public void split() {
			float d=0.1f*var0;
			mean1=mean0-d;
			mean0+=d;
			logw0=logMath.linearToLog(0.5);
			logw1=logw0;
		}
		public double[] trainEM(float[] xs) {
			double[] post = {0,0};
			for (int i=0;i<Parms.nitersGMMtraining;i++) {
				Arrays.fill(post, 0);
				float sumx0=0, sumxx0=0;
				float sumx1=0, sumxx1=0;
				for (float x : xs) {
					float l0=logw0+getLoglike(mean0,var0,gconst0,x);
					float l1=logw1+getLoglike(mean1,var1,gconst1,x);
					float normConst = logMath.addAsLinear(l0, l1);
					double post0=logMath.logToLinear(l0-normConst);
					double post1=1-post0;
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
		private float getLoglike(float m, float v, float g, float x) {
			double inexp=((x-m)*(x-m))/v;
	        inexp/=2.0;
	        double loglikeYt = - g - inexp;
	        return (float)loglikeYt;
		}
		private float calcGconst(float var) {
			double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(var);
			co/=2.0;            
			return (float)co;
		}
	}
}
