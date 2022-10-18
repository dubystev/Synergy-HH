import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;
import AbstractClasses.ProblemDomain.HeuristicType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Final version, cleaned version of EA_ILS_mut_acmv3
 * Unnecessary codes were removed from the parent class to get this version
 */
public class EA_ILS_final extends HyperHeuristic {
    //heuristics
    int[] ls_llh;
    int[] pert_llh;

    //evaluations of heuristics, evolution operators and their evaluation metrics
    int[] op_scores;
    int size_of_pairs;
    int[] HeuristicParamScore;
    int total_HP_score;
    int[] HeuristicParamScore2;
    int total_HP_score2;

    //concerning parameters
    int param0, param1, param2;
    double current_acm, acm;
    int K; //number of perturbative heuristics
    int K2; //number of local search heuristics
    ArrayList<Integer> param_window;
    ArrayList<Integer> param_window2;
    ArrayList<Double> param_acm;
    int paramL = 5; //the length of the parameter window per LLH

    //general variables
    int h0, h1; //the two heuristics to apply
    int cur = 0, prop = 1, best = 2; //memory locations for current and new solutions
    int offset = 3;
    double e_cur, e_prop, e_best; //evaluations of the current, new and best solutions respectively
    long execTime;
    final double[] param = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6};
    final double[] paramI = {0.5, 0.6, 0.7};
    private Dictionary hType;
    private Dictionary hPos;
    boolean isPaired; //to note if a pair of heuristic was applied
    ProblemDomain problem;
    String[] acc_mechanisms;
    Acceptance_Mechanism accept;

    String DOMAIN;
    long objSeed;
    int iter_number;

    public EA_ILS_final(long seed, long time, String strDomain, int batch_iter){
        super(seed);
        objSeed = seed;
        DOMAIN = strDomain;
        iter_number = batch_iter;
        execTime = time / 1000;
    }

    private void setup(ProblemDomain objProblem){
        hType = new Hashtable();
        hPos = new Hashtable();
        int[] temp_llh;
        ArrayList<Integer> temp = new ArrayList<>(); //temporary store of LLHs
        int pos = 0;
        this.problem = objProblem;
        ls_llh = problem.getHeuristicsOfType(HeuristicType.LOCAL_SEARCH);
        int[] LLH_DOS = problem.getHeuristicsThatUseDepthOfSearch(); //get all the LLHs that use depth_of_search parameter
        int[] LLH_IM = problem.getHeuristicsThatUseIntensityOfMutation(); //LLHs that use intensity_of_mutation parameter
        for(int i=0; i<ls_llh.length; i++){
            hType.put(ls_llh[i], 'n');
            hPos.put(ls_llh[i], pos++);
        }
        pos = 0;
        temp_llh = problem.getHeuristicsOfType(HeuristicType.MUTATION);
        for(int i=0; i<temp_llh.length; i++){
            temp.add(temp_llh[i]);
            hType.put(temp_llh[i], 'n');
            hPos.put(temp_llh[i], pos++);
        }
        temp_llh = problem.getHeuristicsOfType(HeuristicType.RUIN_RECREATE);
        //K = ls_llh.length + pert_llh.length + temp_llh.length;
        for(int i=0; i<temp_llh.length; i++){
            temp.add(temp_llh[i]);
            hType.put(temp_llh[i], 'n');
            hPos.put(temp_llh[i], pos++);
        }

        //set-up the set of perturbative LLHs for EA-ILS
        pert_llh = new int[temp.size()];
        int h = 0;
        for(int lhh : temp)
            pert_llh[h++] = lhh;

        K = ls_llh.length + pert_llh.length;
        for(int i=0; i<LLH_DOS.length; i++)
            hType.put(LLH_DOS[i], 'l');
        for(int i=0; i<LLH_IM.length; i++){
            try{if((char)hType.get(LLH_IM[i]) == 'l')
                hType.put(LLH_IM[i], 'b');
            else
                hType.put(LLH_IM[i], 'm');}
            catch(Exception ex){
                //empty
            }
        }
    }

    private void init(){
        //variables about the acceptance of solutions
        acc_mechanisms = new String[] {"FLS"};
        accept = new Acceptance_Mechanism(execTime, acc_mechanisms);

        K = pert_llh.length;
        K2 = ls_llh.length;
        total_HP_score = param.length;
        HeuristicParamScore = new int[param.length];
        total_HP_score2 = paramI.length;
        HeuristicParamScore2 = new int[paramI.length];
        param_b_score = new int[param.length];
        total_b = param.length;
        op_scores = new int[operators.length];
        for(int i=0; i<op_sum; i++)
            op_scores[i] = 1;
        for(int i=0; i<param.length; i++)
            param_b_score[i] = 1;
        LS_scores = new int[ls_llh.length];
        for(int i=0; i<ls_llh.length; i++)
            LS_scores[i] = 1;
        LS_sum = ls_llh.length;
        transLS = new int[ls_llh.length][];
        for(int i=0; i<ls_llh.length; i++)
            transLS[i] = new int[ls_llh.length];
        for(int i=0; i<ls_llh.length; i++)
            for(int j=0; j<ls_llh.length; j++)
                transLS[i][j] = 1;

        trans_sum = new int[ls_llh.length];
        for(int i=0; i<ls_llh.length; i++)
            trans_sum[i] = ls_llh.length;

        for(int i=0; i<param.length; i++)
            HeuristicParamScore[i] = 1;

        for(int i=0; i<paramI.length; i++)
            HeuristicParamScore2[i] = 1;

        param_window = new ArrayList<>();
        param_window2 = new ArrayList<>();
        param_acm = new ArrayList<>();

        //L = pert_llh.length * 2;
        L = pert_llh.length;
        L2 = pert_llh.length;
        //L = 10;
        //double delta; //delta value after applying a set of LLHs in an ILS fashion
        int val = 0;
        ArrayList<Integer> temp = new ArrayList<>();
        ArrayList<Integer> temp2 = new ArrayList<>();
        for(int i=0; i<param.length * 2; i++){
            temp.add(val);
            val = (val + 1) % param.length;
        }
        val = 0;
        for(int i=0; i<paramI.length * 2; i++){
            temp2.add(val);
            val = (val + 1) % paramI.length;
        }
        Collections.shuffle(temp);
        Collections.shuffle(temp2);
        for(int i=0; i<paramL; i++){
            param_window.add(temp.get(i));
            param_window2.add(temp2.get(i));
        }

        locked = false;
        cur_best_seq = new int[LEN];
        for(int i=0; i<LEN; i++)
            cur_best_seq[i] = -1;
        archive = new ArrayList<>();
        archive2 = new ArrayList<>();
        time_to_check = (double)execTime / 15;
        interval = time_to_check;

        problem.setMemorySize(offset);
        problem.initialiseSolution(cur);
        e_cur = problem.getFunctionValue(cur);
        e_best = e_cur;
    }

    double apply(int h){
        int heu, s0, s1;
        double e1; //new evaluation
        if(h == 0){
            heu = h0;
            s0 = cur;
        }
        else{
            heu = h1;
            s0 = prop;
        }
        s1 = prop;
        e1 = problem.applyHeuristic(heu, s0, s1);
        return e1;
    }

    double theta2 = 0.5;
    void setParam(int pos){
        //if the first type is a MUT or RR LLH
        int paramVal;
        switch(pos){
            case 0:
                if(rng.nextDouble() < theta2)
                    paramVal = param_window.get(rng.nextInt(param_window.size()));
                else
                    paramVal = rng.nextInt(param.length);
                problem.setIntensityOfMutation(param[paramVal]);
                param0 = paramVal;
                break;
            case 1:
                if(rng.nextDouble() < theta2)
                    paramVal = param_window.get(rng.nextInt(param_window.size()));
                else
                    paramVal = rng.nextInt(param.length);
                problem.setIntensityOfMutation(param[paramVal]);
                param1 = paramVal;
                break;
            case 2:
                if(rng.nextDouble() < theta2)
                    paramVal = param_window2.get(rng.nextInt(param_window2.size()));
                else
                    paramVal = rng.nextInt(paramI.length);
                problem.setDepthOfSearch(paramI[paramVal]);
                param2 = paramVal;
                break;
            default:
                System.out.println("ERROR occurred in parameter generation");
                System.exit(1);
                break;
        }
    }

    int param_b;
    int[] param_b_score;
    int total_b;
    void updateParam(){
        if((char)hType.get(h0) != 'n'){
            param_window.add(param0);
            if(param_window.size() > paramL)
                param_window.remove(0);
        }

        //update the parameter settings of the second heuristic only if it was paired with the first
        if(isPaired){
            if((char)hType.get(h1) != 'n'){
                param_window.add(param1);
                if(param_window.size() > paramL)
                    param_window.remove(0);
            }
        }

        if(p_list.size() >= 1){
            for(int i=0; i<p_list.size(); i++){
                param_window2.add(p_list.get(i));
                if(param_window2.size() > paramL){
                    param_window2.remove(0);
                }
            }
        }
    }

    void updateLS(){
        //take care the LS parameters
        if(r_list.size() == 1){
            LS_scores[r_list.get(0)]++;
            LS_sum++;
        }
        else if(r_list.size() > 1){
            for(int i=0; i<r_list.size(); i++){
                int p1 = r_list.get(i);
                if(i+1 < r_list.size()){
                    int p2 = r_list.get(i+1);
                    transLS[p1][p2]++;
                    trans_sum[p1]++;
                }
                LS_scores[p1]++;
                LS_sum++;
            }
        }
    }

    int wild_mut;
    double pw = 0.5;
    void mutation_w(int[] seq){
        int loc = rng.nextInt(LEN);
        if(rng.nextDouble() < pw){ //wild mutation
            for(int i=0; i<LEN; i++)
                seq[i] = rng.nextInt(pert_llh.length);
            return;
        }
        if(rng.nextBoolean()){ //randomly select a LLH and fix it in a particular location
            int h = rng.nextInt(pert_llh.length);
            seq[loc] = h;
        }else{ // replace/remove a member
            if(loc == 0 && seq[1] != -1){
                seq[0] = seq[1];
                seq[1] = -1;
            }
            else if(loc == 0 && seq[1] == -1){
                //replace with a random LLH
                int h = rng.nextInt(pert_llh.length);
                seq[loc] = h;
            }
            else
                seq[loc] = -1;
        }
    }

    void mutation(int[] seq){
        int loc = rng.nextInt(LEN);
        if(rng.nextDouble() < 0.5){ //randomly select a LLH and fix it in a particular location
            int h = rng.nextInt(pert_llh.length);
            seq[loc] = h;
        }else{ // replace/remove a member
            if(loc == 0 && seq[1] != -1){
                seq[0] = seq[1];
                seq[1] = -1;
            }
            else if(loc == 0 && seq[1] == -1){
                //replace with a random LLH
                int h = rng.nextInt(pert_llh.length);
                seq[loc] = h;
            }
            else
                seq[loc] = -1;
        }
        wild_mut = 0;
    }

    void mutation_0(int[] seq){
        int loc = rng.nextInt(LEN);
        if(rng.nextDouble() < 0.5){ //randomly select a LLH and fix it in a particular location
            int h = rng.nextInt(pert_llh.length);
            seq[loc] = h;
        }
    }

    void mutation_1(int[] seq){
        int loc = rng.nextInt(LEN);
        if(loc == 0 && seq[1] != -1){
            seq[0] = seq[1];
            seq[1] = -1;
        }
        else if(loc == 0 && seq[1] == -1){
            //replace with a random LLH
            int h = rng.nextInt(pert_llh.length);
            seq[loc] = h;
        }
        else
            seq[loc] = -1;
    }

    /**
     *
     * @param seq the selected operator sequence from the archive
     * @param seq_pos the position of the operator sequence in the archive, needed for the combination operators
     */
    final int[] operators = {3};
    int op_sum = operators.length;
    int op_; // the selected operator index during an iteration
    void apply_op(int[] seq){
        op_ = rng.nextInt(operators.length); // debug
        int selected_op = operators[op_];
        //op_ = (op_ + 1) % operators.length;
        switch(selected_op){
            case 0:
                mutation(seq);
                break;
            case 1:
                mutation_0(seq);
                break;
            case 2:
                mutation_1(seq);
                break;
            case 3:
                mutation_w(seq);
                break;
            case 4:
                //System.out.print(" Just used the Apply operator, i.e. no changes to the sequence ");
                return; // do nothing to the current operation sequence
            default:
                System.out.println("Error in selecting an evolutionary operator");
                System.out.println("Report this bug. Exiting...");
                break;
        }
    }

    void add_to_archive(int[] seq){
        archive.add(seq);
        if (archive.size() > L)
            archive.remove(0);
    }

    void add_to_archive2(int[] seq, int iter){
        for(int i=0; i<iter; i++) {
            archive.add(seq);
            if (archive.size() > L)
                archive.remove(0);
        }
    }

    public boolean Monte_Carlo(double eval, double eval_0){
        if(eval < 1){
            eval *= 10000;
            eval_0 *= 10000;
        }
        double delta = -(eval - eval_0);
        double rnd = rng.nextDouble();
        return(rnd < Math.exp(delta));
    }

    int[] op_seq; //an operation sequence
    ArrayList<String> output;
    int[] cur_best_seq; //the last operation sequence that found a new best solution
    int LEN = 2; // maximum length of an operation sequence
    int p = 1; // number of offspring generated from an operation sequence per iteration
    int L; // length of archive
    int L2;
    ArrayList<int[]> archive;
    ArrayList<int[]> archive2;
    double time_to_check;
    double time_out = 0.52; //fraction of execution time to start considering only the items in the archive during evol.
    boolean locked;
    double theta;
    double interval;
    int n_trials = 1;
    int n_iter = 0; //number of iterations completed so far
    int acm_trials = 15;
    int acc_imp = 0;
    int no_imp_acm = 0;
    int imp_acm;
    double[] set = {0.38, 0.25, 0.15};
    //double[] set = {0.50, 0.35, 0.2};
    BufferedWriter wr = null;
    int code;
    /**
     * The method solve which implements the main logic of the HH
     * @param objProblem represents the problem domain object
     */
    @Override
    protected void solve(ProblemDomain objProblem){
        setup(objProblem);
        init();
        output = new ArrayList<>();
        //theta = theta_arr[rng.nextInt(theta_arr.length)];
        theta = 0.3;

        /**
         * generate g operation sequences and put them into the archive
         * each operation sequence has a unique first element
         */
        ArrayList<Integer> temp = new ArrayList<>();
        ArrayList<Integer> temp2 = new ArrayList<>();
        for(int i=0; i<pert_llh.length; i++)
            temp.add(i);
        for(int i=0; i<set.length; i++)
            temp2.add(i);
        for(int g=0; g<pert_llh.length; g++){
            op_seq = new int[LEN];
            int index = rng.nextInt(temp.size());
            int index2;
            if(g < set.length){
                index2 = rng.nextInt(temp2.size());
                param_acm.add(set[temp2.get(index2)]);
                temp2.remove(index2);
            }
            if(rng.nextBoolean()){ // generate an operation sequence with a length of 2
                for(int i=0; i<2; i++){
                    int pos;
                    if(i==0) pos = temp.get(index);
                    else pos = rng.nextInt(pert_llh.length);
                    op_seq[i] = pos;
                }
            }else {
                int pos = temp.get(index);
                op_seq[0] = pos;
                op_seq[1] = -1;
            }
            temp.remove(index);
            //param_acm.add(rng.nextInt(set.length));
            archive.add(op_seq);
            archive2.add(op_seq);
        }

        acm = param_acm.get(rng.nextInt(param_acm.size()));
        current_acm = acm;
        accept.FLS_set_temp(acm);

        while(!hasTimeExpired()){
            int arc_pos;
            op_seq = new int[LEN];
            arc_pos = rng.nextInt(archive.size());
            System.arraycopy(archive.get(arc_pos), 0, op_seq, 0, LEN);

            code = 0;
            for(int i=0; i<=p; i++) {
                op_ = -1;
                int[] temp_seq = new int[LEN];
                System.arraycopy(op_seq, 0, temp_seq, 0, LEN);
                wild_mut = -1;
                if (i >= 1){
                    apply_op(temp_seq);
                    code = 1;
                }

                if (temp_seq[1] >= 0) { //if the length of the operation sequence is maximum
                    h0 = pert_llh[temp_seq[0]];
                    h1 = pert_llh[temp_seq[1]];
                } else
                    h0 = pert_llh[temp_seq[0]];

                if ((char) hType.get(h0) != 'n')
                    setParam(0);
                if (temp_seq[1] != -1 && (char) hType.get(h1) != 'n')
                    setParam(1);

                int noimp = 0; int imp = 0;
                while(noimp < n_trials) {
                    e_prop = apply(0);
                    hasTimeExpired();
                    if (temp_seq[1] == -1) { //no double shaking
                        LS();
                        isPaired = false;
                    } else {
                        e_prop = apply(1);
                        hasTimeExpired();
                        LS();
                        isPaired = true;
                    }

                    if(no_imp_acm == acm_trials){ //comment the block to put off set_accept_strategy()
                        if(rng.nextDouble() < theta) //try 0.35
                            acm = mutate();
                        else
                            acm = param_acm.get(rng.nextInt(param_acm.size()));

                        if(imp_acm > 0) {
                            param_acm.add(current_acm);
                            if (param_acm.size() > paramL + 2) //paramL * 2
                                param_acm.remove(0);
                        }

                        if(current_acm != acm)
                            accept.FLS_set_temp(acm);

                        current_acm = acm;
                        imp_acm = 0;
                        no_imp_acm = 0;
                    }

                    //boolean mc_acc = false;
                    //if(!problem.compareSolutions(prop, cur) && MC_Accept(e_prop, e_cur)) mc_acc = true;
                    if (!problem.compareSolutions(prop, cur) && accept.accept(e_prop, e_cur, e_best, getElapsedTime())) {
                        updateParam();
                        if (e_prop < e_best) {
                            problem.copySolution(prop, best);
                            e_best = e_prop;
                            updateLS();
                            imp++;
                            acc_imp++;
                            imp_acm++;
                            no_imp_acm = 0;
                            noimp = 0;
                        }
                        else{
                            no_imp_acm++;
                            noimp++;
                        }
                        problem.copySolution(prop, cur);
                        e_cur = e_prop;
                    }
                    else
                    {
                        noimp++;
                        no_imp_acm++;
                    }
                    //iter++;

                    //solution quality tracking after some interval, not part of the algorithm
                    if((double)getElapsedTime() / 1000 >= time_to_check){
                        System.out.print("Best after " + (int)time_to_check + " seconds: " + e_best + " ");
                        output.add("Best after " + (int)time_to_check + " seconds: " + e_best + " ");
                        time_to_check += interval;
                    }

                    hasTimeExpired();
                    n_iter++;
                }

                if(imp > 0){
                    output.add(String.format("{%d, %d} +%d ", temp_seq[0], temp_seq[1], imp));
                    add_to_archive2(temp_seq, imp); //add the sequence to the archive if a better solution is found
                    //add_to_archive(temp_seq); //add the sequence to the archive if a better solution is found
                }
                param0 = param1 = param2 = param_b = -1;
            }
        }

        output.add("\n\n");
        for(int i=0; i<archive.size(); i++){
            System.out.printf("{%d, %d}, ", archive.get(i)[0], archive.get(i)[1]);
            output.add(String.format("{%d, %d}, ", archive.get(i)[0], archive.get(i)[1]));
        }
        for(int j=0; j<param_window.size(); j++){
            System.out.printf("%d ", param_window.get(j));
            output.add(String.format("%d ", param_window.get(j)));
        }
        System.out.printf("| ");
        output.add("| ");
        for(int j=0; j<param_window2.size(); j++){
            System.out.printf("%d ", param_window2.get(j));
            output.add(String.format("%d ", param_window2.get(j)));
        }
        System.out.printf("| ");
        output.add("| ");
        for(int j=0; j<param_acm.size(); j++){
            System.out.printf("%.2f ", param_acm.get(j));
            output.add(String.format("%.2f ", param_acm.get(j)));
        }
        /*System.out.printf("| ");
        for(int j=0; j<param_window3.size(); j++)
            System.out.printf("%d ", param_window3.get(j));*/
        System.out.println("EA-ILS run's ended");

        try {
            wr = new BufferedWriter(new FileWriter("C:/Outputs/" + DOMAIN + "-" + objSeed + "-output"
                    + iter_number + ".txt"));
            wr.write("EA-ILS Trace\n");
            for(int i=0; i<output.size(); i++)
                wr.write(output.get(i));
            wr.newLine();
            wr.write("Fitness of final solution: " + problem.getBestSolutionValue());
            //store number of total iterations + local search hits
            wr.newLine(); wr.newLine();
            wr.write("Number of Iterations: " + n_iter); wr.newLine();
            double avg_ls_iter = (double)sum_ls_iter / (double)ls_calls;
            wr.write(String.format("Number of Local search hits: %d, Average local search iteration " +
                    "per call: %.2f", ls_calls, avg_ls_iter));
            wr.close();
        } catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    //int op_index = 0; 5
    int[] param_op = {2};
    double mutate(){
        double val;
        int op_index = rng.nextInt(param_op.length);
        int op = param_op[op_index];
        //op_index = (op_index + 1) % param_op.length;
        switch (op){
            case 0:
                val = arithX();
                break;
            case 1:
                val = differential_mut();
                break;
            case 2:
                val = linear_mutation();
                break;
            default:
                val = rng.nextDouble();
                System.out.print("Error in mutating param. Stop the program ");
        }

        return val;
    }

    double[] add_mut = {0.1, 0.2, 0.3, 0.4, 0.5};
    //double[] add_mut = {0.1, 0.2, 0.3};
    double linear_mutation(){
        double p1 = param_acm.get(rng.nextInt(param_acm.size()));
        double rand = add_mut[rng.nextInt(add_mut.length)];
        double p2 = p1 + rand;
        if(p2 > 1.0) p2 = p2 - 1;
        String val = String.format("%.2f", p2);
        p2 = Double.parseDouble(val);
        return p2;
    }

    double lambda;
    double arithX(){
        lambda = rng.nextDouble();
        double p1 = param_acm.get(rng.nextInt(param_acm.size() - 1));
        double p2 = param_acm.get(param_acm.size() - 1); //pick the latest/last rewarding parameter
        double p_out = (lambda * p1) + (1-lambda) * p2;
        if(p_out > 1.0) p_out = p_out - 1;
        String val = String.format("%.2f", p_out);
        p_out = Double.parseDouble(val);
        return p_out;
    }

    double factor = 0.9;
    double differential_mut(){
        ArrayList<Integer> temp = new ArrayList<>();
        double[] p = new double [3];
        for(int i=0; i<param_acm.size(); i++)
            temp.add(i);
        Collections.shuffle(temp);
        for(int i=0; i<3; i++)
            p[i] = param_acm.get(temp.get(i));

        //factor = rng.nextDouble();
        double p_out = Math.abs(p[0] + factor * (p[1] - p[2]));
        if(p_out > 1.0) p_out = p_out - 1;
        String val = String.format("%.2f", p_out);
        p_out = Double.parseDouble(val);
        return p_out;
    }

    int RWS_LS(int prev){
        int[] score;
        int total;
        if(prev == -1){
            score = LS_scores;
            total = LS_sum;
        }
        else{
            score = transLS[prev];
            total = trans_sum[prev];
        }

        double threshold = rng.nextDouble() * total;
        int i = 0;
        double cumm = score[i];
        while(cumm < threshold){
            i++;
            cumm += score[i];
        }
        return i;
    }

    ArrayList<Integer> r_list = new ArrayList();
    ArrayList<Integer> p_list = new ArrayList();
    int ls_calls = 0; //number of local search calls
    int ls_iter;
    int sum_ls_iter;
    int[] LS_scores;
    int LS_sum;
    int[][] transLS; int[] trans_sum;
    private void LS(){
        ls_calls++;
        ls_iter = 0;
        r_list.clear(); //clear the reward list
        p_list.clear();
        double e_ls; //temporary local search evaluation
        int h_ls;
        int i_cur, i_prev = -1;
        i_cur = RWS_LS(i_prev);
        //i_cur = rng.nextInt(ls_llh.length);
        h_ls = ls_llh[i_cur];
        setParam(2); //set the parameters for the Local Search heuristic
        //code = (code + 1) % 2;
        e_ls = problem.applyHeuristic(h_ls, prop, prop);
        ls_iter++;
        hasTimeExpired();
        while(e_ls < e_prop){
            r_list.add(i_cur);
            p_list.add(param2);
            e_prop = e_ls;
            i_prev = i_cur;
            i_cur = RWS_LS(i_prev);
            h_ls = ls_llh[i_cur];
            setParam(2); //set the parameters for the Local Search heuristic
            //code = (code + 1) % 2;
            e_ls = problem.applyHeuristic(h_ls, prop, prop);
            ls_iter++;
            hasTimeExpired();
        }
        sum_ls_iter += ls_iter;
    }

    @Override
    public String toString() {
        return String.format("EA-ILS @ guided mutation - final + %d %s", n_trials, (n_trials > 1) ?
                "trials" : "trial");
    }
}
