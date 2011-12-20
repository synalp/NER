#!/bin/tcsh

set JCP = "bin:../utils/bin:../jsafran/jsafran.jar:../../softs/mallet-2.0.5/dist/mallet.jar:../../softs/mallet-2.0.5/dist/mallet-deps.jar:../../softs/mallet-2.0.5/lib/trove-2.0.2.jar:../syntponct/lib/anna-2.jar"

if ("$TERM" == "cygwin") then
	set JCP = `echo "$JCP" | sed 's,:,;,g'`
	setenv PATH "$PATH"":../../tools"
else
	setenv PATH $PATH":../../tools"
endif

rm res.log
touch res.log

#goto train

echo "Corpus_preparation - transformation into the standford format"
foreach en (figure)

foreach tt (trn test)
echo 'en='$en
#awk '{ str = $0; sub("/<a href\b[^>]*>(.*?)</a>/","XX", str); print str; }' /global/agey/pavel/CTK/NER/$tt.txt >! xx

#sed 's,<a .*?href="?(.+?)" mce_href="?(.+?)"?.*?>(.+?)</a>,[$1] {$2},ig;' /global/agey/pavel/CTK/NER/$tt.txt >! xx 

#<a href\b[^>]*>(.*?)</a>
#exit
awk -v ne=$en -f make-good-format.awk /global/agey/pavel/CTK/NER/$tt.txt >! ./$tt.tmp

awk 'BEGIN{i=1; }{if (NF >= 1) {print i"\t"$1; i++;} else {printf ("\n"); ; i=1;}}' $tt.tmp >! $tt.conll

#java -Xmx2G -cp "$JCP" is2.lemmatizer.Lemmatizer -model ../syntponct/models/lemma-cz.model -test $tt.conll -out lemmas.$tt

java -Xmx2G -cp  "$JCP" is2.tag3.Tagger -model ../syntponct/models/tag-cz.model -test $tt.conll -out $tt.POS

paste ./$tt.tmp $tt.POS >! $tt.all

#without error deletion
#awk '{if (NF >=1) {if ($1 != "") {print $1 "\t" $8 "\t" $2;} } else {printf("\n");} }' $tt.all >! $tt.$en.tab

#post-processing - errors deletion <a href ...
awk '{if (NF >=1) {if ($1 != "") {print $1 "\t" $8 "\t" $2;} } else {printf("\n");} }' $tt.all | awk '{if ($3 == "" || $3 == "NO" || $3 == "figureB" || $3 == "figureI") print $0;}' >! $tt.$en.tab

end
end
#exit

train:
echo "CRF training"
foreach en (figure)
  sed 's,trainFile=synfeats0.tab,trainFile=trn.'$en'.tab,g' syncz.props >! tmp.props
   java -Xmx7g -cp ../jsafran/lib/detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop tmp.props
  mv kiki.mods en.$en.mods
#end

test:
echo "Testing"
 java -Xmx7g -cp ../jsafran/lib/detcrf.jar edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier en.$en.mods -testFile test.$en.tab > test.log

echo "Evaluation"
cut -f2 test.log >! testgold.log
cut -f3 test.log >! testrec.log
sed 's,\(.*\)B$,B-\1,g' testgold.log | sed 's,\(.*\)I$,I-\1,g' >! testgold.col
sed 's,\(.*\)B$,B-\1,g' testrec.log | sed 's,\(.*\)I$,I-\1,g' >! testrec.col
cut -f1 test.log >! words.col
paste words.col testgold.col testrec.col | ./conlleval.pl -d '\t' -o NO

end

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

