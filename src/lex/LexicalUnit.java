/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lex;



import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.CNConstants;

/**
 * This class mainly stores all the possible lexicalUnits that could be present
 * in a sentence, this class has been created for loading
 * the values from the database. This corresponds to the basic table:
 * lexicalUnit, which defines the same feature in the vector space
 * that will be used by the semantic role labeling classifier.
 *
 * @author rojasbar
* <!--
* Modifications History
* Date      	  Author   	Description
* Apr 9, 2010          rojasbar       Creation-
 * -->
*/

public class LexicalUnit implements Cloneable{


    private Long    id;
    private String  name;
    private Lemma   lemma;
    private String  pattern;
    
    public LexicalUnit(){
        
    }

   public LexicalUnit(String lexicalUnit){
       
        this.lemma = new Lemma();
        this.lemma.setName(CNConstants.CHAR_NULL);
        this.name = lexicalUnit;
        setPattern(); 
        
   }

    public LexicalUnit(Lemma lemma, String lexicalUnit){
        this.lemma = lemma;
        this.name = lexicalUnit;
       setPattern();
    }
    
    private void setPattern(){
        String wordshape="";
        char[] chars = new char[name.length()];
        name.getChars(0, name.length(), chars, 0);
        for(int i=0; i<chars.length;i++){
            if(Character.isDigit(chars[i]))
                wordshape+="d";
            else if(Character.isUpperCase(chars[i]))
                wordshape+="X";
            else if(Character.isLowerCase(chars[i]))
                wordshape+="x";
            else //probably symbol or punctuation
                wordshape+="S";
            
           
       }
       this.pattern=wordshape;
    }
    
    public void setId(Long id){
        this.id = id;
    }

    public Long getId(){
        return this.id;
    }
    public void setLemma(Lemma lemma){
        this.lemma = lemma;
    }
    public void setLemma(String lemmaName) {
        
        this.lemma = new Lemma();
        this.lemma.setName(lemmaName);
        
    }

    public void setName(String lexicalUnit){

            String notEmptyValue = lexicalUnit.length() == 0 ? CNConstants.CHAR_NULL : lexicalUnit;
            //solving encoding problems
            this.name = notEmptyValue;
            setPattern();
    }

    public String getName(){
        return this.name;
    }


    public Lemma getLemma(){
        return this.lemma;
    }

   public String getPattern(){
       return this.pattern;
   }    
    
    //toString
    @Override
    public String toString(){
        return this.name;
    }

    public String getCleanLexicalUnit(){

       String cleanLu = name;

       if(cleanLu.startsWith("*"))
           cleanLu = cleanLu.substring(1, cleanLu.length());

       if(cleanLu.startsWith("'"))
           cleanLu = cleanLu.substring(1, cleanLu.length());

       cleanLu = cleanLu.replace("(", "");
       cleanLu = cleanLu.replace(")", "");



       return cleanLu;

   }
   @Override
    public Object clone() {
        try {
            LexicalUnit lu = (LexicalUnit)super.clone();
            lu.setLemma((Lemma)lemma.clone());
            return lu;
        } catch (CloneNotSupportedException ex) {
            return this;
        }
        

    }  

}
