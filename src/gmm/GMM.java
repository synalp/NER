package gmm;

import java.util.Arrays;

import Jama.Matrix;
import java.util.List;
import linearclassifier.AnalyzeLClassifier;
import linearclassifier.Margin;

/**
 * This class is an interface with
 * the Stanford linear classifier
 * It retrieves the features and weights computed
 * by the classifier
 * 
 * based on the GMM class implemented by Christophe Cerisara.
 * 
 * @author rojasbar
 * <!--
 * Modifications History
 * Date             Author   	Description
 * Feb 11, 2014     rojasbar  	Changing the class corpus by AnalyzeLClassifier and adapting classes to new project
 * -->
 */
public class GMM {
    // this is inverse full-diag variance
    private Matrix vars[];
    protected double[] gconst;
    protected double[][] means;
    public final double[] logWeights;
    final int nlabs;
    final LogMath logMath = new LogMath();
    protected double[] tmp;
    //protected String classifier;
    
    public GMM(final int nclasses, final float priors[]) {
        this(nclasses,priors,true);
        vars = new Matrix[nclasses];
    }
    protected GMM(final int nclasses, final float priors[], final boolean compLog) {
        means = new double[nclasses][nclasses];
        gconst = new double[nclasses];
        nlabs=nclasses;
        logWeights=new double[nclasses];
        if (compLog)
            for (int i=0;i<nlabs;i++) logWeights[i] = logMath.linearToLog(priors[i]);
        else
            for (int i=0;i<nlabs;i++) logWeights[i] = priors[i];
        tmp = new double[nlabs];
    }
    protected GMM(final int nclasses, final double priors[], final boolean compLog) {
        means = new double[nclasses][nclasses];
        gconst = new double[nclasses];
        nlabs=nclasses;
        logWeights=new double[nclasses];
        if (compLog)
            for (int i=0;i<nlabs;i++) logWeights[i] = logMath.linearToLog(priors[i]);
        else
            for (int i=0;i<nlabs;i++) logWeights[i] = priors[i];
        tmp = new double[nlabs];
    }
    /*
    public void setClassifier(String classifierGroup){
        this.classifier=classifierGroup;
    }*/
    
    public double getVar(int y, int a, int b) {
        return vars[y].get(a, b);
    }
    
    public double getMean(int y, int j) {
        return means[y][j];
    }
    
    public double getLike(int y, float[] z) {
        double loglike = getLoglike(y, z);
        double like = logMath.logToLinear((float)loglike);
        return like;
    }
    protected double getLoglike(int y, float[] z) {
        // TODO
        return Double.NaN;
    }

    public double getLoglike(Margin margin) {
        final float[] z = new float[nlabs];
        double loglike=0;
        int numOfInstances= margin.getLabelPerInstances().size();
        for (int instance=0;instance<numOfInstances;instance++) {
            List<Integer> featuresByInstance = margin.getFeaturesPerInstance(instance);
            for (int lab=0;lab<nlabs;lab++){ 
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(instance, lab);
                else
                    z[lab] = margin.getScore(featuresByInstance,lab);
            }
            double loglikeEx=logMath.linearToLog(0);
            for (int y=0;y<nlabs;y++) {
                Arrays.fill(tmp, 0);
                for (int j=0;j<nlabs;j++) {
                    for (int k=0;k<nlabs;k++)
                        tmp[j]+=vars[y].get(j, k)*(z[k]-means[y][k]);
                }
                double o=0;
                for (int j=0;j<nlabs;j++) o += (z[j]-means[y][j])*tmp[j];
                o/=2.0;
                double loglikeYt = logWeights[y] - gconst[y] - o;
                loglikeEx = logMath.addAsLinear((float)loglikeEx, (float)loglikeYt);
            }
            loglike +=loglikeEx;
        }
        return loglike;
    }
    
      
    public void train1gauss(Margin margin) {
        final float[] z = new float[nlabs];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
        }
        int numberOfInstances=margin.getLabelPerInstances().size();
        for (int instance=0;instance<numberOfInstances;instance++) {
            List<Integer> featuresByInstance = margin.getFeaturesPerInstance(instance);
            for (int lab=0;lab<nlabs;lab++) {
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(instance, lab);
                else
                    z[lab] = margin.getScore(featuresByInstance,lab);
            }
            for (int i=0;i<nlabs;i++) {
                means[0][i]+=z[i];
            }
        }
        for (int i=0;i<nlabs;i++) {
            means[0][i]/=(float)numberOfInstances;
            for (int j=1;j<nlabs;j++) means[j][i]=means[0][i];
        }
        vars[0]=new Matrix(nlabs, nlabs);
        for (int instance=0;instance<numberOfInstances;instance++) {
            List<Integer> featuresByInstance = margin.getFeaturesPerInstance(instance);
            for (int lab=0;lab<nlabs;lab++) {
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(instance, lab);
                else
                    z[lab] = margin.getScore(featuresByInstance,lab);
                
                tmp[lab] = z[lab]-means[0][lab];
            }
            for (int i=0;i<nlabs;i++) {
                for (int j=0;j<nlabs;j++) {
                    double v=vars[0].get(i,j);
                    v+=tmp[i]*tmp[j];
                    vars[0].set(i, j, v);
                }
            }
        }
        for (int i=0;i<nlabs;i++) {
            for (int j=0;j<nlabs;j++) {
                vars[0].set(i, j, vars[0].get(i,j)/(double)numberOfInstances);
            }
        }
        
        // precompute gconst
        double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(vars[0].det());
        co/=2.0;
        for (int i=0;i<nlabs;i++) gconst[i]=co;

        // inverse variance
        try {
            Matrix m = vars[0].inverse();
            for (int i=0;i<nlabs;i++) vars[i]=m;
        } catch (Exception e) {
            System.out.println("ERROR MATRIX ");
            for (int i=0;i<nlabs;i++)
                for (int j=0;j<nlabs;j++)
                    System.out.println(i+" "+j+" "+vars[0].get(i, j));
            System.exit(1);
        }
        
        double loglike = getLoglike(margin);
        System.out.println("train1gauss loglike "+loglike);
    }

    public int getDimension(){
        return nlabs;
    }

}
