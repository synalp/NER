package gmm;


import edu.stanford.nlp.classify.LinearClassifier;

import edu.stanford.nlp.util.Pair;
import java.util.Arrays;
import java.util.List;
import linearclassifier.AnalyzeClassifier;
import linearclassifier.Margin;
import tools.CNConstants;


/**
 * This is a classical diagonal GMM, but with constant weights !
 * It models P(f(X)|Y)=\sum_Y P(Y) g(f(X);mu_y,var_y)
 * where g() is the Gaussian function, f(X) is the vector of linear scores (one score per class),
 * mu_y is the mean vector associated to examples that belong to class y,
 * var_y is the corresponding _diagonal_ variance matrix, so encoded as a vector.
 * 
 * 
 * taken from the GMM class implemented by Christophe Cerisara.
 * 
 * @author xtof
 * <!--
 * Modifications History
 * Date             Author   	Description
 * Sept 22, 2014    rojasbar  	One dimensional GMM
 * -->
 */
public class GMMD1Diag extends GMMD1 {
    final double minvar = 0.01;
    private GMMD1 oracleGMM;
    
    private float minMean=Float.MAX_VALUE;
    private float maxMean=Float.MIN_VALUE;
    private float maxSigma=Float.MIN_VALUE;
    
    // this is diagonal variance (not inverse !)
    double[] diagvar;
    
    public GMMD1Diag(final int nclasses, final float priors[]) {
        super(nclasses,priors,true);
        diagvar = new double[ngauss];
    }
    private GMMD1Diag(final int nclasses, final double priors[], boolean compLog) {
        super(nclasses,priors,compLog);
        diagvar = new double[ngauss];
    }
    public GMMD1Diag clone() {
        GMMD1Diag g = new GMMD1Diag(ngauss, logWeights, false);
        for (int y=0;y<ngauss;y++) {
            g.means = Arrays.copyOf(means,ngauss);
            g.diagvar = Arrays.copyOf(diagvar, ngauss);
        }
        g.gconst = Arrays.copyOf(gconst, ngauss);
        return g;
    }
    
    public double getVar(int y) {
        return diagvar[y];
    }
    
    protected double getLoglike(int y, float z) {
        
       double inexp=((z-means[y])*(z-means[y]))/diagvar[y];
        
        inexp/=2.0;
   
        
        double loglikeYt = - gconst[y] - inexp;
        
        return loglikeYt;
    }
    /**
     * 
     * @param y first dimension of  mu
     * @param l second dimension of mu
     * @param x x
     * @return 
     */
    public double getLike(int y, float x){
        
        double inexp= Math.pow((x-means[y]),2)/(2.0*diagvar[y]);
        double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(diagvar[y]);
        co/=2.0;
        
        double loglike=- co - inexp;
        //double like = logMath.logToLinear((float)loglike);
        double like = Math.exp(loglike);
        //System.out.println("mean["+y+"]["+l+"]"+means[y][l]+" var["+y+"]["+l+"]"+diagvar[y][l]+" gconst["+y+"]="+gconst[y]+" constant="+co+ " inexp "+inexp+ "loglike "+loglike+ "like "+like);
        return like;
    }
    
    public double getProbability(float x, float mu, float var){
        double inexp = ((x-mu)*(x-mu))/(2*var);
        double consts = logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(var);
        double loglike= 0.5*(- consts - inexp);
        return logMath.logToLinear((float)loglike);
        
        
    }
    
    @Override
    public double getLoglike(AnalyzeClassifier analyzer, Margin margin) {
       float z = 0f;
        double loglike=0;
        for (int instance=0;instance<analyzer.getNumberOfInstances();instance++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, instance);
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(instance, 0);
            else
                z = margin.getScore(featuresByInstance,0);
            
            double loglikeEx=logMath.linearToLog(0);
            for (int y=0;y<ngauss;y++) {
                double loglikeYt = logWeights[y] + getLoglike(y, z);
                if(y==0)
                    loglikeEx=loglikeYt;
                else
                    loglikeEx = logMath.addAsLinear((float)loglikeEx, (float)loglikeYt);
            }
            loglike +=loglikeEx;
        }
        return loglike;
    }

    public void trainOracle(AnalyzeClassifier analyzer, Margin margin) {
        float z = 0f;
        for (int i=0;i<ngauss;i++) {
            Arrays.fill(means, 0);
            Arrays.fill(diagvar, 0);
        }
        int[] nex = new int[ngauss];
        Arrays.fill(nex, 0);
        for (int ex=0;ex<analyzer.getNumberOfInstances();ex++) {
            List<Integer> instance = analyzer.getFeaturesPerInstance(classifier, ex);
            
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(ex, 0);
            else
                z = margin.getScore(instance,0);
           
            int goldLab = analyzer.getLabelsPerInstance(classifier,ex);
            nex[goldLab]++;
            for (int i=0;i<ngauss;i++) {
                means[goldLab]+=z;
            }
        }
        
        for (int y=0;y<ngauss;y++) {
            if (nex[y]==0)
                means[y]=0;
            else
                means[y]/=(float)nex[y];
                
        }
        
        for (int ex=0;ex<analyzer.getNumberOfInstances();ex++) {
            List<Integer> features = analyzer.getFeaturesPerInstance(classifier, ex);
            
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(ex, 0);
            else
                z = margin.getScore(features,0);
            
            int goldLab = analyzer.getLabelsPerInstance(classifier,ex);
            for(int i=0; i<ngauss;i++){
                tmp[i] = z-means[i];
                diagvar[goldLab]+=tmp[i]*tmp[i];   
            }
   
        }
        for (int y=0;y<ngauss;y++) {

            if (nex[y]==0)
                diagvar[y] = minvar;
                    
                
            else{
                
                    diagvar[y] /= (double)nex[y];
                    if (diagvar[y] < minvar) diagvar[y]=minvar;
                    
            }    
            double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(diagvar[y]);
            co/=2.0;
            gconst[y]=co;
        }
        
        double loglike = getLoglike(analyzer, margin);
        System.out.println("trainoracle loglike "+loglike+" nex "+analyzer.getNumberOfInstances());
    }
    
    


    /**
     * reassigns each frame to one mixture, and retrain the mean and var
     * 
     * @param analyzer
     */

   
    /**
     * reassigns each frame to one mixture, and retrain the mean and var
     * 
     * @param analyzer
     */
    public void trainViterbi(AnalyzeClassifier analyzer, Margin margin) {
        float z = 0f;
        final GMMD1Diag gmm0 = this.clone();
        
        Arrays.fill(means, 0);
        Arrays.fill(diagvar, 0);
        
        int[] nex = new int[ngauss];
        double[] nk = new double[ngauss];
        
        Arrays.fill(nex, 0);
        Arrays.fill(nk, 0.0);
        
               
        for (int inst=0;inst<analyzer.getNumberOfInstances();inst++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, inst);
            
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(inst, 0);
            else                
                z = margin.getScore(featuresByInstance,0);
 
            //logWeights has already the priors which is the extra pattern pi
            float normConst = 0f;
            for (int y=0;y<ngauss;y++){ 
                tmp[y]=gmm0.logWeights[y] + gmm0.getLoglike(y, z);
                if(y==0)
                    normConst=(float)tmp[y];
                else
                    normConst=  logMath.addAsLinear(normConst,(float)tmp[y]);
                
            }
            
             
            for (int y=0;y<ngauss;y++){ 
                nex[y]++;
                double posterior=logMath.logToLinear((float)tmp[y]-normConst);
                nk[y]+=posterior;
                means[y]+=posterior*z;  
                
            }
            //System.out.println(" instance "+inst + "\n normConst = "+ normConst +"  sum mean ["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"]  nk="+Arrays.toString(nk) );
        }
        
        for (int y=0;y<ngauss;y++) {
            if (nk[y]==0)
                means[y]=0; //or means[y][i]=Float.MAX_VALUE; ?
            else
                means[y]/=nk[y];
                    
                
                
        }
        //System.out.println("["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"] " + " nk="+Arrays.toString(nk) );   
        for (int inst=0;inst<analyzer.getNumberOfInstances();inst++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, inst);
            
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(inst, 0);
            else                
                z = margin.getScore(featuresByInstance,0);
            
            float normConst = logMath.linearToLog(0);
            for(int y=0; y<ngauss;y++){
                tmp[y]=gmm0.logWeights[y] + gmm0.getLoglike(y, z);
                
                if(y==0)
                    normConst=(float)tmp[y];
                else
                    normConst=  logMath.addAsLinear(normConst,(float)tmp[y]);

            }
            for(int y=0; y< ngauss; y++){
                double mudiff = z-means[y];
                double posterior=logMath.logToLinear((float)tmp[y]-normConst);
                diagvar[y]+=posterior*(mudiff*mudiff);                
            }
                
            
            
        }
        
        for (int y=0;y<ngauss;y++) {
            
            if (nk[y]==0)
                 diagvar[y] = minvar;
                    
                
            else{
                    diagvar[y] /= nk[y];
                    
                    if (diagvar[y] < minvar) 
                        diagvar[y]=minvar;
                    
                    
            }
            double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(diagvar[y]);
            co/=2.0;
            gconst[y]=co;
            //System.out.println("diagvar["+y+"]="+Arrays.toString(diagvar[y]));
        }
    }
    
    /**
     * Assuming all mixtures are initially equal, moves away in opposite directions every mixture
     */
    public void split() {
        final double ratio = 0.1;
        for (int y=1;y<ngauss;y++) {
            means[y]=means[0];
            diagvar[y]=diagvar[0];
            
        }
        
        for (int y=0;y<ngauss;y++) {
            if (y%2==0){
                means[y]+=Math.sqrt(diagvar[y])*ratio;
                //means[y][1]=-means[y][0];
            }else{
                means[y]-=Math.sqrt(diagvar[y])*ratio;
                //means[y][1]=-means[y][0];
            }    
        }
        System.out.println("split means=["+means[0]+","+-means[0]+";\n"+means[1]+","+-means[1]+"]");
        
    }
    /**
     * after splitting by trainViterbi
     * means00 = mean of scores computed with model 0 = mu00 = mu-y=0-NO (mu_kk)
     * means01 = mean of scores computed with model 1 = mu01 = mu-y=0-YES (mu_kl)
     * means10 = mean of scores computed with model 0 = mu10 = mu-y=1-NO (mu_kl)
     * means11 = mean of scores computed with model 1 = mu11 = mu-y=1-YES (mu_kk)
     * 
     * @param analyze
     * @param margin 
     */
    @Override
    public void train1gauss(AnalyzeClassifier analyze, Margin margin) {
        float z = 0f;
        
        Arrays.fill(means, 0);
        
        for (int ex=0;ex<analyze.getNumberOfInstances();ex++) {
            List<Integer> featuresByInstance = analyze.getFeaturesPerInstance(classifier, ex);
            
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(ex, 0);
            else                
                z = margin.getScore(featuresByInstance,0);
            //System.out.println("lab="+lab+" z[lab]="+z[lab]);
            assert !Float.isNaN(z);
            
            
            means[0]+=z;
            
        }
        
        means[0]/=(float)analyze.getNumberOfInstances();
        for (int j=1;j<ngauss;j++) means[j]=means[0];
        
        Arrays.fill(diagvar, 0);
        for (int ex=0;ex<analyze.getNumberOfInstances();ex++) {
            List<Integer> featuresByInstance = analyze.getFeaturesPerInstance(classifier, ex);
            
            if(Margin.GENERATEDDATA)
                z = margin.getGenScore(ex, 0);
            else                
                z = margin.getScore(featuresByInstance,0);
            assert !Float.isNaN(z);
            tmp[0] = z-means[0];
            diagvar[0]+=tmp[0]*tmp[0];
            
        }
        assert analyze.getNumberOfInstances()>0;

        
        // precompute gconst
        /*
         * log de
         * (2pi)^{d/2} * |Covar|^{1/2} 
         */
        
        
        diagvar[0] /= (double)analyze.getNumberOfInstances();
        if (diagvar[0] < minvar) diagvar[0]=minvar;
        for (int j=1;j<ngauss;j++) diagvar[j]=diagvar[0];
            
        

        for (int i=0;i<ngauss;i++) {
            double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(diagvar[i]);
            co/=2.0;            
            gconst[i]=co; 
        }     
            //double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(diagvar[y][l]);
            //co/=2.0;
            
        System.out.println("train1gauss means=["+means[0]+","+-means[0]+";\n"+means[1]+","+-means[1]+"]");
        System.out.println("train1gauss var=["+diagvar[0]+","+-diagvar[0]+";\n"+diagvar[1]+","+-diagvar[1]+"]");
        System.out.println("train1gauss var=["+gconst[0]+","+gconst[1]+"]");
    }
    
    
    public void train(AnalyzeClassifier analyzer, Margin margin) {
        final int niters=50;
        train1gauss(analyzer, margin);
        double loglike = getLoglike(analyzer, margin);
        assert !Double.isNaN(loglike);
        double sqerr = Double.NaN;
        if (oracleGMM!=null) sqerr = squareErr(oracleGMM);
        System.out.println("train1gauss loglike "+loglike+" nex "+analyzer.getNumberOfInstances()+ "sqerr "+sqerr);
        split();
        for (int iter=0;iter<niters;iter++) {
            trainViterbi(analyzer, margin);
            loglike = getLoglike(analyzer, margin);
            sqerr = Double.NaN;
            if (oracleGMM!=null) sqerr = squareErr(oracleGMM);
            System.out.println("trainviterbi iter "+iter+" loglike "+loglike+" nex "+analyzer.getNumberOfInstances()+ " sqerr "+sqerr);
        }
    }
    
    public double squareErr(GMMD1 g) {
        double sqerr=0;
        for (int y=0;y<ngauss;y++) {
            sqerr += (means[y]-g.means[y])*(means[y]-g.means[y]);
        }
        return sqerr;
    }
    
    public Pair<Double,Double> getInterval(float nSigma){
            
            
            for(int i=0; i<ngauss;i++){
                
                 double mean= getMean(i);
                 double sigma= Math.sqrt(getVar(i));

                 if(mean < minMean)
                     minMean=(float)mean;
                 if(mean > maxMean)
                     maxMean=(float)mean;
                 if(sigma > maxSigma)
                     maxSigma=(float)sigma;

                
            }

            Double lo= new Double(minMean-(maxSigma*nSigma));
            Double hi= new Double(maxMean+(maxSigma*nSigma));
            return new Pair(lo,hi);
    }
    
    public float getMaxSigma(){
        return this.maxSigma;
    }


}
