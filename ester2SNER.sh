#!/bin/bash

JCP="bin:lib/jsafran.jar:lib/mallet.jar:lib/mallet-deps.jar:lib/trove.jar:lib/org.annolab.tt4j-1.0.12.jar:build/classes::lib/anna.jar"
#allens="pers fonc org loc prod time amount"
allens="pers org loc prod"
#allens="pn"
# debug

dest2="/home/rojasbar/development/contnomina/corpus/ESTER2ftp/package_scoring_ESTER2-v1.7/information_extraction_task"
export PATH=$PATH:$dest2/tools

if [ "1" == "0" ]; then
echo "conversion du train en .xml"
mkdir train
for i in /home/rojasbar/development/contnomina/corpus/ESTER2ftp/EN/train/trs_train_EN_v1.1/*.trs
#for i in /home/rojasbar/development/contnomina/corpus/ESTER2ftp/EN/test/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,trs$,,g' | awk '{print $NF}'`"xml"
  echo "convert train to xml "$i" "$j
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.ESTER2EN -trs2xml tmp.trsl
  mv output.xml oo2.xml
  java -cp "$JCP" jsafran.ponctuation.UttSegmenter oo2.xml
  mv -f output.xml /tmp/
  pushd .
  java -cp "$JCP" jsafran.JSafran -retag /tmp/output.xml
  mv -f output_treetagged.xml /tmp/
  popd
  mv /tmp/output_treetagged.xml train/$j
done
fi

if [ "1" == "0" ]; then
echo "conversion du dev en .xml"
mkdir dev 
for i in /home/rojasbar/development/contnomina/corpus/ESTER2ftp/EN/dev/*.trs
#for i in /home/rojasbar/development/contnomina/corpus/ESTER2ftp/EN/test/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,trs$,,g' | awk '{print $NF}'`"xml"
  echo "convert dev to xml "$i" "$j
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.ESTER2EN -trs2xml tmp.trsl
  mv output.xml oo2.xml
  java -cp "$JCP" jsafran.ponctuation.UttSegmenter oo2.xml
  mv -f output.xml /tmp/
  pushd .
  java -cp "$JCP" jsafran.JSafran -retag /tmp/output.xml
  mv -f output_treetagged.xml /tmp/
  popd
  mv /tmp/output_treetagged.xml dev/$j
done
fi

#NO PARSING
if [ "1" == "0" ]; then
echo "parsing du train et du test"
#cp -f ../jsafran/mate.mods.FTBfull ./mate.mods
cp -f ../jsafran/mate.mods.ETB ./mate.mods
#mkdir train 
for i in `cat esterTrainALL.xmll` 
do
  java -Xmx20g -cp "$JCP" jsafran.MateParser -parse $i
  mv output.xml $i
done
for i in `cat esterTestALL.xmll`
do
  java -Xmx20g -cp "$JCP" jsafran.MateParser -parse $i
  mv output.xml $i
done
fi

if [ "1" == "0" ]; then
echo "create training files for CRF"
#ls parser/*_mate.xml > train.xmll
#echo "no syntax"
ls train/*.xml > train.xmll
for i in $allens 
do
  echo $i
  # merge toutes les ENs qui commencent par $i en un seul fichier groups.$i.tab.crf
  # laisse le champs syntaxique vide
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER train.xmll $i
  cp -f groups.$i.tab.crf groups.$i.tab.crf.train
done
fi

if [ "1" == "0" ]; then
echo "train CRF"
for en in $allens 
#unk
do
  sed 's,trainFile=synfeats0.tab,trainFile=groups.'$en'.tab.crf.train,g' syn.props > tmp.props
  java -Xmx20g -cp ../stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar  edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods
done
fi

###############################################################
if [ "1" == "0" ]; then
echo "create the graphs.xml files from the gold test TRS"
rm -rf test
mkdir test
touch test/trs2xml.list
for i in /home/rojasbar/development/contnomina/corpus/ESTER2ftp/EN/test/*.trs 
do
  j=`echo $i | sed 's,/, ,g;s,\.trs$,,g' | awk '{print $NF}'`
  echo $i" "$j".xml"
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.ESTER2EN -trs2xml tmp.trsl
  grep -v -e '^<group> ' output.xml > oo2.xml
  java -cp "$JCP" jsafran.ponctuation.UttSegmenter oo2.xml
  mv -f output.xml /tmp/
  pushd .
  cd ../jsafran
  java -cp "$JCP" jsafran.JSafran -retag /tmp/output.xml
  mv -f output_treetagged.xml /tmp/
  popd
  mv /tmp/output_treetagged.xml test/$j".xml"
  echo $i" test/"$j".xml" >> test/trs2xml.list
done
fi

if [ "1" == "0" ]; then
echo "conversion du test en .xml, similar to train"
rm -rf test
mkdir test
for i in /home/rojasbar/development/contnomina/corpus/ESTER2ftp/EN/test/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,trs$,,g' | awk '{print $NF}'`"xml"
  echo "convert test to xml "$i" "$j
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.ESTER2EN -trs2xml tmp.trsl
  mv output.xml oo2.xml
  java -cp "$JCP" jsafran.ponctuation.UttSegmenter oo2.xml
  mv -f output.xml /tmp/
  pushd .
  cd ../jsafran
  java -cp "$JCP" jsafran.JSafran -retag /tmp/output.xml
  mv -f output_treetagged.xml /tmp/
  popd
  mv /tmp/output_treetagged.xml test/$j
  echo $i" test/"$j >> test/trs2xml.list
done
fi
if [ "1" == "0" ]; then
echo "create the TAB files from the groups in the graphs.xml files"
ls test/*.xml | grep -v -e merged > test.xmll
for i in $allens
do
  echo $i
  # merge toutes les ENs qui commencent par $i en un seul fichier groups.$i.tab.crf
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER test.xmll $i
  cp -f groups.$i.tab.crf groups.$i.tab.crf.test
done
fi

if [ "1" == "0" ]; then
for en in $allens
do
  echo "test the CRF for $en"
  # I use a compiled version instead of the jar because I put verbose=2 so that to see all samples from Gibbs, and not just the best one
  java -Xmx1g -cp ../stanfordNLP/stanford-ner-2014-01-04/stanford-ner-2014-01-04.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile groups.$en.tab.crf.test > test.$en.log
done
fi
#exit

# eval chaque EN individuellement
if [ "1" == "0" ]; then
for en in $allens
do
  echo "evals individuelles baseline for $en $allens" 
  ./conlleval.pl -d '\t' -o NO < test.$en.log > res.log
  #./conlleval.pl -d '\t' -o NO < test.$en.log | grep $en >> res.log
done
fi

# merge les res dans un seul stmne
if [ "0" == "0" ]; then
echo "put all CRF outputs into a single xml file"
#ls test/*.xml | grep -v -e merged > train.xmll
java -cp "$JCP" ester2.ESTER2EN -mergeens esterTestALL.xmll $allens
echo "convert the graph.xml into a .stm-ne file"
nl=`wc -l test/trs2xml.list | cut -d' ' -f1`
for (( c=1; c<=$nl; c++ )) 
do
  echo " c =  $c "
  trs=`awk '{if (NR=='$c') print $1}' test/trs2xml.list`
  grs=`awk '{if (NR=='$c') print $2}' test/trs2xml.list | sed 's,\.xml,.xml.merged.xml,g'`
  out=`echo $grs | sed 's,\.xml\.merged\.xml,,g'`".stm-ne"
  echo "build stmne from $trs $grs $out"
  java -Xmx1g -cp "$JCP" ester2.STMNEParser -project2stmne $grs $trs $out
done
fi

# eval selon protocole ESTER2
if [ "0" == "0" ]; then
#analysis/spans/score-ne -rd $dest2/../../EN/test/ -cfg analysis/spans/NE-ESTER2.cfg -dic $dest2/tools/ESTER1-dictionnary-v1.9.1.dic test/*.stm-ne
score-ne -rd $dest2/../../EN/test/ -cfg $dest2/example/ref/NE-ESTER2.cfg -dic $dest2/tools/ESTER1-dictionnary-v1.9.1.dic test/*.stm-ne
fi

