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
 * Feb 11, 2014     rojasbar  	Changing the class Corpus by AnalyzeClassifier
 * -->
 */
public class GMMDiag extends GMM {
    final double minvar = 0.01;
    private GMM oracleGMM;
    
    private float minMean=Float.MAX_VALUE;
    private float maxMean=Float.MIN_VALUE;
    private float maxSigma=Float.MIN_VALUE;
    
    // this is diagonal variance (not inverse !)
    double[][] diagvar;
    
    public GMMDiag(final int nclasses, final float priors[]) {
        super(nclasses,priors,true);
        diagvar = new double[nlabs][nlabs];
    }
    private GMMDiag(final int nclasses, final double priors[], boolean compLog) {
        super(nclasses,priors,compLog);
        diagvar = new double[nlabs][nlabs];
    }
    public GMMDiag clone() {
        GMMDiag g = new GMMDiag(nlabs, logWeights, false);
        for (int y=0;y<nlabs;y++) {
            g.means[y] = Arrays.copyOf(means[y], nlabs);
            g.diagvar[y] = Arrays.copyOf(diagvar[y], nlabs);
        }
        g.gconst = Arrays.copyOf(gconst, nlabs);
        return g;
    }
    
    public double getVar(int y, int a, int b) {
        if (a!=b) return 0;
        return diagvar[y][a];
    }
    
    protected double getLoglike(int y, float[] z) {
        for (int k=0;k<nlabs;k++)
            tmp[k]=(z[k]-means[y][k])/diagvar[y][k];
        double o=0;
        for (int j=0;j<nlabs;j++) o += (z[j]-means[y][j])*tmp[j];
        o/=2.0;
        double loglikeYt = - gconst[y] - o;
        return loglikeYt;
    }
    /**
     * 
     * @param y first dimension of  mu
     * @param l second dimension of mu
     * @param x x
     * @return 
     */
    public double getLike(int y, int l, float x){
        
        double inexp= Math.pow((x-means[y][l]),2)/(2.0*diagvar[y][l]);
        double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(diagvar[y][l]);
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
    
    public double getLoglike(AnalyzeClassifier analyzer, Margin margin) {
        final float[] z = new float[nlabs];
        double loglike=0;
        for (int instance=0;instance<analyzer.getNumberOfInstances();instance++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, instance);
            for (int lab=0;lab<nlabs;lab++){
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(instance, lab);
                else
                    z[lab]=z[lab] = margin.getScore(featuresByInstance,lab);
            }
            double loglikeEx=logMath.linearToLog(0);
            for (int y=0;y<nlabs;y++) {
                double loglikeYt = logWeights[y] + getLoglike(y, z);
                loglikeEx = logMath.addAsLinear((float)loglikeEx, (float)loglikeYt);
            }
            loglike +=loglikeEx;
        }
        return loglike;
    }

    public void trainOracle(AnalyzeClassifier analyzer, Margin margin) {
        final float[] z = new float[nlabs];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        int[] nex = new int[nlabs];
        Arrays.fill(nex, 0);
        for (int ex=0;ex<analyzer.getNumberOfInstances();ex++) {
            List<Integer> instance = analyzer.getFeaturesPerInstance(classifier, ex);
            for (int lab=0;lab<nlabs;lab++) {
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(ex, lab);
                else
                    z[lab] = margin.getScore(instance,lab);
            }
            int goldLab = analyzer.getLabelsPerInstance(classifier,ex);
            nex[goldLab]++;
            for (int i=0;i<nlabs;i++) {
                means[goldLab][i]+=z[i];
            }
        }
        
        for (int y=0;y<nlabs;y++) {
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) means[y][i]=0;
            else
                for (int i=0;i<nlabs;i++) {
                    means[y][i]/=(float)nex[y];
                }
        }
        
        for (int ex=0;ex<analyzer.getNumberOfInstances();ex++) {
            List<Integer> features = analyzer.getFeaturesPerInstance(classifier, ex);
            for (int i=0;i<nlabs;i++) {
                if(Margin.GENERATEDDATA)
                    z[i] = margin.getGenScore(ex, i);
                else
                    z[i] = margin.getScore(features,i);
            }
            int goldLab = analyzer.getLabelsPerInstance(classifier,ex);
            for (int i=0;i<nlabs;i++) {
                tmp[i] = z[i]-means[goldLab][i];
                diagvar[goldLab][i]+=tmp[i]*tmp[i];
            }
        }
        for (int y=0;y<nlabs;y++) {
            double det=1;
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    det *= diagvar[y][i];
                }
            else
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] /= (double)nex[y];
                    if (diagvar[y][i] < minvar) diagvar[y][i]=minvar;
                    det *= diagvar[y][i];
                }
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(det);
            co/=2.0;
            gconst[y]=co;
        }
        
        double loglike = getLoglike(analyzer, margin);
        System.out.println("trainoracle loglike "+loglike+" nex "+analyzer.getNumberOfInstances());
    }
    
    
    public void trainViterbi2(AnalyzeClassifier analyzer, Margin margin) {
        final float[] z = new float[nlabs];
        final GMMDiag gmm0 = this.clone();
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        int[] nex = new int[nlabs];
        
        Arrays.fill(nex, 0);
        
        int[] ex2lab = new int[analyzer.getNumberOfInstances()];
        
        for (int inst=0;inst<analyzer.getNumberOfInstances();inst++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, inst);
            for (int lab=0;lab<nlabs;lab++) {
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(inst, lab);
                else
                    z[lab] = margin.getScore(featuresByInstance,lab);
            }
            Arrays.fill(tmp, 0);
            
            //logWeights has already the priors which is the extra pattern pi
            for (int y=0;y<nlabs;y++){ 
                tmp[y]=gmm0.logWeights[y] + gmm0.getLoglike(y, z);
                
                
            }
            
            
            int besty=0;
            for (int y=1;y<nlabs;y++)
                if (tmp[y]>tmp[besty]) besty=y;
            nex[besty]++;
            ex2lab[inst]=besty;
            for (int i=0;i<nlabs;i++) {
                means[besty][i]+=z[i];
            }
            

            
        }
        
        for (int y=0;y<nlabs;y++) {
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) means[y][i]=0;
            else
                for (int i=0;i<nlabs;i++) {
                    means[y][i]/=(float)nex[y];
                }
        }
            
        for (int inst=0;inst<analyzer.getNumberOfInstances();inst++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, inst);
            for (int i=0;i<nlabs;i++) {
                if(Margin.GENERATEDDATA)
                    z[i] = margin.getGenScore(inst, i);
                else                
                    z[i] = margin.getScore(featuresByInstance,i);
            }
            
            int besty = ex2lab[inst];
            
            for (int i=0;i<nlabs;i++) {
                tmp[i] = z[i]-means[besty][i];
                diagvar[besty][i]+=tmp[i]*tmp[i];
                
            }
            
            
        }
        
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            else
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] /= (double)nex[y];
                    
                    if (diagvar[y][i] < minvar) 
                        diagvar[y][i]=minvar;
                    
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
            co/=2.0;
            gconst[y]=co;
            //System.out.println("diagvar["+y+"]="+Arrays.toString(diagvar[y]));
        }
    }    
    /**
     * reassigns each frame to one mixture, and retrain the mean and var
     * 
     * @param analyzer
     */
    public void trainViterbi(AnalyzeClassifier analyzer, Margin margin) {
        final float[] z = new float[nlabs];
        final GMMDiag gmm0 = this.clone();
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        int[] nex = new int[nlabs];
        double[] nk = new double[nlabs];
        
        Arrays.fill(nex, 0);
        Arrays.fill(nk, 0.0);
        
        //int[] ex2lab = new int[analyzer.getNumberOfInstances()];
        
        for (int inst=0;inst<analyzer.getNumberOfInstances();inst++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, inst);
            for (int lab=0;lab<nlabs;lab++) {
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(inst, lab);
                else                
                    z[lab] = margin.getScore(featuresByInstance,lab);
            }
            Arrays.fill(tmp, 0);

            //logWeights has already the priors which is the extra pattern pi
            float normConst = logMath.linearToLog(0);
            for (int y=0;y<nlabs;y++){ 
                tmp[y]=gmm0.logWeights[y] + gmm0.getLoglike(y, z);
                normConst=  logMath.addAsLinear(normConst,(float)tmp[y]);
                
            }
            
            /*
            int besty=0;
            for (int y=1;y<nlabs;y++)
                if (tmp[y]>tmp[besty]) besty=y;
            nex[besty]++;
            ex2lab[inst]=besty;
            for (int i=0;i<nlabs;i++) {
                means[besty][i]+=z[i];
            }*/
            
            for (int y=0;y<nlabs;y++){ 
                nex[y]++;
                //ex2lab[inst]=y;
                double posterior=logMath.logToLinear((float)tmp[y]-normConst);
                nk[y]+=posterior;
                means[y][0]+=posterior*z[y];  
                means[y][1]=-means[y][0];
            }
            //System.out.println(" instance "+inst + "\n normConst = "+ normConst +"  sum mean ["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"]  nk="+Arrays.toString(nk) );
        }
        
        for (int y=0;y<nlabs;y++) {
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) means[y][i]=0;
            else
                for (int i=0;i<nlabs;i++){
                    if(nk[y]==0)
                        means[y][i]=Float.MAX_VALUE;
                    else
                        means[y][i]/=nk[y];
                }    
                
                
        }
        System.out.println("["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"] " + " nk="+Arrays.toString(nk) );   
        for (int inst=0;inst<analyzer.getNumberOfInstances();inst++) {
            List<Integer> featuresByInstance = analyzer.getFeaturesPerInstance(classifier, inst);
            for (int i=0;i<nlabs;i++) {
                if(Margin.GENERATEDDATA)
                    z[i] = margin.getGenScore(inst, i);
                else                
                    z[i] = margin.getScore(featuresByInstance,i);
            }
            /*
            int besty = ex2lab[inst];
            
            for (int i=0;i<nlabs;i++) {
                tmp[i] = z[i]-means[besty][i];
                diagvar[besty][i]+=tmp[i]*tmp[i];
                
            }*/
            float normConst = logMath.linearToLog(0);
            for (int y=0;y<nlabs;y++){ 
                tmp[y]=gmm0.logWeights[y] + gmm0.getLoglike(y, z);
                normConst=  logMath.addAsLinear(normConst,(float)tmp[y]);
            }
            for (int y=0;y<nlabs;y++){ 
                double mudiff = z[y]-means[y][0];
                double posterior=logMath.logToLinear((float)tmp[y]-normConst);
                diagvar[y][0]+=posterior*(mudiff*mudiff);
                diagvar[y][1]=diagvar[y][0];
                
            }
            
        }
        
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            else
                for (int i=0;i<nlabs;i++) {
                    if(nk[y]==0)
                        diagvar[y][i]=Float.MAX_VALUE;
                    else
                        diagvar[y][i] /= nk[y];
                    
                    if (diagvar[y][i] < minvar) 
                        diagvar[y][i]=minvar;
                    
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
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
        for (int y=1;y<nlabs;y++) {
            for (int i=0;i<nlabs;i++) {
                means[y][i]=means[0][i];
                diagvar[y][i]=diagvar[0][i];
            }
        }
        for (int y=0;y<nlabs;y++) {
            if (y%2==0)
                for (int i=0;i<nlabs;i++)
                    means[y][i]+=Math.sqrt(diagvar[y][i])*ratio;
            else
                for (int i=0;i<nlabs;i++)
                    means[y][i]-=Math.sqrt(diagvar[y][i])*ratio;
        }
    }
    /**
     * after splitting by trainViterbi
     * means00 = mean of scores computed with model 0 = mu00 = mu-y=0-NO (mu_kk)
     * means01 = mean of scores computed with model 1 = mu01 = mu-y=0-YES (mu_kl)
     * means10 = mean of scores computed with model 0 = mu10 = mu-y=1-NO (mu_kl)
     * means11 = mean of scores computed with model 1 = mu01 = mu-y=1-YES (mu_kk)
     * 
     * @param analyze
     * @param margin 
     */
    public void train1gauss(AnalyzeClassifier analyze, Margin margin) {
        final float[] z = new float[nlabs];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
        }
        for (int ex=0;ex<analyze.getNumberOfInstances();ex++) {
            List<Integer> featuresByInstance = analyze.getFeaturesPerInstance(classifier, ex);
            for (int lab=0;lab<nlabs;lab++) {
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(ex, lab);
                else                
                    z[lab] = margin.getScore(featuresByInstance,lab);
                //System.out.println("lab="+lab+" z[lab]="+z[lab]);
                assert !Float.isNaN(z[lab]);
            }
            for (int i=0;i<nlabs;i++) {
                means[0][i]+=z[i];
            }
        }
        for (int i=0;i<nlabs;i++) {
            means[0][i]/=(float)analyze.getNumberOfInstances();
            for (int j=1;j<nlabs;j++) means[j][i]=means[0][i];
        }
        Arrays.fill(diagvar[0], 0);
        for (int ex=0;ex<analyze.getNumberOfInstances();ex++) {
            List<Integer> featuresByInstance = analyze.getFeaturesPerInstance(classifier, ex);
            for (int i=0;i<nlabs;i++) {
                if(Margin.GENERATEDDATA)
                    z[i] = margin.getGenScore(ex, i);
                else                
                    z[i] = margin.getScore(featuresByInstance,i);
                assert !Float.isNaN(z[i]);
                tmp[i] = z[i]-means[0][i];
            }
            for (int i=0;i<nlabs;i++) {
                diagvar[0][i]+=tmp[i]*tmp[i];
            }
        }
        assert analyze.getNumberOfInstances()>0;
        for (int i=0;i<nlabs;i++) {
            diagvar[0][i] /= (double)analyze.getNumberOfInstances();
            if (diagvar[0][i] < minvar) diagvar[0][i]=minvar;
            
        }
        
        assert analyze.getNumberOfInstances()>0;
        
        // precompute gconst
        /*
         * log de
         * (2pi)^{d/2} * |Covar|^{1/2} 
         */
        double det=1;
        for (int i=0;i<nlabs;i++) {
            diagvar[0][i] /= (double)analyze.getNumberOfInstances();
            if (diagvar[0][i] < minvar) diagvar[0][i]=minvar;
            det *= diagvar[0][i];
        }
        double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(det);
        co/=2.0;
        for (int i=0;i<nlabs;i++) gconst[i]=co;  
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
    
    public double squareErr(GMM g) {
        double sqerr=0;
        for (int y=0;y<nlabs;y++) {
            for (int i=0;i<nlabs;i++) sqerr += (means[y][i]-g.means[y][i])*(means[y][i]-g.means[y][i]);
        }
        return sqerr;
    }
    
    public Pair<Double,Double> getInterval(float nSigma){
            
            
            for(int i=0; i<nlabs;i++){
                for(int j=0; j<nlabs;j++){
                 double mean= getMean(i, j);
                 double sigma= Math.sqrt(getVar(i, j, j));

                 if(mean < minMean)
                     minMean=(float)mean;
                 if(mean > maxMean)
                     maxMean=(float)mean;
                 if(sigma > maxSigma)
                     maxSigma=(float)sigma;

                }
            }

            Double lo= new Double(minMean-(maxSigma*nSigma));
            Double hi= new Double(maxMean+(maxSigma*nSigma));
            return new Pair(lo,hi);
    }
    
    public float getMaxSigma(){
        return this.maxSigma;
    }
    

}
