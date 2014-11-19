package gmm;



import edu.stanford.nlp.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import linearclassifier.Margin;


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
 * Changed trainViterbi, to compute the parameters based on the posterior probability
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
    
    // parameters to tune;
    public static double splitRatio=0.1;
    public static int nitersTraining=20;
    public static double toleranceTraining=0.0004; //2e-05;
    
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
        double sumsqdiffy=0;
        for (int j=0;j<nlabs;j++)
            sumsqdiffy += (z[j]-means[y][j])*tmp[j];
        sumsqdiffy/=2.0;
            
        
        double loglikeYt = - gconst[y] - sumsqdiffy;
        
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
    
    public double getLoglike(Margin margin) {
        final float[] z = new float[nlabs];
        double loglike=0;
        int numInstances = margin.getNumberOfInstances();
        for (int instance=0;instance<numInstances;instance++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(instance);
            for (int lab=0;lab<nlabs;lab++){
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(instance, lab);
                else
                    z[lab]=z[lab] = margin.getScore(featuresByInstance,lab);
            }
            double loglikeEx=logMath.linearToLog(0);
            for (int y=0;y<nlabs;y++) {
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

    public void trainOracle(Margin margin) {
        final float[] z = new float[nlabs];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        int[] nex = new int[nlabs];
        Arrays.fill(nex, 0);
        int numInstances = margin.getNumberOfInstances();
        for (int ex=0;ex<numInstances;ex++) {
            List<Integer> instance = margin.getFeaturesPerInstance(ex);
            for (int lab=0;lab<nlabs;lab++) {
                if(Margin.GENERATEDDATA)
                    z[lab] = margin.getGenScore(ex, lab);
                else
                    z[lab] = margin.getScore(instance,lab);
            }
            int goldLab = margin.getLabelPerInstance(ex);
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
        
        for (int ex=0;ex<numInstances;ex++) {
            List<Integer> features = margin.getFeaturesPerInstance(ex);
            for (int i=0;i<nlabs;i++) {
                if(Margin.GENERATEDDATA)
                    z[i] = margin.getGenScore(ex, i);
                else
                    z[i] = margin.getScore(features,i);
            }
            int goldLab = margin.getLabelPerInstance(ex);
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
        
        double loglike = getLoglike(margin);
        System.out.println("trainoracle loglike "+loglike+" nex "+numInstances);
    }
    
    

    public void trainViterbiOLD(Margin margin) {
        final float[] z = new float[nlabs];
        final GMMDiag gmm0 = this.clone();
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        int[] nex = new int[nlabs];
        
        Arrays.fill(nex, 0);
        int numInstances = margin.getNumberOfInstances();
        int[] ex2lab = new int[numInstances];
        
        for (int inst=0;inst<numInstances;inst++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)                        
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            for (int lab=0;lab<nlabs;lab++) {
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
            
        for (int inst=0;inst<numInstances;inst++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)                        
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            for (int i=0;i<nlabs;i++) {
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

   
    /**
     * reassigns each frame to one mixture, and retrain the mean and var
     * 
     * @param analyzer
     * @return the array of marginal posteriors per class
     */
    public double[] trainViterbi(Margin margin) {
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
        
        int numInstances = margin.getNumberOfInstances();       
        for (int inst=0;inst<numInstances;inst++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
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
                if(y==0)
                    normConst=(float)tmp[y];
                else
                    normConst=  logMath.addAsLinear(normConst,(float)tmp[y]);
                
            }
            
             
            for (int y=0;y<nlabs;y++){ 
                nex[y]++;
                //ex2lab[inst]=y;
                double posterior=logMath.logToLinear((float)tmp[y]-normConst);
                nk[y]+=posterior;
                for (int l=0;l<nlabs;l++){ 
                    means[y][l]+=posterior*z[l];  
                   
                }
            }
            //System.out.println(" instance "+inst + "\n normConst = "+ normConst +"  sum mean ["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"]  nk="+Arrays.toString(nk) );

        }
        
        for (int y=0;y<nlabs;y++) {
            if (nk[y]==0)
                for (int i=0;i<nlabs;i++) 
                    means[y][i]=0; //or means[y][i]=Float.MAX_VALUE; ?
            else
                for (int i=0;i<nlabs;i++){
                    means[y][i]/=nk[y];
                }    
                
                
        }
        //System.out.println("["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"] " + " nk="+Arrays.toString(nk) );   
        for (int inst=0;inst<numInstances;inst++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            for (int i=0;i<nlabs;i++) {
                if(Margin.GENERATEDDATA)
                    z[i] = margin.getGenScore(inst, i);
                else                
                    z[i] = margin.getScore(featuresByInstance,i);
            }

            float normConst = logMath.linearToLog(0);
            for (int y=0;y<nlabs;y++){ 
                tmp[y]=gmm0.logWeights[y] + gmm0.getLoglike(y, z);
                if(y==0)
                    normConst=(float)tmp[y];
                else
                    normConst=  logMath.addAsLinear(normConst,(float)tmp[y]);
            }
            for (int y=0;y<nlabs;y++){ 
                double posterior=logMath.logToLinear((float)tmp[y]-normConst);
                
                for (int i=0;i<nlabs;i++){ 
                    double mudiff = z[i]-means[y][i];
                    diagvar[y][i]+=posterior*(mudiff*mudiff);
                    
                }
                 
            }
            
        }
        
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (nk[y]==0)
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            else
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] /= nk[y];
                    
                    if (diagvar[y][i] < minvar) 
                        diagvar[y][i]=minvar;
                    
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
            co/=2.0;
            gconst[y]=co;
            
            //change logWeights
            logWeights[y]=logMath.linearToLog(nk[y]/nex[y]);
           
        }
         //System.out.println("priors: "+ Arrays.toString(logWeights));
        //System.out.println("trainviterbi");
        //printMean();
        //printVariace();   
        return nk;
    }
    
    /**
     * Assuming all mixtures are initially equal, moves away in opposite directions every mixture
     */
    public void split() {
        
        for (int y=1;y<nlabs;y++) {
            for (int i=0;i<nlabs;i++) {
                means[y][i]=means[0][i];
                diagvar[y][i]=diagvar[0][i];
            }
        }
        /*
        //This code is constrained for the binary case
        for (int y=0;y<nlabs;y++) {
            if (y%2==0){
                means[y][0]+=Math.sqrt(diagvar[y][0])*ratio;
                means[y][1]=-means[y][0];
            }else{
                means[y][0]-=Math.sqrt(diagvar[y][1])*ratio;
                means[y][1]=-means[y][0];
            }    
        }
        //*/
        /*
        for(int y=0;y<nlabs;y++){
            if (y%nlabs==0){
                for (int i=0;i<nlabs;i++)
                    means[y][i]+=Math.sqrt(diagvar[y][i])*ratio;
            }else{
                for (int i=0;i<nlabs;i++)
                    means[y][i]-=Math.sqrt(diagvar[y][i])*(ratio+y%nlabs);            
            }    
        }
        //*/
        int nSigma=0;
        for(int y=0;y<nlabs;y++){
             if (y%2==0){
                 nSigma++;
                //double delta=Math.sqrt(diagvar[y][0])*nSigma;
                for (int l=0;l<nlabs;l++){
                    double sign=(means[y][l]<0)?-1:1;
                    means[y][l]= sign*(Math.abs(means[y][l])+Math.sqrt(diagvar[y][l])*nSigma*splitRatio);        
                }
             }else{
                 //double delta=Math.sqrt(diagvar[y][0])*nSigma;
                 for (int l=0;l<nlabs;l++){
                    double sign=(means[y][l]<0)?-1:1;
                    means[y][l]= sign*(Math.abs(means[y][l])-Math.sqrt(diagvar[y][l])*nSigma*splitRatio);
                 }   
             }
        }
        ///*
        System.out.println("split ");
        printMean();
        printVariace();
        //*/
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
    public void train1gauss(Margin margin) {
        final float[] z = new float[nlabs];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
        }
        int numInstances = margin.getNumberOfInstances();
        for (int ex=0;ex<numInstances;ex++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)
                featuresByInstance = margin.getFeaturesPerInstance(ex);
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
            means[0][i]/=(float)numInstances;
            for (int j=1;j<nlabs;j++) means[j][i]=means[0][i];
        }
        Arrays.fill(diagvar[0], 0);
        for (int ex=0;ex<numInstances;ex++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(ex);
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
        assert numInstances>0;

        // precompute gconst
        /*
         * log de
         * (2pi)^{d/2} * |Covar|^{1/2} 
         */
        double logdet=0;
        for (int i=0;i<nlabs;i++) {
            
            diagvar[0][i] /= (double) numInstances;
            if (diagvar[0][i] < minvar) diagvar[0][i]=minvar;
            for (int j=1;j<nlabs;j++) diagvar[j][i]=diagvar[0][i];
            logdet += logMath.linearToLog(diagvar[0][i]);
        }

        
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
            co/=2.0;            
            for (int i=0;i<nlabs;i++) gconst[i]=co; 
            //double co=logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(diagvar[y][l]);
            //co/=2.0;

        ///*
        System.out.println("train1gauss");
        printMean();
        printVariace(); 
        System.out.println("train1gauss var=["+gconst[0]+","+gconst[1]+"]");
        //*/
    }
    
    /**
     * 
     * @param margin
     * @return posterior per class
     */
    public double[] train(Margin margin) {
    	// TODO: how are these parms estimated ? use fair estimation on dev !
        train1gauss(margin);
        double loglike = getLoglike(margin);
        assert !Double.isNaN(loglike);
        //double sqerr = Double.NaN;
        //if (oracleGMM!=null) sqerr = squareErr(oracleGMM);
        System.out.println("train1gauss loglike "+loglike+" nex "+margin.getNumberOfInstances());
        split();
        double previousLogLike=loglike;
        double[] postPerClass=null;
        for (int iter=0;iter<nitersTraining;iter++) {
            postPerClass=trainViterbi(margin);
            loglike = getLoglike(margin);
            if(Math.abs(loglike-previousLogLike)<toleranceTraining)
                break;
            
            previousLogLike=loglike;
            //sqerr = Double.NaN;
            //if (oracleGMM!=null) sqerr = squareErr(oracleGMM);
            
            //System.out.println("trainviterbi iter "+iter+" loglike "+loglike+" nex "+margin.getNumberOfInstances()+ " sqerr "+sqerr);
        }
        return postPerClass;
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

    public void printMean(){
            String mean="mean=[";
            for(int i=0; i<  nlabs;i++){
                for(int j=0; j<  nlabs;j++){
                    mean+= means[i][j];
                    if(j<nlabs-1)
                        mean+=" , ";
                } 
                if(i<nlabs-1)
                    mean+=";";
            }    
            mean+="]";
            System.out.println(mean);        
    }
    public void printVariace(){
            String var="var=[";
            for(int i=0; i<  nlabs;i++){
                for(int j=0; j<  nlabs;j++){
                    var+= diagvar[i][j];
                    if(j<nlabs-1)
                        var+=" , ";
                } 
                if(i<nlabs-1)
                    var+=";";
            }    
            var+="]";
            System.out.println(var);        
    }    
}
