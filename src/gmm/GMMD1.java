package gmm;

import java.util.Arrays;

import Jama.Matrix;
import java.util.List;
import linearclassifier.AnalyzeClassifier;
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
 * Feb 11, 2014     rojasbar  	Changing the class corpus by AnalyzeClassifier and adapting classes to new project
 * -->
 */
public class GMMD1 {
    // this is inverse full-diag variance
    double[]  var;
    protected double[] gconst;
    protected double[] means;
    final double[] logWeights;
    final int ngauss;
    final LogMath logMath = new LogMath();
    protected double[] tmp;
   
    
    public GMMD1(final int nclasses, final float priors[]) {
        this(nclasses,priors,true);
        var = new double[nclasses];
    }
    protected GMMD1(final int nclasses, final float priors[], final boolean compLog) {
        means = new double[nclasses];
        gconst = new double[nclasses];
        ngauss=nclasses;
        logWeights=new double[nclasses];
        if (compLog)
            for (int i=0;i<ngauss;i++) logWeights[i] = logMath.linearToLog(priors[i]);
        else
            for (int i=0;i<ngauss;i++) logWeights[i] = priors[i];
        tmp = new double[ngauss];
    }
    protected GMMD1(final int nclasses, final double priors[], final boolean compLog) {
        means = new double[nclasses];
        gconst = new double[nclasses];
        ngauss=nclasses;
        logWeights=new double[nclasses];
        if (compLog)
            for (int i=0;i<ngauss;i++) logWeights[i] = logMath.linearToLog(priors[i]);
        else
            for (int i=0;i<ngauss;i++) logWeights[i] = priors[i];
        tmp = new double[ngauss];
    }
    

    public double getVar(int y) {
        return var[y];
    }
    
    public double getMean(int y) {
        return means[y];
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
        float z = 0f;
        double loglike=0;
        int numberOfInstances=margin.getLabelPerInstances().size();
        for (int instance=0;instance<numberOfInstances;instance++) {
            List<Integer> featuresByInstance = margin.getFeaturesPerInstance(instance);
            for (int lab=0;lab<ngauss;lab++){ 
                if(Margin.GENERATEDDATA)
                    z = margin.getGenScore(instance, 0);
                else
                    z = margin.getScore(featuresByInstance,0);
            }
            double loglikeEx=logMath.linearToLog(0);
            for (int y=0;y<ngauss;y++) {
               
                
                 tmp[y]+=var[y]*(z-means[y]);
                
                double o= (z-means[y])*tmp[y];
                o/=2.0;
                double loglikeYt = logWeights[y] - gconst[y] - o;
                loglikeEx = logMath.addAsLinear((float)loglikeEx, (float)loglikeYt);
            }
            loglike +=loglikeEx;
        }
        return loglike;
    }
    
      
    public void train1gauss(Margin margin) {
        float z = 0f;
        for (int i=0;i<ngauss;i++) {
            Arrays.fill(means, 0);
        }
        int numberOfInstances=margin.getLabelPerInstances().size();
        for (int instance=0;instance<numberOfInstances;instance++) {
            List<Integer> featuresByInstance = margin.getFeaturesPerInstance(instance);
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(instance, 0);
            else
                z = margin.getScore(featuresByInstance,0);
            
            for (int i=0;i<ngauss;i++) {
                means[i]+=z;
            }
        }
        for (int i=0;i<ngauss;i++) {
            means[i]/=(float)numberOfInstances;
            for (int j=1;j<ngauss;j++) means[j]=means[i];
        }
        
        for (int instance=0;instance<numberOfInstances;instance++) {
            List<Integer> featuresByInstance = margin.getFeaturesPerInstance(instance);
            
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(instance, 0);
            else
                z = margin.getScore(featuresByInstance,0);
                
            tmp[0] = z-means[0];
            
            
            double v=var[0];
            v+=tmp[0]*tmp[0];
            var[0]= v;
                
            
        }
        for (int i=0;i<ngauss;i++) 
                var[i]/=(double)numberOfInstances;
        
        // precompute gconst

        for (int i=0;i<ngauss;i++){
            double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(var[i]);
            co/=2.0;            
            gconst[i]=co;
        }    


        
        double loglike = getLoglike(margin);
        System.out.println("train1gauss loglike "+loglike);
    }

    public int getDimension(){
        return ngauss;
    }

}
