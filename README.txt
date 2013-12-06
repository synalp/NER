Pour faire du Gibbs sampling pour l'inference du CRF, il faut modifier un petit peu le source code car l'option d'afficher tous les samples generes (verbose=2)
n'est plus bien supporte dans le nouveau code, notamment avec les flags prior et annealing.

Mais les samples sont toujours les memes ! En tout cas lorsque le CRF est appris sur tout le train.
La distrib pour NOPERS, BPERS, IPERS est:
dbugsampledist [0.9999999999997726, 1.851957451755258E-13, 1.3370717502042322E-19]
dbugsampledist [0.9999999999906777, 9.400890583621913E-12, 3.779225953947679E-15]
dbugsampledist [0.9999999999984084, 1.5738207996002305E-12, 6.320655710794453E-20]
dbugsampledist [1.0, 8.769474232460899E-14, 1.415931411681605E-22]
dbugsampledist [0.9999999999729425, 2.7078744531192827E-11, 2.766580834902527E-19]
dbugsampledist [0.9999999999417923, 5.81468317756523E-11, 1.1588479922463724E-16]
dbugsampledist [0.9999999999995453, 4.941701572240705E-13, 4.184947594893579E-16]
dbugsampledist [1.0, 1.1135205540028365E-14, 1.849392551155656E-21]
dbugsampledist [1.0, 5.411006902752365E-17, 7.075636103527637E-22]
dbugsampledist [1.0, 1.2379797853154705E-15, 4.434625562955646E-20]

Donc il sample toujours les memes valeurs.

Il faudrait tester avec de nouvelles features qiui permettent de generaliser beaucoup plus !

