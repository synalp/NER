/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package resources;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This class mainly stores all the possible categories in wikipedia, 
 * this class has been created for loading
 * the values from the database. This corresponds to the basic table:
 * Page.
 *
 * @author rojasbar
* <!--
* Modifications History
* Date      	  Author   	Description
* Mar 18, 2014  rojasbar     Creation-
*/

@Entity
@Table(name = "Category",
		uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, insertable = false, updatable = false)
    private Long    id;
    @Column(name = "name")
    private String  name; 

    
    public Category(){
        
    }
    
    public void setId(Long id){
        this.id = id;
    }
    
    public Long getId(){
        return this.id;
    }

    public void setName(String title){
       
            //String notEmptyValue = lemma.length() == 0 ? ESConstants.CHAR_NULL : lemma;
            this.name = title;
        
    }    
    
    
    public String getName(){
        return this.name;
    }



    
    //toString
    @Override
    public String toString(){
        return this.name;
    }    
}
