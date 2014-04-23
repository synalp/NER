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
    public float[] metropolis(NormalDistribution normDist,float delta, int dim){
        
        int naccept=0;
        float[] points = new float[dim];
        //UniformRealDistribution uniformDist= new UniformRealDistribution();
        
        for(int i=0; i<dim; i++){
            Random randomVar = new Random();  
            //double x = lo + randomVar.nextDouble() * (hi-lo);
            double x =normDist.sample();
            //random walk
            double xtrial=  x + (2*randomVar.nextDouble() - 1.0)*delta;
            double ratiopxtrial= normDist.density(xtrial)/normDist.density(x);
            //double ratiopxtrial=1.0;
            if(randomVar.nextDouble() <= ratiopxtrial){
                x=xtrial;
                naccept++;
            }
            
            points[i]= (float) x;
        }
        
        return points;
    }
     public float[] metropolis(float lo, float hi, float delta, int dim){
        
        int naccept=0;
        float[] points = new float[dim];
        UniformRealDistribution uniformDist= new UniformRealDistribution();
        
        for(int i=0; i<dim; i++){
            Random randomVar = new Random();  
            double x = lo + randomVar.nextDouble() * (hi-lo);
            
            //random walk
            double xtrial=  x + (2*randomVar.nextDouble() - 1.0)*delta;
            double ratiopxtrial= uniformDist.density(xtrial)/uniformDist.density(x);
            //double ratiopxtrial=1.0;
            if(randomVar.nextDouble() <= ratiopxtrial){
                x=xtrial;
                naccept++;
            }
            
            points[i]= (float) x;
        }
        
        return points;
    }   
    /**
     * 
     * @param gmm
     * @return 
     */
    public double[] integrate(GMMDiag gmm, String proposal, boolean metropolis){
        //ScatterPlotAPI plotPoints = new ScatterPlotAPI("Sampled Points");
        
        int dim = gmm.getDimension();
        int ntrials=50000;
        float minMean=Float.MAX_VALUE;
        float maxMean=Float.MIN_VALUE;
        float maxSigma=Float.MIN_VALUE;
        double integral=0.0;
        double[] mvIntegral = new double[dim];
        List<PlotAPI> plotIntegralk = new ArrayList<>();
        for(int i=0; i<dim;i++){
            plotIntegralk.add(new PlotAPI("Integral vs trials","Num of trials", "Integral["+i+"]"));       
            for(int j=0; j<dim;j++){
             double mean= gmm.getMean(i, j);
             double sigma= Math.sqrt(gmm.getVar(i, j, j));
             System.out.println("minmean: "+ minMean);
             System.out.println("maxmean: "+ maxMean);
             System.out.println("maxsigma: "+ maxSigma);
             if(mean < minMean)
                 minMean=(float)mean;
             if(mean > maxMean)
                 maxMean=(float)mean;
             if(sigma > maxSigma)
                 maxSigma=(float)sigma;
            
            }
        }
        
        float lo=minMean-(maxSigma*20);
        float hi=maxMean+(maxSigma*20);
        System.out.println("lower bounded= "+lo);
        System.out.println("upper bounded= "+hi);
        //random walk
        
        double sumFx=0.0;
        
        
        for(int i=0; i<ntrials;i++){
            float[] points = new float[dim];
            NormalDistribution normDist = new NormalDistribution();
            if(proposal.equals(CNConstants.GAUSSIAN)){
                normDist = computeEstimatedGaussian(gmm,dim);
                normDist = new NormalDistribution(normDist.getMean(),maxSigma);
                if(metropolis)
                    points = metropolis(normDist,0.5f,dim);
                else
                    points = samplingPoints(normDist,dim);        
                    
            }else {   
                if(metropolis)
                    points = metropolis(lo,hi,0.5f,dim);
                else
                   points = samplingPoints(normDist,dim);  
            }    
            //float[] points = samplingPoints(normDist,dim);
            //plotPoints.addPoint(points,i);
            //System.out.println( points[0]+","+points[1]+"\n");
            for(int k=0; k< dim; k++){
                
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
                    mvIntegral[k]= sumFx/((double)i+1);
                else 
                    mvIntegral[k] = (sumFx/((double)i+1))*Math.pow((hi-lo),dim-1); 
                //System.out.println("INTEGRAL SO FAR ..."+inte);
                plotIntegralk.get(k).addPoint(i,mvIntegral[k]);
            }
        }
        //estimation for the last trial for each dimension
//        for(int k=0; k< dim; k++){
//            if(proposal.equals(CNConstants.GAUSSIAN))
//                mvIntegral[k] = mvIntegral[k]*((ntrials-1)/ntrials);
//            else
//                mvIntegral[k] = mvIntegral[k]*((ntrials-1)/ntrials);
//        
//        
//            System.out.println("sum: "+ sumFx);
//            System.out.println("region: "+ Math.pow((hi-lo),dim-1));
//            System.out.println("****value of estimated integral for [k="+k+"] = "+ mvIntegral[k] );
//        }
        
        return mvIntegral;
    
    }

    
}
