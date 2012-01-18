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

alphaE ~ Gam(1,1)
alphaH ~ Gam(0.1,1)
alphaW ~ Gam(0.1,1)

thetaE ~ DirSym(alphaH, Nen)
thetaH_{k} ~ DirSym(alphaH, VO) , k \in [1,Nen]
thetaW_{k} ~ DirSym(alphaW, VO) , k \in [1,Nen]

e_{n} ~ Mult(thetaE) , n \in [1,N]
h_{n} ~ Mult(thetaH_{e_{n}}) , n \in [1,N]
w_{n} ~ Mult(thetaW_{e_{n}}) , n \in [1,N]

--# --define Nen 3
--# --define alphaE 1
--# --define alphaH 0.1
--# --define alphaW 0.1

--# --loadD enO w VO N ;

--# --collapse thetaH
--# --collapse thetaE
--# --collapse thetaW


Generated using the command:

  hbc compile en2.hier en2.c
*/
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "stats.h"


/**************************** SAMPLING ****************************/

void resample_post_thetaH(int N, int Nen, int VO, int* e, int* h, double** post_thetaH) {
  int k_13;
  double* tmpSP6;
  int n_4;
  int dvv_loop_var_1;
  tmpSP6 = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  for (k_13=1; k_13<=Nen; k_13++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaH_{k@13} | \sum_{n@4 \in [N]} .*(=(k@13, e_{n@4}), IDR(h_{n@4}, 1, VO)), VO) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
      tmpSP6[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP6[(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
    for (n_4=1; n_4<=N; n_4++) {
      tmpSP6[(VO) + (1)-1] += (1.0) * ((((k_13) == (e[n_4-1])) ? 1 : 0));
      tmpSP6[h[n_4-1]-1] += (1.0) * ((((k_13) == (e[n_4-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaH[k_13-1], tmpSP6, VO);
  }
  free(tmpSP6);
}

void resample_post_thetaE(int N, int Nen, int* e, double* post_thetaE) {
  double* tmpSP5;
  int n_3;
  int dvv_loop_var_1;
  tmpSP5 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  /* Implements direct sampling from the following distribution: */
  /*   Delta(post_thetaE | \sum_{n@3 \in [N]} IDR(e_{n@3}, 1, Nen), Nen) */
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    tmpSP5[dvv_loop_var_1-1] = 0.0;
  }
  tmpSP5[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
  for (n_3=1; n_3<=N; n_3++) {
    tmpSP5[(Nen) + (1)-1] += 1.0;
    tmpSP5[e[n_3-1]-1] += 1.0;
  }
  sample_Delta(post_thetaE, tmpSP5, Nen);
  free(tmpSP5);
}

void resample_post_thetaW(int N, int Nen, int VO, int* e, double** post_thetaW, int* w) {
  int k_14;
  double* tmpSP7;
  int n_5;
  int dvv_loop_var_1;
  tmpSP7 = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  for (k_14=1; k_14<=Nen; k_14++) {
    /* Implements direct sampling from the following distribution: */
    /*   Delta(post_thetaW_{k@14} | \sum_{n@5 \in [N]} .*(=(k@14, e_{n@5}), IDR(w_{n@5}, 1, VO)), VO) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=VO; dvv_loop_var_1++) {
      tmpSP7[dvv_loop_var_1-1] = 0.0;
    }
    tmpSP7[(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
    for (n_5=1; n_5<=N; n_5++) {
      tmpSP7[(VO) + (1)-1] += (1.0) * ((((k_14) == (e[n_5-1])) ? 1 : 0));
      tmpSP7[w[n_5-1]-1] += (1.0) * ((((k_14) == (e[n_5-1])) ? 1 : 0));
    }
    sample_Delta(post_thetaW[k_14-1], tmpSP7, VO);
  }
  free(tmpSP7);
}

double resample_alphaE(double alphaE) {
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaE | 1, 1) */
  alphaE = sample_Gam(1, 1);
  return (alphaE);
}

double resample_alphaH(int Nen, int VO, double alphaH, double* post_thetaE, double** post_thetaH) {
  double tmpSP0;
  int k_10;
  int cgds;
  double tmpSP2;
  /* Implements direct sampling from the following distribution: */
  /*   Gam(alphaH | 0.1, /(1.0, -(1.0, /(1.0, +(\sum_{k@10 \in [Nen]} \sum_{cgds \in [VO]} log(.*(/(1.0, sub(.+(alphaH, post_thetaH_{k@10}), +(VO, 1))), .+(alphaH, post_thetaH_{k@10,cgds}))), \sum_{cgds \in [Nen]} log(.*(/(1.0, sub(.+(alphaH, post_thetaE), +(Nen, 1))), .+(alphaH, post_thetaE_{cgds})))))))) */
  tmpSP0 = 0.0;
  for (k_10=1; k_10<=Nen; k_10++) {
    for (cgds=1; cgds<=VO; cgds++) {
      tmpSP0 += log(((1.0) / ((alphaH) + (post_thetaH[k_10-1][(VO) + (1)-1]))) * ((alphaH) + (post_thetaH[k_10-1][cgds-1])));
    }
  }
  tmpSP2 = 0.0;
  for (cgds=1; cgds<=Nen; cgds++) {
    tmpSP2 += log(((1.0) / ((alphaH) + (post_thetaE[(Nen) + (1)-1]))) * ((alphaH) + (post_thetaE[cgds-1])));
  }
  alphaH = sample_Gam(0.1, (1.0) / ((1.0) - ((1.0) / ((tmpSP0) + (tmpSP2)))));
  return (alphaH);
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

void resample_e(int N, double alphaH, double alphaW, int* e, int* h, double* post_thetaE, double** post_thetaH, double** post_thetaW, int* w, int Nen, int VO) {
  int n_15;
  double* tmp_post_e_1;
  int tmp_idx_e_1;
  int dvv_loop_var_1;
  tmp_post_e_1 = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));
  for (n_15=1; n_15<=N; n_15++) {
    post_thetaW[e[n_15-1]-1][(VO) + (1)-1] += (0.0) - ((1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0)));
    post_thetaW[e[n_15-1]-1][w[n_15-1]-1] += (0.0) - ((1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0)));
    post_thetaE[(Nen) + (1)-1] += (0.0) - (1.0);
    post_thetaE[e[n_15-1]-1] += (0.0) - (1.0);
    post_thetaH[e[n_15-1]-1][(VO) + (1)-1] += (0.0) - ((1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0)));
    post_thetaH[e[n_15-1]-1][h[n_15-1]-1] += (0.0) - ((1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0)));
    /* Implements multinomial sampling from the following distribution: */
    /*   (Mult(h_{n@15} | .+(alphaH, sub(post_thetaH, e_{n@15}))))((Mult(w_{n@15} | .+(alphaW, sub(post_thetaW, e_{n@15}))))(Mult(e_{n@15} | .+(alphaH, post_thetaE)))) */
    for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
      tmp_post_e_1[dvv_loop_var_1-1] = 0.0;
    }
    tmp_post_e_1[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
    for (tmp_idx_e_1=1; tmp_idx_e_1<=Nen; tmp_idx_e_1++) {
      tmp_post_e_1[tmp_idx_e_1-1] = (ldf_Mult_smooth(0, alphaH, h[n_15-1], post_thetaH[tmp_idx_e_1-1], 1, VO)) + ((ldf_Mult_smooth(0, alphaW, w[n_15-1], post_thetaW[tmp_idx_e_1-1], 1, VO)) + (ldf_Mult_smooth(0, alphaH, tmp_idx_e_1, post_thetaE, 1, Nen)));
    }
    normalizeLog(tmp_post_e_1, 1, Nen);
    e[n_15-1] = sample_Mult(tmp_post_e_1, 1, Nen);
    post_thetaH[e[n_15-1]-1][(VO) + (1)-1] += (1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0));
    post_thetaH[e[n_15-1]-1][h[n_15-1]-1] += (1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0));
    post_thetaE[(Nen) + (1)-1] += 1.0;
    post_thetaE[e[n_15-1]-1] += 1.0;
    post_thetaW[e[n_15-1]-1][(VO) + (1)-1] += (1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0));
    post_thetaW[e[n_15-1]-1][w[n_15-1]-1] += (1.0) * ((((e[n_15-1]) == (e[n_15-1])) ? 1 : 0));
  }
  free(tmp_post_e_1);
}

void resample_h(int N, double alphaH, int* e, int* h, double** post_thetaH, int VO) {
  int n_16;
  for (n_16=1; n_16<=N; n_16++) {
    post_thetaH[e[n_16-1]-1][(VO) + (1)-1] += (0.0) - ((1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0)));
    post_thetaH[e[n_16-1]-1][h[n_16-1]-1] += (0.0) - ((1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(h_{n@16} | .+(alphaH, sub(post_thetaH, e_{n@16}))) */
    h[n_16-1] = sample_Mult_smooth(alphaH, post_thetaH[e[n_16-1]-1], 1, VO);
    post_thetaH[e[n_16-1]-1][(VO) + (1)-1] += (1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0));
    post_thetaH[e[n_16-1]-1][h[n_16-1]-1] += (1.0) * ((((e[n_16-1]) == (e[n_16-1])) ? 1 : 0));
  }
}

void resample_w(int N, double alphaW, int* e, double** post_thetaW, int* w, int VO) {
  int n_17;
  for (n_17=1; n_17<=N; n_17++) {
    post_thetaW[e[n_17-1]-1][(VO) + (1)-1] += (0.0) - ((1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0)));
    post_thetaW[e[n_17-1]-1][w[n_17-1]-1] += (0.0) - ((1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0)));
    /* Implements direct sampling from the following distribution: */
    /*   Mult(w_{n@17} | .+(alphaW, sub(post_thetaW, e_{n@17}))) */
    w[n_17-1] = sample_Mult_smooth(alphaW, post_thetaW[e[n_17-1]-1], 1, VO);
    post_thetaW[e[n_17-1]-1][(VO) + (1)-1] += (1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0));
    post_thetaW[e[n_17-1]-1][w[n_17-1]-1] += (1.0) * ((((e[n_17-1]) == (e[n_17-1])) ? 1 : 0));
  }
}


/************************* INITIALIZATION *************************/

double initialize_alphaE() {
  double alphaE;
  alphaE = sample_Gam(1.0, 1.0);
  return (alphaE);
}

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

void initialize_e(int* e, int N, int Nen) {
  int n_15;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    e[dvv_loop_var_1-1] = 0;
  }
  e[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_15=1; n_15<=N; n_15++) {
    e[n_15-1] = sample_MultSym(1, Nen);
  }
}

void initialize_h(int* h, int N, int VO) {
  int n_16;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    h[dvv_loop_var_1-1] = 0;
  }
  h[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_16=1; n_16<=N; n_16++) {
    h[n_16-1] = sample_MultSym(1, VO);
  }
}

void initialize_w(int* w, int N, int VO) {
  int n_17;
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=N; dvv_loop_var_1++) {
    w[dvv_loop_var_1-1] = 0;
  }
  w[(N) + (1)-1] = (0) * (((1) + (N)) - (1));
  for (n_17=1; n_17<=N; n_17++) {
    w[n_17-1] = sample_MultSym(1, VO);
  }
}

void initialize_post_thetaH(double** post_thetaH, int N, int Nen, int VO, int* e, int* h) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VO; dvv_loop_var_2++) {
      post_thetaH[dvv_loop_var_1-1][dvv_loop_var_2-1] = 0.0;
    }
    post_thetaH[dvv_loop_var_1-1][(VO) + (1)-1] = (0.0) * (((1) + (VO)) - (1));
  }
  resample_post_thetaH(N, Nen, VO, e, h, post_thetaH);
}

void initialize_post_thetaE(double* post_thetaE, int N, int Nen, int* e) {
  int dvv_loop_var_1;
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    post_thetaE[dvv_loop_var_1-1] = 0.0;
  }
  post_thetaE[(Nen) + (1)-1] = (0.0) * (((1) + (Nen)) - (1));
  resample_post_thetaE(N, Nen, e, post_thetaE);
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

void dump_alphaE(double alphaE) {
  printf("alphaE = ");
  printf("%g", alphaE);
  printf("\n");
}

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

void dump_thetaE(int Nen, double* thetaE) {
  int dvv_loop_var_1;
  printf("thetaE = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    printf("%g", thetaE[dvv_loop_var_1-1]);
    printf(" ");
  }
  printf("\n");
}

void dump_thetaH(int Nen, int VO, double** thetaH) {
  int dvv_loop_var_1;
  int dvv_loop_var_2;
  printf("thetaH = ");
  for (dvv_loop_var_1=1; dvv_loop_var_1<=Nen; dvv_loop_var_1++) {
    for (dvv_loop_var_2=1; dvv_loop_var_2<=VO; dvv_loop_var_2++) {
      printf("%g", thetaH[dvv_loop_var_1-1][dvv_loop_var_2-1]);
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

double compute_log_posterior(int N, int Nen, int VO, double alphaE, double alphaH, double alphaW, int* e, int* h, double* thetaE, double** thetaH, double** thetaW, int* w) {
  double ldfP6_0;
  int n_15;
  double ldfP7_0;
  int n_16;
  double ldfP8_0;
  int n_17;
  ldfP6_0 = 0.0;
  for (n_15=1; n_15<=N; n_15++) {
    ldfP6_0 += ldf_Mult(1, e[n_15-1], thetaE, 1, Nen);
  }
  ldfP7_0 = 0.0;
  for (n_16=1; n_16<=N; n_16++) {
    ldfP7_0 += ldf_Mult(1, h[n_16-1], thetaH[e[n_16-1]-1], 1, VO);
  }
  ldfP8_0 = 0.0;
  for (n_17=1; n_17<=N; n_17++) {
    ldfP8_0 += ldf_Mult(1, w[n_17-1], thetaW[e[n_17-1]-1], 1, VO);
  }
  return ((ldf_Gam(1, alphaE, 1, 1)) + ((ldf_Gam(1, alphaH, 0.1, 1)) + ((ldf_Gam(1, alphaW, 0.1, 1)) + ((0.0) + ((0.0) + ((0.0) + ((ldfP6_0) + ((ldfP7_0) + (ldfP8_0)))))))));
}

/****************************** MAIN ******************************/

int main(int ARGC, char *ARGV[]) {
  double loglik,bestloglik;
  int iter;
  int N;
  int Nen;
  int VO;
  double alphaE;
  double alphaH;
  double alphaW;
  int* e;
  int* h;
  double* post_thetaE;
  double** post_thetaH;
  double** post_thetaW;
  int* w;
  int malloc_dim_1;

  fprintf(stderr, "-- This program was automatically generated using HBC (v 0.7 beta) from en2.hier\n--     see http://hal3.name/HBC for more information\n");
  fflush(stderr);
  setall(time(0),time(0));   /* initialize random number generator */


  /* variables defined with --define */
  Nen = 3;
  alphaE = 1;
  alphaH = 0.1;
  alphaW = 0.1;

  fprintf(stderr, "Loading data...\n");
  fflush(stderr);
  /* variables defined with --loadD */
  w = load_discrete1("enO", &N, &VO);

  /* variables defined with --loadM or --loadMI */

  fprintf(stderr, "Allocating memory...\n");
  fflush(stderr);
  e = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));

  h = (int*) malloc(sizeof(int) * (1+((N) + (1))-(1)));

  post_thetaE = (double*) malloc(sizeof(double) * (1+((Nen) + (1))-(1)));

  post_thetaH = (double**) malloc(sizeof(double*) * (1+(Nen)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    post_thetaH[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  }

  post_thetaW = (double**) malloc(sizeof(double*) * (1+(Nen)-(1)));
  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    post_thetaW[malloc_dim_1-1] = (double*) malloc(sizeof(double) * (1+((VO) + (1))-(1)));
  }


  fprintf(stderr, "Initializing variables...\n");
  fflush(stderr);
  initialize_e(e, N, Nen);
  initialize_h(h, N, VO);
  initialize_post_thetaH(post_thetaH, N, Nen, VO, e, h);
  initialize_post_thetaE(post_thetaE, N, Nen, e);
  initialize_post_thetaW(post_thetaW, N, Nen, VO, e, w);

  for (iter=1; iter<=100; iter++) {
    fprintf(stderr, "iter %d", iter);
    fflush(stderr);
    resample_e(N, alphaH, alphaW, e, h, post_thetaE, post_thetaH, post_thetaW, w, Nen, VO);
    resample_h(N, alphaH, e, h, post_thetaH, VO);

    loglik = compute_log_posterior(N, Nen, VO, alphaE, alphaH, alphaW, e, h, post_thetaE, post_thetaH, post_thetaW, w);
    fprintf(stderr, "\t%g", loglik);
    if ((iter==1)||(loglik>bestloglik)) {
      bestloglik = loglik;
      fprintf(stderr, " *");
    }
    fprintf(stderr, "\n");
    fflush(stderr);
  }

  free(w);

  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    free(post_thetaW[malloc_dim_1-1]);
  }
  free(post_thetaW);

  for (malloc_dim_1=1; malloc_dim_1<=Nen; malloc_dim_1++) {
    free(post_thetaH[malloc_dim_1-1]);
  }
  free(post_thetaH);

  free(post_thetaE);

  free(h);

  free(e);


  return 0;
}
