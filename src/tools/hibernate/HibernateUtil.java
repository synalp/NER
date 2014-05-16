package tools.hibernate;



import resources.Category;
import tools.CNConstants;

import org.hibernate.cfg.Configuration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;


import org.hibernate.cfg.AnnotationConfiguration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.hibernate.encryptor.HibernatePBEEncryptorRegistry;
import resources.Page;



/**
 *  Utilities from hibernate
 *
 * @version 1.0
 * @author rojasbar
 *  @2011/03/10
 * Adapted from Allegro project
 *  @author laura perez
 * 
 * <!-- Modifications History 
 * Date    Description 
 * 
 * -->
 */

public class HibernateUtil {

     private static SessionFactory factory;

    /*Declares, initializes, configures and subsequently returns the ever
     * so useful and important Hibernate Configuration object, of which
     * AnnotationConfiguration is a subclass.
     */
    public static Configuration getInitializedConfiguration() {
              AnnotationConfiguration config = new AnnotationConfiguration();
              /* add all of your JPA annotated classes here!!!*/
              config.addAnnotatedClass(Page.class);
              config.addAnnotatedClass(Category.class);


              
              config.configure(CNConstants.HIBERNATE_CONFIG_FILE);
              return config;
    }
	
	
    public static Session getSession() {
        StandardPBEStringEncryptor strongEncryptor = new StandardPBEStringEncryptor();
        HibernatePBEEncryptorRegistry registry = HibernatePBEEncryptorRegistry.getInstance();
        strongEncryptor.setPassword("ESEncryptor");
        registry.registerPBEStringEncryptor("hibernateEncryptor", strongEncryptor);
        if (factory == null) {
            Configuration config =
          HibernateUtil.getInitializedConfiguration();
            try{
            factory = config.buildSessionFactory();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        Session hibernateSession =
                       factory.getCurrentSession();
        return hibernateSession;
    }

    public static void closeSession() {
        
        HibernateUtil.getSession().close();
    }


/* This light be helpful but is a dangerous method		 
 		public static void recreateDatabase() {
		   Configuration config;
		   config = 
		       HibernateUtil.getInitializedConfiguration();
		   new SchemaExport(config).create(true, true);
		 }
*/
		 
     public static Session beginTransaction() {
       Session hibernateSession;
       hibernateSession = HibernateUtil.getSession();
       hibernateSession.beginTransaction();
       
       return hibernateSession;
     }

     public static void commitTransaction() {
       HibernateUtil.getSession()
                    .getTransaction().commit();
     }

     public static void rollbackTransaction() {
       HibernateUtil.getSession()
                     .getTransaction().rollback();
     }

     public static SessionFactory getSessionFactory(){
         return factory;
     }

      public static void main(String args[]) {
      
          HibernateUtil.beginTransaction();
      }


}
