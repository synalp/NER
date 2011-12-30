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

  gcc -O3 -lm stats.c samplib.c en.c -o en.out

The hierarchical model that this code reflects is:

alphaZ ~ Gam(1,1)
alphaV ~ Gam(0.1,1)
alphaW ~ Gam(0.1,1)
alphaE ~ Gam(0.1,1)

thetaZ ~ DirSym(alphaZ, NtypV)
thetaV_{k} ~ DirSym(alphaV, VV) , k \in [1,NtypV]
thetaE_{k} ~ DirSym(alphaE, Nen) , k \in [1,NtypV]
thetaW_{k} ~ DirSym(alphaW, VO) , k \in [1,Nen]

z_{n} ~ Mult(thetaZ) , n \in [1,N]
v_{n} ~ Mult(thetaV_{z_{n}}) , n \in [1,N]
e_{n} ~ Mult(thetaE_{z_{n}}) , n \in [1,N]
w_{n} ~ Mult(thetaW_{e_{n}}) , n \in [1,N]

--# --define NtypV 3
--# --define Nen 2
--# --define alphaZ 0.1
--# --define alphaV 0.1
--# --define alphaW 0.1

--# --loadD enV v VV N ;
--# --loadD enO w VO N ;

--# --collapse thetaZ
--# --collapse thetaV
--# --collapse thetaE
--# --collapse thetaW


Generated using the command:

  hbc compile en.hier en.c
*/
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "stats.h"


/**************************** SAMPLING ****************************/

void resample_post_thetaZ(int N, int NtypV, double* post_thetaZ, int* z) {
  double* tmpSP7;
  int n_4;
  int dvv_loop_var_1;
  tmpSP7 = (double*) malloc(sizeof(double) * (1+((NtypV) + (1))-(1)));
  /* Implements direct sampling from the following distribution: */
  /*   Delta(post_thetaZ | \sum_{n@4 \in [N]} IDR(z_{n@4}, 1, NtypV), NtypV) */
  for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
    tmpSP7[dvv_loop_var_1-1] = 0.0;
  }
  tmpSP7[(NtypV) + (1)-1] = (0.0) * (((1) + (NtypV)) - (1));
  for (n_4=1; n_4<=N; n_4++) {
    tmpSP7[(NtypV) + (1)-1] += 1.0;
    tmpSP7[z[n_4-1]-1] += 1.0;
  }
  sample_Delta(post_thetaZ, tmpSP7, NtypV);
  free(tmpSP7);
}

void resample_post_thetaV(int N, int NtypV, int VV, double** post_thetaV, int* v, int* z) {
  int k_17;
  double* tmpSP8;
  int n_5;
  int dvv_loop_var_1;
  tmpSP8 = (double*) malloc(sizeof(double) * (1+((VV) + (1))-(1)));
  for (k_17=1; k_17<=NtypV; k_17++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaV_{k@17} | \sum_{n@5 \in [N]} .*(=(k@17, z_{n@5}), IDR(v_{n@5}, 1, VV)), VV) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VV; dvv_loop_var_1++) {
      tmpSP8[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP8[(VV) + (1)-1] = (0.0) * (((1) + (VV)) - (1));
    for (n_5=1; n_5<=N; n_5++) {
      tmpSP8[(VV) + (1)-1] += (1.0) * ((((k_17) == (z[n_5-1])) ? 1 : 0));
      tmpSP8[v[n_5-1]-1] += (1.0) * ((((k_17) == (z[n_5-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaV[k_17-1], tmpSP8, VV);
  }
  free(tmpSP8);
}

void resample_post_thetaE(int N, int Nen, int NtypV, int* e, double** post_thetaE, int* z) {
  int k_18;
  double* tmpSP9;
  int n_6;
  int dvv_loop_var_1;
  tmpSP9 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (k_18=1; k_18<=NtypV; k_18++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaE_{k@18} | \sum_{n@6 \in [N]} .*(=(k@18, z_{n@6}), IDR(e_{n@6}, 1, Nen)), Nen) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmpSP9[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP9[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (n_6=1; n_6<=N; n_6++) {
      tmpSP9[(Nen) + (1)-1] += (1.0) * ((((k_18) == (z[n_6-1])) ? 1 : 0));
      tmpSP9[e[n_6-1]-1] += (1.0) * ((((k_18) == (z[n_6-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaE[k_18-1], tmpSP9, Nen);
  }
  free(tmpSP9);
}

void resample_post_thetaW(int N, int Nen, int VO, int* e, double** post_thetaW, int* w) {
  int k_19;
  double* tmpSP10;
  int n_7;
  int dvv_loop_var_1;
  tmpSP10 = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  for (k_19=1; k_19<=Nen; k_19++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaW_{k@19} | \sum_{n@7 \in [N]} .*(=(k@19, e_{n@7}), IDR(w_{n@7}, 1, VO)), VO) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
      tmpSP10[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP10[(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
    for (n_7=1; n_7<=N; n_7++) {
      tmpSP10[(VO) + (1)-1] += (1.0) * ((((k_19) == (e[n_7-1])) ? 1 : 0));
      tmpSP10[w[n_7-1]-1] += (1.0) * ((((k_19) == (e[n_7-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaW[k_19-1], tmpSP10, VO);
  }
  free(tmpSP10);
}

double resample_alphaZ(int NtypV, double alphaZ, double* post_thetaZ) {
  double tmpSP0;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaZ | 1, /(1.0, -(1.0, /(1.0, \sum_{cgds \in [NtypV]} log(.*(/(1.0, sub(.+(alphaZ, post_thetaZ), +(NtypV, 1))), .+(alphaZ, post_thetaZ_{cgds}))))))) */
  tmpSP0 = 0.0;
  for (cgds=1; cgds<=NtypV; cgds++) {
    tmpSP0 += log(((1.0) / ((alphaZ) + (post_thetaZ[(NtypV) + (1)-1]))) * ((alphaZ) + (post_thetaZ[cgds-1])));
  }
  alphaZ = sample_Gam(1, (1.0) / ((1.0) - ((1.0) / (tmpSP0))));
  return (alphaZ);
}

double resample_alphaV(int NtypV, int VV, double alphaV, double** post_thetaV) {
  double tmpSP1;
  int k_1;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaV | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@1 \in [NtypV]} \sum_{cgds \in [VV]} log(.*(/(1.0, sub(.+(alphaV, post_thetaV_{k@1}), +(VV, 1))), .+(alphaV, post_thetaV_{k@1,cgds}))))))) */
  tmpSP1 = 0.0;
  for (k_1=1; k_1<=NtypV; k_1++) {
    for (cgds=1; cgds<=VV; cgds++) {
      tmpSP1 += log(((1.0) / ((alphaV) + (post_thetaV[k_1-1][(VV) + (1)-1]))) * ((alphaV) + (post_thetaV[k_1-1][cgds-1])));
    }
  }
  alphaV = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP1))));
  return (alphaV);
}

double resample_alphaW(int Nen, int VO, double alphaW, double** post_thetaW) {
  double tmpSP3;
  int k_2;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaW | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@2 \in [Nen]} \sum_{cgds \in [VO]} log(.*(/(1.0, sub(.+(alphaW, post_thetaW_{k@2}), +(VO, 1))), .+(alphaW, post_thetaW_{k@2,cgds}))))))) */
  tmpSP3 = 0.0;
  for (k_2=1; k_2<=Nen; k_2++) {
    for (cgds=1; cgds<=VO; cgds++) {
      tmpSP3 += log(((1.0) / ((alphaW) + (post_thetaW[k_2-1][(VO) + (1)-1]))) * ((alphaW) + (post_thetaW[k_2-1][cgds-1])));
    }
  }
  alphaW = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP3))));
  return (alphaW);
}

double alphaE0 = 1;

double resample_alphaE(int Nen, int NtypV, double alphaE, double** post_thetaE) {
  double tmpSP5;
  int k_3;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaE | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@3 \in [NtypV]} \sum_{cgds \in [Nen]} log(.*(/(1.0, sub(.+(alphaE, post_thetaE_{k@3}), +(Nen, 1))), .+(alphaE, post_thetaE_{k@3,cgds}))))))) */
  tmpSP5 = 0.0;
  for (k_3=1; k_3<=NtypV; k_3++) {
    for (cgds=1; cgds<=Nen; cgds++) {
      tmpSP5 += log(((1.0) / ((alphaE) + (post_thetaE[k_3-1][(Nen) + (1)-1]))) * ((alphaE) + (post_thetaE[k_3-1][cgds-1])));
    }
  }
  alphaE = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP5))));
  return (alphaE);
}

void resample_z(int N, double alphaE, double alphaV, double alphaZ, int* e, double** post_thetaE, double** post_thetaV, double* post_thetaZ, int* v, int* z, int NtypV, int Nen, int VV) {
  int n_20;
  double* tmp_post_z_1;
  int tmp_idx_z_1;
  int dvv_loop_var_1;
  tmp_post_z_1 = (double*) malloc(sizeof(double) * (1+((NtypV) + (1))-(1)));
  for (n_20=1; n_20<=N; n_20++) {

    // enleve l'exemple n_20 des comptes
    post_thetaE[z[n_20-1]-1][(Nen) + (1)-1] += (0.0) - ((1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0)));
    post_thetaE[z[n_20-1]-1][e[n_20-1]-1] += (0.0) - ((1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0)));
    post_thetaV[z[n_20-1]-1][(VV) + (1)-1] += (0.0) - ((1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0)));
    post_thetaV[z[n_20-1]-1][v[n_20-1]-1] += (0.0) - ((1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0)));
    post_thetaZ[(NtypV) + (1)-1] += (0.0) - (1.0);
    post_thetaZ[z[n_20-1]-1] += (0.0) - (1.0);
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(v_{n@20} | .+(alphaV, sub(post_thetaV, z_{n@20}))))((Mult(e_{n@20} | .+(alphaE, sub(post_thetaE, z_{n@20}))))(Mult(z_{n@20} | .+(alphaZ, post_thetaZ)))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
      tmp_post_z_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_z_1[(NtypV) + (1)-1] = (0.0) * (((1) + (NtypV)) - (1));
    for (tmp_idx_z_1=1; tmp_idx_z_1<=NtypV; tmp_idx_z_1++) {
      double alphaEE;
      if (tmp_idx_z_1==1) alphaEE = alphaE0;
      else alphaEE = alphaE;
      tmp_post_z_1[tmp_idx_z_1-1] = (ldf_Mult_smooth(0, alphaV, v[n_20-1], post_thetaV[tmp_idx_z_1-1], 1, VV)) + ((ldf_Mult_smooth(0, alphaEE, e[n_20-1], post_thetaE[tmp_idx_z_1-1], 1, Nen)) + (ldf_Mult_smooth(0, alphaZ, tmp_idx_z_1, post_thetaZ, 1, NtypV)));
    }
    normalizeLog(tmp_post_z_1, 1, NtypV);
    z[n_20-1] = sample_Mult(tmp_post_z_1, 1, NtypV);

    // rajoute le nouvel exemple n_20 qui vient d'etre sample
    post_thetaZ[(NtypV) + (1)-1] += 1.0;
    post_thetaZ[z[n_20-1]-1] += 1.0;
    post_thetaV[z[n_20-1]-1][(VV) + (1)-1] += (1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0));
    post_thetaV[z[n_20-1]-1][v[n_20-1]-1] += (1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0));
    post_thetaE[z[n_20-1]-1][(Nen) + (1)-1] += (1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0));
    post_thetaE[z[n_20-1]-1][e[n_20-1]-1] += (1.0) * ((((z[n_20-1]) == (z[n_20-1])) ? 1 : 0));
  }
  free(tmp_post_z_1);
}

void resample_v(int N, double alphaV, double** post_thetaV, int* v, int* z, int VV) {
  int n_21;
  for (n_21=1; n_21<=N; n_21++) {
    post_thetaV[z[n_21-1]-1][(VV) + (1)-1] += (0.0) - ((1.0) * ((((z[n_21-1]) == (z[n_21-1])) ? 1 : 0)));
    post_thetaV[z[n_21-1]-1][v[n_21-1]-1] += (0.0) - ((1.0) * ((((z[n_21-1]) == (z[n_21-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(v_{n@21} | .+(alphaV, sub(post_thetaV, z_{n@21}))) */
    v[n_21-1] = sample_Mult_smooth(alphaV, post_thetaV[z[n_21-1]-1], 1, VV);
    post_thetaV[z[n_21-1]-1][(VV) + (1)-1] += (1.0) * ((((z[n_21-1]) == (z[n_21-1])) ? 1 : 0));
    post_thetaV[z[n_21-1]-1][v[n_21-1]-1] += (1.0) * ((((z[n_21-1]) == (z[n_21-1])) ? 1 : 0));
  }
}

void resample_e(int N, double alphaE, double alphaW, int* e, double** post_thetaE, double** post_thetaW, int* w, int* z, int Nen, int VO) {
  int n_22;
  double* tmp_post_e_1;
  int tmp_idx_e_1;
  int dvv_loop_var_1;
  tmp_post_e_1 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (n_22=1; n_22<=N; n_22++) {
    post_thetaW[e[n_22-1]-1][(VO) + (1)-1] += (0.0) - ((1.0) * ((((e[n_22-1]) == (e[n_22-1])) ? 1 : 0)));
    post_thetaW[e[n_22-1]-1][w[n_22-1]-1] += (0.0) - ((1.0) * ((((e[n_22-1]) == (e[n_22-1])) ? 1 : 0)));
    post_thetaE[z[n_22-1]-1][(Nen) + (1)-1] += (0.0) - ((1.0) * ((((z[n_22-1]) == (z[n_22-1])) ? 1 : 0)));
    post_thetaE[z[n_22-1]-1][e[n_22-1]-1] += (0.0) - ((1.0) * ((((z[n_22-1]) == (z[n_22-1])) ? 1 : 0)));
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(w_{n@22} | .+(alphaW, sub(post_thetaW, e_{n@22}))))(Mult(e_{n@22} | .+(alphaE, sub(post_thetaE, z_{n@22})))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmp_post_e_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_e_1[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (tmp_idx_e_1=1; tmp_idx_e_1<=Nen; tmp_idx_e_1++) {
      double alphaEE;
      if (z[n_22-1]==1) alphaEE = alphaE0;
      else alphaEE = alphaE;
      tmp_post_e_1[tmp_idx_e_1-1] = (ldf_Mult_smooth(0, alphaW, w[n_22-1], post_thetaW[tmp_idx_e_1-1], 1, VO)) + (ldf_Mult_smooth(0, alphaEE, tmp_idx_e_1, post_thetaE[z[n_22-1]-1], 1, Nen));
    }
    normalizeLog(tmp_post_e_1, 1, Nen);
    e[n_22-1] = sample_Mult(tmp_post_e_1, 1, Nen);
    post_thetaE[z[n_22-1]-1][(Nen) + (1)-1] += (1.0) * ((((z[n_22-1]) == (z[n_22-1])) ? 1 : 0));
    post_thetaE[z[n_22-1]-1][e[n_22-1]-1] += (1.0) * ((((z[n_22-1]) == (z[n_22-1])) ? 1 : 0));
    post_thetaW[e[n_22-1]-1][(VO) + (1)-1] += (1.0) * ((((e[n_22-1]) == (e[n_22-1])) ? 1 : 0));
    post_thetaW[e[n_22-1]-1][w[n_22-1]-1] += (1.0) * ((((e[n_22-1]) == (e[n_22-1])) ? 1 : 0));
  }
  free(tmp_post_e_1);
}

void resample_w(int N, double alphaW, int* e, double** post_thetaW, int* w, int VO) {
  int n_23;
  for (n_23=1; n_23<=N; n_23++) {
    post_thetaW[e[n_23-1]-1][(VO) + (1)-1] += (0.0) - ((1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0)));
    post_thetaW[e[n_23-1]-1][w[n_23-1]-1] += (0.0) - ((1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(w_{n@23} | .+(alphaW, sub(post_thetaW, e_{n@23}))) */
    w[n_23-1] = sample_Mult_smooth(alphaW, post_thetaW[e[n_23-1]-1], 1, VO);
    post_thetaW[e[n_23-1]-1][(VO) + (1)-1] += (1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0));
    post_thetaW[e[n_23-1]-1][w[n_23-1]-1] += (1.0) * ((((e[n_23-1]) == (e[n_23-1])) ? 1 : 0));
  }
}


/************************* INITIALIZATION *************************/

double initialize_alphaZ() {
  double alphaZ;
  alphaZ = sample_Gam(1.0, 1.0);
  return (alphaZ);
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

double initialize_alphaE() {
  double alphaE;
  alphaE = sample_Gam(1.0, 1.0);
  return (alphaE);
}

void initialize_z(int* z, int N, int NtypV) {
  int n_20;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    z[dvv_loop_var_1-1] = 0;
  }
  z[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_20=1; n_20<=N; n_20++) {
    // un Z tire au hasard entre 1 et NtypV
    z[n_20-1] = sample_MultSym(1, NtypV);
  }
}

void initialize_v(int* v, int N, int VV) {
  int n_21;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    v[dvv_loop_var_1-1] = 0;
  }
  v[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_21=1; n_21<=N; n_21++) {
    v[n_21-1] = sample_MultSym(1, VV);
  }
}

void initialize_e(int* e, int N, int Nen) {
  int n_22;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    e[dvv_loop_var_1-1] = 0;
  }
  e[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_22=1; n_22<=N; n_22++) {
    e[n_22-1] = sample_MultSym(1, Nen);
  }
}

void initialize_w(int* w, int N, int VO) {
  int n_23;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    w[dvv_loop_var_1-1] = 0;
  }
  w[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_23=1; n_23<=N; n_23++) {
    w[n_23-1] = sample_MultSym(1, VO);
  }
}

void initialize_post_thetaZ(double* post_thetaZ, int N, int NtypV, int* z) {
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
    post_thetaZ[dvv_loop_var_1-1] = 0.0;
  }
  post_thetaZ[(NtypV) + (1)-1] = (0.0) * (((1) + (NtypV)) - (1));
  resample_post_thetaZ(N, NtypV, post_thetaZ, z);
}

void initialize_post_thetaV(double** post_thetaV, int N, int NtypV, int VV, int* v, int* z) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VV; dvv_loop_var_2++) {
      post_thetaV[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaV[dvv_loop_var_1-1][(VV) + (1)-1] = (0.0) * (((1) + (VV)) - (1));
  }
  resample_post_thetaV(N, NtypV, VV, post_thetaV, v, z);
}

void initialize_post_thetaE(double** post_thetaE, int N, int Nen, int NtypV, int* e, int* z) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nen; dvv_loop_var_2++) {
      post_thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaE[dvv_loop_var_1-1][(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
  }
  resample_post_thetaE(N, Nen, NtypV, e, post_thetaE, z);
}

void initialize_post_thetaW(double** post_thetaW, int N, int Nen, int VO, int* e, int* w) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VO; dvv_loop_var_2++) {
      post_thetaW[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaW[dvv_loop_var_1-1][(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
  }
  resample_post_thetaW(N, Nen, VO, e, post_thetaW, w);
}


/**************************** DUMPING *****************************/

void dump_alphaZ(double alphaZ) {
  printf("alphaZ = ");
  printf("%g", alphaZ);
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

void dump_alphaE(double alphaE) {
  printf("alphaE = ");
  printf("%g", alphaE);
  printf("\n");
}

void dump_thetaZ(int NtypV, double* thetaZ) {
  int dvv_loop_var_1;
  printf("thetaZ = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
    printf("%g", thetaZ[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_thetaV(int NtypV, int VV, double** thetaV) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaV = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VV; dvv_loop_var_2++) {
      printf("%g", thetaV[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
  }
  printf("\n");
}

void dump_thetaE(int Nen, int NtypV, double** thetaE) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaE = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=NtypV; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nen; dvv_loop_var_2++) {
      printf("%g", thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
  }
  printf("\n");
}

void dump_thetaW(int Nen, int VO, double** thetaW) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaW = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VO; dvv_loop_var_2++) {
      printf("%g", thetaW[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
  }
  printf("\n");
}

void dump_z(int N, int* z) {
  int dvv_loop_var_1;
printf("\n");
  printf("z = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", z[dvv_loop_var_1-1]);
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

void dump_e(int N, int* e) {
  int dvv_loop_var_1;
  printf("e = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", e[dvv_loop_var_1-1]);
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

double compute_log_posterior(int N, int Nen, int NtypV, int VO, int VV, double alphaE, double alphaV, double alphaW, double alphaZ, int* e, double** thetaE, double** thetaV, double** thetaW, double* thetaZ, int* v, int* w, int* z) {
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
      double alphaEE;
      if (z[n_20-1]==1) alphaEE = alphaE0;
      else alphaEE = alphaE;
    ldfP8_0 += ldf_Mult(1, z[n_20-1], thetaZ, 1, NtypV)+ ldf_Gam(1, alphaEE, 0.1, 1)/(double)N;
  }
  ldfP9_0 = 0.0;
  for (n_21=1; n_21<=N; n_21++) {
    ldfP9_0 += ldf_Mult(1, v[n_21-1], thetaV[z[n_21-1]-1], 1, VV);
  }
  ldfP10_0 = 0.0;
  for (n_22=1; n_22<=N; n_22++) {
    ldfP10_0 += ldf_Mult(1, e[n_22-1], thetaE[z[n_22-1]-1], 1, Nen);
  }
  ldfP11_0 = 0.0;
  for (n_23=1; n_23<=N; n_23++) {
    ldfP11_0 += ldf_Mult(1, w[n_23-1], thetaW[e[n_23-1]-1], 1, VO);
  }
  return ldf_Gam(1, alphaZ, 1, 1) + ldf_Gam(1, alphaV, 0.1, 1) + ldf_Gam(1, alphaW, 0.1, 1) + ldfP8_0 + ldfP9_0 + ldfP10_0 + ldfP11_0;
}

/****************************** MAIN ******************************/

int main(int ARGC, char *ARGV[]) {
  double loglik,bestloglik;
  int iter;
  int N;
  int Nen;
  int NtypV;
  int VO;
  int VV;
  double alphaE;
  double alphaV;
  double alphaW;
  double alphaZ;
  int* e;
  double** post_thetaE;
  double** post_thetaV;
  double** post_thetaW;
  double* post_thetaZ;
  int* v;
  int* w;
  int* z;
  int malloc_dim_1;

  fprintf(stderr, "-- This program was automatically generated using HBC (v 0.7 beta) from en.hier\n--     see http://hal3.name/HBC for more information\n");
  fflush(stderr);
  setall(time(0),time(0));   /* initialize random number generator */


  /* variables defined with --define */
  NtypV = 10;
  Nen = 7;
  alphaZ = 1;
  alphaV = 0.1;
  alphaW = 0.1;

  // detson rajoute le meme alphaE
  alphaE = 0.1;

  fprintf(stderr, "Loading data...\n");
  fflush(stderr);
  /* variables defined with --loadD */
  v = load_discrete1("enV", &N, &VV);
  w = load_discrete1("enO", &N, &VO);

  /* variables defined with --loadM or --loadMI */

  fprintf(stderr, "Allocating memory...\n");
  fflush(stderr);
  e = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));

  post_thetaE = (double**) malloc(sizeof(double*) * (1+(NtypV)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=NtypV; malloc_dim_1++) {
    post_thetaE[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  }

  post_thetaV = (double**) malloc(sizeof(double*) * (1+(NtypV)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=NtypV; malloc_dim_1++) {
    post_thetaV[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VV) + (1))-(1)));
  }

  post_thetaW = (double**) malloc(sizeof(double*) * (1+(Nen)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    post_thetaW[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  }

  post_thetaZ = (double*) malloc(sizeof(double) * (1+((NtypV) + (1))-(1)));

  z = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));


  fprintf(stderr, "Initializing variables...\n");
  fflush(stderr);
//  alphaE = initialize_alphaE();
  initialize_z(z, N, NtypV);
  initialize_e(e, N, Nen);
  initialize_post_thetaZ(post_thetaZ, N, NtypV, z);
  initialize_post_thetaV(post_thetaV, N, NtypV, VV, v, z);
  initialize_post_thetaE(post_thetaE, N, Nen, NtypV, e, z);
  initialize_post_thetaW(post_thetaW, N, Nen, VO, e, w);

  for (iter=1; iter<=1000; iter++) {
    fprintf(stderr, "iter %d\n", iter);
    fflush(stderr);
//    alphaE = resample_alphaE(Nen, NtypV, alphaE, post_thetaE);
    resample_z(N, alphaE, alphaV, alphaZ, e, post_thetaE, post_thetaV, post_thetaZ, v, z, NtypV, Nen, VV);
    resample_e(N, alphaE, alphaW, e, post_thetaE, post_thetaW, w, z, Nen, VO);

if (iter>=30) {
    dump_z(N,z);
    dump_e(N,e);
}

    loglik = compute_log_posterior(N, Nen, NtypV, VO, VV, alphaE, alphaV, alphaW, alphaZ, e, post_thetaE, post_thetaV, post_thetaW, post_thetaZ, v, w, z);
    fprintf(stderr, "\t%g", loglik);
    if ((iter==1)||(loglik>bestloglik)) {
      bestloglik = loglik;
      fprintf(stderr, " *");
    }
    fprintf(stderr, "\n");
    fflush(stderr);
  }

  free(z);

  free(w);

  free(v);

  free(post_thetaZ);

  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    free(post_thetaW[malloc_dim_1-1]);
  }
  free(post_thetaW);

  for (malloc_dim_1=1; malloc_dim_1<=NtypV; malloc_dim_1++) {
    free(post_thetaV[malloc_dim_1-1]);
  }
  free(post_thetaV);

  for (malloc_dim_1=1; malloc_dim_1<=NtypV; malloc_dim_1++) {
    free(post_thetaE[malloc_dim_1-1]);
  }
  free(post_thetaE);

  free(e);


  return 0;
}
