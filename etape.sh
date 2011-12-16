#!/bin/tcsh

set JCP = "bin:../utils/bin:../jsafran/jsafran.jar:../../softs/mallet-2.0.5/dist/mallet.jar:../../softs/mallet-2.0.5/dist/mallet-deps.jar:../../softs/mallet-2.0.5/lib/trove-2.0.2.jar"

if ("$TERM" == "cygwin") then
	set JCP = `echo "$JCP" | sed 's,:,;,g'`
	setenv PATH "$PATH"":../../tools"
else
	setenv PATH $PATH":../../tools"
endif

foreach en (pers.ind)
  sed 's,trainFile=synfeats0.tab,trainFile=groups.'$en'.tab,g' syn.props >! tmp.props
  java -Xmx20g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
end
java -Xmx20g -cp detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier kiki.mods -testFile test.groups.pers.ind.tab > test.log

java -cp bin ester.Eval test.log NO

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

