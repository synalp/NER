#!/bin/bash

JCP="bin:../utils/bin:../../git/jsafran/jsafran.jar:../../softs/mallet-2.0.5/dist/mallet.jar:../../softs/mallet-2.0.5/dist/mallet-deps.jar:../../softs/mallet-2.0.5/lib/trove-2.0.2.jar"

if [ "$TERM" = "cygwin" ]; then
	echo "transform paths..."
	JCP=`echo "$JCP" | sed 's,:,;,g'`
	export PATH="$PATH"":../../tools"
else
	export PATH=$PATH":../../tools"
fi

echo "extrait les deps (NOM,HEAD) du Gigaword + du train + du test"
LARGECORP=../../git/jsafran/c0b.conll
TRAIN=../../git6/peps/corpus/etape/radios.xml
TEST=../../git6/peps/corpus/etape/devtvs.xml
java -Xmx1g -cp "$JCP" PrepHDB -train $LARGECORP $TRAIN $TEST
exit

echo "unsup clustering de E"
# puis je lance ./en.out
# il faut d'abord compiler a la main en.hier dans la machine virtuelle Ubuntu, puis recopier le
# en.c produit dans le rep courant
gcc -g stats.c samplib.c en.c -o en.exe -lm
# ./en.exe > en.log

# on a maintenant directement les samples des mots du train et du test
# mais il ne faut pas conserver un seul sample: chaque sample suit p(E)=P(E|sample,reste), donc
# on estime p(E) a partir des samples: p(E=e) = #(E=e)/#(E=*)
# et on veut l'EN la plus probable: 
# \hat E = argmax_e P(E=e)
echo "construction des fichiers de train et test pour le CRF"

en=pers.ind

java -Xmx1g -cp "$JCP" PrepHDB -putclass ../../git6/peps/corpus/etape/radios.xml en.log 0 $en
sed 's,trainFile=synfeats0.tab,trainFile=groups.'$en'.tab,g' syn.props > tmp.props
java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
mv kiki.mods en.$en.mods

java -Xmx1g -cp "$JCP" PrepHDB -putclass ../../git6/peps/corpus/etape/devtvs.xml en.log 1 pers.ind
java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile groups.$en.tab > test.log
cut -f2 test.log > testgold.log
cut -f3 test.log > testrec.log
sed 's,\(.*\)B$,B-\1,g' testgold.log | sed 's,\(.*\)I$,I-\1,g' > testgold.col
sed 's,\(.*\)B$,B-\1,g' testrec.log | sed 's,\(.*\)I$,I-\1,g' > testrec.col
cut -f1 test.log > words.col

awk '
	BEGIN {
		while ( getline < "words.col" > 0 )
		{
			f1_co++
			f1[f1_co] = $0
		}
	}
	{
		print f1[NR]"\t"$0
	} ' testgold.col > tty

awk '
	BEGIN {
		while ( getline < "tty" > 0 )
		{
			f1_co++
			f1[f1_co] = $0
		}
	}
	{
		print f1[NR]"\t"$0
	} ' testrec.col > ttyy
awk '{if (NF==3) print}' ttyy > oo.log

# paste words.col testgold.col testrec.col | awk '{if (NF==3) print}' > oo.log
./conlleval.pl -d '\t' -o NO < oo.log | grep $en >> res.log

exit

deb:
echo "affichage des classes E"
java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -test en.log |& tee verbsuj.classes

echo "construction des fichiers de train et test pour le CRF"

set ens = (pers.ind pers.coll loc.add.elec loc.add.phys loc.adm.nat loc.adm.reg loc.adm.sup loc.adm.town loc.fac loc.oro loc.phys.astro loc.phys.geo loc.phys.hydro loc.unk org.adm org.ent amount time.date.abs time.date.rel time.hour.abs time.hour.rel prod.art prod.award prod.doctr prod.fin prod.media prod.object prod.rule prod.serv prod.soft func.coll func.ind event)
set ens = (pers.ind)

java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -putclass ../../git6/peps/corpus/etape/radios.xml en.log
mv -f groups.* corpus/train/
java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -putclass ../../git6/peps/corpus/etape/devtvs.xml en.log
mv -f groups.* corpus/test/

rm res.log
touch res.log
foreach en ($ens)
  sed 's,trainFile=synfeats0.tab,trainFile=corpus/train/groups.'$en'.tab,g' syn.props >! tmp.props
  java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods

  java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile corpus/test/groups.$en.tab >! test.log
  cut -f2 test.log >! testgold.log
  cut -f3 test.log >! testrec.log
  sed 's,\(.*\)B$,B-\1,g' testgold.log | sed 's,\(.*\)I$,I-\1,g' >! testgold.col
  sed 's,\(.*\)B$,B-\1,g' testrec.log | sed 's,\(.*\)I$,I-\1,g' >! testrec.col
  cut -f1 test.log >! words.col
  paste words.col testgold.col testrec.col | awk '{if (NF==3) print}' >! oo.log
  ./conlleval.pl -d '\t' -o NO < oo.log | grep $en >> res.log
end

exit


# ##################################################
# deb:

echo "extrait les deps (NOM,HEAD) du Gigaword"
set LARGECORP = ../../git/jsafran/c0b.conll
java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -train $LARGECORP

# deb:
echo "unsup clustering des HEAD"
set d = ""`pwd`
cd $HOME
cp -f $d/enV .
cp -f $d/enO .
./en.out >! $d/en.log
cd $d

deb:
echo "affichage des classes du HEAD"
java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -test en.log |& tee res.classes

exit
# ##################################################




goto unsup

trainCRF:

set ens = (pers.ind pers.coll loc.add.elec loc.add.phys loc.adm.nat loc.adm.reg loc.adm.sup loc.adm.town loc.fac loc.oro loc.phys.astro loc.phys.geo loc.phys.hydro loc.unk org.adm org.ent amount time.date.abs time.date.rel time.hour.abs time.hour.rel prod.art prod.award prod.doctr prod.fin prod.media prod.object prod.rule prod.serv prod.soft func.coll func.ind event)

set ens = (pers.ind)

if (1 == 2) then
	# sauve dans les fichiers groups.* tous les mots+POStag avec leur "classe" selon une EN particuliere
	pushd .
	java -Xmx1g -cp ../../git/jsafran/jsafran.jar jsafran.GroupManager ../../git6/peps/corpus/etape/radios.xml
	popd
	mv -f groups.* corpus/train/
	pushd .
	java -Xmx1g -cp ../../git/jsafran/jsafran.jar jsafran.GroupManager ../../git6/peps/corpus/etape/devtvs.xml
	popd
	mv -f groups.* corpus/test/
else
	# idem que ci-dessus sauf que ajoute une col pour les classes
	java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -putclass ../../git6/peps/corpus/etape/radios.xml verbsuj.classes
	mv -f groups.* corpus/train/
	java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -putclass ../../git6/peps/corpus/etape/devtvs.xml verbsuj.classes
	mv -f groups.* corpus/test/
endif


rm res.log
touch res.log
foreach en ($ens)
  sed 's,trainFile=synfeats0.tab,trainFile=corpus/train/groups.'$en'.tab,g' syn.props >! tmp.props
  java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods

  java -Xmx1g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile corpus/test/groups.$en.tab >! test.log
  cut -f2 test.log >! testgold.log
  cut -f3 test.log >! testrec.log
  sed 's,\(.*\)B$,B-\1,g' testgold.log | sed 's,\(.*\)I$,I-\1,g' >! testgold.col
  sed 's,\(.*\)B$,B-\1,g' testrec.log | sed 's,\(.*\)I$,I-\1,g' >! testrec.col
  cut -f1 test.log >! words.col
  paste words.col testgold.col testrec.col | awk '{if (NF==3) print}' >! oo.log
  ./conlleval.pl -d '\t' -o NO < oo.log | grep $en >> res.log
end

# java -cp bin ester.Eval test.log NO

# java -Xmx5g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier kicz.mods -testFile $testfile > test.log
# dans git/ponct:
# java -cp dist/ponct.jar tests.Eval


# java -cp "$JCP" etape.CRFEN $*
# java -cp "$JCP" etape.EtapeMaxEnt $*
# java -Xmx1g -cp "$JCP" etape.EtapeLocThenTag $*

exit

giga:
# pour transformer le gigaword French au format JSafran:
set i = 0
while ($i < 341)
  java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" GigawordIO $i
  @ i++
end
# il faut ensuite le segmenter:
java -Xmx1g -cp "../../git/jsafran/jsafran.jar" jsafran.ponctuation.UttSegmenter c$i.xml
mv -f output.xml c$i.xml
# J'ai mis le res dans corpus/Gigaword_French/

# je train le Malt liblinear sur FTB:
java -Xmx1g -jar malt.jar -c ftbmods -l liblinear -i ftbtrain.conll -m learn

# puis je tag le gigaword dans JSafran

# puis je parse le gigaword:
java -Xmx1g -jar malt.jar -c ftbmods -i c0.conll -m parse -o c0b.conll

unsup:

goto tmpx

# puis j'extrais les couples (verbe,sujet)
set LARGECORP = ../../git/jsafran/c0b.conll
# set LARGECORP = synthdata.conll
java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -train $LARGECORP


cd $HOME
cp -f $d/enV .
cp -f $d/enO .
foreach iter (1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)
  ./en.out >! $d/en.log.$iter
end

tmpx:
# puis j'affiche les classes:
cd $d
rm -f verbsuj.classes
touch verbsuj.classes
foreach iter (1)
  java -Xmx1g -cp "../../git/jsafran/jsafran.jar;bin" PrepHDB -test en.log.$iter 3 | grep -e '^classe ' >> verbsuj.classes
end

# reste a moyenner tous les lancers ?? difficile

# rappeler DOM = 03 83 59 20 27

exit

goto trainCRF

# 1er test: j'obtiens des classes de verbes "sémantiques", rassemblant les verbes ayant les memes mots en sujet
# pour avoir des ENs, il faut donner au modele generatif seulement des ENs, et il faut donc les detecter avant.
# Mais on n'est pas oblige d'avoir une bonne detection en F-mesure; il faut une bonne detection en precision !

# 2eme test: modele generique avec seulement les EN... Mais pas forcement mieux, car moins de data, et puis on veut
# regrouper les verbes qui ont les memes types de sujets, pas forcement les memes sujets EN

# analyse des erreurs: il y a peu de classes, et elles sont presque toujours associees a des erreurs de parsing.
# pour qu'elles apportent qqchose, il faut que l'info des classes soit partout !

exit

EN a utiliser:
pers.*
adm.*
loc.*
org.*
amount
time.*
prod.*
func.*
event


