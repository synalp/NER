/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import conll03.CoNLL03Ner;
import tools.CNConstants;

/**
 *
 * @author rojasbar
 */
public class TestingConllExperiments {
    
    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("two arguments are expected : w train_size ");
        }
        try{
            Integer.parseInt(args[2]);
        }catch(Exception ex){
            System.out.println("The second argument is the size of a subset of the trainset");
        }
        switch(args[0]){
            case "w":
                CoNLL03Ner conll = new CoNLL03Ner();
                conll.testingNewWeightsLC(CNConstants.PRNOUN, true, Integer.parseInt(args[2]) );
        }
    }
}
