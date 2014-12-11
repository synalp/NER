package gmm;



import edu.emory.mathcs.backport.java.util.Collections;
import edu.stanford.nlp.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import linearclassifier.Margin;
import optimization.MultiCoreStocCoordDescent;
import tools.CNConstants;
import tools.Histoplot;
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
 * Changed trainEM, to compute the parameters based on the posterior probability
 * 
 * @author xtof
 * <!--
 * Modifications History
 * Date             Author   	Description
 * Sept, 2014       rojasbar  	Changed trainEM, to compute the parameters based on the posterior probability.
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

    
    
    // parameters to tune;
    public static double splitRatio=0.1;
    public static int nitersTraining=20;
    public static double toleranceTraining=0.0004; //2e-05;
    private int CURRENTGMMTRITER=CNConstants.INT_NULL;
    
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
    public double[] trainEM(Margin margin) {
        final float[] z = new float[nlabs];
        margin.sumXSqAll= new double[nlabs];
        final GMMDiag gmm0 = this.clone();
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        int[] nex = new int[nlabs];
        margin.nkAll = new double[nlabs];
        
        
        Arrays.fill(nex, 0);
        Arrays.fill(margin.nkAll, 0.0);
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
                margin.nkAll[y]+=posterior;
                margin.sumXSqAll[y]+=posterior*z[y]*z[y];
                for (int l=0;l<nlabs;l++){ 
                    means[y][l]+=posterior*z[l];  
                    if(this.isBinaryConstrained)
                        break;
                        
                }

            }
            
            //System.out.println(" instance "+inst + "\n normConst = "+ normConst +"  sum mean ["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"]  nk="+Arrays.toString(nk) );

        }
        if(isBinaryConstrained){
           means[0][0]=(margin.nkAll[0]==0)?0:means[0][0]/margin.nkAll[0];
           means[0][1]=-means[0][0];
           means[1][0]=(margin.nkAll[1]==0)?0:means[1][0]/margin.nkAll[1];
           means[1][1]=-means[1][0];           
        }else{
            for (int y=0;y<nlabs;y++) {
                
                if (margin.nkAll[y]==0)
                    for (int i=0;i<nlabs;i++) 
                        means[y][i]=0; //or means[y][i]=Float.MAX_VALUE; ?
                else
                    for (int i=0;i<nlabs;i++){
                        means[y][i]/=margin.nkAll[y];
                    }    


            }
        }
        //System.out.println("["+ means[0][0]+","+means[0][1]+";\n"+ means[1][0]+","+means[1][1]+"] " + " nk="+Arrays.toString(nk) );   
        ///*
         //REMOVED ALL THIS CODE, EXTRA SCAN OVER ALL THE EXAMPLES TOO COSTLY
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
        //*/
        
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (margin.nkAll[y]==0){
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            }else{
                if(this.isBinaryConstrained){
                    //diagvar[y][0] = (margin.sumXSqAll[y]/margin.nkAll[y])-(means[y][0]*means[y][0]);
                    diagvar[y][0] /= margin.nkAll[y];
                    if (diagvar[y][0] < minvar) 
                        diagvar[y][0]=minvar;
                    
                    diagvar[y][1] = diagvar[y][0];
                    logdet += logMath.linearToLog(diagvar[y][0]); 
                    logdet += logMath.linearToLog(diagvar[y][1]); 
                }else
                    for (int i=0;i<nlabs;i++) {
                        diagvar[y][i] = (margin.sumXSqAll[y]/margin.nkAll[y])- (means[y][i]*means[y][i]);

                        if (diagvar[y][i] < minvar) 
                            diagvar[y][i]=minvar;

                        logdet += logMath.linearToLog(diagvar[y][i]);
                    }
            }    
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
            co/=2.0;
            gconst[y]=co;
            
            //change logWeights ... or not ??
            logWeights[y]=logMath.linearToLog(margin.nkAll[y]/nex[y]);
           
        }
         //System.out.println("priors: "+ Arrays.toString(logWeights));
        //System.out.println("trainviterbi");
        //printMean();
        //printVariace();   
        return margin.nkAll;
    }
    
    public double[][] computePartitionMu(Margin margin, GMM gmm0,float[] z,int[] nex){
        double[][] muPart1= new double[means.length][means[0].length];
        double[] nkPart = new double[nlabs];
        
        margin.sumXSqPart=new double[z.length];
        //postPart=new double[z.length];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(muPart1[i], 0);
        }        
        int[] instances = margin.getInstancesCurrThrFeat();
        if(instances == null){
            ErrorsReporting.report("No instances for that feature");
            return null;
        }
        for (int i=0;i<instances.length;i++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            int inst= instances[i];
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(inst, 0);
                else
                    z[0] = margin.getScore(featuresByInstance,0);
                
                z[1]=-z[0];
                //double tmpval = margin.getScore(featuresByInstance,1);
                //System.out.println(" tmp vs z[1]: "+ tmpval + " - " + z[1]);
                
                
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
                //double posterior = margin.post[y];
                nkPart[y]+=posterior;
                margin.sumXSqPart[y]+=posterior*(z[y]*z[y]);
                
                for (int l=0;l<nlabs;l++){ 
                    muPart1[y][l]+=posterior*z[y];  
                    if(this.isBinaryConstrained)
                        break;
                        
                }

            }
            
            
            
        } 
        if(isBinaryConstrained){
           muPart1[0][1]=-muPart1[0][0];
           muPart1[1][1]=-muPart1[1][0];           
        }
        System.out.println(" nkPart="+Arrays.toString(nkPart) );
        return muPart1;
    }
    
    public double[] trainNaiveApproximatedEM(Margin margin) {
        
        int iter = margin.getThreadIteration();
         
        //before splitting in threads
        if(iter==CNConstants.INT_NULL)
            return trainEM(margin);
        
        final float[] z = new float[nlabs];

        final GMMDiag gmm0 = this.clone();

        int[] nex = new int[nlabs];
        
        
        Arrays.fill(nex, 0);
        

        //SCD iteration
        if(iter%40==0)
            return trainEM(margin);
        
        
         
        //compute previous mean for the partition at iteration 0 of gmm
        /*
        if(CURRENTGMMTRITER==0){
            margin.previousMuPart=computePartitionMu( margin,  gmm0, z,nex);
            System.arraycopy( margin.sumXSqPart, 0,margin.previousSumXPart1 , 0, margin.sumXSqPart.length );              
        }
       */
        //compute again the new means
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvar[i], 0);
        }
        //System.out.println("all sum of ss Xpart = "+Arrays.toString(margin.sumXSqAll));
        //System.out.println("previous sum of ss Xpart = "+Arrays.toString(margin.previousSumXPart1));
        //here I have the mean for the instances impacted by the thread        
        double[][] newMuPart1=computePartitionMu( margin,  gmm0, z,nex);
        if(CURRENTGMMTRITER==0){
            for (int i=0;i<nlabs;i++) 
                System.arraycopy( newMuPart1[i], 0,margin.previousMuPart[i] , 0, newMuPart1[i].length );  
            
            System.arraycopy( margin.sumXSqPart, 0,margin.previousSumXPart1 , 0, margin.sumXSqPart.length );
            
        }
        //System.out.println("sum of ss Xpart = "+Arrays.toString(margin.sumXSqPart));
        /*
        for (int l=0;l<nlabs;l++){
            margin.sumXSqAll[l]=Math.abs(margin.sumXSqPart[l]+margin.sumXSqAll[l]-margin.previousSumXPart1[l]);
        } */ 
        
        for (int y=0;y<nlabs;y++)
            for (int l=0;l<nlabs;l++){
                //means[y][l]=gmm0.means[y][l]-(margin.previousMuPart[y][l]/margin.nkAll[y]) +(newMuPart1[y][l]/margin.nkAll[y]);
                means[y][l]=margin.previousMean[y][l]-(margin.previousMuPart[y][l]/margin.nkAll[y]) +(newMuPart1[y][l]/margin.nkAll[y]);
            } 
        ///*
        System.out.println("********trainApproximatedEM*********start debugging*******");
        System.out.println("previous mu ");
        printMatrix(margin.previousMean);   
        System.out.println("previous mu thread partition");
        printMatrix(margin.previousMuPart);           
        System.out.println("current mu thread partition");
        printMatrix(newMuPart1);         
        System.out.println(" nk="+Arrays.toString(margin.nkAll) );   
        System.out.println("NEW MEAN");
        printMean();        
        //*/
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (margin.nkAll[y]==0){
                for (int i=0;i<nlabs;i++) {
                    diagvar[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvar[y][i]);
                }
            }else{
                if(this.isBinaryConstrained){
         
                    diagvar[y][0] =  ((margin.sumXSqAll[y]-margin.previousSumXPart1[y] + margin.sumXSqPart[y])/margin.nkAll[y])- (means[y][0]*means[y][0]);
                    
                    if (diagvar[y][0] < minvar) 
                        diagvar[y][0]=minvar;
                    
                    diagvar[y][1] = diagvar[y][0];
                    logdet += logMath.linearToLog(diagvar[y][0]); 
                    logdet += logMath.linearToLog(diagvar[y][1]); 
                }else
                    for (int i=0;i<nlabs;i++) {
                                    
                        //diagvar[y][i] /= nk[y];
                        //diagvar[y][i] = (margin.previousSumXPart1[y] + margin.sumXSqAll[y]-margin.sumXSqPart[y])-means[y][i];
                        diagvar[y][i] = (margin.sumXSqAll[y]-margin.previousSumXPart1[y] + margin.sumXSqPart[y])-(means[y][i]*means[y][i]);
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
        ///*
        System.out.println(" nk="+Arrays.toString(margin.nkAll) );   
        System.out.println("NEW all sum of ss Xpart = "+Arrays.toString(margin.sumXSqAll));
        System.out.println("previousSumXPart1 = "+Arrays.toString(margin.previousSumXPart1));
        System.out.println("sum of ss Xpart = "+Arrays.toString(margin.sumXSqPart));
        System.out.println("NEW VARIANCE");
        printVariace();
        System.out.println("********trainApproximatedEM*********end debugging*******");  
        //*/
         //System.out.println("priors: "+ Arrays.toString(logWeights));
        //System.out.println("trainNaiveApproximatedEM");
        //printMean();
        //printVariace();  
        for (int i=0;i<nlabs;i++) 
            System.arraycopy( newMuPart1[i], 0,margin.previousMuPart[i] , 0, newMuPart1[i].length );  
            
        System.arraycopy( margin.sumXSqPart, 0,margin.previousSumXPart1 , 0, margin.sumXSqPart.length );        
        margin.previousMean = new double[means.length][means[0].length];
        for(int i=0; i<means.length;i++)
            System.arraycopy(means[i], 0, margin.previousMean[i], 0, means[i].length);  
        
        for(int y=0; y< nlabs;y++)
            logWeights[y]=logMath.linearToLog(margin.nkAll[y]/nex[y]);
        
        return margin.nkAll;
    }  
    
    public  void updatingGMMAfterRiskGradientStep(Margin margin, float gradStep){
        final float[] z = new float[nlabs];
        double[][] muPart1= new double[means.length][means[0].length];
        double[] nkPart = new double[nlabs];
        
        final GMMDiag gmm0 = this.clone();
        
        margin.sumXSqPart=new double[z.length];
        
        int[] instances = margin.getInstancesCurrThrFeat();
            
         for (int i=0;i<instances.length;i++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            int inst= instances[i];
            if(!Margin.GENERATEDDATA)            
                featuresByInstance = margin.getFeaturesPerInstance(inst);
            if(isBinaryConstrained){
                if(Margin.GENERATEDDATA)
                    z[0] = margin.getGenScore(inst, 0);
                else
                    z[0] = margin.getScore(featuresByInstance,0);
                
                z[1]=-z[0];
                //double tmpval = margin.getScore(featuresByInstance,1);
                //System.out.println(" tmp vs z[1]: "+ tmpval + " - " + z[1]);
                
                
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
                
                //ex2lab[inst]=y;
                double posterior=logMath.logToLinear((float)tmp[y]-normConst);
                //double posterior = margin.post[y];
                nkPart[y]+=posterior;
                margin.sumXSqPart[y]+=posterior*(z[y]*z[y]);
                
                for (int l=0;l<nlabs;l++){ 
                    muPart1[y][l]+=posterior*z[y];  
                    if(this.isBinaryConstrained)
                        break;
                        
                }

            }
            
            
            
        } 
        
       muPart1[0][1]=-muPart1[0][0];
       muPart1[1][1]=-muPart1[1][0];           
           
       for (int y=0;y<nlabs;y++)
        for (int l=0;l<nlabs;l++){
            means[y][l]+= muPart1[y][l] * gradStep / margin.nkAll[y];
        } 
       
        margin.previousMean = new double[means.length][means[0].length];
        for(int i=0; i<means.length;i++)
            System.arraycopy(means[i], 0, margin.previousMean[i], 0, means[i].length);  
        
        System.out.println("In updatingGMMAfterRiskGradientStep");
        //printMean();
        //printVariace();
        
//        for (int i=0;i<instances.size();i++){
//                postsum0+=gmm.postPerEx[ex];
//        }        
//            float oldMean0 = gmm.mean0;
//            gmm.mean0 += postsum0 * MultiCoreStocCoordDescent.delta / gmm.post[0];
//            float postsum1=0;
//            for (int ex : exImpacted) postsum1+=1f-gmm.postPerEx[ex];
//            float oldMean1 = gmm.mean1;
//            gmm.mean1 += postsum1 * MultiCoreStocCoordDescent.delta / gmm.post[1];


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
     * after splitting by trainEM
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

        /*
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
    public double[] trainStocEM(Margin margin) {
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
            postPerClass=trainEM(margin);
            loglike = getLoglike(margin);
            if(Math.abs(loglike-previousLogLike)<toleranceTraining) break;
            previousLogLike=loglike;
        }
        System.out.println("***end full gmm training******");
        printMean();
        printVariace();    
        System.out.println("nkAll" + Arrays.toString(margin.nkAll));
        System.out.println("******************************");
        return postPerClass;
    }
    public double[] trainApproxWithoutInit(Margin margin) {
        double loglike = getLoglike(margin);
    	double previousLogLike=loglike;
        double[] postPerClass=null;
        nIterDone=0;

        
        for (int iter=0;iter<nitersTraining;iter++,nIterDone++) {
            CURRENTGMMTRITER=iter;
            postPerClass=trainNaiveApproximatedEM(margin);
            loglike = getLoglike(margin);
            if(Math.abs(loglike-previousLogLike)<toleranceTraining) break;
            previousLogLike=loglike;
        }
        
        System.out.println("***end of training******");
        printMean();
        printVariace();
        ///*
        if(margin.lastRperSCDIter){ 
            margin.previousMean = new double[means.length][means[0].length];
            for(int i=0; i<means.length;i++)
                System.arraycopy(means[i], 0, margin.previousMean[i], 0, means[i].length);
        } 
        //*/
        System.out.println("nkAll" + Arrays.toString(margin.nkAll));
        return postPerClass;
    }    
     public double[] trainStocWithoutInit(Margin margin) {
        double loglike = getLoglike(margin);
    	double previousLogLike=loglike;
        double[] postPerClass=null;
        nIterDone=0;
        for(int s=0; s< 100 ;s++){
            
            for (int iter=0;iter<nitersTraining;iter++,nIterDone++) {
                postPerClass=trainStocEM(margin);
                loglike = getLoglike(margin);
                if(Math.abs(loglike-previousLogLike)<toleranceTraining) break;
                previousLogLike=loglike;
            }
            margin.sampling(0.1);
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
        margin.sampling(0.1);
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
    public double[] trainApproximation(Margin margin) {
    	// just to compute the variance and gconst:
    	if(margin.getThreadIteration()==CNConstants.INT_NULL || margin.getThreadIteration()%40==0)
            train1gauss(margin);
         //keep a copy of the previous mean and variance
        if(margin.previousGmm!=null ){
            for(int i=0; i< nlabs; i++){
                System.arraycopy( margin.previousGmm.means[i], 0,means[i] , 0, nlabs ); 
                System.arraycopy( margin.previousGmm.diagvar[i], 0,diagvar[i] , 0, nlabs ); 
            }    
        }else{
            trainEstimatedGaussians(margin);
        }        
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
    	return trainApproxWithoutInit(margin);
    }    
    
    /**
     * This method assumes the binary constrain
     * f_theta_0(x)= -f_theta_1(x)
     * <ul>
     * <li> Sort the scores f_theta(x) </li>
     *<li> Split the scores in two classes, </li>
     * <li> Estimates Gaussians taking into account the priors </li>
     * </ul>
     * @return 
     */
    public void trainEstimatedGaussians(Margin margin){
        
        int numInstances = margin.getNumberOfInstances();
        List<Double> scoreList = new ArrayList<>();
        double[] scores = new double[numInstances];
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
        }
        
        for (int ex=0;ex<numInstances;ex++) {
            List<Integer> featuresByInstance = new ArrayList<>();
            
            if(!Margin.GENERATEDDATA)
                featuresByInstance = margin.getFeaturesPerInstance(ex);
            
            if(Margin.GENERATEDDATA)
               scoreList.add(new Double(margin.getGenScore(ex, 0)));
            else{                
                scoreList.add(new Double(margin.getScore(featuresByInstance,0)));               
                scores[ex]=margin.getScore(featuresByInstance,0);
            }
        } 
        
       Collections.sort(scoreList);
       //Double negPrior = margin.getPriorMap().get(CNConstants.INT_NULL);
       int partSize = Math.round((float)logMath.logToLinear((float) logWeights[1]) *scoreList.size());
       //Histoplot
       Histoplot.showit(scores ,  scoreList.size());
       List<Double> firstGauss = scoreList.subList(0, partSize);
       List<Double> secondGauss = scoreList.subList(partSize, scoreList.size());
       
       //second gauss corresponds to the class 0
       double mean0=0.0;
       double sqsum0=0.0;
       for(int i=0; i< secondGauss.size();i++){
           mean0+=secondGauss.get(i);
           sqsum0+=secondGauss.get(i)*secondGauss.get(i);
                   
       }
       mean0/=secondGauss.size();
       double[] var= new double[nlabs]; 
       var[0]=sqsum0/secondGauss.size() -  (mean0*mean0);
       //first gauss corresponds to the class 1
       double sqsum1=0.0;
       double mean1=0.0;
       for(int i=0; i< firstGauss.size();i++){
           mean1+=firstGauss.get(i);
           sqsum1+=firstGauss.get(i)*firstGauss.get(i);
       }        
       mean1/=firstGauss.size(); 
       var[1]= sqsum1/firstGauss.size() -  (mean1*mean1);
       means[0][0]=mean0;
       means[0][1]=-mean0;
       means[1][0]=mean1;
       means[1][1]= -mean1;
       
       for (int y=0;y<nlabs;y++) {
           double logdet=0;
           diagvar[y][0]=var[y];
           logdet += logMath.linearToLog(diagvar[y][0]);
           diagvar[y][1]=var[y];
           logdet += logMath.linearToLog(diagvar[y][1]);


           double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logdet;
           co/=2.0;       
           gconst[y]=co;
       
       }             
       System.out.println("trainEstimatedGaussians ***");
       printMean();
       printVariace();
       
    }
    
    public int getSplitIdx(List<Double> list){
        int partiSize = list.size()/2;
        int partSize= Math.round(partiSize);
        List<Double> first = list.subList(0, partSize);
        List<Double> second = list.subList(partSize , list.size());
        if(first.get(first.size()-1) < 0 && second.get(0) > 0)
            return first.size();
        
        if(first.get(first.size()-1) < 0 && second.get(0) < 0){
            return getSplitIdx(second);
        }
        
        return getSplitIdx(first);
        
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
    
        public void printMatrix(double[][] m){
            String matrix="=[";
            for(int i=0; i<  m.length;i++){
                for(int j=0; j<  m[i].length;j++){
                    matrix+= m[i][j];
                    if(j<nlabs-1)
                        matrix+=" , ";
                } 
                if(i<nlabs-1)
                    matrix+=";";
            }    
            matrix+="]";
            System.out.println(matrix);        
    }
}
