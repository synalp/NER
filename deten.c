#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "stats.h"
#include "samplib.h"

/* repasse sur l'initialisation de H en remplacant le random sur le voc par le premier mot du contexte gauche, ou droit */
void detinitH(int *h, int **ctxt, int N, int *N2) {
  int i,j;
  for (i=0;i<N;i++) {
    if (N2[i]>0) {
	j=0;
	if (ctxt[i][j]==-1) {
	  // pas de contexte gauche
	  j++;
	}
	if (j<N2[i]) {
	  h[i]=ctxt[i][j];
	} // sinon, pas de contexte, on garde une init uniforme sur tout le voc
    } // sinon, pas de contexte, on garde une init uniforme sur tout le voc
      // note: ceci ne doit jamais arrivé depuis que j'ai le -1 pour séparer les contextes gauche et droit
  }
}

static int    hs[20];

/* il ne faut pas utiliser celui-ci, car la fonction rand() retourne des nombres bizarres...
   je l'ai deplace dans stats.c, et ca marche !
*/
int ddetsample_Mult_smooth(double eta, double*th, int lo, int hi, int *possibleH, int possibleHlen) {
  int nhs=0;
  int i,j,h;

  for (i=0;i<possibleHlen;i++) {
    h=possibleH[i];
    if (h<0) continue;
    for (j=0;j<nhs;j++)
      if (hs[j]==h) break;
    if (j==nhs) hs[nhs++]=h;
  }

  double s=0;
  for (i=0;i<nhs;i++) { s+=th[hs[i]] + eta; }
  double rv = ranf();
  printf("debug ran %e\n",rv);
  s = rv * s;
  for (i=0; i<nhs; i++) {
    s -= th[hs[i]] + eta;
    if (s<0) { return hs[i]; }
  }
  return hs[0];
}

