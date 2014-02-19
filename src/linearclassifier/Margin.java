/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;

import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.util.Index;
import java.util.Arrays;
import java.util.List;

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
    //Weights
    private double[][] weights;
    private Index<String> labelIdx, featureIdx;
    private LinearClassifier stanfordModel;
    //private int numInstances;
    //private double[] sumfeatsPerInst;

    
    public Margin(LinearClassifier model) {
        weights=model.weights();
        labelIdx=model.labelIndex();
        featureIdx=model.featureIndex();
        stanfordModel = model;
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
       
    public float getScore(int[] features, int label) {
        float sumWeightsOf1Features = 0;
        for (int j=0;j<features.length;j++) {
            sumWeightsOf1Features += weights[features[j]][label];
        }
        return sumWeightsOf1Features;
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
    
    
}
