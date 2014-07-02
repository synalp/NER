/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CRFClassifier;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.crf.CRFLabel;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Triple;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
public class MarginCRF {
    //Weights
    private double[][] weights;
    List<Index<CRFLabel>> labelIndices;
    private Index<String> labelIdx, featureIdx;
    private CRFClassifier stanfordNERModel;
    private Set<String> numClasses;
    //private int numInstances;
    //private double[] sumfeatsPerInst;

    
    public MarginCRF(CRFClassifier model) {
        weights=model.getWeights();
        stanfordNERModel = model;
        labelIndices=model.labelIndices();
        featureIdx=model.featureIndex();
        numClasses = model.labels();
    }    
    
    public void setWeights(double[][] weightss){
        this.weights=weightss;
    }
    
    public double[][] getWeights(){
        return this.weights;
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
    
    public CRFClassifier getClassifier(){
        return this.stanfordNERModel;
    }
    
    public float getScore(List<Integer> features, int typeOflabel) {
        float sumWeightsOf1Features = 0;
        for (int j=0;j<features.size();j++) {
            for(int k=0;k<weights[features.get(j)].length;k++){
                if(k%numClasses.size()==typeOflabel)
                    sumWeightsOf1Features += weights[features.get(j)][k];
            }
        }
        //System.out.println("SCORE:"+sumWeightsOf1Features);
        return sumWeightsOf1Features;
    }   
       
    public float getScore(int[] features, int label) {
        float sumWeightsOf1Features = 0;
        for (int j=0;j<features.length;j++) {
            sumWeightsOf1Features += weights[features[j]][label];
        }
        return sumWeightsOf1Features;
    }

    public double[] getScoreForAllInstancesGivenLabel(List<List<Integer>> features, double[] scores, int label){
        for(int i=0; i< features.size();i++){
            scores[i]=getScore(features.get(i),label);
        }
        return scores;
    }
    
    public int classify(int[] feats, int[] featsPerChain) {
        int bestlab=-1;
        float labscore = -Float.MAX_VALUE;
        for (int i=0;i<getNlabs();i++) {
            float sc = getScore(feats, i);
            if (sc>labscore) {labscore=sc; bestlab=i;}
        }
        return bestlab;
    }    
    
//    public int[] getTopWeights(){
//        int[] featIndexes = new int[50];
//        List<Triple<String,String,Double>> topFeatures = stanfordNERModel.getTopFeatures(0.5, true, 50);
//        int i=0;
//        for(Triple obj:topFeatures){
//            System.out.println(obj.first.toString());
//            featIndexes[i] =featureIdx.indexOf(obj.first.toString());
//            i++;
//            //label
//            //System.out.println(obj.second.toString());
//            //weight
//            //System.out.println(obj.third.toString());
//            
//        }
//        return featIndexes;
//    }
}
