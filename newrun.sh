#!/bin/bash

java -cp bin etape.unsup.SparseRules 

d=`pwd`
cd /home/xtof/corpus/ETAPE2/lnetools/ne-scoring-gen
./ne-scoring-gen generales.lua /home/xtof/corpus/ETAPE2/Dom/Etape/quaero-ne-normalized/19990617_1900_1920_inter_fm_dga.ne $d/rec.ne

