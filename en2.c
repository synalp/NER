/*
This program was automatically generated using:
     __  ____   ____
    / / / /  \ / __/  HBC: The Hierarchical Bayes Compiler
   / /_/ / / // /     http://hal3.name/HBC/
  / __  / --</ /      
 / / / / /  / /___    Version 0.7 beta
 \/ /_/____/\____/    

HBC is a freely available compiler for statistical models.  This generated
code can be built using the following command:

  gcc -O3 -lm stats.c samplib.c en2.c -o en2.out

The hierarchical model that this code reflects is:

alphaC ~ Gam(1,1)
alphaE ~ Gam(1,1)
alphaV ~ Gam(0.1,1)
alphaW ~ Gam(0.1,1)

thetaC ~ DirSym(alphaC, Nc)
thetaE_{k} ~ DirSym(alphaE, Nen) , k \in [1,Nc]
thetaV_{k} ~ DirSym(alphaV, VV) , k \in [1,Nc]
thetaW_{k} ~ DirSym(alphaW, VW) , k \in [1,Nen]

c_{n} ~ Mult(thetaC) , n \in [1,N]
e_{n} ~ Mult(thetaE_{c_{n}}) , n \in [1,N]
v_{n} ~ Mult(thetaV_{c_{n}}) , n \in [1,N]
w_{n} ~ Mult(thetaW_{e_{n}}) , n \in [1,N]

--# --define Nc 5
--# --define Nen 3
--# --define alphaC 1
--# --define alphaE 0.1
--# --define alphaV 0.1
--# --define alphaW 0.1

--# --loadD enO w VW N ;
--# --loadD enV v VV N ;

--# --collapse thetaC
--# --collapse thetaE
--# --collapse thetaV
--# --collapse thetaW


Generated using the command:

  hbc compile en2.hier en2.c
*/
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "stats.h"


/**************************** SAMPLING ****************************/

void resample_post_thetaC(int N, int Nc, int* c, double* post_thetaC) {
  double* tmpSP7;
  int n_4;
  int dvv_loop_var_1;
  tmpSP7 = (double*) malloc(sizeof(double) * (1+((Nc) + (1))-(1)));
  /* Implements direct sampling from the following distribution: */
  /*   Delta(post_thetaC | \sum_{n@4 \in [N]} IDR(c_{n@4}, 1, Nc), Nc) */
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    tmpSP7[dvv_loop_var_1-1] = 0.0;
  }
  tmpSP7[(Nc) + (1)-1] = (0.0) * (((1) + (Nc)) - (1));
  for (n_4=1; n_4<=N; n_4++) {
    tmpSP7[(Nc) + (1)-1] += 1.0;
    tmpSP7[c[n_4-1]-1] += 1.0;
  }
  sample_Delta(post_thetaC, tmpSP7, Nc);
  free(tmpSP7);
}

void resample_post_thetaE(int N, int Nc, int Nen, int* c, int* e, double** post_thetaE) {
  int k_17;
  double* tmpSP8;
  int n_5;
  int dvv_loop_var_1;
  tmpSP8 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (k_17=1; k_17<=Nc; k_17++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaE_{k@17} | \sum_{n@5 \in [N]} .*(=(k@17, c_{n@5}), IDR(e_{n@5}, 1, Nen)), Nen) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmpSP8[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP8[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (n_5=1; n_5<=N; n_5++) {
      tmpSP8[(Nen) + (1)-1] += (1.0) * ((((k_17) == (c[n_5-1])) ? 1 : 0));
      tmpSP8[e[n_5-1]-1] += (1.0) * ((((k_17) == (c[n_5-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaE[k_17-1], tmpSP8, Nen);
  }
  free(tmpSP8);
}

void resample_post_thetaV(int N, int Nc, int VV, int* c, double** post_thetaV, int* v) {
  int k_18;
  double* tmpSP9;
  int n_6,z;
  int dvv_loop_var_1;
  tmpSP9 = (double*) malloc(sizeof(double) * (1+((VV) + (1))-(1)));
printf("debug VV %d %d\n",VV,tmpSP9,tmpSP9+sizeof(double)*(VV+1));
  for (k_18=1; k_18<=Nc; k_18++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaV_{k@18} | \sum_{n@6 \in [N]} .*(=(k@18, c_{n@6}), IDR(v_{n@6}, 1, VV)), VV) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VV; dvv_loop_var_1++) {
      tmpSP9[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP9[(VV) + (1)-1] = (0.0) * (((1) + (VV)) - (1));
    for (n_6=1; n_6<=N; n_6++) {
      tmpSP9[(VV) + (1)-1] += (1.0) * ((((k_18) == (c[n_6-1])) ? 1 : 0));
     z=v[n_6-1]-1;
     tmpSP9[z] += (1.0) * ((((k_18) == (c[n_6-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaV[k_18-1], tmpSP9, VV);
  }
  free(tmpSP9);
printf("PASS\n");
}

void resample_post_thetaW(int N, int Nen, int VW, int* e, double** post_thetaW, int* w) {
  int k_19;
  double* tmpSP10;
  int n_7;
  int dvv_loop_var_1;
  tmpSP10 = (double*) malloc(sizeof(double) * (1+((VW) + (1))-(1)));
  for (k_19=1; k_19<=Nen; k_19++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaW_{k@19} | \sum_{n@7 \in [N]} .*(=(k@19, e_{n@7}), IDR(w_{n@7}, 1, VW)), VW) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VW; dvv_loop_var_1++) {
      tmpSP10[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP10[(VW) + (1)-1] = (0.0) * (((1) + (VW)) - (1));
    for (n_7=1; n_7<=N; n_7++) {
      tmpSP10[(VW) + (1)-1] += (1.0) * ((((k_19) == (e[n_7-1])) ? 1 : 0));
      tmpSP10[w[n_7-1]-1] += (1.0) * ((((k_19) == (e[n_7-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaW[k_19-1], tmpSP10, VW);
  }
  free(tmpSP10);
}

double resample_alphaC(int Nc, double alphaC, double* post_thetaC) {
  double tmpSP0;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaC | 1, /(1.0, -(1.0, /(1.0, \sum_{cgds \in [Nc]} log(.*(/(1.0, sub(.+(alphaC, post_thetaC), +(Nc, 1))), .+(alphaC, post_thetaC_{cgds}))))))) */
  tmpSP0 = 0.0;
  for (cgds=1; cgds<=Nc; cgds++) {
    tmpSP0 += log(((1.0) / ((alphaC) + (post_thetaC[(Nc) + (1)-1]))) * ((alphaC) + (post_thetaC[cgds-1])));
  }
  alphaC = sample_Gam(1, (1.0) / ((1.0) - ((1.0) / (tmpSP0))));
  return (alphaC);
}

double resample_alphaE(int Nc, int Nen, double alphaE, double** post_thetaE) {
  double tmpSP1;
  int k_1;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaE | 1, /(1.0, -(1.0, /(1.0, \sum_{k@1 \in [Nc]} \sum_{cgds \in [Nen]} log(.*(/(1.0, sub(.+(alphaE, post_thetaE_{k@1}), +(Nen, 1))), .+(alphaE, post_thetaE_{k@1,cgds}))))))) */
  tmpSP1 = 0.0;
  for (k_1=1; k_1<=Nc; k_1++) {
    for (cgds=1; cgds<=Nen; cgds++) {
      tmpSP1 += log(((1.0) / ((alphaE) + (post_thetaE[k_1-1][(Nen) + (1)-1]))) * ((alphaE) + (post_thetaE[k_1-1][cgds-1])));
    }
  }
  alphaE = sample_Gam(1, (1.0) / ((1.0) - ((1.0) / (tmpSP1))));
  return (alphaE);
}

double resample_alphaV(int Nc, int VV, double alphaV, double** post_thetaV) {
  double tmpSP3;
  int k_2;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaV | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@2 \in [Nc]} \sum_{cgds \in [VV]} log(.*(/(1.0, sub(.+(alphaV, post_thetaV_{k@2}), +(VV, 1))), .+(alphaV, post_thetaV_{k@2,cgds}))))))) */
  tmpSP3 = 0.0;
  for (k_2=1; k_2<=Nc; k_2++) {
    for (cgds=1; cgds<=VV; cgds++) {
      tmpSP3 += log(((1.0) / ((alphaV) + (post_thetaV[k_2-1][(VV) + (1)-1]))) * ((alphaV) + (post_thetaV[k_2-1][cgds-1])));
    }
  }
  alphaV = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP3))));
  return (alphaV);
}

double resample_alphaW(int Nen, int VW, double alphaW, double** post_thetaW) {
  double tmpSP5;
  int k_3;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaW | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@3 \in [Nen]} \sum_{cgds \in [VW]} log(.*(/(1.0, sub(.+(alphaW, post_thetaW_{k@3}), +(VW, 1))), .+(alphaW, post_thetaW_{k@3,cgds}))))))) */
  tmpSP5 = 0.0;
  for (k_3=1; k_3<=Nen; k_3++) {
    for (cgds=1; cgds<=VW; cgds++) {
      tmpSP5 += log(((1.0) / ((alphaW) + (post_thetaW[k_3-1][(VW) + (1)-1]))) * ((alphaW) + (post_thetaW[k_3-1][cgds-1])));
    }
  }
  alphaW = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP5))));
  return (alphaW);
}

void resample_c(int N, double alphaC, double alphaE, double alphaV, int* c, int* e, double* post_thetaC, double** post_thetaE, double** post_thetaV, int* v, int Nc, int VV, int Nen) {
  int n_20;
  double* tmp_post_c_1;
  int tmp_idx_c_1;
  int dvv_loop_var_1;
  tmp_post_c_1 = (double*) malloc(sizeof(double) * (1+((Nc) + (1))-(1)));
  for (n_20=1; n_20<=N; n_20++) {
    post_thetaV[c[n_20-1]-1][(VV) + (1)-1] += (0.0) - ((1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0)));
    post_thetaV[c[n_20-1]-1][v[n_20-1]-1] += (0.0) - ((1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0)));
    post_thetaE[c[n_20-1]-1][(Nen) + (1)-1] += (0.0) - ((1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0)));
    post_thetaE[c[n_20-1]-1][e[n_20-1]-1] += (0.0) - ((1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0)));
    post_thetaC[(Nc) + (1)-1] += (0.0) - (1.0);
    post_thetaC[c[n_20-1]-1] += (0.0) - (1.0);
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(e_{n@20} | .+(alphaE, sub(post_thetaE, c_{n@20}))))((Mult(v_{n@20} | .+(alphaV, sub(post_thetaV, c_{n@20}))))(Mult(c_{n@20} | .+(alphaC, post_thetaC)))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
      tmp_post_c_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_c_1[(Nc) + (1)-1] = (0.0) * (((1) + (Nc)) - (1));
    for (tmp_idx_c_1=1; tmp_idx_c_1<=Nc; tmp_idx_c_1++) {
      tmp_post_c_1[tmp_idx_c_1-1] = (ldf_Mult_smooth(0, alphaE, e[n_20-1], post_thetaE[tmp_idx_c_1-1], 1, Nen)) + ((ldf_Mult_smooth(0, alphaV, v[n_20-1], post_thetaV[tmp_idx_c_1-1], 1, VV)) + (ldf_Mult_smooth(0, alphaC, tmp_idx_c_1, post_thetaC, 1, Nc)));
    }
    normalizeLog(tmp_post_c_1, 1, Nc);
    c[n_20-1] = sample_Mult(tmp_post_c_1, 1, Nc);
    post_thetaC[(Nc) + (1)-1] += 1.0;
    post_thetaC[c[n_20-1]-1] += 1.0;
    post_thetaE[c[n_20-1]-1][(Nen) + (1)-1] += (1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0));
    post_thetaE[c[n_20-1]-1][e[n_20-1]-1] += (1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0));
    post_thetaV[c[n_20-1]-1][(VV) + (1)-1] += (1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0));
    post_thetaV[c[n_20-1]-1][v[n_20-1]-1] += (1.0) * ((((c[n_20-1]) == (c[n_20-1])) ? 1 : 0));
  }
  free(tmp_post_c_1);
}

void resample_e(int N, double alphaE, double alphaW, int* c, int* e, double** post_thetaE, double** post_thetaW, int* w, int Nen, int VW) {
  int n_21;
  double* tmp_post_e_1;
  int tmp_idx_e_1;
  int dvv_loop_var_1;
  tmp_post_e_1 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (n_21=1; n_21<=N; n_21++) {
    post_thetaW[e[n_21-1]-1][(VW) + (1)-1] += (0.0) - ((1.0) * ((((e[n_21-1]) == (e[n_21-1])) ? 1 : 0)));
    post_thetaW[e[n_21-1]-1][w[n_21-1]-1] += (0.0) - ((1.0) * ((((e[n_21-1]) == (e[n_21-1])) ? 1 : 0)));
    post_thetaE[c[n_21-1]-1][(Nen) + (1)-1] += (0.0) - ((1.0) * ((((c[n_21-1]) == (c[n_21-1])) ? 1 : 0)));
    post_thetaE[c[n_21-1]-1][e[n_21-1]-1] += (0.0) - ((1.0) * ((((c[n_21-1]) == (c[n_21-1])) ? 1 : 0)));
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(w_{n@21} | .+(alphaW, sub(post_thetaW, e_{n@21}))))(Mult(e_{n@21} | .+(alphaE, sub(post_thetaE, c_{n@21})))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmp_post_e_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_e_1[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (tmp_idx_e_1=1; tmp_idx_e_1<=Nen; tmp_idx_e_1++) {
      tmp_post_e_1[tmp_idx_e_1-1] = (ldf_Mult_smooth(0, alphaW, w[n_21-1], post_thetaW[tmp_idx_e_1-1], 1, VW)) + (ldf_Mult_smooth(0, alphaE, tmp_idx_e_1, post_thetaE[c[n_21-1]-1], 1, Nen));
    }
    normalizeLog(tmp_post_e_1, 1, Nen);
    e[n_21-1] = sample_Mult(tmp_post_e_1, 1, Nen);
    post_thetaE[c[n_21-1]-1][(Nen) + (1)-1] += (1.0) * ((((c[n_21-1]) == (c[n_21-1])) ? 1 : 0));
    post_thetaE[c[n_21-1]-1][e[n_21-1]-1] += (1.0) * ((((c[n_21-1]) == (c[n_21-1])) ? 1 : 0));
    post_thetaW[e[n_21-1]-1][(VW) + (1)-1] += (1.0) * ((((e[n_21-1]) == (e[n_21-1])) ? 1 : 0));
    post_thetaW[e[n_21-1]-1][w[n_21-1]-1] += (1.0) * ((((e[n_21-1]) == (e[n_21-1])) ? 1 : 0));
  }
  free(tmp_post_e_1);
}

void resample_v(int N, double alphaV, int* c, double** post_thetaV, int* v, int VV) {
  int n_22;
  for (n_22=1; n_22<=N; n_22++) {
    post_thetaV[c[n_22-1]-1][(VV) + (1)-1] += (0.0) - ((1.0) * ((((c[n_22-1]) == (c[n_22-1])) ? 1 : 0)));
    post_thetaV[c[n_22-1]-1][v[n_22-1]-1] += (0.0) - ((1.0) * ((((c[n_22-1]) == (c[n_22-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(v_{n@22} | .+(alphaV, sub(post_thetaV, c_{n@22}))) */
    v[n_22-1] = sample_Mult_smooth(alphaV, post_thetaV[c[n_22-1]-1], 1, VV);
    post_thetaV[c[n_22-1]-1][(VV) + (1)-1] += (1.0) * ((((c[n_22-1]) == (c[n_22-1])) ? 1 : 0));
    post_thetaV[c[n_22-1]-1][v[n_22-1]-1] += (1.0) * ((((c[n_22-1]) == (c[n_22-1])) ? 1 : 0));
  }
}

void resample_w(int N, double alphaW, int* e, double** post_thetaW, int* w, int VW) {
  int n_23;
  for (n_23=1; n_23<=N; n_23++) {
    post_thetaW[e[n_23-1]-1][(VW) + (1)-1] += (0.0) - ((1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0)));
    post_thetaW[e[n_23-1]-1][w[n_23-1]-1] += (0.0) - ((1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(w_{n@23} | .+(alphaW, sub(post_thetaW, e_{n@23}))) */
    w[n_23-1] = sample_Mult_smooth(alphaW, post_thetaW[e[n_23-1]-1], 1, VW);
    post_thetaW[e[n_23-1]-1][(VW) + (1)-1] += (1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0));
    post_thetaW[e[n_23-1]-1][w[n_23-1]-1] += (1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0));
  }
}


/************************* INITIALIZATION *************************/

double initialize_alphaC() {
  double alphaC;
  alphaC = sample_Gam(1.0, 1.0);
  return (alphaC);
}

double initialize_alphaE() {
  double alphaE;
  alphaE = sample_Gam(1.0, 1.0);
  return (alphaE);
}

double initialize_alphaV() {
  double alphaV;
  alphaV = sample_Gam(1.0, 1.0);
  return (alphaV);
}

double initialize_alphaW() {
  double alphaW;
  alphaW = sample_Gam(1.0, 1.0);
  return (alphaW);
}

void initialize_c(int* c, int N, int Nc) {
  int n_20;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    c[dvv_loop_var_1-1] = 0;
  }
  c[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_20=1; n_20<=N; n_20++) {
    c[n_20-1] = sample_MultSym(1, Nc);
  }
}

void initialize_e(int* e, int N, int Nen) {
  int n_21;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    e[dvv_loop_var_1-1] = 0;
  }
  e[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_21=1; n_21<=N; n_21++) {
    e[n_21-1] = sample_MultSym(1, Nen);
  }
}

void initialize_v(int* v, int N, int VV) {
  int n_22;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    v[dvv_loop_var_1-1] = 0;
  }
  v[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_22=1; n_22<=N; n_22++) {
    v[n_22-1] = sample_MultSym(1, VV);
  }
}

void initialize_w(int* w, int N, int VW) {
  int n_23;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    w[dvv_loop_var_1-1] = 0;
  }
  w[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_23=1; n_23<=N; n_23++) {
    w[n_23-1] = sample_MultSym(1, VW);
  }
}

void initialize_post_thetaC(double* post_thetaC, int N, int Nc, int* c) {
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    post_thetaC[dvv_loop_var_1-1] = 0.0;
  }
  post_thetaC[(Nc) + (1)-1] = (0.0) * (((1) + (Nc)) - (1));
  resample_post_thetaC(N, Nc, c, post_thetaC);
}

void initialize_post_thetaE(double** post_thetaE, int N, int Nc, int Nen, int* c, int* e) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nen; dvv_loop_var_2++) {
      post_thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaE[dvv_loop_var_1-1][(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
  }
  resample_post_thetaE(N, Nc, Nen, c, e, post_thetaE);
}

void initialize_post_thetaV(double** post_thetaV, int N, int Nc, int VV, int* c, int* v) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VV; dvv_loop_var_2++) {
      post_thetaV[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaV[dvv_loop_var_1-1][(VV) + (1)-1] = (0.0) * (((1) + (VV)) - (1));
  }
  resample_post_thetaV(N, Nc, VV, c, post_thetaV, v);
}

void initialize_post_thetaW(double** post_thetaW, int N, int Nen, int VW, int* e, int* w) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VW; dvv_loop_var_2++) {
      post_thetaW[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaW[dvv_loop_var_1-1][(VW) + (1)-1] = (0.0) * (((1) + (VW)) - (1));
  }
  resample_post_thetaW(N, Nen, VW, e, post_thetaW, w);
}


/**************************** DUMPING *****************************/

void dump_alphaC(double alphaC) {
  printf("alphaC = ");
  printf("%g", alphaC);
  printf("\n");
}

void dump_alphaE(double alphaE) {
  printf("alphaE = ");
  printf("%g", alphaE);
  printf("\n");
}

void dump_alphaV(double alphaV) {
  printf("alphaV = ");
  printf("%g", alphaV);
  printf("\n");
}

void dump_alphaW(double alphaW) {
  printf("alphaW = ");
  printf("%g", alphaW);
  printf("\n");
}

void dump_thetaC(int Nc, double* thetaC) {
  int dvv_loop_var_1;
  printf("thetaC = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    printf("%g", thetaC[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_thetaE(int Nc, int Nen, double** thetaE) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaE = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nen; dvv_loop_var_2++) {
      printf("%g", thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
  }
  printf("\n");
}

void dump_thetaV(int Nc, int VV, double** thetaV) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaV = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VV; dvv_loop_var_2++) {
      printf("%g", thetaV[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
  }
  printf("\n");
}

void dump_thetaW(int Nen, int VW, double** thetaW) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaW = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VW; dvv_loop_var_2++) {
      printf("%g", thetaW[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
  }
  printf("\n");
}

void dump_c(int N, int* c) {
  int dvv_loop_var_1;
  printf("c = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", c[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_e(int N, int* e) {
  int dvv_loop_var_1;
  printf("e = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", e[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_v(int N, int* v) {
  int dvv_loop_var_1;
  printf("v = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", v[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_w(int N, int* w) {
  int dvv_loop_var_1;
  printf("w = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", w[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}


/*************************** LIKELIHOOD ***************************/

double compute_log_posterior(int N, int Nc, int Nen, int VV, int VW, double alphaC, double alphaE, double alphaV, double alphaW, int* c, int* e, double* thetaC, double** thetaE, double** thetaV, double** thetaW, int* v, int* w) {
  double ldfP8_0;
  int n_20;
  double ldfP9_0;
  int n_21;
  double ldfP10_0;
  int n_22;
  double ldfP11_0;
  int n_23;
  ldfP8_0 = 0.0;
  for (n_20=1; n_20<=N; n_20++) {
    ldfP8_0 += ldf_Mult(1, c[n_20-1], thetaC, 1, Nc);
  }
  ldfP9_0 = 0.0;
  for (n_21=1; n_21<=N; n_21++) {
    ldfP9_0 += ldf_Mult(1, e[n_21-1], thetaE[c[n_21-1]-1], 1, Nen);
  }
  ldfP10_0 = 0.0;
  for (n_22=1; n_22<=N; n_22++) {
    ldfP10_0 += ldf_Mult(1, v[n_22-1], thetaV[c[n_22-1]-1], 1, VV);
  }
  ldfP11_0 = 0.0;
  for (n_23=1; n_23<=N; n_23++) {
    ldfP11_0 += ldf_Mult(1, w[n_23-1], thetaW[e[n_23-1]-1], 1, VW);
  }
  return ((ldf_Gam(1, alphaC, 1, 1)) + ((ldf_Gam(1, alphaE, 1, 1)) + ((ldf_Gam(1, alphaV, 0.1, 1)) + ((ldf_Gam(1, alphaW, 0.1, 1)) + ((0.0) + ((0.0) + ((0.0) + ((0.0) + ((ldfP8_0) + ((ldfP9_0) + ((ldfP10_0) + (ldfP11_0))))))))))));
}

/****************************** MAIN ******************************/

int main(int ARGC, char *ARGV[]) {
  double loglik,bestloglik;
  int iter;
  int N;
  int Nc;
  int Nen;
  int VV;
  int VW;
  double alphaC;
  double alphaE;
  double alphaV;
  double alphaW;
  int* c;
  int* e;
  double* post_thetaC;
  double** post_thetaE;
  double** post_thetaV;
  double** post_thetaW;
  int* v;
  int* w;
  int malloc_dim_1;

  fprintf(stderr, "-- This program was automatically generated using HBC (v 0.7 beta) from en2.hier\n--     see http://hal3.name/HBC for more information\n");
  fflush(stderr);
  setall(time(0),time(0));   /* initialize random number generator */


  /* variables defined with --define */
  Nc = 6;
  Nen = 5;
  alphaC = 0.9;
  alphaE = 0.0000001;
  alphaV = 0.0001;
  alphaW = 0.0001;

  fprintf(stderr, "Loading data...\n");
  fflush(stderr);
  /* variables defined with --loadD */
  w = load_discrete1("enO", &N, &VW);
  v = load_discrete1("enV", &N, &VV);

  /* variables defined with --loadM or --loadMI */

  fprintf(stderr, "Allocating memory...\n");
  fflush(stderr);
  c = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));

  e = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));

  post_thetaC = (double*) malloc(sizeof(double) * (1+((Nc) + (1))-(1)));

  post_thetaE = (double**) malloc(sizeof(double*) * (1+(Nc)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    post_thetaE[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  }

  post_thetaV = (double**) malloc(sizeof(double*) * (1+(Nc)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    post_thetaV[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VV) + (1))-(1)));
  }

  post_thetaW = (double**) malloc(sizeof(double*) * (1+(Nen)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    post_thetaW[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VW) + (1))-(1)));
  }


  fprintf(stderr, "Initializing variables...\n");
  fflush(stderr);
  initialize_c(c, N, Nc);
  initialize_e(e, N, Nen);
  initialize_post_thetaC(post_thetaC, N, Nc, c);
  initialize_post_thetaE(post_thetaE, N, Nc, Nen, c, e);
  initialize_post_thetaV(post_thetaV, N, Nc, VV, c, v);
  initialize_post_thetaW(post_thetaW, N, Nen, VW, e, w);

  for (iter=1; iter<=100; iter++) {
    fprintf(stderr, "iter %d", iter);
    fflush(stderr);
    resample_c(N, alphaC, alphaE, alphaV, c, e, post_thetaC, post_thetaE, post_thetaV, v, Nc, VV, Nen);
    resample_e(N, alphaE, alphaW, c, e, post_thetaE, post_thetaW, w, Nen, VW);

if (iter>=20) {
  dump_c(N,c);
  dump_e(N,e);
}

    loglik = compute_log_posterior(N, Nc, Nen, VV, VW, alphaC, alphaE, alphaV, alphaW, c, e, post_thetaC, post_thetaE, post_thetaV, post_thetaW, v, w);
    fprintf(stderr, "\t%g", loglik);
    if ((iter==1)||(loglik>bestloglik)) {
      bestloglik = loglik;
      fprintf(stderr, " *");
    }
    fprintf(stderr, "\n");
    fflush(stderr);
  }

  free(w);

  free(v);

  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    free(post_thetaW[malloc_dim_1-1]);
  }
  free(post_thetaW);

  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    free(post_thetaV[malloc_dim_1-1]);
  }
  free(post_thetaV);

  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    free(post_thetaE[malloc_dim_1-1]);
  }
  free(post_thetaE);

  free(post_thetaC);

  free(e);

  free(c);


  return 0;
}
