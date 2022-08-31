package hh_project;

import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;
import AbstractClasses.ProblemDomain.HeuristicType;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class ILS_conf extends HyperHeuristic{
    //heuristics
    int[] ls_llh;
    int[] mut_llh;
    int[] rr_llh;

    //evaluations of heuristics, SpeedNew (proposed by Adriaensen et al. 2014) is used..
    double[][] eval; //evaluations of LLHs based on SpeedNew
    int[][] accepted; //total number of times a LLH's solution was accepted
    int[][] timeToSolve; //total duration spent on producing a solution for a LLH

    //the alpha and beta values for heuristic type pairings, Thompson Sampling parameters
    int r; //immediate reward of a heuristic pair := {0, 1}
    int[] alpha;
    int[] beta;
    double[] prob;
    final int W = 200000; //sliding-window size for LLH-type pairings
    ArrayList<slidingW> SW; //the sliding window

    //concerning parameters
    int param0, param1, param2;
    int K; //number of heuristics
    int[][] HeuristicParamScore;
    int[] total_HP_score;

    //general variables
    int p0, p1;
    int i0, i1;
    int h0, h1; //the two heuristics to apply
    ArrayList<Integer> ls;
    int cur = 0, news = 1; //memory locations for current and new solutions
    double e_cur, e_news, e_best; //evaluations of the current, new and best solutions respectively
    long execTime;
    final double[] param = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    private Dictionary hType;
    private Dictionary hPos;
    private final int types = 2; //the number of heuristic types
    private int[] pairings = {1, 2, 4, 5};
    boolean isPaired; //to note if a pair of heuristic was applied
    ProblemDomain problem;
    Acceptance_Mechanism accept;

    public ILS_conf(long seed, long time){
        super(seed);
        execTime = time / 1000;
    }

    private void setup(ProblemDomain objProblem){
        hType = new Hashtable();
        hPos = new Hashtable();
        int pos = 0;
        this.problem = objProblem;
        ls_llh = problem.getHeuristicsOfType(HeuristicType.LOCAL_SEARCH);
        int[] LLH_DOS = problem.getHeuristicsThatUseDepthOfSearch(); //get all the LLHs that use depth_of_search parameter
        int[] LLH_IM = problem.getHeuristicsThatUseIntensityOfMutation(); //LLHs that use intensity_of_mutation parameter
        for(int i=0; i<ls_llh.length; i++){
            hType.put(ls_llh[i], 'n');
            hPos.put(ls_llh[i], pos++);
        }
        mut_llh = problem.getHeuristicsOfType(HeuristicType.MUTATION);
        for(int i=0; i<mut_llh.length; i++){
            hType.put(mut_llh[i], 'n');
            hPos.put(mut_llh[i], pos++);
        }
        rr_llh = problem.getHeuristicsOfType(HeuristicType.RUIN_RECREATE);
        K = ls_llh.length + mut_llh.length + rr_llh.length;
        for(int i=0; i<rr_llh.length; i++){
            hType.put(rr_llh[i], 'n');
            hPos.put(rr_llh[i], pos++);
        }
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
        accept = new Acceptance_Mechanism(execTime, new String[]{"FLS"});
        total_HP_score = new int[K];
        HeuristicParamScore = new int[K][];
        param_b_score = new int[param.length];
        total_b = param.length;
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
        for(int i=0; i<K; i++)
            total_HP_score[i] = param.length;
        for(int i=0; i<K; i++)
            HeuristicParamScore[i] = new int[param.length];
        for(int i=0; i<K; i++)
            for(int j=0; j<param.length; j++)
                HeuristicParamScore[i][j] = 1;

        eval = new double[2][];
        accepted = new int[2][];
        timeToSolve = new int[2][];

        eval[0] = new double[mut_llh.length];
        eval[1] = new double[rr_llh.length];
        accepted[0] = new int[mut_llh.length];
        accepted[1] = new int[rr_llh.length];
        timeToSolve[0] = new int[mut_llh.length];
        timeToSolve[1] = new int[rr_llh.length];

        for(int i=0; i<mut_llh.length; i++)
            eval[0][i] = Double.MAX_VALUE;
        for(int i=0; i<rr_llh.length; i++)
            eval[1][i] = Double.MAX_VALUE;

        alpha = new int[pairings.length];
        beta = new int[pairings.length];
        problem.initialiseSolution(cur);
        SW = new ArrayList();
        e_cur = problem.getFunctionValue(cur);
        e_best = e_cur;
    }

    int no_sampling = 0;
    double sample(double a, double b){
        double alpha_ = a + b;
        double beta_;
        double u1, u2, w, v;
        if(Math.min(a, b) <= 1.0)
            beta_ = Math.max(1/a, 1/b);
        else
            beta_ = Math.sqrt((alpha_ - 2.0) / (2 * a * b - alpha_));
        double gamma = a * 1/beta_;

        int no_head_way = 0;
        while(true){
            u1 = rng.nextDouble();
            u2 = rng.nextDouble();
            v = beta_ * Math.log(u1 / (1 - u1));
            w = a * Math.exp(v);
            double tmp = Math.log(alpha_ / (b + w));
            double arg1 = alpha_ * tmp + (gamma * v) - 1.3862944;
            double arg2 = Math.log(u1 * u1 * u2);
            if(arg1 >= arg2)
                break;
            no_head_way++;
            if(no_head_way==25){
                no_sampling++;
                return (a / (a+b)); //return expected convergence mean
            }
        }

        double x = w / (b + w);
        return x;
    }

    int selectOption(){
        double max;
        int maxPos;
        max = prob[0];
        maxPos = 0;
        for(int i = 1; i<pairings.length; i++){
            if(prob[i] > max){
                max = prob[i];
                maxPos = i;
            }
        }
        return maxPos;
    }

    /**
     * returns an index of one of the eleven parameters for a parameterised LLH
     * @param index an index which represents a LLH type
     * @return the index of the parameter selected
     */
    int RWS_param(int index){
        double total;
        double cumm;
        if(index > -1){
            total = total_HP_score[index];
            cumm = HeuristicParamScore[index][0];
            double threshold = rng.nextDouble() * total;
            int i = 0;
            while(cumm < threshold){
                i++;
                cumm += HeuristicParamScore[index][i];
            }
            return i;
        }
        else{
            total = total_b;
            cumm = param_b_score[0];
            double threshold = rng.nextDouble() * total;
            int i = 0;
            while(cumm < threshold){
                i++;
                cumm += param_b_score[i];
            }
            return i;
        }
    }

    int RWS(int heuType, int pos){
        int[] h;
        switch(heuType)
        {
            case 0:
                h = mut_llh;
                break;
            case 1:
                h = rr_llh;
                break;
            default:
                h = ls_llh;
                break;
        }
        double total = 0;
        for(int i = 0; i < eval[heuType].length; i++)
            total += eval[heuType][i];
        double threshold = rng.nextDouble() * total;
        int opt = 0;
        double cumm = eval[heuType][0];
        while(cumm < threshold){
            opt++;
            cumm += eval[heuType][opt];
        }

        if(pos == 0)
            h0 = h[opt];
        else
            h1 = h[opt];
        return opt;
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
            s0 = news;
        }
        s1 = news;
        e1 = problem.applyHeuristic(heu, s0, s1);
        return e1;
    }

    void setParam(int heu, int pos){
        //if the first type is a MUT or RR LLH
        int paramVal;
        int index = (int)hPos.get(heu);
        switch(pos){
            case 0:
                paramVal = RWS_param(index);
                problem.setIntensityOfMutation(param[paramVal]);
                param0 = paramVal;
                break;
            case 1:
                paramVal = RWS_param(index);
                problem.setIntensityOfMutation(param[paramVal]);
                param1 = paramVal;
                break;
            case 2: //if LS heuristics called the method
                param2 = RWS_param(index); //LS parameters occupy row 2 (third row) of the parameter matrix
                problem.setDepthOfSearch(param[param2]);
                break;
            default:
                param_b = RWS_param(-1);
                problem.setDepthOfSearch(param[param_b]);
                break;
        }
    }

    int param_b;
    int[] param_b_score;
    int total_b;
    void updateParam(){
        if((char)hType.get(h0) != 'n'){
            int x = (int)hPos.get(h0);
            HeuristicParamScore[x][param0]++;
            total_HP_score[x]++;
            if((char)hType.get(h0) == 'b'){
                param_b_score[param_b]++;
                total_b++;
            }
        }
        //update the parameter settings of the second heuristic only if it was paired with the first
        if(isPaired){
            if((char)hType.get(h1) != 'n'){
                int x = (int)hPos.get(h1);
                HeuristicParamScore[x][param1]++;
                total_HP_score[x]++;
                if((char)hType.get(h1) == 'b' && h0 != h1){
                    param_b_score[param_b]++;
                    total_b++;
                }
            }
        }

        //take care the LS parameters
        if(r_list.size() == 1){
            int x;
            int heu = ls_llh[r_list.get(0)];
            x = (int)hPos.get(heu);
            HeuristicParamScore[x][p_list.get(0)]++;
            total_HP_score[x]++;
            LS_scores[r_list.get(0)]++;
            LS_sum++;
        }
        else if(r_list.size() > 1){
            int x;
            for(int i=0; i<r_list.size(); i++){
                int p1 = r_list.get(i);
                if(i+1 < r_list.size()){
                    int p2 = r_list.get(i+1);
                    transLS[p1][p2]++;
                    trans_sum[p1]++;
                }
                int h = ls_llh[p1];
                x = (int)hPos.get(h);
                LS_scores[p1]++;
                LS_sum++;
                HeuristicParamScore[x][p_list.get(i)]++;
                total_HP_score[x]++;
            }
        }
    }

    /**
     * The method solve which implements the main logic of the HH
     * @param objProblem represents the problem domain object
     */
    @Override
    protected void solve(ProblemDomain objProblem){
        setup(objProblem);
        init();
        prob = new double[pairings.length];

        while(!hasTimeExpired()){
            r = 0;
            for(int i=0; i<pairings.length; i++)
                prob[i] = sample(alpha[i] + 1, beta[i] + 1);
            int option = selectOption(); //select the pair with the maximum reward, BP
            int pairValue = pairings[option];
            p0 = pairValue / types;
            p1 = pairValue % types;
            if(p0 < types){
                i0 = RWS(p0, 0);
                i1 = RWS(p1, 1);
            }
            else
                i0 = RWS(p1, 0);

            if((char)hType.get(h0) != 'n')
                setParam(h0, 0);
            if((char)hType.get(h0) == 'b')
                setParam(h0, -1);

            long start = getElapsedTime(); //record the time before application of heuristics
            e_news = apply(0);
            hasTimeExpired();
            if(p0 == types){ //no double shaking
                LS();
                isPaired = false;
                p0 = p1; //in order to use the right row of the matrix..
                //param0 = param1;
            }
            else{
                if((char)hType.get(h1) != 'n')
                    setParam(h1, 1);
                if((char)hType.get(h1) == 'b' && h0 != h1)
                    setParam(h1, -1);
                e_news = apply(1);
                hasTimeExpired();
                LS();
                isPaired = true;
            }

            long elapsedTime = getElapsedTime();
            long solveTime = elapsedTime - start + 1;
            //if(isPaired) solveTime /= 2;
            timeToSolve[p0][i0] += solveTime;
            if(h0 != h1 && isPaired)
                timeToSolve[p1][i1] += solveTime;
            if(!problem.compareSolutions(news, cur) && accept.accept(e_news, e_cur, e_best, getElapsedTime())){
                accepted[p0][i0]++;
                if(h0 != h1 && isPaired)
                    accepted[p1][i1]++;
                problem.copySolution(news, cur);
                e_cur = e_news;

                if(e_cur < e_best){
                    r = 1; //BP
                    e_best = e_cur;
                    /*if(isPaired)
                        pairs.add(h0 + ", " + h1);*/
                    updateParam();
                }
            }
            updateTS(option); //update alpha and beta parameters for the just applied heuristic pair
            eval[p0][i0] = (1.0 + accepted[p0][i0])/timeToSolve[p0][i0];
            if(h0 != h1 && isPaired) //BP
                eval[p1][i1] = (1.0 + accepted[p1][i1])/timeToSolve[p1][i1]; //edited
            param0 = param1 = param2 = param_b = -1;
        }

        System.out.println("Sampling could not be applied: " + no_sampling + " time(s)");
        System.out.println("hType-P run's ended");
    }

    /**
     * Updates the alpha and beta parameters of a heuristic-type pair after application
     * @param pI the index of the pair
     */
    private void updateTS(int pI){
        slidingW tempW = new slidingW();
        tempW.h = pI;
        tempW.reward = r;
        alpha[pI] += r;
        beta[pI] += 1-r;
        SW.add(tempW);
        if(SW.size() > W){
            //insert and update the LLH reward
            int h_stored = SW.get(0).h;
            int r_stored = SW.get(0).reward;
            if(r_stored > 0)
                alpha[h_stored]--;
            else
                beta[h_stored]--;
            SW.remove(0);
        }
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
    int[] LS_scores;
    int LS_sum;
    int[][] transLS; int[] trans_sum;
    private void LS(){
        r_list.clear(); //clear the reward list
        p_list.clear();
        double e_ls; //temporary local search evaluation
        int h_ls;
        int i_cur, i_prev = -1;
        i_cur = RWS_LS(i_prev);
        h_ls = ls_llh[i_cur];
        setParam(h_ls, 2); //set the parameters for the Local Search heuristic
        e_ls = problem.applyHeuristic(h_ls, news, news);
        hasTimeExpired();
        while(e_ls < e_news){
            r_list.add(i_cur);
            p_list.add(param2);
            e_news = e_ls;
            i_prev = i_cur;
            i_cur = RWS_LS(i_prev);
            h_ls = ls_llh[i_cur];
            setParam(h_ls, 2); //set the parameters for the Local Search heuristic
            e_ls = problem.applyHeuristic(h_ls, news, news);
            hasTimeExpired();
        }
    }

    @Override
    public String toString() {
        return "hType-P {1,2,4,5}";
    }
}
