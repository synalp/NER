/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package linearclassifier;

import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Triple;
import java.util.Arrays;
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
    //private int numInstances;
    //private double[] sumfeatsPerInst;
    private float[][] generatedScores;

    
    public Margin(LinearClassifier model) {
        weights=model.weights();
        labelIdx=model.labelIndex();
        featureIdx=model.featureIndex();
        stanfordModel = model;
    }    
    
    public void setWeights(double[][] weightss){
        this.weights=weightss;
    }
    public void updateWeights(double[][] weightss){
        for(int l=0; l< weightss.length;l++)
          this.weights[l]=Arrays.copyOf(weightss[l],weightss[l].length);
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
    
    public float getGenScore(int instance, int label){
        return generatedScores[instance][label];
    }
       
    public float getScore(int[] features, int label) {
        float sumWeightsOf1Features = 0;
        for (int j=0;j<features.length;j++) {
            sumWeightsOf1Features += weights[features[j]][label];
        }
        return sumWeightsOf1Features;
    }
    
    public void generateRandomScore(int ninst){
        /*double[] scores0= new double[ninst];
        double[] scores1= new double[ninst];
        Arrays.fill(scores0, 0.0);
        Arrays.fill(scores1, 0.0);*/
        float[][] genScores= new float[ninst][2];
        NormalDistribution distr0 = new NormalDistribution(8, 1);
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
    
    public int[] getTopWeights(){
        int[] featIndexes = new int[50];
        List<Triple<String,String,Double>> topFeatures = stanfordModel.getTopFeatures(0.5, true, 50);
        int i=0;
        for(Triple obj:topFeatures){
            System.out.println(obj.first.toString());
            featIndexes[i] =featureIdx.indexOf(obj.first.toString());
            i++;
            //label
            //System.out.println(obj.second.toString());
            //weight
            //System.out.println(obj.third.toString());
            
        }
        return featIndexes;
    }
}
