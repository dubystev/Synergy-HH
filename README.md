# Synergy-HH
Implementing hyper-heuristic selection strategies towards creating a synergy between them.

Some existing selection hyper-heuristics by other authors are avaialble: TSHH.java was re-implemented based on the algorithm outlined in the paper (#1) that published it; 
FS-ILS (#2) was taken from the author's github page (https://github.com/Steven-Adriaensen/FS-ILS)

The folder named Result Data (https://github.com/dubystev/Synergy-HH/tree/master/Result%20Data) stores the raw median objective function values obtained by TS-ILS, an algorithm that improved on the workings of FS-ILS by incorporating some 
ideas in TSHH and presented in IEEE Congress on Evolutionary Computation 2021 (https://cec2021.mini.pw.edu.pl/upload/CEC-2021/_IEEE-CEC-2021-paper-abstracts.pdf), paper #438. 

The source code for TS-ILS has not yet been published. The result files can be used as basis for comparing the algorithm's performance with other algorithms on the HyFlex test 
suite (containing benchmark instances for six different combinatorial optimization problems).

More details/results of the overall goal of this project will be unveiled as publications are being finalised.

#1. Alanazi, F. (2016). Adaptive Thompson sampling for hyper-heuristics. In 2016 IEEE Symposium Series on Computational Intelligence (SSCI) (pp. 1-8). IEEE.

#2. Adriaensen, S., Brys, T., & Nowé, A. (2014). Fair-share ILS: a simple state-of-the-art iterated local search hyperheuristic. In Proceedings of the 2014 annual 
    conference on genetic and evolutionary computation (pp. 1303-1310).
