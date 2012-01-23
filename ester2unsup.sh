#!/bin/bash

# realise un apprentissage non supervise; base sur en2.hier et en2.c

# objectif=faire ceci sur le gigaword + ESTER2 train + ESTER2 test
# en forcant les classes E seulement sur la partie ESTER2 train
# puis en recuperant les samples de E sur la partie ESTER2 test
# reste a moyenner ces samples sur ESTER2 test et a garder le meilleur comme feature pour le CRF

# en2.c doit etre modifie pour:
# 1- lire les "gold E" sur la partie ESTER2 train (cf. en.c qui fait ca)
# 2- modifier le sampling de H: forcer H a ne prendre des valeurs QUE sur le voisinnage de w

JCP="bin:../utils/bin:../../git/jsafran/jsafran.jar"

if [ "1" == "0" ]; then
echo "save Gigaword as .xml"
for (( i=1; i<341; i++ ))
do
java -cp "$JCP" GigawordIO $i
java -cp "$JCP" jsafran.JSafran -retag c$i.xml
mv -f output.xml c$i.xml
done
fi

if [ "0" == "0" ]; then
echo "unsup clustering"
echo "c0b.conll" > unlab.xmll
ls train/*.xml > train.xmll
ls test/*.xml | grep -v -e merged > test.xmll
java -cp "$JCP" ester2.Unsup -creeObs test.xmll unlab.xmll train.xmll test.xmll > creeobs.log
exit
gcc -g stats.c samplib.c en2.c -o en2.exe -lm
./en2.exe | tee en.log
java -cp "$JCP" ester2.Unsup -analyse en.log > an.log
fi

if [ "1" == "0" ]; then
echo "create TAB files for CRF training"
for i in pers fonc org loc prod time amount unk
do
  echo $i
  java -Xmx1g -cp "$JCP" ester2.ESTER2EN -saveNER train.xmll $i
  cp -f groups.$i.tab groups.$i.tab.train
done
fi

if [ "0" == "0" ]; then
echo "Insert in the CRF TAB files the (syntactic) class obtained from unsup clustering"
grep indexobsfile creeobs.log | head -2 > /tmp/yy
debtrain=`head -1 /tmp/yy | cut -d' ' -f2`
debtest=`tail -1 /tmp/yy | cut -d' ' -f2`
for i in pers fonc org loc prod time amount unk
do
  echo $i
  java -cp "$JCP" ester2.Unsup -inserttab groups.$i.tab.train en.log $debtrain $debtest train.xmll
exit
done
fi

