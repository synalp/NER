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

alphaH ~ Gam(0.1,1)
alphaW ~ Gam(0.1,1)
alphaE ~ Gam(1,1)

thetaW ~ DirSym(alphaW, VO)
thetaE_{k} ~ DirSym(alphaE, Nen) , k \in [1,VO]
thetaH_{k} ~ DirSym(alphaH, VH) , k \in [1,Nen]

w_{n} ~ Mult(thetaW) , n \in [1,N]
e_{n} ~ Mult(thetaE_{w_{n}}) , n \in [1,N]
h_{n} ~ Mult(thetaH_{e_{n}}) , n \in [1,N]

--# --define Nen 3
--# --define alphaH 0.1
--# --define alphaE 0.1
--# --define alphaW 0.1

--# --loadD enV h VH N ;
--# --loadD enO w VO N ;

--# --collapse thetaH
--# --collapse thetaE
--# --collapse thetaW


Generated using the command:

  hbc compile en.hier en.c
*/
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "stats.h"

int *gold;

/**************************** SAMPLING ****************************/

void resample_post_thetaH(int N, int Nen, int VH, int* e, int* h, double** post_thetaH) {
  int k_14;
  double* tmpSP7;
  int n_5;
  int dvv_loop_var_1;
  tmpSP7 = (double*) malloc(sizeof(double) * (1+((VH) + (1))-(1)));
  for (k_14=1; k_14<=Nen; k_14++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaH_{k@14} | \sum_{n@5 \in [N]} .*(=(k@14, e_{n@5}), IDR(h_{n@5}, 1, VH)), VH) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VH; dvv_loop_var_1++) {
      tmpSP7[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP7[(VH) + (1)-1] = (0.0) * (((1) + (VH)) - (1));
    for (n_5=1; n_5<=N; n_5++) {
      tmpSP7[(VH) + (1)-1] += (1.0) * ((((k_14) == (e[n_5-1])) ? 1 : 0));
      tmpSP7[h[n_5-1]-1] += (1.0) * ((((k_14) == (e[n_5-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaH[k_14-1], tmpSP7, VH);
  }
  free(tmpSP7);
}

void resample_post_thetaE(int N, int Nen, int VO, int* e, double** post_thetaE, int* w) {
  int k_13;
  double* tmpSP6;
  int n_4;
  int dvv_loop_var_1;
  tmpSP6 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (k_13=1; k_13<=VO; k_13++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaE_{k@13} | \sum_{n@4 \in [N]} .*(=(k@13, w_{n@4}), IDR(e_{n@4}, 1, Nen)), Nen) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmpSP6[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP6[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (n_4=1; n_4<=N; n_4++) {
      tmpSP6[(Nen) + (1)-1] += (1.0) * ((((k_13) == (w[n_4-1])) ? 1 : 0));
      tmpSP6[e[n_4-1]-1] += (1.0) * ((((k_13) == (w[n_4-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaE[k_13-1], tmpSP6, Nen);
  }
  free(tmpSP6);
}

void resample_post_thetaW(int N, int VO, double* post_thetaW, int* w) {
  double* tmpSP5;
  int n_3;
  int dvv_loop_var_1;
  tmpSP5 = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  /* Implements direct sampling from the following distribution: */
  /*   Delta(post_thetaW | \sum_{n@3 \in [N]} IDR(w_{n@3}, 1, VO), VO) */
  for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
    tmpSP5[dvv_loop_var_1-1] = 0.0;
  }
  tmpSP5[(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
  for (n_3=1; n_3<=N; n_3++) {
    tmpSP5[(VO) + (1)-1] += 1.0;
    tmpSP5[w[n_3-1]-1] += 1.0;
  }
  sample_Delta(post_thetaW, tmpSP5, VO);
  free(tmpSP5);
}

double resample_alphaH(int Nen, int VH, double alphaH, double** post_thetaH) {
  double tmpSP0;
  int k_0;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaH | 0.1, /(1.0, -(1.0, /(1.0, \sum_{k@0 \in [Nen]} \sum_{cgds \in [VH]} log(.*(/(1.0, sub(.+(alphaH, post_thetaH_{k@0}), +(VH, 1))), .+(alphaH, post_thetaH_{k@0,cgds}))))))) */
  tmpSP0 = 0.0;
  for (k_0=1; k_0<=Nen; k_0++) {
    for (cgds=1; cgds<=VH; cgds++) {
      tmpSP0 += log(((1.0) / ((alphaH) + (post_thetaH[k_0-1][(VH) + (1)-1]))) * ((alphaH) + (post_thetaH[k_0-1][cgds-1])));
    }
  }
  alphaH = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP0))));
  return (alphaH);
}

double resample_alphaW(int VO, double alphaW, double* post_thetaW) {
  double tmpSP2;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaW | 0.1, /(1.0, -(1.0, /(1.0, \sum_{cgds \in [VO]} log(.*(/(1.0, sub(.+(alphaW, post_thetaW), +(VO, 1))), .+(alphaW, post_thetaW_{cgds}))))))) */
  tmpSP2 = 0.0;
  for (cgds=1; cgds<=VO; cgds++) {
    tmpSP2 += log(((1.0) / ((alphaW) + (post_thetaW[(VO) + (1)-1]))) * ((alphaW) + (post_thetaW[cgds-1])));
  }
  alphaW = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / (tmpSP2))));
  return (alphaW);
}

double resample_alphaE(int Nen, int VO, double alphaE, double** post_thetaE) {
  double tmpSP3;
  int k_2;
  int cgds;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaE | 1, /(1.0, -(1.0, /(1.0, \sum_{k@2 \in [VO]} \sum_{cgds \in [Nen]} log(.*(/(1.0, sub(.+(alphaE, post_thetaE_{k@2}), +(Nen, 1))), .+(alphaE, post_thetaE_{k@2,cgds}))))))) */
  tmpSP3 = 0.0;
  for (k_2=1; k_2<=VO; k_2++) {
    for (cgds=1; cgds<=Nen; cgds++) {
      tmpSP3 += log(((1.0) / ((alphaE) + (post_thetaE[k_2-1][(Nen) + (1)-1]))) * ((alphaE) + (post_thetaE[k_2-1][cgds-1])));
    }
  }
  alphaE = sample_Gam(1, (1.0) / ((1.0) - ((1.0) / (tmpSP3))));
  return (alphaE);
}

void resample_w(int N, double alphaE, double alphaW, int* e, double** post_thetaE, double* post_thetaW, int* w, int VO, int Nen) {
  int n_15;
  double* tmp_post_w_1;
  int tmp_idx_w_1;
  int dvv_loop_var_1;
  tmp_post_w_1 = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  for (n_15=1; n_15<=N; n_15++) {
    post_thetaW[(VO) + (1)-1] += (0.0) - (1.0);
    post_thetaW[w[n_15-1]-1] += (0.0) - (1.0);
    post_thetaE[w[n_15-1]-1][(Nen) + (1)-1] += (0.0) - ((1.0) * ((((w[n_15-1]) == (w[n_15-1])) ? 1 : 0)));
    post_thetaE[w[n_15-1]-1][e[n_15-1]-1] += (0.0) - ((1.0) * ((((w[n_15-1]) == (w[n_15-1])) ? 1 : 0)));
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(e_{n@15} | .+(alphaE, sub(post_thetaE, w_{n@15}))))(Mult(w_{n@15} | .+(alphaW, post_thetaW))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
      tmp_post_w_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_w_1[(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
    for (tmp_idx_w_1=1; tmp_idx_w_1<=VO; tmp_idx_w_1++) {
      tmp_post_w_1[tmp_idx_w_1-1] = (ldf_Mult_smooth(0, alphaE, e[n_15-1], post_thetaE[tmp_idx_w_1-1], 1, Nen)) + (ldf_Mult_smooth(0, alphaW, tmp_idx_w_1, post_thetaW, 1, VO));
    }
    normalizeLog(tmp_post_w_1, 1, VO);
    w[n_15-1] = sample_Mult(tmp_post_w_1, 1, VO);
    post_thetaE[w[n_15-1]-1][(Nen) + (1)-1] += (1.0) * ((((w[n_15-1]) == (w[n_15-1])) ? 1 : 0));
    post_thetaE[w[n_15-1]-1][e[n_15-1]-1] += (1.0) * ((((w[n_15-1]) == (w[n_15-1])) ? 1 : 0));
    post_thetaW[(VO) + (1)-1] += 1.0;
    post_thetaW[w[n_15-1]-1] += 1.0;
  }
  free(tmp_post_w_1);
}

void resample_e(int N, double alphaE, double alphaH, int* e, int* h, double** post_thetaE, double** post_thetaH, int* w, int Nen, int VH) {
  int n_16;
  double* tmp_post_e_1;
  int tmp_idx_e_1;
  int dvv_loop_var_1;
  tmp_post_e_1 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (n_16=1; n_16<=N; n_16++) {
    post_thetaE[w[n_16-1]-1][(Nen) + (1)-1] += (0.0) - ((1.0) * ((((w[n_16-1]) == (w[n_16-1])) ? 1 : 0)));
    post_thetaE[w[n_16-1]-1][e[n_16-1]-1] += (0.0) - ((1.0) * ((((w[n_16-1]) == (w[n_16-1])) ? 1 : 0)));
    post_thetaH[e[n_16-1]-1][(VH) + (1)-1] += (0.0) - ((1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0)));
    post_thetaH[e[n_16-1]-1][h[n_16-1]-1] += (0.0) - ((1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0)));

    if (gold[n_16-1]>0) e[n_16-1]=gold[n_16-1];
    else {
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(h_{n@16} | .+(alphaH, sub(post_thetaH, e_{n@16}))))(Mult(e_{n@16} | .+(alphaE, sub(post_thetaE, w_{n@16})))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmp_post_e_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_e_1[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (tmp_idx_e_1=1; tmp_idx_e_1<=Nen; tmp_idx_e_1++) {
      tmp_post_e_1[tmp_idx_e_1-1] = (ldf_Mult_smooth(0, alphaH, h[n_16-1], post_thetaH[tmp_idx_e_1-1], 1, VH)) + (ldf_Mult_smooth(0, alphaE, tmp_idx_e_1, post_thetaE[w[n_16-1]-1], 1, Nen));
    }
    normalizeLog(tmp_post_e_1, 1, Nen);
    e[n_16-1] = sample_Mult(tmp_post_e_1, 1, Nen);
    }

    post_thetaH[e[n_16-1]-1][(VH) + (1)-1] += (1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0));
    post_thetaH[e[n_16-1]-1][h[n_16-1]-1] += (1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0));
    post_thetaE[w[n_16-1]-1][(Nen) + (1)-1] += (1.0) * ((((w[n_16-1]) == (w[n_16-1])) ? 1 : 0));
    post_thetaE[w[n_16-1]-1][e[n_16-1]-1] += (1.0) * ((((w[n_16-1]) == (w[n_16-1])) ? 1 : 0));
  }
  free(tmp_post_e_1);
}

void resample_h(int N, double alphaH, int* e, int* h, double** post_thetaH, int VH) {
  int n_17;
  for (n_17=1; n_17<=N; n_17++) {
    post_thetaH[e[n_17-1]-1][(VH) + (1)-1] += (0.0) - ((1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0)));
    post_thetaH[e[n_17-1]-1][h[n_17-1]-1] += (0.0) - ((1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(h_{n@17} | .+(alphaH, sub(post_thetaH, e_{n@17}))) */
    h[n_17-1] = sample_Mult_smooth(alphaH, post_thetaH[e[n_17-1]-1], 1, VH);
    post_thetaH[e[n_17-1]-1][(VH) + (1)-1] += (1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0));
    post_thetaH[e[n_17-1]-1][h[n_17-1]-1] += (1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0));
  }
}


/************************* INITIALIZATION *************************/

double initialize_alphaH() {
  double alphaH;
  alphaH = sample_Gam(1.0, 1.0);
  return (alphaH);
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

void initialize_w(int* w, int N, int VO) {
  int n_15;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    w[dvv_loop_var_1-1] = 0;
  }
  w[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_15=1; n_15<=N; n_15++) {
    w[n_15-1] = sample_MultSym(1, VO);
  }
}

void initialize_e(int* e, int N, int Nen) {
  int n_16;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    e[dvv_loop_var_1-1] = 0;
  }
  e[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_16=1; n_16<=N; n_16++) {
    e[n_16-1] = sample_MultSym(1, Nen);
  }
}

void initialize_h(int* h, int N, int VH) {
  int n_17;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    h[dvv_loop_var_1-1] = 0;
  }
  h[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_17=1; n_17<=N; n_17++) {
    h[n_17-1] = sample_MultSym(1, VH);
  }
}

void initialize_post_thetaH(double** post_thetaH, int N, int Nen, int VH, int* e, int* h) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VH; dvv_loop_var_2++) {
      post_thetaH[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaH[dvv_loop_var_1-1][(VH) + (1)-1] = (0.0) * (((1) + (VH)) - (1));
  }
  resample_post_thetaH(N, Nen, VH, e, h, post_thetaH);
}

void initialize_post_thetaE(double** post_thetaE, int N, int Nen, int VO, int* e, int* w) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nen; dvv_loop_var_2++) {
      post_thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaE[dvv_loop_var_1-1][(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
  }
  resample_post_thetaE(N, Nen, VO, e, post_thetaE, w);
}

void initialize_post_thetaW(double* post_thetaW, int N, int VO, int* w) {
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
    post_thetaW[dvv_loop_var_1-1] = 0.0;
  }
  post_thetaW[(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
  resample_post_thetaW(N, VO, post_thetaW, w);
}


/**************************** DUMPING *****************************/

void dump_alphaH(double alphaH) {
  printf("alphaH = ");
  printf("%g", alphaH);
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

void dump_thetaW(int VO, double* thetaW) {
  int dvv_loop_var_1;
  printf("thetaW = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
    printf("%g", thetaW[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_thetaE(int Nen, int VO, double** thetaE) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaE = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=Nen; dvv_loop_var_2++) {
      printf("%g", thetaE[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
  }
  printf("\n");
}

void dump_thetaH(int Nen, int VH, double** thetaH) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaH = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VH; dvv_loop_var_2++) {
      printf("%g", thetaH[dvv_loop_var_1-1][dvv_loop_var_2-1]);
      printf(" ");
    }
    printf(" ; ");
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

void dump_e(int N, int* e) {
  int dvv_loop_var_1;
  printf("e = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", e[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_h(int N, int* h) {
  int dvv_loop_var_1;
  printf("h = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    printf("%d", h[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}


/*************************** LIKELIHOOD ***************************/

double compute_log_posterior(int N, int Nen, int VH, int VO, double alphaE, double alphaH, double alphaW, int* e, int* h, double** thetaE, double** thetaH, double* thetaW, int* w) {
  double ldfP6_0;
  int n_15;
  double ldfP7_0;
  int n_16;
  double ldfP8_0;
  int n_17;
  ldfP6_0 = 0.0;
  for (n_15=1; n_15<=N; n_15++) {
    ldfP6_0 += ldf_Mult(1, w[n_15-1], thetaW, 1, VO);
  }
  ldfP7_0 = 0.0;
  for (n_16=1; n_16<=N; n_16++) {
    ldfP7_0 += ldf_Mult(1, e[n_16-1], thetaE[w[n_16-1]-1], 1, Nen);
  }
  ldfP8_0 = 0.0;
  for (n_17=1; n_17<=N; n_17++) {
    ldfP8_0 += ldf_Mult(1, h[n_17-1], thetaH[e[n_17-1]-1], 1, VH);
  }
  return ((ldf_Gam(1, alphaH, 0.1, 1)) + ((ldf_Gam(1, alphaW, 0.1, 1)) + ((ldf_Gam(1, alphaE, 1, 1)) + ((0.0) + ((0.0) + ((0.0) + ((ldfP6_0) + ((ldfP7_0) + (ldfP8_0)))))))));
}

/****************************** MAIN ******************************/

int main(int ARGC, char *ARGV[]) {
  double loglik,bestloglik;
  int iter;
  int N;
  int Nen;
  int VH;
  int VO;
  double alphaE;
  double alphaH;
  double alphaW;
  int* e;
  int* h;
  double** post_thetaE;
  double** post_thetaH;
  double* post_thetaW;
  int* w;
  int malloc_dim_1;

  fprintf(stderr, "-- This program was automatically generated using HBC (v 0.7 beta) from en.hier\n--     see http://hal3.name/HBC for more information\n");
  fflush(stderr);
  setall(time(0),time(0));   /* initialize random number generator */


  /* variables defined with --define */
  Nen = 4;
  alphaH = 0.5;
  alphaE = 0.1;
  alphaW = 0.8;

  fprintf(stderr, "Loading data...\n");
  fflush(stderr);
  /* variables defined with --loadD */
  h = load_discrete1("enV", &N, &VH);
  w = load_discrete1("enO", &N, &VO);

  /* variables defined with --loadM or --loadMI */

  fprintf(stderr, "Allocating memory...%d\n",N);
  fflush(stderr);
  e = (int*) malloc(sizeof(int) * (1+N));
  gold = (int *)malloc(sizeof(int)*N);
  if (0==0) {
	/* lecture des classes gold pour une partie du corpus */
	FILE *f = fopen("tmpgolds.txt","r");
	int tmp;
	int k=0,max=0,min=1000,first=-1;
  	for (;;) {
    		fscanf(f, "%d", &tmp);
		if (feof(f)) break;
		if (k>=N) {
			fprintf(stderr,"ROOROR ! %d %d\n",k,N);
		}
		if (tmp>max) max=tmp;
		if (tmp<min) min=tmp;
		if (tmp<0) gold[k]=-1;
		else {
			gold[k]=tmp+1; // car les classes commencent à 1 ici
			if (first<0) first=k;
		 }
    		k++;
  	}
	fclose(f);
	fprintf(stderr,"detson gold loaded %d %d %d %d %d\n",k,N,min,max,first);
	fflush(stderr);
  }

  fprintf(stderr, "debug1...%d\n",VO);
  fflush(stderr);

  post_thetaE = (double**) malloc(sizeof(double*) * (1+(VO)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=VO; malloc_dim_1++) {
    post_thetaE[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  }

  fprintf(stderr, "debug1...\n");
  fflush(stderr);

  post_thetaH = (double**) malloc(sizeof(double*) * (1+(Nen)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    post_thetaH[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VH) + (1))-(1)));
  }

  fprintf(stderr, "debug1...\n");
  fflush(stderr);

  post_thetaW = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));


  fprintf(stderr, "Initializing variables...\n");
  fflush(stderr);
  initialize_e(e, N, Nen);
  initialize_post_thetaH(post_thetaH, N, Nen, VH, e, h);
  initialize_post_thetaE(post_thetaE, N, Nen, VO, e, w);
  initialize_post_thetaW(post_thetaW, N, VO, w);

  {
	int i;
	for (i=0;i<N;i++) {
//		if (gold[i]>0) e[i+1]=gold[i];
	}
  }

  for (iter=1; iter<=200; iter++) {
    fprintf(stderr, "iter %d", iter);
    fflush(stderr);
    resample_e(N, alphaE, alphaH, e, h, post_thetaE, post_thetaH, w, Nen, VH);
    if (iter>=20) {
      printf("\n");
      dump_e(N,e);
    }

    loglik = compute_log_posterior(N, Nen, VH, VO, alphaE, alphaH, alphaW, e, h, post_thetaE, post_thetaH, post_thetaW, w);
    fprintf(stderr, "\t%g", loglik);
    if ((iter==1)||(loglik>bestloglik)) {
      bestloglik = loglik;
      fprintf(stderr, " *");
    }
    fprintf(stderr, "\n");
    fflush(stderr);
  }

  free(w);

  free(post_thetaW);

  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    free(post_thetaH[malloc_dim_1-1]);
  }
  free(post_thetaH);

  for (malloc_dim_1=1; malloc_dim_1<=VO; malloc_dim_1++) {
    free(post_thetaE[malloc_dim_1-1]);
  }
  free(post_thetaE);

  free(h);

  free(e);


  return 0;
}
