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
alphaD ~ Gam(1,1)
alphaE ~ Gam(0.1,1)
alphaV ~ Gam(0.1,1)
alphaW ~ Gam(0.1,1)

thetaC ~ DirSym(alphaC, Nc)
thetaD ~ DirSym(alphaD, Nd)
thetaE_{k,l} ~ DirSym(alphaE, Nen) , k \in [1,Nc] , l \in [1,Nd]
thetaV_{k} ~ DirSym(alphaV, VV) , k \in [1,Nc]
thetaW_{k} ~ DirSym(alphaW, VW) , k \in [1,Nen]

c_{n} ~ Mult(thetaC) , n \in [1,N]
d_{n} ~ Mult(thetaD) , n \in [1,N]
e_{n} ~ Mult(thetaE_{c_{n},d_{n}}) , n \in [1,N]
v_{n} ~ Mult(thetaV_{c_{n}}) , n \in [1,N]
w_{n} ~ Mult(thetaW_{e_{n}}) , n \in [1,N]

--# --define Nc 10
--# --define Nen 6
--# --define alphaC 2
--# --define alphaD 2
--# --define alphaE 0.001
--# --define alphaV 0.001
--# --define alphaW 0.001

--# --loadD enO w VW N ;
--# --loadD enV v VV N ;
--# --loadD enD d Nd N ;

--# --collapse thetaC
--# --collapse thetaD
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
  double* tmpSP9;
  int n_5;
  int dvv_loop_var_1;
  tmpSP9 = (double*) malloc(sizeof(double) * (1+((Nc) + (1))-(1)));
  /* Implements direct sampling from the following distribution: */
  /*   Delta(post_thetaC | \sum_{n@5 \in [N]} IDR(c_{n@5}, 1, Nc), Nc) */
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    tmpSP9[dvv_loop_var_1-1] = 0.0;
  }
  tmpSP9[(Nc) + (1)-1] = (0.0) * (((1) + (Nc)) - (1));
  for (n_5=1; n_5<=N; n_5++) {
    tmpSP9[(Nc) + (1)-1] += 1.0;
    tmpSP9[c[n_5-1]-1] += 1.0;
  }
  sample_Delta(post_thetaC, tmpSP9, Nc);
  free(tmpSP9);
}

void resample_post_thetaD(int N, int Nd, int* d, double* post_thetaD) {
  double* tmpSP10;
  int n_6;
  int dvv_loop_var_1;
  tmpSP10 = (double*) malloc(sizeof(double) * (1+((Nd) + (1))-(1)));
  /* Implements direct sampling from the following distribution: */
  /*   Delta(post_thetaD | \sum_{n@6 \in [N]} IDR(d_{n@6}, 1, Nd), Nd) */
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nd; dvv_loop_var_1++) {
    tmpSP10[dvv_loop_var_1-1] = 0.0;
  }
  tmpSP10[(Nd) + (1)-1] = (0.0) * (((1) + (Nd)) - (1));
  for (n_6=1; n_6<=N; n_6++) {
    tmpSP10[(Nd) + (1)-1] += 1.0;
    tmpSP10[d[n_6-1]-1] += 1.0;
  }
  sample_Delta(post_thetaD, tmpSP10, Nd);
  free(tmpSP10);
}

void resample_post_thetaV(int N, int Nc, int VV, int* c, double** post_thetaV, int* v) {
  int k_23;
  double* tmpSP12;
  int n_8;
  int dvv_loop_var_1;
  tmpSP12 = (double*) malloc(sizeof(double) * (1+((VV) + (1))-(1)));
  for (k_23=1; k_23<=Nc; k_23++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaV_{k@23} | \sum_{n@8 \in [N]} .*(=(k@23, c_{n@8}), IDR(v_{n@8}, 1, VV)), VV) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VV; dvv_loop_var_1++) {
      tmpSP12[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP12[(VV) + (1)-1] = (0.0) * (((1) + (VV)) - (1));
    for (n_8=1; n_8<=N; n_8++) {
      tmpSP12[(VV) + (1)-1] += (1.0) * ((((k_23) == (c[n_8-1])) ? 1 : 0));
      tmpSP12[v[n_8-1]-1] += (1.0) * ((((k_23) == (c[n_8-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaV[k_23-1], tmpSP12, VV);
  }
  free(tmpSP12);
}

void resample_post_thetaW(int N, int Nen, int VW, int* e, double** post_thetaW, int* w) {
  int k_24;
  double* tmpSP13;
  int n_9;
  int dvv_loop_var_1;
  tmpSP13 = (double*) malloc(sizeof(double) * (1+((VW) + (1))-(1)));
  for (k_24=1; k_24<=Nen; k_24++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaW_{k@24} | \sum_{n@9 \in [N]} .*(=(k@24, e_{n@9}), IDR(w_{n@9}, 1, VW)), VW) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VW; dvv_loop_var_1++) {
      tmpSP13[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP13[(VW) + (1)-1] = (0.0) * (((1) + (VW)) - (1));
    for (n_9=1; n_9<=N; n_9++) {
      tmpSP13[(VW) + (1)-1] += (1.0) * ((((k_24) == (e[n_9-1])) ? 1 : 0));
      tmpSP13[w[n_9-1]-1] += (1.0) * ((((k_24) == (e[n_9-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaW[k_24-1], tmpSP13, VW);
  }
  free(tmpSP13);
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

double resample_alphaD(int Nd, double alphaD, double* post_thetaD) {
  double tmpSP1;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaD | 1, /(1.0, -(1.0, /(1.0, \sum_{cgds \in [Nd]} log(.*(/(1.0, sub(.+(alphaD, post_thetaD), +(Nd, 1))), .+(alphaD, post_thetaD_{cgds}))))))) */
  tmpSP1 = 0.0;
  for (cgds=1; cgds<=Nd; cgds++) {
    tmpSP1 += log(((1.0) / ((alphaD) + (post_thetaD[(Nd) + (1)-1]))) * ((alphaD) + (post_thetaD[cgds-1])));
  }
  alphaD = sample_Gam(1, (1.0) / ((1.0) - ((1.0) / (tmpSP1))));
  return (alphaD);
}

double resample_alphaE(int Nc, int Nd, int Nen, double alphaE, double*** thetaE) {
  double tmpSP2;
  int k_2;
  int l_92;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaE | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@2 \in [Nc]} \sum_{l@92 \in [Nd]} \sum_{cgds \in [Nen]} log(.*(/(1.0, sub(thetaE_{k@2,l@92}, +(Nen, 1))), thetaE_{k@2,l@92,cgds})))))) */
  tmpSP2 = 0.0;
  for (k_2=1; k_2<=Nc; k_2++) {
    for (l_92=1; l_92<=Nd; l_92++) {
      for (cgds=1; cgds<=Nen; cgds++) {
        tmpSP2 += log(((1.0) / (thetaE[k_2-1][l_92-1][(Nen) + (1)-1])) * (thetaE[k_2-1][l_92-1][cgds-1]));
      }
    }
  }
  alphaE = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP2))));
  return (alphaE);
}

double resample_alphaV(int Nc, int VV, double alphaV, double** post_thetaV) {
  double tmpSP5;
  int k_3;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaV | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@3 \in [Nc]} \sum_{cgds \in [VV]} log(.*(/(1.0, sub(.+(alphaV, post_thetaV_{k@3}), +(VV, 1))), .+(alphaV, post_thetaV_{k@3,cgds}))))))) */
  tmpSP5 = 0.0;
  for (k_3=1; k_3<=Nc; k_3++) {
    for (cgds=1; cgds<=VV; cgds++) {
      tmpSP5 += log(((1.0) / ((alphaV) + (post_thetaV[k_3-1][(VV) + (1)-1]))) * ((alphaV) + (post_thetaV[k_3-1][cgds-1])));
    }
  }
  alphaV = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP5))));
  return (alphaV);
}

double resample_alphaW(int Nen, int VW, double alphaW, double** post_thetaW) {
  double tmpSP7;
  int k_4;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaW | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@4 \in [Nen]} \sum_{cgds \in [VW]} log(.*(/(1.0, sub(.+(alphaW, post_thetaW_{k@4}), +(VW, 1))), .+(alphaW, post_thetaW_{k@4,cgds}))))))) */
  tmpSP7 = 0.0;
  for (k_4=1; k_4<=Nen; k_4++) {
    for (cgds=1; cgds<=VW; cgds++) {
      tmpSP7 += log(((1.0) / ((alphaW) + (post_thetaW[k_4-1][(VW) + (1)-1]))) * ((alphaW) + (post_thetaW[k_4-1][cgds-1])));
    }
  }
  alphaW = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP7))));
  return (alphaW);
}

void resample_thetaE(int N, int Nc, int Nd, int Nen, double alphaE, int* c, int* d, int* e, double*** thetaE) {
  int k_22;
  int l_37;
  double* tmpSP11;
  int n_7;
  double* vec_var_1;
  double* vec_var_0;
  int dvv_loop_var_1;
  tmpSP11 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  vec_var_1 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  vec_var_0 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    vec_var_0[dvv_loop_var_1-1] = alphaE;
  }
  vec_var_0[(Nen) + (1)-1] = (alphaE) * (((1) + (Nen)) - (1));
  for (k_22=1; k_22<=Nc; k_22++) {
    for (l_37=1; l_37<=Nd; l_37++) {
      /* Implements direct sampling from the following distribution: */
      /*   Dir(thetaE_{k@22,l@37} | +(vec(alphaE, 1, Nen), \sum_{n@7 \in [N]} .*(=(l@37, d_{n@7}), .*(=(k@22, c_{n@7}), IDR(e_{n@7}, 1, Nen)))), Nen) */
      for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
        tmpSP11[dvv_loop_var_1-1] = 0.0;
      }
      tmpSP11[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
      for (n_7=1; n_7<=N; n_7++) {
        tmpSP11[(Nen) + (1)-1] += ((1.0) * ((((k_22) == (c[n_7-1])) ? 1 : 0))) * ((((l_37) == (d[n_7-1])) ? 1 : 0));
        tmpSP11[e[n_7-1]-1] += ((1.0) * ((((k_22) == (c[n_7-1])) ? 1 : 0))) * ((((l_37) == (d[n_7-1])) ? 1 : 0));
      }
      sample_Dir(thetaE[k_22-1][l_37-1], add_vec_r_1(vec_var_1, vec_var_0, tmpSP11, 1, Nen), Nen);
    }
  }
  free(tmpSP11);
  free(vec_var_1);
  free(vec_var_0);
}

void resample_c(int N, double alphaC, double alphaV, int* c, int* d, int* e, double* post_thetaC, double** post_thetaV, double*** thetaE, int* v, int Nc, int VV, int Nen) {
  int n_25;
  double* tmp_post_c_1;
  int tmp_idx_c_1;
  int dvv_loop_var_1;
  tmp_post_c_1 = (double*) malloc(sizeof(double) * (1+((Nc) + (1))-(1)));
  for (n_25=1; n_25<=N; n_25++) {
    post_thetaV[c[n_25-1]-1][(VV) + (1)-1] += (0.0) - ((1.0) * ((((c[n_25-1]) == (c[n_25-1])) ? 1 : 0)));
    post_thetaV[c[n_25-1]-1][v[n_25-1]-1] += (0.0) - ((1.0) * ((((c[n_25-1]) == (c[n_25-1])) ? 1 : 0)));
    post_thetaC[(Nc) + (1)-1] += (0.0) - (1.0);
    post_thetaC[c[n_25-1]-1] += (0.0) - (1.0);
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(e_{n@25} | sub(thetaE, c_{n@25}, d_{n@25})))((Mult(v_{n@25} | .+(alphaV, sub(post_thetaV, c_{n@25}))))(Mult(c_{n@25} | .+(alphaC, post_thetaC)))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
      tmp_post_c_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_c_1[(Nc) + (1)-1] = (0.0) * (((1) + (Nc)) - (1));
    for (tmp_idx_c_1=1; tmp_idx_c_1<=Nc; tmp_idx_c_1++) {
      tmp_post_c_1[tmp_idx_c_1-1] = (ldf_Mult(0, e[n_25-1], thetaE[tmp_idx_c_1-1][d[n_25-1]-1], 1, Nen)) + ((ldf_Mult_smooth(0, alphaV, v[n_25-1], post_thetaV[tmp_idx_c_1-1], 1, VV)) + (ldf_Mult_smooth(0, alphaC, tmp_idx_c_1, post_thetaC, 1, Nc)));
    }
    normalizeLog(tmp_post_c_1, 1, Nc);
    c[n_25-1] = sample_Mult(tmp_post_c_1, 1, Nc);
    post_thetaC[(Nc) + (1)-1] += 1.0;
    post_thetaC[c[n_25-1]-1] += 1.0;
    post_thetaV[c[n_25-1]-1][(VV) + (1)-1] += (1.0) * ((((c[n_25-1]) == (c[n_25-1])) ? 1 : 0));
    post_thetaV[c[n_25-1]-1][v[n_25-1]-1] += (1.0) * ((((c[n_25-1]) == (c[n_25-1])) ? 1 : 0));
  }
  free(tmp_post_c_1);
}

void resample_d(int N, double alphaD, int* c, int* d, int* e, double* post_thetaD, double*** thetaE, int Nd, int Nen) {
  int n_26;
  double* tmp_post_d_1;
  int tmp_idx_d_1;
  int dvv_loop_var_1;
  tmp_post_d_1 = (double*) malloc(sizeof(double) * (1+((Nd) + (1))-(1)));
  for (n_26=1; n_26<=N; n_26++) {
    post_thetaD[(Nd) + (1)-1] += (0.0) - (1.0);
    post_thetaD[d[n_26-1]-1] += (0.0) - (1.0);
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(e_{n@26} | sub(thetaE, c_{n@26}, d_{n@26})))(Mult(d_{n@26} | .+(alphaD, post_thetaD))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nd; dvv_loop_var_1++) {
      tmp_post_d_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_d_1[(Nd) + (1)-1] = (0.0) * (((1) + (Nd)) - (1));
    for (tmp_idx_d_1=1; tmp_idx_d_1<=Nd; tmp_idx_d_1++) {
      tmp_post_d_1[tmp_idx_d_1-1] = (ldf_Mult(0, e[n_26-1], thetaE[c[n_26-1]-1][tmp_idx_d_1-1], 1, Nen)) + (ldf_Mult_smooth(0, alphaD, tmp_idx_d_1, post_thetaD, 1, Nd));
    }
    normalizeLog(tmp_post_d_1, 1, Nd);
    d[n_26-1] = sample_Mult(tmp_post_d_1, 1, Nd);
    post_thetaD[(Nd) + (1)-1] += 1.0;
    post_thetaD[d[n_26-1]-1] += 1.0;
  }
  free(tmp_post_d_1);
}

void resample_e(int N, double alphaW, int* c, int* d, int* e, double** post_thetaW, double*** thetaE, int* w, int Nen, int VW) {
  int n_27;
  double* tmp_post_e_1;
  int tmp_idx_e_1;
  int dvv_loop_var_1;
  tmp_post_e_1 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (n_27=1; n_27<=N; n_27++) {
    post_thetaW[e[n_27-1]-1][(VW) + (1)-1] += (0.0) - ((1.0) * ((((e[n_27-1]) == (e[n_27-1])) ? 1 : 0)));
    post_thetaW[e[n_27-1]-1][w[n_27-1]-1] += (0.0) - ((1.0) * ((((e[n_27-1]) == (e[n_27-1])) ? 1 : 0)));
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(w_{n@27} | .+(alphaW, sub(post_thetaW, e_{n@27}))))(Mult(e_{n@27} | sub(thetaE, c_{n@27}, d_{n@27}))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmp_post_e_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_e_1[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (tmp_idx_e_1=1; tmp_idx_e_1<=Nen; tmp_idx_e_1++) {
      tmp_post_e_1[tmp_idx_e_1-1] = (ldf_Mult_smooth(0, alphaW, w[n_27-1], post_thetaW[tmp_idx_e_1-1], 1, VW)) + (ldf_Mult(0, tmp_idx_e_1, thetaE[c[n_27-1]-1][d[n_27-1]-1], 1, Nen));
    }
    normalizeLog(tmp_post_e_1, 1, Nen);
    e[n_27-1] = sample_Mult(tmp_post_e_1, 1, Nen);
    post_thetaW[e[n_27-1]-1][(VW) + (1)-1] += (1.0) * ((((e[n_27-1]) == (e[n_27-1])) ? 1 : 0));
    post_thetaW[e[n_27-1]-1][w[n_27-1]-1] += (1.0) * ((((e[n_27-1]) == (e[n_27-1])) ? 1 : 0));
  }
  free(tmp_post_e_1);
}

void resample_v(int N, double alphaV, int* c, double** post_thetaV, int* v, int VV) {
  int n_28;
  for (n_28=1; n_28<=N; n_28++) {
    post_thetaV[c[n_28-1]-1][(VV) + (1)-1] += (0.0) - ((1.0) * ((((c[n_28-1]) == (c[n_28-1])) ? 1 : 0)));
    post_thetaV[c[n_28-1]-1][v[n_28-1]-1] += (0.0) - ((1.0) * ((((c[n_28-1]) == (c[n_28-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(v_{n@28} | .+(alphaV, sub(post_thetaV, c_{n@28}))) */
    v[n_28-1] = sample_Mult_smooth(alphaV, post_thetaV[c[n_28-1]-1], 1, VV);
    post_thetaV[c[n_28-1]-1][(VV) + (1)-1] += (1.0) * ((((c[n_28-1]) == (c[n_28-1])) ? 1 : 0));
    post_thetaV[c[n_28-1]-1][v[n_28-1]-1] += (1.0) * ((((c[n_28-1]) == (c[n_28-1])) ? 1 : 0));
  }
}

void resample_w(int N, double alphaW, int* e, double** post_thetaW, int* w, int VW) {
  int n_29;
  for (n_29=1; n_29<=N; n_29++) {
    post_thetaW[e[n_29-1]-1][(VW) + (1)-1] += (0.0) - ((1.0) * ((((e[n_29-1]) == (e[n_29-1])) ? 1 : 0)));
    post_thetaW[e[n_29-1]-1][w[n_29-1]-1] += (0.0) - ((1.0) * ((((e[n_29-1]) == (e[n_29-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(w_{n@29} | .+(alphaW, sub(post_thetaW, e_{n@29}))) */
    w[n_29-1] = sample_Mult_smooth(alphaW, post_thetaW[e[n_29-1]-1], 1, VW);
    post_thetaW[e[n_29-1]-1][(VW) + (1)-1] += (1.0) * ((((e[n_29-1]) == (e[n_29-1])) ? 1 : 0));
    post_thetaW[e[n_29-1]-1][w[n_29-1]-1] += (1.0) * ((((e[n_29-1]) == (e[n_29-1])) ? 1 : 0));
  }
}


/************************* INITIALIZATION *************************/

double initialize_alphaC() {
  double alphaC;
  alphaC = sample_Gam(1.0, 1.0);
  return (alphaC);
}

double initialize_alphaD() {
  double alphaD;
  alphaD = sample_Gam(1.0, 1.0);
  return (alphaD);
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

void initialize_thetaE(double*** thetaE, int Nc, int Nd, int Nen) {
  int k_22;
  int l_37;
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  int dvv_loop_var_3;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nd; dvv_loop_var_2++) {
      for (dvv_loop_var_3=1; dvv_loop_var_3<=Nen; dvv_loop_var_3++) {
        thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1][dvv_loop_var_3-1] = 0.0;
      }
      thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1][(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    }
  }
  for (k_22=1; k_22<=Nc; k_22++) {
    for (l_37=1; l_37<=Nd; l_37++) {
      sample_DirSym(thetaE[k_22-1][l_37-1], 1.0, Nen);
    }
  }
}

void initialize_c(int* c, int N, int Nc) {
  int n_25;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    c[dvv_loop_var_1-1] = 0;
  }
  c[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_25=1; n_25<=N; n_25++) {
    c[n_25-1] = sample_MultSym(1, Nc);
  }
}

void initialize_d(int* d, int N, int Nd) {
  int n_26;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    d[dvv_loop_var_1-1] = 0;
  }
  d[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_26=1; n_26<=N; n_26++) {
    d[n_26-1] = sample_MultSym(1, Nd);
  }
}

void initialize_e(int* e, int N, int Nen) {
  int n_27;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    e[dvv_loop_var_1-1] = 0;
  }
  e[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_27=1; n_27<=N; n_27++) {
    e[n_27-1] = sample_MultSym(1, Nen);
  }
}

void initialize_v(int* v, int N, int VV) {
  int n_28;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    v[dvv_loop_var_1-1] = 0;
  }
  v[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_28=1; n_28<=N; n_28++) {
    v[n_28-1] = sample_MultSym(1, VV);
  }
}

void initialize_w(int* w, int N, int VW) {
  int n_29;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    w[dvv_loop_var_1-1] = 0;
  }
  w[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_29=1; n_29<=N; n_29++) {
    w[n_29-1] = sample_MultSym(1, VW);
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

void initialize_post_thetaD(double* post_thetaD, int N, int Nd, int* d) {
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nd; dvv_loop_var_1++) {
    post_thetaD[dvv_loop_var_1-1] = 0.0;
  }
  post_thetaD[(Nd) + (1)-1] = (0.0) * (((1) + (Nd)) - (1));
  resample_post_thetaD(N, Nd, d, post_thetaD);
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

void dump_alphaD(double alphaD) {
  printf("alphaD = ");
  printf("%g", alphaD);
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

void dump_thetaD(int Nd, double* thetaD) {
  int dvv_loop_var_1;
  printf("thetaD = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nd; dvv_loop_var_1++) {
    printf("%g", thetaD[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_thetaE(int Nc, int Nd, int Nen, double*** thetaE) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  int dvv_loop_var_3;
  printf("thetaE = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nc; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nd; dvv_loop_var_2++) {
      for (dvv_loop_var_3=1; dvv_loop_var_3<=Nen; dvv_loop_var_3++) {
        printf("%g", thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1][dvv_loop_var_3-1]);
        printf(" ");
      }
      printf(" ; ");
    }
    printf(" ;; ");
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

void dump_d(int N, int* d) {
  int dvv_loop_var_1;
  printf("d = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", d[dvv_loop_var_1-1]);
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

double compute_log_posterior(int N, int Nc, int Nd, int Nen, int VV, int VW, double alphaC, double alphaD, double alphaE, double alphaV, double alphaW, int* c, int* d, int* e, double* thetaC, double* thetaD, double*** thetaE, double** thetaV, double** thetaW, int* v, int* w) {
  double ldfP7_0;
  double ldfP7_1;
  int l_37;
  int k_22;
  double ldfP10_0;
  int n_25;
  double ldfP11_0;
  int n_26;
  double ldfP12_0;
  int n_27;
  double ldfP13_0;
  int n_28;
  double ldfP14_0;
  int n_29;
  ldfP7_0 = 0.0;
  for (k_22=1; k_22<=Nc; k_22++) {
    ldfP7_1 = 0.0;
    for (l_37=1; l_37<=Nd; l_37++) {
      ldfP7_1 += ldf_DirSym(1, thetaE[k_22-1][l_37-1], alphaE, Nen);
    }
    ldfP7_0 += ldfP7_1;
  }
  ldfP10_0 = 0.0;
  for (n_25=1; n_25<=N; n_25++) {
    ldfP10_0 += ldf_Mult(1, c[n_25-1], thetaC, 1, Nc);
  }
  ldfP11_0 = 0.0;
  for (n_26=1; n_26<=N; n_26++) {
    ldfP11_0 += ldf_Mult(1, d[n_26-1], thetaD, 1, Nd);
  }
  ldfP12_0 = 0.0;
  for (n_27=1; n_27<=N; n_27++) {
    ldfP12_0 += ldf_Mult(1, e[n_27-1], thetaE[c[n_27-1]-1][d[n_27-1]-1], 1, Nen);
  }
  ldfP13_0 = 0.0;
  for (n_28=1; n_28<=N; n_28++) {
    ldfP13_0 += ldf_Mult(1, v[n_28-1], thetaV[c[n_28-1]-1], 1, VV);
  }
  ldfP14_0 = 0.0;
  for (n_29=1; n_29<=N; n_29++) {
    ldfP14_0 += ldf_Mult(1, w[n_29-1], thetaW[e[n_29-1]-1], 1, VW);
  }
  return ((ldf_Gam(1, alphaC, 1, 1)) + ((ldf_Gam(1, alphaD, 1, 1)) + ((ldf_Gam(1, alphaE, 0.1, 1)) + ((ldf_Gam(1, alphaV, 0.1, 1)) + ((ldf_Gam(1, alphaW, 0.1, 1)) + ((0.0) + ((0.0) + ((ldfP7_0) + ((0.0) + ((0.0) + ((ldfP10_0) + ((ldfP11_0) + ((ldfP12_0) + ((ldfP13_0) + (ldfP14_0)))))))))))))));
}

/****************************** MAIN ******************************/

int main(int ARGC, char *ARGV[]) {
  double loglik,bestloglik;
  int iter;
  int N;
  int Nc;
  int Nd;
  int Nen;
  int VV;
  int VW;
  double alphaC;
  double alphaD;
  double alphaE;
  double alphaV;
  double alphaW;
  int* c;
  int* d;
  int* e;
  double* post_thetaC;
  double* post_thetaD;
  double** post_thetaV;
  double** post_thetaW;
  double*** thetaE;
  int* v;
  int* w;
  int malloc_dim_1;
  int malloc_dim_2;

  fprintf(stderr, "-- This program was automatically generated using HBC (v 0.7 beta) from en2.hier\n--     see http://hal3.name/HBC for more information\n");
  fflush(stderr);
  setall(time(0),time(0));   /* initialize random number generator */


  /* variables defined with --define */
  Nc = 30;
  Nen = 20;
  alphaC = 2;
  alphaD = 2;
  alphaE = 1.0e-5;
  alphaV = 1.0e-3;
  alphaW = 1.0e-3;

  fprintf(stderr, "Loading data...\n");
  fflush(stderr);
  /* variables defined with --loadD */
  w = load_discrete1("enO", &N, &VW);
  v = load_discrete1("enV", &N, &VV);
  d = load_discrete1("enD", &N, &Nd);

  printf ("sizes %d %d %d %d\n",N,VW,VV,Nd);
  printf ("debug V %d\n",v[154623]);

  /* variables defined with --loadM or --loadMI */

  fprintf(stderr, "Allocating memory...\n");
  fflush(stderr);
  c = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));

  e = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));

  post_thetaC = (double*) malloc(sizeof(double) * (1+((Nc) + (1))-(1)));

  post_thetaD = (double*) malloc(sizeof(double) * (1+((Nd) + (1))-(1)));

  post_thetaV = (double**) malloc(sizeof(double*) * (1+(Nc)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    post_thetaV[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VV) + (1))-(1)));
  }

  post_thetaW = (double**) malloc(sizeof(double*) * (1+(Nen)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    post_thetaW[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VW) + (1))-(1)));
  }

  thetaE = (double***) malloc(sizeof(double**) * (1+(Nc)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    thetaE[malloc_dim_1-1] = (double**) malloc(sizeof(double*) * (1+(Nd)-(1)));
    for (malloc_dim_2=1; malloc_dim_2<=Nd; malloc_dim_2++) {
      thetaE[malloc_dim_1-1][malloc_dim_2-1] = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
    }
  }


  fprintf(stderr, "Initializing variables...\n");
  fflush(stderr);
  initialize_thetaE(thetaE, Nc, Nd, Nen);
  initialize_c(c, N, Nc);
  initialize_e(e, N, Nen);
  initialize_post_thetaC(post_thetaC, N, Nc, c);
  initialize_post_thetaD(post_thetaD, N, Nd, d);
  initialize_post_thetaV(post_thetaV, N, Nc, VV, c, v);
  initialize_post_thetaW(post_thetaW, N, Nen, VW, e, w);

  for (iter=1; iter<=100; iter++) {
    fprintf(stderr, "iter %d", iter);
    fflush(stderr);
    resample_thetaE(N, Nc, Nd, Nen, alphaE, c, d, e, thetaE);
    resample_c(N, alphaC, alphaV, c, d, e, post_thetaC, post_thetaV, thetaE, v, Nc, VV, Nen);
    resample_e(N, alphaW, c, d, e, post_thetaW, thetaE, w, Nen, VW);

    loglik = compute_log_posterior(N, Nc, Nd, Nen, VV, VW, alphaC, alphaD, alphaE, alphaV, alphaW, c, d, e, post_thetaC, post_thetaD, thetaE, post_thetaV, post_thetaW, v, w);
    fprintf(stderr, "\t%g", loglik);
    if ((iter==1)||(loglik>bestloglik)) {
      bestloglik = loglik;
      fprintf(stderr, " *");
      printf("\n");
      dump_c(N,c);
      dump_e(N,e);
    }
    fprintf(stderr, "\n");
    fflush(stderr);
  }

  free(w);

  free(v);

  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    for (malloc_dim_2=1; malloc_dim_2<=Nd; malloc_dim_2++) {
      free(thetaE[malloc_dim_1-1][malloc_dim_2-1]);
    }
    free(thetaE[malloc_dim_1-1]);
  }
  free(thetaE);

  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    free(post_thetaW[malloc_dim_1-1]);
  }
  free(post_thetaW);

  for (malloc_dim_1=1; malloc_dim_1<=Nc; malloc_dim_1++) {
    free(post_thetaV[malloc_dim_1-1]);
  }
  free(post_thetaV);

  free(post_thetaD);

  free(post_thetaC);

  free(e);

  free(d);

  free(c);


  return 0;
}
