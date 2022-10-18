
//package hh_project;

import java.util.ArrayList;
import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;
import AbstractClasses.ProblemDomain.HeuristicType;

/**
 * Sliding Window structure
 */
class slidingW {
    public int h;
    public int reward; //reward for improvement of global-best or otherwise; value in {0, 1}
    
    public slidingW(){
        h = 0;
        reward = 0;
    }
}

/**
 * Implements Thompson Sampling Hyper-heuristic of Alanazi (2016)
 * Ref. = "Alanazi, F. (2016). Adaptive Thompson sampling for hyper-heuristics. In 2016 IEEE Symposium Series on Computational 
 * Intelligence (SSCI) (pp. 1-8). IEEE."
 * @author Stephen Adubi
 * @since 16-06-2019
 */

public class TSHH extends HyperHeuristic {
    ProblemDomain problem;
    int paramVal_mut;
    int paramVal_LS;
    int[] HPool_mut; //set of available mutational LLHs for the problem
    int[] HPool_RR; //set of available ruin-and-recreate LLHs for the problem
    int[] HPool_LS; //set of available Local Search (LS) LLHs for the problem
    int[] HPool_pert; //the union of HPool_mut and HPool_RR
    final double[] param = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    int K_ls; // = HPool_LS.length; //size of the LLH pool for LS
    int K_pert; // = HPool_mut.length; //size of the LLH pool for perturbation
    int offset; // = K_ls;
    int K; // = K_ls + K_mut; //size of the LLH pool
    
    //TSHH variables & parameters
    double[] pr; //sampled probabilities for parameters
    double[] prob_pert; //sampled probabilities for mutation based LLHs, determined in every iteration
    double[] prob_LS; //sampled probabilities for LS-based LLHs, determined in every iteration
    long[] alpha_p; //number of successes of the mutation LLHs
    long[] beta_p; //number of failures of the mutation LLHs
    long[] alpha_l; //number of successes of the LS LLHs
    long[] beta_l; //number of failures of the LS LLHs
    long[] alpha_mut;
    long[] beta_mut;
    long[] alpha_LS;
    long[] beta_LS;
    final long W = 400000; //sliding-window size for LLHs
    
    int no_sampling;
    double iom, dos;
    int n_accept, imprv;
    //memory locations of solutions
    private final int s_0 = 0;
    private final int s_1 = 1;
    private final int s_2 = 2;
    //their evaluations
    double e_0; 
    double e_1;
    double e_2; //evaluation of the current solution AKA proposed solution; solution gotten after an iteration.
    double e_best; //evaluation of the global-best solution
    char[] usesParam_LS;
    char[] usesParam_pert;
    ArrayList<slidingW> SW; //the sliding window
    Acceptance_Mechanism accept;
    final long execTime; //total execution time for the HH in seconds
    
    public TSHH(long seed, long time){
        super(seed);
        execTime = time / 1000;
    }
    
    private void setup(ProblemDomain problem){
        //initialize all variables
        int[] HPool_with_param; //a list of LLHs controlled by the use of parameters, a subset of HPool
	HPool_LS = problem.getHeuristicsOfType(HeuristicType.LOCAL_SEARCH);
        K_ls = HPool_LS.length;
	HPool_mut = problem.getHeuristicsOfType(HeuristicType.MUTATION);
	HPool_RR = problem.getHeuristicsOfType(HeuristicType.RUIN_RECREATE);
	HPool_pert = new int[HPool_mut.length + HPool_RR.length];
        K_pert = HPool_pert.length;
        K = K_ls + K_pert;
        System.arraycopy(HPool_mut, 0, HPool_pert, 0, HPool_mut.length);
        System.arraycopy(HPool_RR, 0, HPool_pert, HPool_mut.length, HPool_RR.length);
        
        usesParam_LS = new char[K_ls];
        usesParam_pert = new char[K_pert];
        for(int i=0; i<K_ls; i++)
            usesParam_LS[i] = 'n';
        for(int i=0; i<K_pert; i++)
            usesParam_pert[i] = 'n';
        int[] LLH_DOS = problem.getHeuristicsThatUseDepthOfSearch(); //get all the LLHs that use depth_of_search parameter
        int[] LLH_IM = problem.getHeuristicsThatUseIntensityOfMutation(); //LLHs that use intensity_of_mutation parameter
        HPool_with_param = new int[LLH_DOS.length + LLH_IM.length];
        System.arraycopy(LLH_DOS, 0, HPool_with_param, 0, LLH_DOS.length);
        System.arraycopy(LLH_IM, 0, HPool_with_param, LLH_DOS.length, LLH_IM.length);
        for(int i=0; i<LLH_IM.length; i++){
            int index = find(HPool_pert, LLH_IM[i]); //locate where the parameterised LLH is stored in the main pool
            usesParam_pert[index] = 'm'; //update its uP value
            if(find(LLH_DOS, LLH_IM[i]) > -1)
                usesParam_pert[index] = 'b';
        }
        for(int i=0; i<LLH_DOS.length; i++){
            int index = find(HPool_LS, LLH_DOS[i]); //locate where the parameterised LLH is stored in the main pool
            if(index > -1)
                usesParam_LS[index] = 'l'; //update its uP value
        }
        
	this.problem = problem;
        this.problem.setMemorySize(3);
        problem.initialiseSolution(s_0);
	e_best = e_0 = problem.getFunctionValue(s_0);
    }
    
    private int find(int[] arr, int h){
        for(int i=0; i<arr.length; i++)
            if(arr[i] == h)
                return i;
        return -1;
    }
    
    /**
     * Implements the beta distribution technique for sampling probabilities using two parameters alpha and beta
     * <p>
     * This Java implementation's ported from the C# implementation of Dr. James McCaffrey.
     * For his explanation of the Thompson Sampling technique, visit https://msdn.microsoft.com/en-us/magazine/mt829274.aspx
     * @param a parameter alpha
     * @param b parameter beta
     * @return sampled probability
     */
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
    
    void init(){
        accept = new Acceptance_Mechanism(execTime, new String[]{"IE"});
        SW = new ArrayList();
        n_accept = 0;
        imprv = 0;
        
        pr = new double[param.length];
        prob_LS = new double[K_ls];
        prob_pert = new double[K_pert];
        alpha_p = new long[K_pert];
        beta_p = new long[K_pert];
        alpha_l = new long[K_ls];
        beta_l = new long[K_ls];
        alpha_mut = new long[param.length];
        beta_mut = new long[param.length];
        alpha_LS = new long[param.length];
        beta_LS = new long[param.length];
    }
    
    int selectOption(String heuType){
        double max;
        int maxPos;
        if(heuType.equals("mut")){
            max = prob_pert[0];
            maxPos = 0;
            for(int i = 1; i<K_pert; i++){
                if(prob_pert[i] > max){
                    max = prob_pert[i];
                    maxPos = i;
                }
            }
        }
        else{
            max = prob_LS[0];
            maxPos = 0;
            for(int i = 1; i<K_ls; i++){
                if(prob_LS[i] > max){
                    max = prob_LS[i];
                    maxPos = i;
                }
            }
        }
        
        return maxPos; //return the heuristic with the maximum reward
    }
    
    int selectMaxParam(){
        double max = pr[0];
        int maxPos = 0;
        for(int i = 1; i<param.length; i++){
            if(pr[i] > max){
                max = pr[i];
                maxPos = i;
            }
        }
        
        return maxPos;
    }
    
    @Override
    protected void solve(ProblemDomain problem) {
        String line = "";
        setup(problem);
        init();
        while(!hasTimeExpired())
        {
            int r = 0; //immediate reward := {0, 1}
            for(int i=0; i<K_pert; i++)
                prob_pert[i] = sample(alpha_p[i] + 1, beta_p[i] + 1); //sample the confidence probability of heuristic i in the permutation class
            int h_mut = selectOption("mut");
            if(usesParam_pert[h_mut] != 'n'){
                for(int i=0; i<param.length; i++)
                    pr[i] = sample(alpha_mut[i] + 1, beta_mut[i] + 1);
                paramVal_mut = selectMaxParam();
                iom = param[paramVal_mut];
                problem.setIntensityOfMutation(iom);
            }
            if(usesParam_pert[h_mut] == 'b'){
                for(int i=0; i<param.length; i++)
                    pr[i] = sample(alpha_LS[i] + 1, beta_LS[i] + 1);
                paramVal_LS = selectMaxParam();
                dos = param[paramVal_LS];
                problem.setDepthOfSearch(dos);
            }
            int LLH_mut = HPool_pert[h_mut];
            e_1 = problem.applyHeuristic(LLH_mut, s_0, s_1);
            hasTimeExpired();
            
            for(int i=0; i<K_ls; i++)
                prob_LS[i] = sample(alpha_l[i] + 1, beta_l[i] + 1);
            int h_LS = selectOption("LS");
            if(usesParam_LS[h_LS] != 'n'){
                for(int i=0; i<param.length; i++)
                    pr[i] = sample(alpha_LS[i] + 1, beta_LS[i] + 1);
                paramVal_LS = selectMaxParam();
                dos = param[paramVal_LS];
                problem.setDepthOfSearch(dos);
            }
            int LLH_LS = HPool_LS[h_LS];
            e_2 = problem.applyHeuristic(LLH_LS, s_1, s_2);
            hasTimeExpired();
            //The acceptance mechanism is Improving-or-Equal (IE)
            if(accept.accept(e_2, e_0, e_best, getElapsedTime())){
                n_accept++;
                problem.copySolution(s_2, s_0);
                e_0 = e_2;
                r = (e_2 < e_best) ? 1 : 0;
                if(r==1){
                    e_best = e_2;
                    imprv++;
                }
            }
            
            slidingW temp_mut = new slidingW();
            slidingW temp_LS = new slidingW();
            temp_mut.h = h_mut; temp_LS.h = h_LS;
            temp_mut.reward = r; temp_LS.reward = r;
            alpha_p[h_mut] += r; alpha_l[h_LS] += r;
            beta_p[h_mut] += 1-r; beta_l[h_LS] += 1-r;
            SW.add(temp_mut);
            SW.add(temp_LS);
            if(SW.size() > W){
                //insert and update the LLH reward
                int h_stored1 = SW.get(0).h;
                int r_stored = SW.get(0).reward;
                int h_stored2 = SW.get(1).h;
                if(r_stored > 0){
                    alpha_p[h_stored1] -= r_stored;
                    alpha_l[h_stored2] -= r_stored;
                }
                else{
                    beta_p[h_stored1]--;
                    beta_l[h_stored2]--;
                }
                
                SW.remove(0);
                SW.remove(0);
            }
            
            if(usesParam_pert[h_mut] != 'n'){
                alpha_mut[paramVal_mut] += r;
                beta_mut[paramVal_mut] += 1-r;
            }
            
            if(usesParam_LS[h_LS] != 'n'){
                alpha_LS[paramVal_LS] += r;
                beta_LS[paramVal_LS] += 1-r;
            }
        }
        line += "After " + getElapsedTime()/1000.0 + " seconds... ";
        line += "Accepted solutions: " + n_accept + ", Number of new best: " + imprv;
        line += ", objective value: " + e_best;
        line += "\n";
        System.out.print(line);
        System.out.println("Sampling could not be applied: " + no_sampling + " time(s)");
        System.out.println("Thompson Sampling HH run's ended");
    }
    
    @Override
    public String toString() {
        return "Thompson_HH @dubystev version";
    }
}
