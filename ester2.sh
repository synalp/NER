#!/bin/bash

JCP="bin:../../git/jsafran/jsafran.jar:../../softs/mallet-2.0.5/dist/mallet.jar:../../softs/mallet-2.0.5/dist/mallet-deps.jar:../../softs/mallet-2.0.5/lib/trove-2.0.2.jar"

allens="pers fonc org loc prod time amount"
dest2="/home/didiot/NER_Xtof/ESTER2ftp/package_scoring_ESTER2-v1.7/information_extraction_task"
export PATH=$PATH:$dest2/tools

if [ "0" == "1" ]; then
echo "conversion du train en .xml"
mkdir train
for i in /home/xtof/corpus/ESTER2ftp/EN/train/trs_train_EN_v1.1/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,trs$,,g' | awk '{print $NF}'`"xml"
  echo "convert train to xml "$i" "$j
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.ESTER2EN -trs2xml tmp.trsl
  mv output.xml oo2.xml
  java -cp "$JCP" jsafran.ponctuation.UttSegmenter oo2.xml
  mv -f output.xml /tmp/
  pushd .
  cd ../../git/jsafran
  java -cp "$JCP" jsafran.JSafran -retag /tmp/output.xml
  mv -f output_treetagged.xml /tmp/
  popd
  mv /tmp/output_treetagged.xml train/$j
done
fi

if [ "1" == "0" ]; then
echo "parsing du train et du test"
cp -f ../../git/jsafran/mate.mods.FTBfull ./mate.mods
mkdir train
for i in train/*.xml
do
  java -cp "$JCP" jsafran.MateParser -parse $i
  mv output.xml $i
done
ls test/*.xml | grep -v -e merged > test.xmll
for i in `cat test.xmll`
do
  java -cp "$JCP" jsafran.MateParser -parse $i
  mv output.xml $i
done
fi

if [ "1" == "0" ]; then
echo "create training files for CRF"
ls train/*_mate.xml > tmp.xmll
echo "no syntax"
ls train/*.xml > tmp.xmll
for i in pers fonc org loc prod time amount
do
  echo $i
  # merge toutes les ENs qui commencent par $i en un seul fichier groups.$i.tab
  # laisse le champs syntaxique vide
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER tmp.xmll $i
  cp -f groups.$i.tab groups.$i.tab.train
done
fi

if [ "0" == "0" ]; then
echo "train CRF"
for en in pers fonc org loc prod time amount unk
do
  sed 's,trainFile=synfeats0.tab,trainFile=groups.'$en'.tab.train,g' syn.props > tmp.props
  java -Xmx20g -cp ../../softs/stanfordNER/stanford-ner-2013-11-12/stanford-ner-2013-11-12.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods
done
fi

###############################################################
if [ "1" == "0" ]; then
echo "create the graphs.xml files from the gold test TRS"
rm -rf test
mkdir test
touch test/trs2xml.list
for i in /home/xtof/corpus/ESTER2ftp/EN/test/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,\.trs$,,g' | awk '{print $NF}'`
  echo $i" "$j".xml"
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.ESTER2EN -trs2xml tmp.trsl
  grep -v -e '^<group> ' output.xml > oo2.xml
  java -cp "$JCP" jsafran.ponctuation.UttSegmenter oo2.xml
  java -cp "$JCP" jsafran.JSafran -retag output.xml
  mv output_treetagged.xml test/$j".xml"
  echo $i" test/"$j".xml" >> test/trs2xml.list
done
fi

if [ "0" == "0" ]; then
echo "create the TAB files from the groups in the graphs.xml files"
ls test/*.xml | grep -v -e merged > tmp.xmll
for i in $allens
do
  echo $i
  # merge toutes les ENs qui commencent par $i en un seul fichier groups.$i.tab
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER tmp.xmll $i
  cp -f groups.$i.tab groups.$i.tab.test
done
fi
exit

if [ "1" == "0" ]; then
for en in $allens
do
  echo "test the CRF for $en"
  java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile groups.$en.tab > test.$en.log
done
fi

# eval chaque EN individuellement
if [ "1" == "0" ]; then
echo "evals individuelles baseline" >> res.log
for en in $allens
do
  ./conlleval.pl -d '\t' -o NO < test.$en.log | grep $en >> res.log
done
fi

# merge les res dans un seul stmne
if [ "1" == "0" ]; then
echo "put all CRF outputs into a single xml file"
ls test/*.xml | grep -v -e merged > tmp.xmll
java -cp "$JCP" ester2.ESTER2EN -mergeens tmp.xmll $allens
echo "convert the graph.xml into a .stm-ne file"
nl=`wc -l test/trs2xml.list | cut -d' ' -f1`
for (( c=1; c<=$nl; c++ ))
do
  trs=`awk '{if (NR=='$c') print $1}' test/trs2xml.list`
  grs=`awk '{if (NR=='$c') print $2}' test/trs2xml.list | sed 's,\.xml,.xml.merged.xml,g'`
  out=`echo $grs | sed 's,\.xml\.merged\.xml,,g'`".stm-ne"
  echo "build stmne from $trs $grs $out"
  java -Xmx1g -cp "$JCP" ester2.STMNEParser -project2stmne $grs $trs $out
done
fi

# eval selon protocole ESTER2
if [ "0" == "0" ]; then
score-ne -rd $dest2/../../EN/test/ -cfg $dest2/example/ref/NE-ESTER2.cfg -dic $dest2/tools/ESTER1-dictionnary-v1.9.1.dic test/*.stm-ne
fi

