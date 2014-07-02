/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lex;

import tools.CNConstants;


/**
 * This class mainly stores all the possible lemmas that could be present
 * in a sentence, this class has been created for loading
 * the values from the database. This corresponds to the basic table:
 * lemma, which defines the same feature in the vector space
 * that will be used by the semantic role labeling classifier.
 *
 * @author rojasbar
* <!--
* Modifications History
* Date      	  Author   	Description
* Mar 11, 2011          rojasbar       Creation-
*/

public class Lemma implements Cloneable{

    
    private Long    id;
    private String  name;

    public Lemma(){
                
    }

    public void setId(Long id){
        this.id = id;
    }
    
    public Long getId(){
        return this.id;
    }

    public void setName(String lemma){
       
            String notEmptyValue = lemma.length() == 0 ? CNConstants.CHAR_NULL : lemma;
            this.name = notEmptyValue;
        
    }


    public String getName(){
        
      return name.contains("|")?name.substring(0, name.indexOf("|")):name;
        
    }



    //toString
    @Override
    public String toString(){
        return this.name;
    }

    @Override
    public Object clone() {
        try {
            return (Lemma)super.clone();
        } catch (CloneNotSupportedException ex) {
            return this;
        }
        

    }  
    
}
