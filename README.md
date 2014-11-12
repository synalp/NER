NAME ENTITY RECOGNITION 
-----------------------

This software implements several experiments by using the following toolkits

* Stanford Linear Classifier
* Stanford NER
* SVMLight, tree-kernels
* Label propagation algorithm  JUNTO

In addition it access external ressources, namely a database with the wikipedia pages until Feb 2014

We also implemented a weakly supervised algorithm that is first initialized with the weights given by the stanford linear classifier trained on little data.

Configuration
-------------

etc/ner.properties , this file has the variables necesary to configure the different classifiers

The following packages and classes interface with these utilities
--------
Packages
-----------------------------------------
Interface with Stanford Linear Classifier
-----------------------------------------
* src/linearclassifier
	AnalyzeClassifier.java :
	* interfaces with Stanford Linear Classifier 
	* implements a weakly supervised algorithm based on risk minimisation.
	It can use the closed form for the risk estimation or a numerical approximation to the risk.
	* configuration: you can give to the classifier the name of classifier, for the moment the following types are supported
		* "pers": binary classifier for detecting whether or not one word is a person
		* "org":  binary classifier for detecting whether or not one word is a organization
		* "prod":  binary classifier for detecting whether or not one word is a product 
		* "loc":  binary classifier for detecting whether or not one word is a localization 
        	* "pn": if there is one general classifier that detects a proper name
		* "all": multiclass classifier detecting the categories: person,organization,produt and localization.
						All these constants are setted in the class tools.CNConstans.
		* input: set the static variables LISTTRAINFILES and LISTTESTFILES, which are files containing
                                                the list of files to process, see as examples esterTrain.xmll and esterTest.
						YOU MUST SET THE LIST OF FILES TO TRAIN AND TEST IN THE PROPERTIES FILE: ner.properties
					You can set a flag for using wikipedia as an extrafeature.
					If entity found in wikipedia as person, place,organization or product. (it can take up to 2h)
        Margin.java, store stanford linear classifier weights, features, and instances.

        NumericalIntegration.java, implements the numerical  approximation to the risk
* src/gmm : All classes for the gmm-training

----------------------------------------
Interface with Stanford CRF
----------------------------------------
src/CRFClassifier
	AnalyzeCRFClassifier.java, interfaces with stanford NER
					You can use gazetters, by using file  gazettes/gazette.txt or gazettelcase.txt (all in lowercase)
	Margin.java, class that stores the weights, features and instances of the CRF classifier
        AnalyzeSemiCRF.java, intefaces with a semi-crf implementation
	YOU MUST SET THE LIST OF FILES TO TRAIN AND TEST IN THE PROPERTIES FILE: ner.properties
-----------------------------------------
Interface with SVMLight
-----------------------------------------

* src/svm
       AnalyzeSVMClassifier.java, interfaces to SVMLight, prepares the input and evaluates the output.
			There are several input files, it generates dependency trees for using tree kernel, polynomial kerner or linear kernel, it can even use
			the same features as the Stanford linear classifier.
	YOU MUST SET THE LIST OF FILES TO TRAIN AND TEST IN THE PROPERTIES FILE: ner.properties

* src/lex , necessary classes for storing utterances, words, lexical unix, postags and dependency trees

-----------------------------------------
External Resources
-----------------------------------------

* src/resources
       WikipediaAPI.java, access to wikipedia pages in French all stored in a mysql database, up to feb 2104
       For the database configuration, in a mysql database, create an user "contonmina/contnomina" in localhost, 
       Create the wikipedia database by executing the script in wikipedia/db/dbWikibackupMar32014.sql (11G)  
       YOU MUST SET THE DATABASE SETTING IN THE PROPERTIES FILE:ner.properties and in the hibernate configuration file: src/hibernate.cfg.xml

* src/labelpropagation
	LabelPropagation.java, prepares the input for the JUNTO label propagation toolit and evaluates its output file
       
-----------------------------------------
Using the output of the ASR
-----------------------------------------

* src/reco
	ASROut.java  Alignment of the ASR output, calls the Linera Classifier and CRF
	Capitalization.java CRF for automic capitalizing the output of the ASR
        YOU MUST SET THE DATABASE SETTING IN THE PROPERTIES FILE:ner.properties and in the hibernate configuration file: src/hibernate.cfg.xml

