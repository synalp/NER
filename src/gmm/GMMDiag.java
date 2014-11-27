package gmm;



import edu.stanford.nlp.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import linearclassifier.Margin;
import utils.ErrorsReporting;


/**
 * This is a classical diagonal GMM, but with constant weights !
 * It models P(f(X)|Y)=\sum_Y P(Y) g(f(X);mu_y,var_y)
 * where g() is the Gaussian function, f(X) is the vector of linear scores (one score per class),
 * mu_y is the mean vector associated to examples that belong to class y,
 * var_y is the corresponding _diagonal_ variance matrix, so encoded as a vector.
 * 
 * BIG PROBLEM:
 * 
 * when fixing constant logWeights, then, after splitting, the 2 means may not converge towards the right values because
 * the weights impact is larger than the difference in loglike.
 * So whatever the relative position of both means, either mean0>mean1 or mean1>mean0, the very same set of frames will be assigned
 * to mean0 or mean1 because of the weight.
 * Because we start from means close to the average value, the set of frames assigned to each gaussian is likely to contain frames
 * from both data modes. So the means will stay somewhere between both modes, and converge very slowly, if ever.
 * 
 * This problem can be solved by starting from the extreme values, instead of the average ?
 * 
 * taken from the GMM class implemented by Christophe Cerisara.
 * 
 * Changed trainViterbi, to compute the parameters based on the posterior probability
 * 
 * @author xtof
 * <!--
 * Modifications History
 * Date             Author   	Description
 * Sept, 2014       rojasbar  	Changed trainViterbi, to compute the parameters based on the posterior probability.
 * Feb 11, 2014     rojasbar    Changing the class Corpus by AnalyzeClassifier.
 * -->
 */
public class GMMDiag extends GMM {
    final double minvar = 0.01;
    private GMM oracleGMM;
    
    private float minMean=Float.MAX_VALUE;
    private float maxMean=Float.MIN_VALUE;
    private float maxSigma=Float.MIN_VALUE;
    private Random rnd = new Random();
    private boolean isBinaryConstrained=false;
    
    // this is diagonal variance (not inverse !)
    double[][] diagvar;
    //for the partitioning -> approximate EM
    double[][] previousMuPart;
    double[] previousSumXPart;
    double[] sumXPart;
    double[] postPart;
    double[] nkPart;
    // parameters to tune;
    public static double splitRatio=0.1;
    public static int nitersTraining=20;
    public static double toleranceTraining=0.0004; //2e-05;
    
    int[] samples = null;
    
    public GMMDiag(final int nclasses, final float priors[],boolean isBinaryConstr) {
        super(nclasses,priors,true);
        diagvar = new double[nlabs][nlabs];
        this.isBinaryConstrained=isBinaryConstr;
    }
    private GMMDiag(final int nclasses, final double priors[], boolean compLog,boolean isBinaryConstr) {
        super(nclasses,priors,compLog);
        diagvar = new double[nlabs][nlabs];
        this.isBinaryConstrained=isBinaryConstr;
    }
    public GMMDiag clone() {
        GMMDiag g = new GMMDiag(nlabs, logWeights, false,isBinaryConstrained);
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
     * @param margin
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
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(inst, 0);
                else
                    z[0] = margin.getScore(featuresByInstance,0);
                z[1]=-z[0];
                
            }else{
                for (int lab=0;lab<nlabs;lab++) {
                    if(Margin.GENERATEDDATA)
                        z[lab] = margin.getGenScore(inst, lab);
                    else                
                        z[lab] = margin.getScore(featuresByInstance,lab);
                }
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
                    if(this.isBinaryConstrained)
                        break;
                        
                }

            }
            
            //System.out.println(" instance "+inst + "\n normConst = "+ normConst +"  sum mean ["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"]  nk="+Arrays.toString(nk) );

        }
        if(isBinaryConstrained){
           means[0][0]=(nk[0]==0)?0:means[0][0]/nk[0];
           means[0][1]=-means[0][0];
           means[1][0]=(nk[1]==0)?0:means[1][0]/nk[1];
           means[1][1]=-means[1][0];           
        }else{
            for (int y=0;y<nlabs;y++) {
                if (nk[y]==0)
                    for (int i=0;i<nlabs;i++) 
                        means[y][i]=0; //or means[y][i]=Float.MAX_VALUE; ?
                else
                    for (int i=0;i<nlabs;i++){
                        means[y][i]/=nk[y];
                    }    


            }
        }
        //System.out.println("["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"] " + " nk="+Arrays.toString(nk) );   
        for (int inst=0;inst<numInstances;inst++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(inst, 0);
                else
                    z[0] = margin.getScore(featuresByInstance,0);
                z[1] = -z[0];
            }else{            
                for (int i=0;i<nlabs;i++) {
                    if(Margin.GENERATEDDATA)
                        z[i] = margin.getGenScore(inst, i);
                    else                
                        z[i] = margin.getScore(featuresByInstance,i);
                }
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
                    if(this.isBinaryConstrained)
                        break;
                }
                
                 
            }
            
        }
        
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (nk[y]==0){
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            }else{
                if(this.isBinaryConstrained){
                    diagvar[y][0] /= nk[y];
                    if (diagvar[y][0] < minvar) 
                        diagvar[y][0]=minvar;
                    
                    diagvar[y][1] = diagvar[y][0];
                    logdet += logMath.linearToLog(diagvar[y][0]); 
                    logdet += logMath.linearToLog(diagvar[y][1]); 
                }else
                    for (int i=0;i<nlabs;i++) {
                        diagvar[y][i] /= nk[y];

                        if (diagvar[y][i] < minvar) 
                            diagvar[y][i]=minvar;

                        logdet += logMath.linearToLog(diagvar[y][i]);
                    }
            }    
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
            co/=2.0;
            gconst[y]=co;
            
            //change logWeights ... or not ??
            // logWeights[y]=logMath.linearToLog(nk[y]/nex[y]);
           
        }
         //System.out.println("priors: "+ Arrays.toString(logWeights));
        //System.out.println("trainviterbi");
        //printMean();
        //printVariace();   
        return nk;
    }
    
    public double[][] computePartitionMu(Margin margin, GMM gmm0,float[] z,int[] nex){
        double[][] muPart1= new double[means.length][];
        nkPart = new double[nlabs];
        sumXPart=new double[z.length];
        postPart=new double[z.length];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(muPart1[i], 0);
        }        
        List<Integer> instances = margin.getInstancesCurrThrFeat();
        
        for (int i=0;i<instances.size();i++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            int inst= instances.get(i);
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(inst, 0);
                else
                    z[0] = margin.getScore(featuresByInstance,0);
                z[1]=-z[0];
                
            }else{
                for (int lab=0;lab<nlabs;lab++) {
                    if(Margin.GENERATEDDATA)
                        z[lab] = margin.getGenScore(inst, lab);
                    else                
                        z[lab] = margin.getScore(featuresByInstance,lab);
                }
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
                nkPart[y]+=posterior;
                sumXPart[y]+=z[y];
                postPart[y]=posterior;
                for (int l=0;l<nlabs;l++){ 
                    muPart1[y][l]+=posterior*z[l];  
                    if(this.isBinaryConstrained)
                        break;
                        
                }

            }
            
            //System.out.println(" instance "+inst + "\n normConst = "+ normConst +"  sum mean ["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"]  nk="+Arrays.toString(nk) );
            
        } 
        if(isBinaryConstrained){
           muPart1[0][0]=(nkPart[0]==0)?0:muPart1[0][0]/nkPart[0];
           muPart1[0][1]=-muPart1[0][0];
           muPart1[1][0]=(nkPart[1]==0)?0:muPart1[1][0]/nkPart[1];
           muPart1[1][1]=-muPart1[1][0];           
        }else{
            for (int y=0;y<nlabs;y++) {
                if (nkPart[y]==0)
                    for (int l=0;l<nlabs;l++) 
                        muPart1[y][l]=0; //or means[y][i]=Float.MAX_VALUE; ?
                else
                    for (int l=0;l<nlabs;l++){
                        muPart1[y][l]/=nkPart[y];
                    }    


            }
        } 
        return muPart1;
    }
    
    public double[] trainApproximatedEM(Margin margin) {
        
        previousMuPart= new double[means.length][];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(previousMuPart[i], 0);
        } 
 
        final float[] z = new float[nlabs];
        //keep a copy of the previous mean and variance
        final GMMDiag gmm0 = this.clone();
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        int[] nex = new int[nlabs];
        double[] nk = new double[nlabs];
        
        Arrays.fill(nex, 0);
        Arrays.fill(nk, 0.0);
        List<Integer> instances = margin.getInstancesCurrThrFeat();
        
        int iter = margin.getThreadIteration();
        if(iter%1000==0){
            previousMuPart=computePartitionMu( margin,  gmm0, z,nex);
            previousSumXPart=sumXPart;
            return trainViterbi(margin);
        }   

        double[][] newMuPart1=computePartitionMu( margin,  gmm0, z,nex);
        nk=nkPart;
        //here I have the mean for the instances impacted by the thread
        for (int y=0;y<nlabs;y++)
            for (int l=0;l<nlabs;l++)
                means[y][l]=gmm0.means[y][l]-previousMuPart[y][l]+newMuPart1[y][l];
                
        //System.out.println("["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"] " + " nk="+Arrays.toString(nk) );   

        double[][] prvMuDif= new double[means.length][];
        for (int y=0;y<nlabs;y++){ 
            double posterior=postPart[y];
            for (int l=0;l<nlabs;l++){ 
                double mudiff = (sumXPart[y]*sumXPart[y]);
                prvMuDif[y][l]= (nlabs*posterior)*(previousSumXPart[y]*previousSumXPart[y]);
                diagvar[y][l]=(nlabs*posterior)*mudiff;
                if(this.isBinaryConstrained)
                    break;
            }


        }

        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (nk[y]==0){
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            }else{
                if(this.isBinaryConstrained){
                    diagvar[y][0] = gmm0.diagvar[y][0]- (prvMuDif[y][0]/nk[y] -previousMuPart[y][0])+ (diagvar[y][0]/nk[y]-means[y][0]);
                    
                    if (diagvar[y][0] < minvar) 
                        diagvar[y][0]=minvar;
                    
                    diagvar[y][1] = diagvar[y][0];
                    logdet += logMath.linearToLog(diagvar[y][0]); 
                    logdet += logMath.linearToLog(diagvar[y][1]); 
                }else
                    for (int i=0;i<nlabs;i++) {
                        //diagvar[y][i] /= nk[y];
                        diagvar[y][i] = gmm0.diagvar[y][i]- (prvMuDif[y][i]/nk[y] -previousMuPart[y][i])+ (diagvar[y][i]/nk[y]-means[y][i]);
                        if (diagvar[y][i] < minvar) 
                            diagvar[y][i]=minvar;

                        logdet += logMath.linearToLog(diagvar[y][i]);
                    }
            }    
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
            co/=2.0;
            gconst[y]=co;
            
            //change logWeights ... or not ??
            // logWeights[y]=logMath.linearToLog(nk[y]/nex[y]);
           
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
//        for(int y=0;y<nlabs;y++){
//            if (y%nlabs==0){
//                for (int i=0;i<nlabs;i++)
//                    means[y][i]+=Math.sqrt(diagvar[y][i])*splitRatio;
//            }else{
//                for (int i=0;i<nlabs;i++)
//                    means[y][i]-=Math.sqrt(diagvar[y][i])*(splitRatio+y%nlabs);            
//            }    
//        }

        // we assume only 2 classes !!!
        means[0][0]+=Math.sqrt(diagvar[0][0])*splitRatio;
        means[1][0]-=Math.sqrt(diagvar[1][0])*splitRatio;
        means[0][1]=-means[0][0];
        means[1][1]=-means[1][0];
        
        /*
         * why such a complex split ???
         * PROBLEM: it does not work when inverting the weights because of priors inversion !!
         * so I replace it with the much simpler split just above
         */
/*
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
*/
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
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(ex, 0);
                else                
                    z[0] = margin.getScore(featuresByInstance,0);               
                z[1] = -z[0];
                        
            }else{
                for (int lab=0;lab<nlabs;lab++) {
                    if(Margin.GENERATEDDATA)
                        z[lab] = margin.getGenScore(ex, lab);
                    else                
                        z[lab] = margin.getScore(featuresByInstance,lab);
                    //System.out.println("lab="+lab+" z[lab]="+z[lab]);
                    assert !Float.isNaN(z[lab]);
                }
            }    
            for (int i=0;i<nlabs;i++) {
                means[0][i]+=z[i];
            }
        }
        for (int i=0;i<nlabs;i++) {
            means[0][i]/=(float)numInstances;
            for (int j=1;j<nlabs;j++) 
                means[j][i]=means[0][i];
        }
        Arrays.fill(diagvar[0], 0);
        for (int ex=0;ex<numInstances;ex++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(ex);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(ex, 0);
                else                
                    z[0] = margin.getScore(featuresByInstance,0);               
                z[1] = -z[0];
                tmp[0] = z[0]-means[0][0]; 
                tmp[1] = z[1]-means[0][1];
            }else
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
            for (int j=1;j<nlabs;j++) 
                diagvar[j][i]=diagvar[0][i];
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
     * Uses a randomly selected subset of instances
     * @param margin
     * @return 
     */
    public double[] trainStocViterbi(Margin margin) {
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
        
        if(margin.getNumSamples()==0)
            ErrorsReporting.report("The sampling should be done outside gmm");
        samples=margin.getSamples();
        for (int s=0;s<samples.length;s++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            int inst = samples[s];
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(inst, 0);
                else                
                    z[0] = margin.getScore(featuresByInstance,0);
                z[1]=-z[0];
            }else{           
                for (int lab=0;lab<nlabs;lab++) {
                    if(Margin.GENERATEDDATA)
                        z[lab] = margin.getGenScore(inst, lab);
                    else                
                        z[lab] = margin.getScore(featuresByInstance,lab);
                }
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
                    if(isBinaryConstrained)
                        break;
                    
                }
            }
            //System.out.println(" instance "+inst + "\n normConst = "+ normConst +"  sum mean ["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"]  nk="+Arrays.toString(nk) );

        }
        if(isBinaryConstrained){
           means[0][0]=(nk[0]==0)?0:means[0][0]/nk[0];
           means[0][1]=-means[0][0];
           means[1][0]=(nk[1]==0)?0:means[1][0]/nk[1];
           means[1][1]=-means[1][0];           
        }else{        
            for (int y=0;y<nlabs;y++) {
                if (nk[y]==0)
                    for (int i=0;i<nlabs;i++) 
                        means[y][i]=0; //or means[y][i]=Float.MAX_VALUE; ?
                else
                    for (int i=0;i<nlabs;i++){
                        means[y][i]/=nk[y];
                    }    


            }
        }
        //System.out.println("["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"] " + " nk="+Arrays.toString(nk) );   
        for (int s=0;s<samples.length;s++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            int inst= samples[s];
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(inst, 0);
                else                
                    z[0] = margin.getScore(featuresByInstance,0);
                z[1]=-z[0];
            }else{            
                for (int i=0;i<nlabs;i++) {
                    if(Margin.GENERATEDDATA)
                        z[i] = margin.getGenScore(inst, i);
                    else                
                        z[i] = margin.getScore(featuresByInstance,i);
                }
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
                    if(this.isBinaryConstrained)
                        break;
                }
                 
            }
            
        }
        
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (nk[y]==0){
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            }else{
                if(this.isBinaryConstrained){
                    diagvar[y][0] /= nk[y];
                    if (diagvar[y][0] < minvar) 
                        diagvar[y][0]=minvar;
                    
                    diagvar[y][1] = diagvar[y][0];
                    logdet += logMath.linearToLog(diagvar[y][0]); 
                    logdet += logMath.linearToLog(diagvar[y][1]); 
                }else                
                    for (int i=0;i<nlabs;i++) {
                        diagvar[y][i] /= nk[y];

                        if (diagvar[y][i] < minvar) 
                            diagvar[y][i]=minvar;

                        logdet += logMath.linearToLog(diagvar[y][i]);
                    }
            }    
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
            co/=2.0;
            gconst[y]=co;
            
            //change logWeights ... or not ??
            // logWeights[y]=logMath.linearToLog(nk[y]/nex[y]);
           
        }
         //System.out.println("priors: "+ Arrays.toString(logWeights));
        //System.out.println("trainviterbi");
        //printMean();
        //printVariace();   
        return nk;
    }
        

    
    public void trainStoch1gauss(Margin margin) {
        final float[] z = new float[nlabs];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
        }
        if(margin.getNumSamples()==0)
            ErrorsReporting.report("The sampling should be done outside gmm");
        
        samples=margin.getSamples();
 
        for (int s=0;s<samples.length;s++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            int ex =samples[s];
            if(!Margin.GENERATEDDATA)
                featuresByInstance = margin.getFeaturesPerInstance(ex);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(ex, 0);
                else                
                    z[0] = margin.getScore(featuresByInstance,0);
                z[1]=-z[0];
            }else{            
                for (int lab=0;lab<nlabs;lab++) {
                    if(Margin.GENERATEDDATA)
                        z[lab] = margin.getGenScore(ex, lab);
                    else                
                        z[lab] = margin.getScore(featuresByInstance,lab);
                    //System.out.println("lab="+lab+" z[lab]="+z[lab]);
                    assert !Float.isNaN(z[lab]);
                }
            }
            for (int i=0;i<nlabs;i++) {
                means[0][i]+=z[i];
            }
        }
        for (int i=0;i<nlabs;i++) {
            means[0][i]/=(float)samples.length;
            for (int j=1;j<nlabs;j++) 
                means[j][i]=means[0][i];
        }
        Arrays.fill(diagvar[0], 0);
        for (int s=0;s<samples.length;s++) {
            int ex = samples[s];
            List<Integer> featuresByInstance = new ArrayList<>();
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(ex);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(ex, 0);
                else                
                    z[0] = margin.getScore(featuresByInstance,0);
                z[1]=-z[0];
                tmp[0] = z[0]-means[0][0];
                tmp[1] = z[1]-means[0][1];
            }else{            
                for (int i=0;i<nlabs;i++) {
                    if(Margin.GENERATEDDATA)
                        z[i] = margin.getGenScore(ex, i);
                    else                
                        z[i] = margin.getScore(featuresByInstance,i);
                    assert !Float.isNaN(z[i]);
                    tmp[i] = z[i]-means[0][i];
                }
            }
            for (int i=0;i<nlabs;i++) {
                diagvar[0][i]+=tmp[i]*tmp[i];
                  
                
            }
        }
        assert samples.length>0;

        // precompute gconst
        /*
         * log de
         * (2pi)^{d/2} * |Covar|^{1/2} 
         */
        double logdet=0;
        for (int i=0;i<nlabs;i++) {
            diagvar[0][i] /= (double) samples.length;
            if (diagvar[0][i] < minvar) diagvar[0][i]=minvar;
            for (int j=1;j<nlabs;j++) 
                diagvar[j][i]=diagvar[0][i];
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
        
    public int nIterDone=0; 
    
    public double[] trainWithoutInit(Margin margin) {
        double loglike = getLoglike(margin);
    	double previousLogLike=loglike;
        double[] postPerClass=null;
        nIterDone=0;
        for (int iter=0;iter<nitersTraining;iter++,nIterDone++) {
            postPerClass=trainViterbi(margin);
            loglike = getLoglike(margin);
            if(Math.abs(loglike-previousLogLike)<toleranceTraining) break;
            previousLogLike=loglike;
        }
        return postPerClass;
    }
     public double[] trainStocWithoutInit(Margin margin) {
        double loglike = getLoglike(margin);
    	double previousLogLike=loglike;
        double[] postPerClass=null;
        nIterDone=0;
        for (int iter=0;iter<nitersTraining;iter++,nIterDone++) {
            postPerClass=trainStocViterbi(margin);
            loglike = getLoglike(margin);
            if(Math.abs(loglike-previousLogLike)<toleranceTraining) break;
            previousLogLike=loglike;
        }
        return postPerClass;
    }   
    /**
     * 
     * @param margin
     * @return posterior per class
     */
    public double[] train(Margin margin) {
    	// just to compute the variance and gconst:
    	train1gauss(margin);
    	// compute extreme values
    	List<List<Integer>> feats = margin.getFeaturesPerInstances();
    	float scmin=Float.MAX_VALUE,scmax=-Float.MAX_VALUE;
    	for (List<Integer> f : feats) {
    		float sc=margin.getScore(f, 0);
    		if (sc<scmin) scmin=sc;
    		if (sc>scmax) scmax=sc;
    	}
    	means[0][0]=scmax; means[0][1]=-scmax;
    	means[1][0]=scmin; means[1][1]=-scmin;
    	return trainWithoutInit(margin);
    }
    
    public double[] trainStoc(Margin margin) {
    	// just to compute the variance and gconst:
    	trainStoch1gauss(margin);
    	// compute extreme values
    	List<List<Integer>> feats = margin.getFeaturesPerInstances();
    	float scmin=Float.MAX_VALUE,scmax=-Float.MAX_VALUE;
    	for (List<Integer> f : feats) {
    		float sc=margin.getScore(f, 0);
    		if (sc<scmin) scmin=sc;
    		if (sc>scmax) scmax=sc;
    	}
    	means[0][0]=scmax; means[0][1]=-scmax;
    	means[1][0]=scmin; means[1][1]=-scmin;
    	return trainStocWithoutInit(margin);
    }
    
    public double[] computePosteriors(Margin margin) {
    	double[] nk = new double[nlabs];
    	final float[] z = new float[nlabs];
    	Arrays.fill(nk, 0.0);
    	Arrays.fill(tmp, 0);
    	int numInstances = margin.getNumberOfInstances();       
    	for (int inst=0;inst<numInstances;inst++) {
    		List<Integer> featuresByInstance = margin.getFeaturesPerInstance(inst);
    		for (int lab=0;lab<nlabs;lab++)
    			z[lab] = margin.getScore(featuresByInstance,lab);
    		double normConst = logWeights[0] + getLoglike(0, z);
    		tmp[0]=normConst;
    		for (int y=1;y<nlabs;y++){
    			tmp[y]=logWeights[y] + getLoglike(y, z);
    			normConst=  logMath.addAsLinear((float)normConst,(float)tmp[y]);
    		}
    		for (int y=0;y<nlabs;y++){ 
    			double posterior=logMath.logToLinear((float)tmp[y]-(float)normConst);
    			nk[y]+=posterior;
    		}        
    	}
    	return nk;
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
