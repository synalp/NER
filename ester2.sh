#!/bin/bash

JCP="bin:../../git/jsafran/jsafran.jar:../../softs/mallet-2.0.5/dist/mallet.jar:../../softs/mallet-2.0.5/dist/mallet-deps.jar:../../softs/mallet-2.0.5/lib/trove-2.0.2.jar"

if [ "0" == "0" ]; then
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

if [ "0" == "0" ]; then
ls train/*.xml > tmp.xmll
for i in pers fonc org loc prod time amount unk
do
  echo $i
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER tmp.xmll $i
done
fi

if [ "0" == "0" ]; then
for en in pers fonc org loc prod time amount unk
do
  sed 's,trainFile=synfeats0.tab,trainFile=groups.'$en'.tab,g' syn.props > tmp.props
  java -Xmx20g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods
done
fi

###############################################################
if [ "0" == "0" ]; then
mkdir test
for i in /home/xtof/corpus/ESTER2ftp/EN/test/*.trs
do
  j=`echo $i | sed 's,/, ,g;s,trs$,,g' | awk '{print $NF}'`"xml"
  echo $i" "$j
  echo $i > tmp.trsl
  java -cp "$JCP" ester2.STMNEParser tmp.trsl
  mv yy.stm-ne test/$j.stm-ne
  ici=`pwd`
  sed 's,'$ici'/yy.stm-ne,'$ici'/test/'$j'.stm-ne,g' output.xml > test/$j
done
fi

if [ "0" == "0" ]; then
ls test/*.xml > tmp.xmll
for i in pers fonc org loc prod time amount unk
do
  echo $i
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER tmp.xmll $i
done
fi

if [ "0" == "0" ]; then
for en in pers fonc org loc prod time amount unk
do
  java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile groups.$en.tab > test.$en.log
done
fi

# TODO: merge les res dans un seul stmne

