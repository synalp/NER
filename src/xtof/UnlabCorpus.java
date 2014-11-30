package xtof;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.Arrays;

import tools.Histoplot;
import xtof.Coresets.GMMDiag;

/**
 * contains only the features from the gigaword
 * 
 * @author xtof
 *
 */
public class UnlabCorpus {
	public int featureSpaceSize;
	public int[][] feats;

	public static void main(String args[]) {
		UnlabCorpus m = loadFeatureFile();
		LinearModelNoStanford c = new LinearModelNoStanford(m);
		float[] sc = c.computeAllScores();
		RiskMachine.GMMDiag gmm = new RiskMachine.GMMDiag();
		double[] post = gmm.train(sc);
		System.out.println("post "+post[0]+" "+post[1]);
		System.out.println("means "+gmm.mean0+" "+gmm.mean1);
		
		/*
		 * les post estimes ici sont completement faux: on trouve du presque 50%
		 * alors que les priors calcules sur le train de 20 phrases donnent 14%
		 */
		
		Histoplot.showit(sc);
		
		
//		Coresets cs = new Coresets();
//		cs.buildcoreset(sc,100);
	}

	public static UnlabCorpus loadFeatureFile() {
		System.out.println("loading gigaword features...");
		UnlabCorpus c = new UnlabCorpus();
		int nread = 0;
		c.featureSpaceSize=0;
		try {
			// this file has been saved in TestGigaword
			DataInputStream g = new DataInputStream(new FileInputStream("unlabfeats.dat"));
			final int nmax = 800;
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
