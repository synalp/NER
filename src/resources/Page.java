/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package resources;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This class mainly stores all the possible pages in wikipedia, 
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
@Table(name = "Page",
		uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, insertable = false, updatable = false)
    private Long    id;
    @Column(name = "name")
    private String  name; 
    @Column(name = "text")
    private String text;
    
    public Page(){
        
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
    
    public void setText(String pageContent){
        this.text = pageContent;
    }
    
    public String getText(){
        return this.text;
    }
    
    //toString
    @Override
    public String toString(){
        return this.name;
    }    
}
