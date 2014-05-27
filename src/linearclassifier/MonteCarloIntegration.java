/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;


import edu.stanford.nlp.stats.Distribution;
import gmm.GMMDiag;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import tools.CNConstants;
import tools.PlotAPI;
import tools.ScatterPlotAPI;



/**
 *
 * @author rojasbar
 */
public class MonteCarloIntegration {
    
    private Random randomVar = new Random(); 
    
    public void errorAnalysis(GMMDiag gmm,float[] py,int dim){
        
        int numRuns=10;
        double sumRisk=0.0;
        double sumRiskSq=0.0;
        for(int r=1;r<=numRuns;r++){
            float risk=0f;
            for(int k=0; k< dim; k++){
                    risk+=py[k]*integrate(gmm,k,CNConstants.UNIFORM, true,false);
            }
            sumRisk+=risk;
            sumRiskSq+=risk*risk;
            System.out.println("run ["+ r + "] : R="+risk);
        }
        double msquare= (sumRisk/(double)numRuns)*(sumRisk/(double)numRuns);
        double mssquare=sumRiskSq/(double)numRuns;
        double diff= mssquare-msquare;
        System.out.println(" Error: "+ diff);
    }
    
     public void errorAnalysisBinInt(GMMDiag gmm,float[] py,int dim){
        
        int numRuns=10;
        double sumRisk=0.0;
        double sumRiskSq=0.0;
        for(int r=1;r<=numRuns;r++){
            float risk=0f;
            for(int k=0; k< dim; k++){
                    risk+=py[k]*integrateBinaryCase(gmm,k,CNConstants.UNIFORM, true,false);
            }
            sumRisk+=risk;
            sumRiskSq+=risk*risk;
            System.out.println("run ["+ r + "] : R="+risk);
        }
        double msquare= (sumRisk/(double)numRuns)*(sumRisk/(double)numRuns);
        double mssquare=sumRiskSq/(double)numRuns;
        double diff= mssquare-msquare;
        System.out.println(" Error: "+ diff);
    }
     
    public NormalDistribution computeEstimatedGaussian(GMMDiag gmm, int dim){
         double accprodmu=0.0, accprodvar=0.0;
         for(int i=0; i<dim;i++){
             double prodmu=0.0, prodvar=0.0;
            for(int j=0; j<dim;j+=2){    
                
                double  prevmu = gmm.getMean(i, j);
                double  prevvar = gmm.getVar(i, j, j);       
                
                double mu = gmm.getMean(i,j+1);
                double var = gmm.getVar(i, j+1, j+1);  
                prodvar =  (prevvar*var)/(prevvar+var);
                prodmu =  (prevmu*var+mu*prevvar)/(prevvar+var);
                
            }
            if(accprodmu==0.0 && accprodvar==0.0){
                accprodmu=prodmu; accprodvar=prodvar;
            }else{    
                accprodmu= (accprodvar*prodvar)/(accprodvar+prodvar);
                accprodvar= (accprodmu*prodvar+prodmu*accprodvar)/(accprodvar+prodvar);
            }
            
         } 
         System.out.println("mu prod: "+ accprodmu+" \t "+ " sigma prod "+ Math.sqrt(accprodvar));
         NormalDistribution normDist = new NormalDistribution(accprodmu, Math.sqrt(accprodvar));
         return normDist;
    }
      public float[] samplingPoints(float lo, float hi,int dim){
        float[] points = new float[dim];
        Random randomVar = new Random();  
        for(int i=0; i<dim; i++){
            double x =lo + randomVar.nextDouble() * (hi-lo);
            points[i]= (float) x;
        }
        return points;
    }  
    
    public float[] samplingPoints(NormalDistribution normDist,int dim){
        float[] points = new float[dim];
        for(int i=0; i<dim; i++){
            double x =normDist.sample();
            points[i]= (float) x;
        }
        return points;
    }
    public float[] metropolis(float[] points,NormalDistribution normDist,float delta, int dim){
        
        int naccept=0;
        
        //UniformRealDistribution uniformDist= new UniformRealDistribution();
        
        for(int i=0; i<dim; i++){
           
            //double x = lo + randomVar.nextDouble() * (hi-lo);
            float x = points[i];
            //random walk
            float xtrial=  x + (2*randomVar.nextFloat() - 1f)*delta;
            double ratiopxtrial= normDist.density(xtrial)/normDist.density(x);
            //double ratiopxtrial=1.0;
            if(randomVar.nextFloat() <= ratiopxtrial){
                x=xtrial;
                naccept++;
            }
            
            points[i]= (float) x;
        }
        
        return points;
    }
     public float[] metropolis(float[] points,float lo, float hi, float delta, int dim){
        
        int naccept=0;
        
        UniformRealDistribution uniformDist= new UniformRealDistribution(lo,hi);
        
        for(int i=0; i<dim; i++){
  
            //random walk
            float xtrial=  points[i] + (2*randomVar.nextFloat() - 1f)*delta;
            double ratiopxtrial= uniformDist.density(xtrial)/uniformDist.density(points[i]);
            //double ratiopxtrial=1.0;
            if(randomVar.nextDouble() <= ratiopxtrial){
                points[i]=xtrial;
                naccept++;
            }
            
            
        }
        
        return points;
    }   
    /**
     * 
     * @param gmm
     * @return 
     */
    public double integrate(GMMDiag gmm, int k, String proposal, boolean metropolis, boolean isplot){
        //ScatterPlotAPI plotPoints = new ScatterPlotAPI("Sampled Points");
        
        int dim = gmm.getDimension();
        int ntrials=50000;
        float minMean=Float.MAX_VALUE;
        float maxMean=Float.MIN_VALUE;
        float maxSigma=Float.MIN_VALUE;
        float minSigma=Float.MAX_VALUE;
        double integral=0.0;
        PlotAPI plotIntegral = null;
        if(isplot)
            plotIntegral =new PlotAPI("Integral vs trials","Num of trials", "Integral["+k+"]");       
        for(int i=0; i<dim;i++){
            
            for(int j=0; j<dim;j++){
             double mean= gmm.getMean(i, j);
             double sigma= Math.sqrt(gmm.getVar(i, j, j));
             /*System.out.println("minmean: "+ minMean);
             System.out.println("maxmean: "+ maxMean);
             System.out.println("minsigma: "+ minSigma);
             System.out.println("maxsigma: "+ maxSigma);*/
             if(mean < minMean)
                 minMean=(float)mean;
             if(mean > maxMean)
                 maxMean=(float)mean;
             if(sigma > maxSigma)
                 maxSigma=(float)sigma;
            
            }
        }
        
        float lo=minMean-(maxSigma);
        float hi=maxMean+(maxSigma);
        //System.out.println("lower bounded= "+lo);
        //System.out.println("upper bounded= "+hi);
        NormalDistribution normDist = new NormalDistribution();
        if(proposal.equals(CNConstants.GAUSSIAN))
            normDist = computeEstimatedGaussian(gmm,dim);
        //random walk
        double sumFx=0.0;
        
        float[] points = new float[dim];
        for(int i=0; i<dim; i++){
            points[i] =  lo +  randomVar.nextFloat() * (hi-lo); 
        }
        for(int i=0; i<ntrials;i++){
           
            
            if(proposal.equals(CNConstants.GAUSSIAN)){
                //normDist = new NormalDistribution(normDist.getMean(), normDist.getStandardDeviation());
                //normDist = new NormalDistribution(normDist.getMean(),maxSigma);
                normDist = new NormalDistribution(normDist.getMean(),maxSigma*200);
                if(metropolis)
                    points = metropolis(points,normDist,0.5f,dim);
                else
                    points = samplingPoints(normDist,dim);        
                    
            }else {   
                if(metropolis)
                    points = metropolis(points,lo,hi,0.5f,dim);
                else
                   points = samplingPoints(normDist,dim);  
            }    
            //float[] points = samplingPoints(normDist,dim);
            //plotPoints.addPoint(points,i);
            //System.out.println( points[0]+","+points[1]+"\n");
            
                
                float alphaSum=0.0f;
                for(int j=0; j< dim ; j++){
                    if(j==k)
                        continue;
                    alphaSum+=points[j];
                }
                float oneMinusSum= 1 - alphaSum;
                points[k]=oneMinusSum;
                double firstGauss= gmm.getLike(k,k, points[k]);
                double prodOfGauss=1.0;

                for(int l=0;l< dim; l++){
                    if(l==k)
                        continue;
                    prodOfGauss*=gmm.getLike(k,l, points[l]);
                }

                double lossTerm=0.0;
            
                for(int m=0; m<dim; m++){
                    if(m==k)
                        continue;
                    double val=(points[m]>-1)?points[m]:-0.99;
                    lossTerm+=1.0+val;
                }

                double f=firstGauss*prodOfGauss*lossTerm;

                //System.out.println("TRIAL ..."+i+ " Function "+ f);
                sumFx+=f;
                if(proposal.equals(CNConstants.GAUSSIAN))
                    sumFx+=f/normDist.density(points[k]);
                else
                    sumFx+=f;
                //System.out.println("SUM ..."+sumFx);
                
                if(proposal.equals(CNConstants.GAUSSIAN))
                    integral= sumFx/((double)i+1);
                else 
                    integral = (sumFx/((double)i+1))*Math.pow((hi-lo),dim-1); 
                //System.out.println("INTEGRAL SO FAR ..."+inte);
                if(isplot)
                    plotIntegral.addPoint(i,integral);
            
        }
        //estimation for the last trial for each dimension
        
            if(proposal.equals(CNConstants.GAUSSIAN))
                integral = integral*((ntrials-1)/(double)ntrials);
            else
                integral = integral*((ntrials-1)/(double)ntrials);
        
            /*
            System.out.println("sum: "+ sumFx);
            System.out.println("region: "+ Math.pow((hi-lo),dim-1));
            System.out.println("****value of estimated integral for [k="+k+"] = "+ integral );
            */
        
        return integral;
    
    }

    public double integrateBinaryCase(GMMDiag gmm, int k, String proposal, boolean metropolis, boolean isplot){
         //ScatterPlotAPI plotPoints = new ScatterPlotAPI("Sampled Points");
        
        int dim = gmm.getDimension();
        int ntrials=50000;
        float minMean=Float.MAX_VALUE;
        float maxMean=Float.MIN_VALUE;
        float maxSigma=Float.MIN_VALUE;
        float minSigma=Float.MAX_VALUE;
        double integral=0.0;
        PlotAPI plotIntegral = null;
        if(isplot)
            plotIntegral =new PlotAPI("Integral vs trials","Num of trials", "Integral["+k+"]");       
        for(int i=0; i<dim;i++){
            
            for(int j=0; j<dim;j++){
             double mean= gmm.getMean(i, j);
             double sigma= Math.sqrt(gmm.getVar(i, j, j));
             /*System.out.println("minmean: "+ minMean);
             System.out.println("maxmean: "+ maxMean);
             System.out.println("minsigma: "+ minSigma);
             System.out.println("maxsigma: "+ maxSigma);*/
             if(mean < minMean)
                 minMean=(float)mean;
             if(mean > maxMean)
                 maxMean=(float)mean;
             if(sigma > maxSigma)
                 maxSigma=(float)sigma;
            
            }
        }
        
        float lo=minMean-(maxSigma);
        float hi=maxMean+(maxSigma);
        //System.out.println("lower bounded= "+lo);
        //System.out.println("upper bounded= "+hi);
        NormalDistribution normDist = new NormalDistribution();
        if(proposal.equals(CNConstants.GAUSSIAN))
            normDist = computeEstimatedGaussian(gmm,dim);
        //random walk
        double sumFx=0.0;
        for(int i=0; i<ntrials;i++){
            float[] points = new float[dim];
            
            if(proposal.equals(CNConstants.GAUSSIAN)){
                //normDist = new NormalDistribution(normDist.getMean(), normDist.getStandardDeviation());
                //normDist = new NormalDistribution(normDist.getMean(),maxSigma);
                normDist = new NormalDistribution(normDist.getMean(),maxSigma*200);
                if(metropolis)
                    points = metropolis(points,normDist,0.5f,dim);
                else
                    points = samplingPoints(normDist,dim);        
                    
            }else {   
                if(metropolis)
                    points = metropolis(points,lo,hi,0.5f,dim);
                else
                   points = samplingPoints(normDist,dim);  
            }    
            //float[] points = samplingPoints(normDist,dim);
            //plotPoints.addPoint(points,i);
            //System.out.println( points[0]+","+points[1]+"\n");
            
                
                float alphazero=points[0];
                points[1]=-alphazero;
                
                double prodOfGauss=1.0;

                for(int l=0;l< dim; l++){
                   prodOfGauss*=gmm.getLike(k,l, points[l]);
                }

                
                double lossTerm=0.0;
            
                for(int m=0; m<dim; m++){
                    double val=0.0;
                    if(m==0){
                        val=(points[m]<0.5)?points[m]:0.5;
                        lossTerm=1.0-2*val;
                    }else{
                        val=(points[m]>-0.5)?points[m]:-0.5;
                        lossTerm=1.0+2*val;
                    }     
                }
                
                double f=prodOfGauss*lossTerm;

                //System.out.println("TRIAL ..."+i+ " Function "+ f);
                sumFx+=f;
                if(proposal.equals(CNConstants.GAUSSIAN))
                    sumFx+=f/normDist.density(points[k]);
                else
                    sumFx+=f;
                //System.out.println("SUM ..."+sumFx);
                
                if(proposal.equals(CNConstants.GAUSSIAN))
                    integral= sumFx/((double)i+1);
                else 
                    integral = (sumFx/((double)i+1))*Math.pow((hi-lo),dim-1); 
                //System.out.println("INTEGRAL SO FAR ..."+inte);
                if(isplot)
                    plotIntegral.addPoint(i,integral);
            
        }
        //estimation for the last trial for each dimension
        
            if(proposal.equals(CNConstants.GAUSSIAN))
                integral = integral*((ntrials-1)/(double)ntrials);
            else
                integral = integral*((ntrials-1)/(double)ntrials);
        
            /*
            System.out.println("sum: "+ sumFx);
            System.out.println("region: "+ Math.pow((hi-lo),dim-1));
            System.out.println("****value of estimated integral for [k="+k+"] = "+ integral );
            */
        
       return integral; 
    } 
}
