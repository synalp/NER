/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *This class encapsulates the head and dependents words
 * @author rojasbar
 * <!--
 * Modifications History
 * Date      	  Author   	Description
 * Apr 8, 2010    rojasbar  	Creation
 * -->
 */
public class Dependency implements Serializable{
    private static final long serialVersionUID = 1L; 
    private Word head;
    private HashMap<Integer,Word> dependents= new HashMap<>();
    private HashMap<Integer, String> relations= new HashMap<>();

    //field for the db mapping
    private Long utteranceId;

    
    public Dependency(Word head){
        this.head = head;
        this.dependents = new HashMap<>();
        this.relations  = new HashMap<>();
        this.utteranceId = head.getUtterance().getId();

    }

    public void addDependent(Word dependent, String relation){
        this.dependents.put(dependent.getPosition(), dependent);
        this.relations.put(dependent.getPosition(), relation);
    }

    public Word getHead(){
        return this.head;
    }

    public HashMap<Integer, String> getRelations(){
        return this.relations;
    }

    public boolean containsRelation(String rel){
        for(String relation:relations.values()){
            if(relation.equals(rel))
                return true;
        }
        return false;
    }

    public List<Word> getDependantsUnderRelation(String rel){
        List<Word> depsUnderRel = new ArrayList<Word>();

        for(Integer key:relations.keySet()){
            if(relations.get(key).equals(rel))
                depsUnderRel.add(dependents.get(key));
        }
        return depsUnderRel;
    }

    public String getRelation(Integer index){
        return this.relations.get(index);
    }

    public List<Word> getAllDependents(){
        List<Word> deps = new ArrayList(this.dependents.values());
               
        return deps;
    }

    public List<Word> getLeftDependents(){
        List<Word> deps = new ArrayList();
        List<Integer> keys = new ArrayList(this.dependents.keySet());
        Collections.sort(keys);
        for(Integer key:keys){
            if(key > head.getPosition())
                break;

            deps.add(this.dependents.get(key));
        }

        return deps;

    }

    public List<Word> getRightDependents(){
        List<Word> deps = new ArrayList();
        List<Integer> keys = new ArrayList(this.dependents.keySet());
        Collections.sort(keys);
        for(int i= keys.size()-1; i>=0; i--){
            if(keys.get(i) <= head.getPosition())
                break;
            
            deps.add(0, this.dependents.get( keys.get(i)));
        }

        return deps;

    }

    public boolean containsDependent(Integer id){
        return this.dependents.containsKey(id);
    }

    public Word getDependent(Integer wordId){
       return  this.dependents.get(wordId);
    }

    

    public HashMap<Integer,Word> getDependents(){
        return this.dependents;
    }

    public Integer getNumberOfDependents(){
        return this.dependents.size();
    }

    public Long getUtteranceDBId(){
        return this.utteranceId;
    }

    public boolean isRelationIn(String relation){

        return relations.values().contains(relation);
    }

    /**
     * Returns the keys asociated with the given relation
     * @param relation
     * @return
     */
    public List<Integer> getKeysForRelation(String relation){
        List<Integer> indexes = new ArrayList<Integer>();
        
        for(Integer key:relations.keySet()){
            if(relations.get(key).compareTo(relation)==0){
                indexes.add(key);
            }
        }
        
        return indexes;
    }


}
