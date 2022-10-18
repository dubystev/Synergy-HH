# Synergy-HH
Implementing hyper-heuristic selection strategies towards creating a synergy between them.

Some existing selection hyper-heuristics by other authors are avaialble: TSHH.java was re-implemented based on the algorithm outlined in the paper (#1) that published it; 
FS-ILS (#2) was taken from the author's github page (https://github.com/Steven-Adriaensen/FS-ILS)

The folder named Result Data (https://github.com/dubystev/Synergy-HH/tree/master/Result%20Data) stores the raw median objective function values obtained by TS-ILS, an algorithm that improved on the workings of FS-ILS by incorporating some 
ideas in TSHH and presented in IEEE Congress on Evolutionary Computation 2021 (https://ieeexplore.ieee.org/document/9504841). 

The source code for TS-ILS (#3) is named ILS_conf.java; https://github.com/dubystev/Synergy-HH/blob/master/src/hh_project/ILS_conf.java. The result files can be used as basis for comparing the algorithm's performance with other algorithms on the HyFlex test 
suite (containing benchmark instances for six different combinatorial optimization problems).

#1. Alanazi, F. (2016). Adaptive Thompson sampling for hyper-heuristics. In 2016 IEEE Symposium Series on Computational Intelligence (SSCI) (pp. 1-8). IEEE.

#2. Adriaensen, S., Brys, T., & Nowé, A. (2014). Fair-share ILS: a simple state-of-the-art iterated local search hyperheuristic. In Proceedings of the 2014 annual conference on genetic and evolutionary computation (pp. 1303-1310).

#3. Adubi, S. A., Oladipupo, O. O., & Olugbara, O. O. (2021). Configuring the Perturbation Operations of an Iterated Local Search Algorithm for Cross-domain Search: A Probabilistic Learning Approach. In 2021 IEEE Congress on Evolutionary Computation (CEC) (pp. 1372-1379). IEEE.

#4. Adubi, S. A., Oladipupo, O. O., & Olugbara, O. O. (2021). Submitted to a Journal outlet, under review.
