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
LARGECORP=../../git/jsafran/c0b.conll
TRAIN=../../git6/peps/corpus/etape/radios.xml
TEST=../../git6/peps/corpus/etape/devtvs.xml

echo "save enO, enO.contextes et voc0"
java -cp "$JCP" PrepHDB -save4HBC $LARGECORP $TRAIN $TEST
exit

gcc -g stats.c samplib.c en2.c -o en2.exe -lm
./en2.exe

