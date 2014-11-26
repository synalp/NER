package xtof;

/**
 * put here all the parameters that I may need to tune on dev
 * 
 * @author xtof
 *
 */
public class Parms {
	public static int nitersGMMtraining = 100;
	public static float minvarGMM = 0.001f;
	public static int nitersRiskOptim = 1000;
	public static float finiteDiffDelta = 0.01f;
	public static float gradientStep = 1f;
}
