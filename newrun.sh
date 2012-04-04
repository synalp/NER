#!/bin/bash

# java -cp bin etape.unsup.TwoRules 

d=`pwd`
cd /home/xtof/corpus/ETAPE2/lnetools/ne-scoring-gen
./ne-scoring-gen generales.lua /home/xtof/corpus/ETAPE2/Dom/Etape/quaero-ne-normalized/19990621_1900_1920_inter_fm_dga.ne $d/corp.ne

