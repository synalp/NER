package xtof;

/**
 * put here all the parameters that I may need to tune on dev
 * 
 * @author xtof
 *
 */
public class Parms {
	// GMM training
	public static int nitersGMMtraining = 100;
	public static float minvarGMM = 0.001f;

	// risk optimization
	public static int nitersRiskOptim = 1000;
	public static float finiteDiffDelta = 0.01f;
	public static float gradientStep = 1f;
	
	// risk optimization with approximation
	public static int nitersRiskOptimApprox = 1000;
	public static int nitersRiskOptimGlobal = 1000;

	// size of the copora used for training
	public static final int nuttsLCtraining = 20;
	public static final int nuttsCRFtraining = 20;

}
