/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package resources;



import de.tudarmstadt.ukp.wikipedia.api.Category;
import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.Title;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiInitializationException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiPageNotFoundException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;

import tools.CNConstants;
import tools.hibernate.HibernateUtil;

/**
 * This method calls the JWPL API
 * for querying a mysql database with
 * one static version of wikipedia
 * 
 * TODO: Update the database automatically with a most
 * recent version of the documents
 * 
 * @author rojasbar
 */
public class WikipediaAPI implements WikiConstants {
    public static int NSIMILARPAGES=10;
    // configure the database connection parameters
    static DatabaseConfiguration dbConfig = new DatabaseConfiguration();
    static Wikipedia wiki = null;
    static HashMap<String,resources.Page> allPages = new HashMap<>();
    static String GAZSPATH="gazettes";
    static String PERSFILE="personGazette";
    static String ORGFILE="orgGazette";
    static String LOCFILE="locGazette";
    static String PRODFILE="prodGazette";
    static int BUFFERSIZE=50000;
    
    public static void setConfiguration(){
        try {
            dbConfig.setHost("localhost");
            dbConfig.setDatabase("wikidb");
            dbConfig.setUser("contnomina");
            dbConfig.setPassword("contnomina");
            dbConfig.setLanguage(Language.french);    
            
            // Create a new French wikipedia.
            wiki = new Wikipedia(dbConfig);
        } catch (WikiInitializationException ex) {
            ex.printStackTrace();
        }
    }
    /*
    public static void queryingByCategory(String query) {
        try {
            setConfiguration();

            

            // Get the category "Säugetiere" (mammals)
            String title = query ;
            Category cat;
            cat = wiki.getCategory(title);
                
           

            StringBuilder sb = new StringBuilder();

            // the title of the category
            sb.append("Title : " + cat.getTitle() + LF);
            sb.append(LF);

            // the number of links pointing to this page (number of superordinate categories)
            sb.append("# super categories : " + cat.getParents().size() + LF);
            for (Category parent : cat.getParents()) {
                sb.append("  " + parent.getTitle() + LF);
            }
            sb.append(LF);
           
            // the number of links in this page pointing to other pages (number of subordinate categories)
            sb.append("# sub categories : " + cat.getChildren().size() + LF);
            for (Category child : cat.getChildren()) {
                sb.append("  " + child.getTitle() + LF);
            }
            sb.append(LF);

            // the number of pages that are categorized under this category
            sb.append("# pages : " + cat.getArticles().size() + LF);
            for (Page page : cat.getArticles()) {
                sb.append("  " + page.getTitle() + LF);
            }
           
            // extract only the pageIDs of pages that are categorized under this category  
            sb.append("# pageIDs : " + cat.getArticleIds().size() + LF);
            for (int pageID : cat.getArticleIds()) {
                sb.append("  " + pageID + LF);
            }
           
            System.out.println(sb);
        }  catch (WikiPageNotFoundException e) {
                System.out.println("Category " + query + " does not exist");
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }*/
    
    public static resources.Category queryingByCategory(String topic) {
        resources.Category cat=null;
        Session session = HibernateUtil.beginTransaction();
        SQLQuery query = session.createSQLQuery("select c.* from Category c where lower(c.name) = lower(:subject) ");
        query.setString("subject", topic);    
        List result = query.addEntity("c", resources.Category.class).list();   
        if(result.isEmpty())
            return null;

        cat = (resources.Category) result.get(0);    
        session.close();
        return cat;
    }
 
       public static List<resources.Category> getSuperCategory(String topic) {
        List<resources.Category> cats= new ArrayList<>();
        Session session = HibernateUtil.beginTransaction();
        SQLQuery query = session.createSQLQuery("select c.* from Category c where c.id in (select ci.id from Category c2, category_outlinks ci where ci.outLinks=c2.pageId and lower(c2.name)=lower(:subject))");
        query.setString("subject", topic);    
        List result = query.addEntity("c", resources.Category.class).list();   

        for(int i=0; i< result.size();i++){
            resources.Category cat = (resources.Category) result.get(i);    
            cats.add(cat);
        }
        
        session.close();
        return cats;
    } 
    
    public static String queryingByPage(String topic) {
        resources.Page page=null;
        
        try {
            Title toptitle=new Title(topic);
            if(allPages.isEmpty()){
                System.out.println("Looking at the database");
                Session session = HibernateUtil.beginTransaction();
                SQLQuery query = session.createSQLQuery("select p.* from Page p where lower(p.name) = lower(:subject) ");
                query.setString("subject", toptitle.getWikiStyleTitle());    
                List result = query.addEntity("p", resources.Page.class).list();   
                if(result.isEmpty())
                    return "";

                page = (resources.Page) result.get(0);    
                allPages.put(page.getName(), page);
                session.close();
            }else{
                System.out.println("Looking in memory... "+topic);
                boolean found=false;
                for(String title:allPages.keySet()){
                    if(title.contains(topic)){
                        page= allPages.get(title);
                        found=true;
                        break;
                    }

                }
                if(!found){
										return "";
										/*
                    System.out.println("Not found in memory ... "+topic);
                    Session session = HibernateUtil.beginTransaction();
                    SQLQuery query = session.createSQLQuery("select p.* from Page p where lower(p.name) = lower(:subject) ");
                    query.setString("subject", toptitle.getWikiStyleTitle());    
                    List result = query.addEntity("p", resources.Page.class).list();   
                    if(result.isEmpty())
                        return "";

                    page = (resources.Page) result.get(0);    
                    allPages.put(page.getName(), page);
                    session.close();
									*/
                }
            }
            
            
            return(page.getText());

        } catch (Exception ex) {
           return ""; 
        }
    }
    
    public static void getSimilarPages(String topic){
        try{
            setConfiguration();
            Map<Page, Double> ranking = wiki.getSimilarPages(topic, NSIMILARPAGES);
            
            for(Page page:ranking.keySet()){
                System.out.println(page.getText());
            }
        }catch (Exception ex){
            
        }
    }
    
    public static void loadWiki(){
        
        Integer lowerBound=1;
        Integer upperBound=BUFFERSIZE;
        try{
        for(int i=0; i<300; i++){
            System.out.println("processingPages Iteration: "+ i+" lowerBound :"+ lowerBound + " upperBound : "+ upperBound);
            HashMap<String,resources.Page> tmpMap= new HashMap<>();
            Session session = HibernateUtil.beginTransaction();
            session.beginTransaction(); 
            try{


                Query query = session.createQuery("from Page p where id >= :lower and id <= :upper");
                query.setInteger("lower", lowerBound);    
                query.setInteger("upper", upperBound);           
                //List returnList = query.addEntity("p", de.tudarmstadt.ukp.wikipedia.api.hibernate.Page.class).list();    

                ScrollableResults scrollableResults = query.setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
                while (scrollableResults.next()) {       
                    resources.Page page = (resources.Page) scrollableResults.get()[0];

                    Title title= new Title(page.getName());
                    tmpMap.put(title.getPlainTitle(), page);
                }
                session.close();
                
            }catch(Exception ex){
                session.close();
                ex.printStackTrace();
            }

            allPages.putAll(tmpMap);
            System.out.println("...."+allPages.size()+" pages loaded");
            lowerBound+=BUFFERSIZE;
            upperBound+=BUFFERSIZE;
        }
        }catch(Exception ex){
             
                ex.printStackTrace();
            }
    }
    
    public static String processPage(String topic){
        
        String result=queryingByPage(topic);
        String entity=CNConstants.CHAR_NULL;  
        
        Pattern p = Pattern.compile("==\\s*Nom commun\\s*==");
        Matcher m = p.matcher(result);
        if(m.find())
            entity= CNConstants.CHAR_NULL;         
        
        p = Pattern.compile("(==\\s*Biographie\\s*==)|(est un (\\[\\[)?(prénom|nom propre|(\\[\\[)?patronyme(\\]\\])*)(\\]\\])?)|(==\\s*Patronyme\\s*==)|(==\\s*Personnalités)|((\\{\\{)?nom de famille)");    
        m = p.matcher(result);
        if(m.find())
            entity= CNConstants.PERS;
        //Are all sigles consider as proper noun?
        p = Pattern.compile("(\\{\\{)?Sigle");
        Pattern p2 = Pattern.compile("pronom\\s.*\\s?français");
        m = p.matcher(result);
        Matcher m2 = p2.matcher(result);
        if(m.find() && !m2.find())
            entity = CNConstants.PRNOUN;
        
        p = Pattern.compile("\\| fabricant");
        m = p.matcher(result);
        if(m.find())
            entity=CNConstants.PROD;
        
        p = Pattern.compile("==[=]?\\s*Géographie");
        m = p.matcher(result);
        if(m.find())
            entity=CNConstants.LOC;
        if(result.contains("Société")&& !result.contains("est un [[document]]"))
            entity=CNConstants.ORG;
        try {
            List<resources.Category> cats = getSuperCategory(topic);
            
            for (resources.Category parent : cats) {
                Title title = new Title(parent.getName());
                if(title.getPlainTitle().contains("Ville"))
                    entity=CNConstants.LOC;
            }
        } catch (WikiApiException ex) {
            
        }
        
        
        return entity;    
        
    }
    
    public static void processingPages(String person, String place, String organization, String product){
     
        try {

            //set of gazettes
            OutputStreamWriter personFile = new OutputStreamWriter(new FileOutputStream(new File(person)), "UTF8");            
            OutputStreamWriter placeFile = new OutputStreamWriter(new FileOutputStream(new File(place)), "UTF8");
            OutputStreamWriter orgFile = new OutputStreamWriter(new FileOutputStream(new File(organization)), "UTF8");
            OutputStreamWriter prodFile = new OutputStreamWriter(new FileOutputStream(new File(product)), "UTF8");

            Integer lowerBound=1;
            Integer upperBound=BUFFERSIZE;
            for(int i=0;i<300;i++){
               Session session = HibernateUtil.beginTransaction();
                session.beginTransaction();     
//                session.flush();
//                session.clear();
                System.out.println("processingPages Iteration: "+ i+" lowerBound :"+ lowerBound + " upperBound : "+ upperBound);
                //SQLQuery query = session.createSQLQuery("select p.* from Page p limit "+lowerBound+"," + upperBound); 
                Query query = session.createQuery("from Page where id >= :lower and id <= :upper");
                
                
                query.setInteger("lower", lowerBound);    
                query.setInteger("upper", upperBound);
                ScrollableResults scrollableResults = query.setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
                
                    
                
                lowerBound+=BUFFERSIZE;
                upperBound+=BUFFERSIZE;
                /*
                List returnList= new ArrayList();
                
                returnList = query.addEntity("p", de.tudarmstadt.ukp.wikipedia.api.hibernate.Page.class).list();    
                if(returnList.isEmpty())
                    break;
                */
                String line="";
                while (scrollableResults.next()) {
                    //de.tudarmstadt.ukp.wikipedia.api.hibernate.Page hpage= (de.tudarmstadt.ukp.wikipedia.api.hibernate.Page) item;
                    resources.Page page = (resources.Page) scrollableResults.get()[0];
                    String text = page.getText();
                    session.evict(page);
                    Title title= new Title(page.getName());
                    String pagTitle=title.getPlainTitle();
                    //Page page= new Page (this, hpage.getId(), hpage) ;
                    //We are not interested in common names
                    Pattern p = Pattern.compile("==\\s*Nom commun\\s*==");
                    Matcher m = p.matcher(text);
                    if(m.find())
                        continue;  
                    //not interested in French pronouns neather
                    p = Pattern.compile("pronom\\s.*\\s?français");
                    m = p.matcher(text);                    
                    if(m.find())
                        continue;  
                    
                    p = Pattern.compile("(==\\s*Biographie\\s*==)|(est un (\\[\\[)?(prénom|nom propre|(\\[\\[)?patronyme(\\]\\])*)(\\]\\])?)|(==\\s*Patronyme\\s*==)|(==\\s*Personnalités)|((\\{\\{)?nom de famille)");    
                    m = p.matcher(text);
                    if(m.find()){
                        
                        
                        if(pagTitle.contains("département")){
                            line="loc\t"+pagTitle+"\n";
                            placeFile.append(line);
                            placeFile.flush();
                            continue;
                        }
                        if(pagTitle.contains("Années"))
                            continue;
                        
                        line="pers\t"+pagTitle+"\n";    
                        //stored in personList       
                        personFile.append(line);
                        personFile.flush();
                        continue;
                    }
                    
                    p = Pattern.compile("\\| fabricant");
                    m = p.matcher(text);
                    if(m.find()){
                        line="prod\t"+pagTitle+"\n";
                        prodFile.append(line);
                        prodFile.flush();
                        continue;
                    }                    
                    
                    p = Pattern.compile("==[=]?\\s*Géographie");
                    m = p.matcher(text);
                    if(m.find()){
                        line="loc\t"+pagTitle+"\n";
                        placeFile.append(line);
                        placeFile.flush();
                        continue;
                    }
                        
                    if(page.getText().contains("Société")&& !page.getText().contains("est un [[document]]")){
                        line="org\t"+pagTitle+"\n";
                        orgFile.append(line);
                        orgFile.flush();
                        continue;
                    }
                        
                    p = Pattern.compile("(\\{\\{)?Sigle");
                    m = p.matcher(text);
                    if(m.find()){                   
                        line="org\t"+pagTitle+"\n";
                        orgFile.append(line);
                        orgFile.flush();
                        continue;
                    }
             
                }
                session.close();
            }
            } catch (Exception ex) {
                
                ex.printStackTrace();
                //Logger.getLogger(Wikipedia.class.getName()).log(Level.SEVERE, null, ex);
            }
       
        
        return ;
    }

    
    
    public static void creatingGazetters(){
        
        String sep=System.getProperty("file.separator");
        processingPages(GAZSPATH+sep+PERSFILE, GAZSPATH+sep+LOCFILE, GAZSPATH+sep+ORGFILE, GAZSPATH+sep+PRODFILE);
    }
    
    public static void main(String[] args){
        /*
        WikipediaAPI.queryingByCategory("Paris");
        WikipediaAPI.queryingByCategory("François Hollande");*/
        //WikipediaAPI.queryingByCategory("Paris");
        //System.out.println(WikipediaAPI.queryingByPage("Paris"));
        ///*
        //WikipediaAPI.loadWiki();
        
        //System.out.println(WikipediaAPI.queryingByPage("police"));
        //System.out.println(WikipediaAPI.queryingByPage("Lyon"));
        //System.out.println(WikipediaAPI.queryingByPage("Cali"));
        //System.out.println(WikipediaAPI.queryingByPage("Arsen_Avakov"));
        //stem.out.println(WikipediaAPI.queryingByPage("journal"));
        //System.out.println(WikipediaAPI.queryingByPage("Rojas"));
        //wikipediaAPI.getSimilarPages("Paris");
        //System.out.println(WikipediaAPI.queryingByPage("Jean-Marc_Ayrault"));
        ///*
        System.out.println("Jean-Marc_Ayrault "+WikipediaAPI.processPage("Jean-Marc_Ayrault"));
        System.out.println(WikipediaAPI.processPage("Arsen_Avakov"));
        System.out.println(WikipediaAPI.processPage("iPhone"));
        System.out.println(WikipediaAPI.processPage("France Télécom"));
        System.out.println(WikipediaAPI.processPage("Paris"));
        System.out.println(WikipediaAPI.processPage("Cali"));
        //*/
        //System.out.println("demi: "+WikipediaAPI.processPage("police"));
        //*/
        /*
        String result=WikipediaAPI.queryingByPage("iPhone");
        System.out.println(result);
        //Pattern p = Pattern.compile("(==\\s*Biographie\\s*==)|(est un (\\[\\[)?(prénom|nom propre)(\\]\\])?)|(==\\s*Patronyme\\s*==)");
        Pattern p=Pattern.compile("\\| fabricant");
        Matcher m = p.matcher(result);
        if(m.find())
            System.out.println("Match");
        else
            System.out.println("Don't Match");
        */
        //wikipediaAPI.queryingByCategory("Manuel Valls");
        //WikipediaAPI.creatingGazetters();
        //WikipediaAPI.loadWiki();
        
    }
}
