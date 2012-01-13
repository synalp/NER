#!/bin/bash

JCP="bin:../../git/jsafran/jsafran.jar:../../softs/mallet-2.0.5/dist/mallet.jar:../../softs/mallet-2.0.5/dist/mallet-deps.jar:../../softs/mallet-2.0.5/lib/trove-2.0.2.jar"

allens="pers fonc org loc prod time amount unk"
dest2="/home/xtof/corpus/ESTER2ftp/package_scoring_ESTER2-v1.7/information_extraction_task"
export PATH=$PATH:$dest2/tools

if [ "1" == "0" ]; then
mkdir train
for i in /home/xtof/corpus/ESTER2ftp/EN/train/trs_train_EN_v1.1/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,trs$,,g' | awk '{print $NF}'`"xml"
  echo $i" "$j
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.STMNEParser tmp.trsl
  mv output.xml train/$j
done
fi

if [ "1" == "0" ]; then
ls train/*.xml > tmp.xmll
for i in pers fonc org loc prod time amount unk
do
  echo $i
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER tmp.xmll $i
  cp -f groups.$i.tab groups.$i.tab.train
done
fi

if [ "1" == "0" ]; then
for en in pers fonc org loc prod time amount unk
do
  sed 's,trainFile=synfeats0.tab,trainFile=groups.'$en'.tab,g' syn.props > tmp.props
  java -Xmx20g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods
done
fi

###############################################################
if [ "1" == "0" ]; then
rm -rf test
mkdir test
for i in /home/xtof/corpus/ESTER2ftp/EN/test/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,\.trs$,,g' | awk '{print $NF}'`
  echo $i" "$j".xml"
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.STMNEParser tmp.trsl
  mv yy.stm-ne test/$j.stm-ne
  ici=`pwd`
  sed 's,'$ici'/yy.stm-ne,'$ici'/test/'$j'.stm-ne,g' output.xml > test/$j".xml"
done
fi

if [ "1" == "0" ]; then
ls test/*.xml | grep -v -e merged > tmp.xmll
for i in $allens
do
  echo $i
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER tmp.xmll $i
done
fi

if [ "1" == "0" ]; then
for en in $allens
do
  java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile groups.$en.tab > test.$en.log
done
fi

# eval chaque EN individuellement
echo "evals individuelles baseline" >> res.log
if [ "1" == "0" ]; then
for en in $allens
do
  ./conlleval.pl -d '\t' -o NO < test.$en.log | grep $en >> res.log
done
fi

# merge les res dans un seul stmne
if [ "0" == "0" ]; then
ls test/*.xml | grep -v -e merged > tmp.xmll
java -Xmx1g -cp "$JCP" ester2.ESTER2EN -mergeens tmp.xmll $allens
fi

# eval selon protocole ESTER2
if [ "0" == "0" ]; then
for i in test/*.xml.stm-ne
do
  j=`echo $i | sed 's,\.xml\.stm-ne,.stm-ne,g'`
  echo "renaming $i into $j"
  mv -f $i $j
done
score-ne -rd $dest2/../../EN/test/ -cfg $dest2/example/ref/NE-ESTER2.cfg -dic $dest2/tools/ESTER1-dictionnary-v1.9.1.dic test/*.stm-ne
fi

