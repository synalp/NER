/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import conll03.CoNLL03Ner;
import linearclassifier.AnalyzeLClassifier;
import tools.CNConstants;

/**
 *
 * @author rojasbar
 */
public class TestingConllExperiments {
    
    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("two arguments are expected : [w|gw] train_size ");
        }
        try{
            Integer.parseInt(args[1]);
        }catch(Exception ex){
            System.out.println("The second argument is the size of a subset of the trainset");
        }
        CoNLL03Ner conll = new CoNLL03Ner();
        switch(args[0]){
            
            case "w":
                conll.testingNewWeightsLC(CNConstants.PRNOUN, true, Integer.parseInt(args[1]) );
                break;
            case "gw":
                AnalyzeLClassifier.TRAINSIZE=Integer.parseInt(args[1]);
                conll.runningWeaklySupStanfordLC(CNConstants.PRNOUN, true );
                break;                
        }
    }
}
