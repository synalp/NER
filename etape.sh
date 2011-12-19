#!/bin/tcsh

set JCP = "bin:../utils/bin:../jsafran/jsafran.jar:../../softs/mallet-2.0.5/dist/mallet.jar:../../softs/mallet-2.0.5/dist/mallet-deps.jar:../../softs/mallet-2.0.5/lib/trove-2.0.2.jar"

if ("$TERM" == "cygwin") then
	set JCP = `echo "$JCP" | sed 's,:,;,g'`
	setenv PATH "$PATH"":../../tools"
else
	setenv PATH $PATH":../../tools"
endif

set ens = (pers.ind pers.coll loc.add.elec loc.add.phys loc.adm.nat loc.adm.reg loc.adm.sup loc.adm.town loc.fac loc.oro loc.phys.astro loc.phys.geo loc.phys.hydro loc.unk org.adm org.ent amount time.date.abs time.date.rel time.hour.abs time.hour.rel prod.art prod.award prod.doctr prod.fin prod.media prod.object prod.rule prod.serv prod.soft func.coll func.ind event)

echo "train"
foreach en ($ens)
  sed 's,trainFile=synfeats0.tab,trainFile=corpus/train/groups.'$en'.tab,g' syn.props >! tmp.props
  java -Xmx20g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods
end

echo "test"
rm res.log
touch res.log
foreach en ($ens)
  java -Xmx20g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile corpus/test/groups.$en.tab >! test.log
  cut -f2 test.log >! testgold.log
  cut -f3 test.log >! testrec.log
  sed 's,\(.*\)B$,B-\1,g' testgold.log | sed 's,\(.*\)I$,I-\1,g' >! testgold.col
  sed 's,\(.*\)B$,B-\1,g' testrec.log | sed 's,\(.*\)I$,I-\1,g' >! testrec.col
  cut -f1 test.log >! words.col
  paste words.col testgold.col testrec.col | ./conlleval.pl -d '\t' -o NO | grep $en >> res.log
end

# java -cp bin ester.Eval test.log NO

# java -Xmx5g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier kicz.mods -testFile $testfile > test.log
# dans git/ponct:
# java -cp dist/ponct.jar tests.Eval


# java -cp "$JCP" etape.CRFEN $*
# java -cp "$JCP" etape.EtapeMaxEnt $*
# java -Xmx1g -cp "$JCP" etape.EtapeLocThenTag $*

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

