/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Triple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.distribution.NormalDistribution;
import tools.Histoplot;

/**
 * This class is an interface with
 * the Stanford linear classifier
 * It retrieves the features and weights computed
 * by the classifier
 * 
 * taken from the Margin class implemented by Christophe Cerisara.
 * 
 * @author rojasbar
 */
public class Margin {
    public static boolean GENERATEDDATA=false;
    //Weights
    private double[][] weights;
    private Index<String> labelIdx, featureIdx;
    private LinearClassifier stanfordModel;
    private String classifierBinFile;
    //private int numInstances;
    //private double[] sumfeatsPerInst;
    private float[][] generatedScores;
    private List<List<Integer>> featsperInst = new ArrayList<>();
    private List<Integer> labelperInst = new ArrayList<>();    
    
    //paralell coordinate gradient
    
    private List<List<Double>> originalWeights = new ArrayList<>();
    List<List<Double>> shuffleWeights = new ArrayList<>();
    private int startIndex=0;
    private int endIndex=0;
    
    private List<List<Double>> subListOfFeatures= new ArrayList<>();
    private HashMap<Integer,Integer> shuffleAndOrFeatIdxMap = new HashMap<>();
    private HashMap<Integer,Integer> orAndShuffleFeatIdxMap = new HashMap<>();
    double[][] orWeightsCopy ;
    private int numInstances=0;
    
    public Margin(){
        
    }
    
    public Margin(LinearClassifier model) {
        weights=model.weights();
        labelIdx=model.labelIndex();
        featureIdx=model.featureIndex();
        stanfordModel = model;
    }    
    
    public void setWeights(double[][] weightss){
        this.weights=weightss;
    }

    public void setWeight(int index, double[] values){
        this.weights[index]=values;
    }    
    
    public void updateWeights(double[][] weightss){
        for(int l=0; l< weightss.length;l++)
          this.weights[l]=Arrays.copyOf(weightss[l],weightss[l].length);
    }    
    
    public double[][] getWeights(){
        return this.weights;
    }
    
    public boolean areSameWeights(double[][] otherWeights){
        boolean areSame=true;
        for(int i=0; i< weights.length;i++){
            for(int j=0; j< weights[i].length;j++){
                if(weights[i][j]!=otherWeights[i][j]){
                    areSame=false;
                    break;
                }    
            }
        }
        return areSame;
        
    }
    
    /**
     * Return the weights for a given feature in X
     * @param feature
     * @return 
     */
    public double[] getWeights(int feature){
        return weights[feature];
    }
    /*
     * Return the number of features
     */
    public int getNfeats() {
    	return weights.length;
    }
    /**
     * Return the number of labels
     * @return 
     */
    public int getNlabs() {
        return weights[0].length;
    }
    
    public Index<String> getLabelIndex(){
        return this.labelIdx;
    }

    public Index<String> getFeatureIndex(){
        return this.featureIdx;
    }  
    
    public LinearClassifier getClassifier(){
        return this.stanfordModel;
    }
    
    public float getScore(List<Integer> features, int label) {
        float sumWeightsOf1Features = 0;
        for (int j=0;j<features.size();j++) {
            sumWeightsOf1Features += weights[features.get(j)][label];
        }
        //System.out.println("SCORE:"+sumWeightsOf1Features);
        return sumWeightsOf1Features;
    }
    
    public float getGenScore(int instance, int label){
        return generatedScores[instance][label];
    }
    public double[][] getGenScore(){
        double[][] scores = new double[generatedScores.length][generatedScores[0].length];
        
        for(int i=0;i<generatedScores.length;i++)
            for(int j=0;j<generatedScores[i].length;j++)
               scores[i][j]= generatedScores[i][j];
        
        return scores;
    }       
    
    public float getScore(int[] features, int label) {
        float sumWeightsOf1Features = 0;
        for (int j=0;j<features.length;j++) {
            sumWeightsOf1Features += weights[features[j]][label];
        }
        return sumWeightsOf1Features;
    }
    
    public void generateBinaryRandomScore(int ninst){
        /*double[] scores0= new double[ninst];
        double[] scores1= new double[ninst];
        Arrays.fill(scores0, 0.0);
        Arrays.fill(scores1, 0.0);*/
        float[][] genScores= new float[ninst][2];
        NormalDistribution distr0 = new NormalDistribution(8, 0.5);
        NormalDistribution distr1 =  new NormalDistribution(2, 0.5);
        Random r = new Random();
        for(int i=0; i<ninst; i++){
            float rnd=r.nextFloat();
            genScores[i][0]=(rnd<0.9)?(float) distr0.sample():(float) distr1.sample();
            genScores[i][1]=-genScores[i][0];
            //scores0[i]=genScores[i][0];
            //scores1[i]=genScores[i][1];

        } 
        generatedScores=genScores;
        //Histoplot.showit(scores0, ninst);
        //Histoplot.showit(scores1, ninst);
    }
    public void generateRandomScore(int ninst, float[] priors){
        double[] scores= new double[ninst];
        Arrays.fill(scores, 0.0);
        
        float[][] genScores= new float[ninst][priors.length];
        List<NormalDistribution> distrs = new ArrayList(); 
        int k=0;
        int initialMean=8;
        for(int p=0; p<priors.length;p++){
            int mean=initialMean+k;
            double std= 0.5/(double) (p+1);
            distrs.add(new NormalDistribution(mean,std));
            System.out.println("Gaussian No. " + p + " mean " + mean + "  variance "+ std*std );
            if(p%2==0)
                k+=4*(p+1);
            else
                k-=4*(p+1);
        }
//        for(int i=0; i<ninst; i++){
//            for(int p=0; p<priors.length; p++){
//                genScores[i][p]=(float) distrs.get(p).sample();
//                
//            }
//            scores[i]=genScores[i][1];
//        }
        
        
        List<Double> priorList = new ArrayList<>();
        List<Double> sortedList = new ArrayList<>();
        for(int p=0; p<priors.length;p++){
            priorList.add(new Double(priors[p]));
            sortedList.add(new Double(priors[p]));
        }
        Collections.sort(sortedList);
        Random r = new Random();
        
        for(int i=0; i<ninst; i++){
            
            float rnd=r.nextFloat();
            
            Arrays.fill(genScores[i], 0f);
            for(int p=sortedList.size()-1; p>=0;p--){
                if(rnd<sortedList.get(p)){
                    int idx=priorList.indexOf(sortedList.get(p));
                    genScores[i][idx]=(float) distrs.get(idx).sample();
                    scores[i]= genScores[i][idx];
                    break;
                }else
                    rnd-=sortedList.get(p);
                    
     
            }
            
            float sumNonZeroVars=0f;
            List<Integer> zeroIds=new ArrayList<>();
            for(int l=0; l<priors.length;l++){
                if(genScores[i][l]==0)
                    zeroIds.add(l);
                else
                    sumNonZeroVars+=genScores[i][l];
            }
            for(int l=0; l<zeroIds.size();l++){
                //genScores[i][zeroIds.get(l)]= (1-sumNonZeroVars)*((priors[l]*priors.length)/ (float) zeroIds.size());
                genScores[i][zeroIds.get(l)]= (1-sumNonZeroVars)/ (float) zeroIds.size();
                
                
                
            }

        } 
        
        generatedScores=genScores;
        System.out.println(AnalyzeLClassifier.printMatrix(getGenScore()));
        Histoplot.showit(scores, ninst);
        
    }
    public double[] getScoreForAllInstancesLabel0(List<List<Integer>> features,double[] scores){
        for(int i=0; i< features.size();i++){
            scores[i]=getScore(features.get(i),0);
        }
        return scores;
    }
 
    public double[] getScoreForAllInstancesLabel1(List<List<Integer>> features,double[] scores){
        for(int i=0; i< features.size();i++){
            scores[i]=getScore(features.get(i),1);
        }
        return scores;
    }  
    
    public int classify(int[] feats) {
        int bestlab=-1;
        float labscore = -Float.MAX_VALUE;
        for (int i=0;i<getNlabs();i++) {
            float sc = getScore(feats, i);
            if (sc>labscore) {labscore=sc; bestlab=i;}
        }
        return bestlab;
    }    
    
    public int[] getTopWeights(double threshold,int numFeats){
        int[] featIndexes = new int[numFeats];
        List<Triple<String,String,Double>> topFeatures = stanfordModel.getTopFeatures(threshold, true, numFeats);
        int i=0;
        for(Triple obj:topFeatures){
            //System.out.println(obj.first.toString());
            featIndexes[i] =featureIdx.indexOf(obj.first.toString());
            i++;
            //label
            //System.out.println(obj.second.toString());
            //weight
            //System.out.println(obj.third.toString());
            
        }
        return featIndexes;
    }
    
    /**
     * The inner list contains all the weights for a given column of the matrix of weights
     */
    public void setOrWeights(){
       
        for(int col=0; col<weights[0].length; col++){
            List<Double> dim = new ArrayList<>();
            for(int i=0;i< weights.length;i++){
                dim.add(weights[i][col]);
            }
            originalWeights.add(dim);
        }
    }
    
    public List<Double> getOrWeights(int dimension){
        if(originalWeights.isEmpty())
            setOrWeights();
        
        return originalWeights.get(dimension);
    }
    
    public List<Double> shuffleWeights(){
        List<Double> shuffledW0 = new ArrayList<>(getOrWeights(0));
        
        Collections.shuffle(shuffledW0);
        shuffleWeights.add(shuffledW0);
        //set the indexes
        for(int i=0;i<shuffledW0.size();i++){
            shuffleAndOrFeatIdxMap.put(i,getOrWeights(0).indexOf(shuffledW0.get(i)));
            orAndShuffleFeatIdxMap.put(getOrWeights(0).indexOf(shuffledW0.get(i)), i);
        }  
        for(int col=1;col<getNlabs();col++){
            List<Double> shuffleWeightsDim= new ArrayList<>();
            for(int sIdx=0;sIdx<shuffledW0.size();sIdx++){
               int orIdx=this.shuffleAndOrFeatIdxMap.get(sIdx);
               shuffleWeightsDim.add(weights[orIdx][col]);              
            }
            shuffleWeights.add(shuffleWeightsDim);
        }
        
        return shuffleWeights.get(0);
    }
    
    public void copySharedyInfoParallelGrad(Margin margin){
        this.featsperInst=margin.featsperInst;
        this.labelperInst=margin.labelperInst;
        this.originalWeights= margin.originalWeights;
        this.shuffleWeights=margin.shuffleWeights;
        this.shuffleAndOrFeatIdxMap.putAll(margin.shuffleAndOrFeatIdxMap);
        this.orAndShuffleFeatIdxMap.putAll(margin.orAndShuffleFeatIdxMap); 
    }
    
    public List<Double> getShuffleWeights(){
        return this.shuffleWeights.get(0);
    }
    
    public List<Double> getShuffleWeights(int dimension){
        return this.shuffleWeights.get(dimension);
    }   
    public void setSubListOfShuffleFeats(int dimension, int startIdx, int endIdx){
        this.startIndex=startIdx;
        this.endIndex=endIdx;
        if(endIdx>shuffleWeights.get(0).size())
            endIdx=shuffleWeights.get(0).size();        
 

        subListOfFeatures.add(dimension,shuffleWeights.get(dimension).subList(startIdx, endIdx));
        
        
    }
    public void setSubListOfFeats(int dimension, int startIdx, int endIdx){
        this.startIndex=startIdx;
        this.endIndex=endIdx;
        if(endIdx>originalWeights.get(dimension).size())
            endIdx=originalWeights.get(dimension).size();
        subListOfFeatures.add(originalWeights.get(dimension).subList(startIdx, endIdx));
        
        
    } 
    public void setSubListOfFeats(int dimension){
        if(this.endIndex==0)
            return;

        if(endIndex>originalWeights.get(dimension).size())
            endIndex=originalWeights.get(dimension).size();
        subListOfFeatures.add(originalWeights.get(dimension).subList(startIndex, endIndex));
        
        
    }      
    public List<Double> getSubListOfFeats(int dimension){
        return this.subListOfFeatures.get(dimension);
    }
    
    public void copyOrWeightsBeforGradient(){
        if(weights.length==0)
            return;
        
        orWeightsCopy  = new double[weights.length][];
        int nlabs=weights[0].length;
        for(int i=0; i<weights.length; i++){
            Arrays.copyOf(weights[i],nlabs );
        }       
    }
    
    public void updatingStocGradientStep(int dimension,int subListIndex, double value){
        int shuffledIndex= startIndex+subListIndex;
        int index = shuffleAndOrFeatIdxMap.get(shuffledIndex);

        if(dimension == 2){
            weights[index][0]=value;
            weights[index][1]=-weights[index][0]; 
        }else{
            weights[index][dimension]=value;
        }
        
        
    }
    
    public void updatingGradientStep(int dimension,int subListIndex, double value){
        int index= startIndex+subListIndex;
        if(dimension == 2){
            weights[index][0]=value;
            weights[index][1]=-weights[index][0]; 
        }else{
            weights[index][dimension]=value;
        }
        
    }    
    public double[] getPartialShuffledWeight(int subListIndex){
        int shuffledIndex= startIndex+subListIndex;
        int index = shuffleAndOrFeatIdxMap.get(shuffledIndex);
        return(weights[index]);
       
    }
     public double[] getPartialWeight(int subListIndex){
        int index= startIndex+subListIndex;
        
        return(weights[index]);
       
    }   
    public double[][] getOriginalWeights(){
        return orWeightsCopy;
    }
    
    public int getOrIndexFromShuffled(int subListIndex){
        int shuffledIndex= startIndex+subListIndex;
        return this.shuffleAndOrFeatIdxMap.get(shuffledIndex);
    }
    public int getShuffledIndexFromOriginal(int orgIndex){
        return this.orAndShuffleFeatIdxMap.get(orgIndex);
    }    
    public int getOrWeightIndex(int subListIndex){
        int index= startIndex+subListIndex;
        return index;
    } 
    
    public int getSubSetStartIndex(){
        return this.startIndex;
    }
    public boolean isIndexInSubset(int orFeatIdx){
        if(startIndex <= orFeatIdx && orFeatIdx < this.endIndex)
            return true;
        return false;
    }
    
    public int getSubSetIndex(int orFeatIdx){
        int subSetIdx=-1;
        if(isIndexInSubset(orFeatIdx)){
            subSetIdx=orFeatIdx-this.startIndex;
        }
        return subSetIdx;
            
    }
    public int getNumberOfInstances(){
        if(numInstances==0){
            numInstances = getLabelPerInstances().size();
        }    
        return this.numInstances;
    }
    public void setFeaturesPerInstance(List<List<Integer>> featspInst){
        this.featsperInst = featspInst;
    }
    /**
     * Return the features per instance associated 
     * @param classifier
     * @param instance
     * @return 
     */    
    public List<Integer> getFeaturesPerInstance(Integer instance){
      return this.featsperInst.get(instance);   
    }
    public List<List<Integer>> getFeaturesPerInstances(){
      return this.featsperInst;   
    }  
    
    public void setLabelPerInstance(List<Integer> lblPerInsts){
        this.labelperInst=lblPerInsts;
        this.numInstances=lblPerInsts.size();
    }
    
    public Integer getLabelPerInstance(Integer instance){
      return this.labelperInst.get(instance);   
    }    
    public List<Integer> getLabelPerInstances(){
      return this.labelperInst;   
    }       
    
    public void setBinaryFile(String filename){
        this.classifierBinFile = filename;
    }
    public String getBinaryFile(){
        return this.classifierBinFile;
    }
    public void setNumberOfInstances(int numInst){
        this.numInstances=numInst;
    }    
}
