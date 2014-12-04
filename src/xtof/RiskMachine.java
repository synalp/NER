package xtof;

import linearclassifier.AnalyzeLClassifier;
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
	// marginal posteriors and priors per class:
	double[] priors;
	
	public RiskMachine(double[] priors) {
		this.priors=priors;
	}
	
	/**
	 * retrains the GMM
	 * 
	 * @param scores
	 * @param gmm
	 * @return
	 */
	public float computeRisk(float[] scores, GMMDiag gmm) {
		// first, I find the 2 modes of the scores, without using any prior, just by looking at the data:
	  	gmm.train(scores);
	  	return computeRisk(gmm);
	}
	/**
	 * Assumes that the GMM has already been trained
	 * 
	 * @param scores
	 * @param gmm
	 * @return
	 */
	public float computeRisk(GMMDiag gmm) {
	  	// then I switch priors if they are in reverse order than post
	  	if ((priors[0]>priors[1] && gmm.post[1]>gmm.post[0])||
	  			(priors[0]<priors[1] && gmm.post[1]<gmm.post[0])) {
	  		System.out.println("reverting priors");
	  		double d=priors[0];
	  		priors[0]=priors[1];
	  		priors[1]=d;
	  	}
	  	// I can now compute the risk
	  	final float sqrtpi = (float)Math.sqrt(Math.PI);
	  	final float pi = (float)Math.PI;
	  	final float sigma00 = (float)Math.sqrt(gmm.var0);
	  	final float sigma10 = (float)Math.sqrt(gmm.var1);
	  	final float var00 = gmm.var0;
	  	final float var10 = gmm.var1;
	  	final float mean00  = gmm.mean0;
	  	final float mean10  = gmm.mean1;
	      
	  	float t1 = (float)priors[0]*(1f-2f*mean00)/(4f*sigma00*sqrtpi) * (1f+(float)AnalyzeLClassifier.erf( (0.5-mean00)/sigma00 ));
	  	float t2 = (float)priors[0]/(2f*pi) * (float)Math.exp( -(0.5f-mean00)*(0.5f-mean00)/var00 );
	  	float t3 = (float)priors[1]*(1f+2f*mean10)/(4f*sigma10*sqrtpi) * (1f-(float)AnalyzeLClassifier.erf( (-0.5-mean10)/sigma10 ));
	  	float t4 = (float)priors[1]/(2f*pi) * (float)Math.exp( -(-0.5f-mean10)*(-0.5f-mean10)/var10 );
	  	return t1+t2+t3+t4;
	}
	
	// TODO: move this method elsewhere
	public static void updateGMMAfterRiskGradientStep(GMMDiag gmm, int nex, int[] exImpacted, float gradStep) {
		float postsum0=0;
		for (int ex : exImpacted) postsum0+=gmm.postPerEx[ex];
		float oldMean0 = gmm.mean0;
		gmm.mean0 += postsum0 * gradStep / gmm.post[0];
		float postsum1=0;
		for (int ex : exImpacted) postsum1+=1f-gmm.postPerEx[ex];
		float oldMean1 = gmm.mean1;
		gmm.mean1 += postsum1 * gradStep / gmm.post[1];

		// bug ? I have very strange values for the var...
//		gmm.var0 += (gmm.mean0-oldMean0)*(gmm.mean0-oldMean0) +
//				gradStep*(gradStep-2f*gmm.mean0) + 2f*gradStep*oldMean0*(float)exImpacted.length/(float)nex;
//		if (gmm.var0<Parms.minvarGMM) gmm.var0=Parms.minvarGMM;
//		gmm.var1 += (gmm.mean1-oldMean1)*(gmm.mean1-oldMean1) +
//				gradStep*(gradStep-2f*gmm.mean1) + 2f*gradStep*oldMean1*(float)exImpacted.length/(float)nex;
//		if (gmm.var1<Parms.minvarGMM) gmm.var1=Parms.minvarGMM;
//		gmm.gconst0=gmm.calcGconst(gmm.var0);
//		gmm.gconst1=gmm.calcGconst(gmm.var1);
	}
	
	/**
	 * This class does a full EM training of a 2-class GMM, without taking into account priors !
	 * 
	 * @author xtof
	 *
	 */
	public static class GMMDiag {
		// mean0 always represents the mode with the highest score
		public float mean0, var0, gconst0, logw0;
		public float mean1, var1, gconst1, logw1;
		// contribution of each instance to the posterior:
		float[] postPerEx;
		double[] post = {0,0};
		protected LogMath logMath = new LogMath();
		
		public double[] train(float[] x) {
			train1gauss(x);
			split();
			System.out.println("1gauss "+mean0+" "+mean1);
			postPerEx = new float[x.length];
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
			var1=var0;
			gconst1=gconst0;
			logw0=logMath.linearToLog(0.5);
			logw1=logw0;
		}
		public double[] trainEM(float[] xs) {
			double[] post = {0,0};
			for (int i=0;i<Parms.nitersGMMtraining;i++) {
				Arrays.fill(post, 0);
				float sumx0=0, sumxx0=0;
				float sumx1=0, sumxx1=0;
				for (int t=0;t<xs.length;t++) {
					float x = xs[t];
					float l0=logw0+getLoglike(mean0,var0,gconst0,x);
					float l1=logw1+getLoglike(mean1,var1,gconst1,x);
					float normConst = logMath.addAsLinear(l0, l1);
					double post0=logMath.logToLinear(l0-normConst);
					postPerEx[t]=(float)post0;
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
			this.post[0]=post[0];
			this.post[1]=post[1];
			return post;
		}
		public float getLoglike(float[] sc) {
			float l=0;
			for (int i=0;i<sc.length;i++) {
				float ll = logw0+getLoglike(mean0, var0, gconst0, sc[i]);
				l=logMath.addAsLinear(l, ll);
				ll = logw1+getLoglike(mean1, var1, gconst1, sc[i]);
				l=logMath.addAsLinear(l, ll);
			}
			return l;
		}
		protected float getLoglike(float m, float v, float g, float x) {
			double inexp=((x-m)*(x-m))/v;
	        inexp/=2.0;
	        double loglikeYt = - g - inexp;
	        return (float)loglikeYt;
		}
		protected float calcGconst(float var) {
			double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(var);
			co/=2.0;            
			return (float)co;
		}
	}
}
