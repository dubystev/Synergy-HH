import java.util.ArrayList;
import java.util.Random;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Implements a module for various move-acceptance mechanisms (AMs).<br>
 * Adopted variable names (see below):<br>
 * eval: the cost evaluation of a proposed solution<br>
 * eval_0: the cost evaluation of the incumbent solution<br>
 * bestEval: the cost evaluation of the best solution found so far
 * @author Stephen A. Adubi
 * @since 04-06-2019
 * @modified 19-08-2019
 */

public class Acceptance_Mechanism{
    //global variables
    double e_received; //the evaluation of the solution that was sent from the engaged HH object for acceptance
    double e_best; //the evaluation of the best solution found so far by the HH
    double runTime_; //the total amount of time that elapsed after an improvement check
    long elapsedTime; //the elapsed time of the HH run
    int NCC;
    int imprv_wT; //number of improvements within a time threshold

    private final long execTime; //total execution time alloted for the algorithm's run
    private final Random RAND;

    //parameter(s) for Threshold Acceptance
    double T, T_int;
    private double t_delta;
    double allowT; //the amount of consecutive number of seconds allowed for HH to run with an unchanged new best
    double time_elapsed_since_global_update;
    double lastTime_update; //the last time a new best solution was found

    //parameter(s) for FS-ILS Acceptance Mechanism
    private double temp;
    private int n_impr; //number of improvements
    private double avg_impr; //mean improvement

    //parameter(s) for SA_Accept in Adriaensen et al. (2014b)
    private double SA_temp;
    private int n_impr_SA; //number of improvements
    private double avg_impr_SA; //mean improvement

    //parameter(s) for Great Deluge (GD) and Simulated Annealing (SA) Acceptance methods
    public int phase;
    public int maxPhase;
    public double phaseTime; //the time required for a phase
    public double endPhaseTime; //the time the current phase is meant to end

    //parameter(s) for AA1
    private int Counter;

    //parameter(s) for AA2
    //private int acceptanceRate;

    int prevCounter = 0;
    private int mode; // takes a value in {0, 1}, 0 means only one AS is used, 1 means potentially a lot of AS can be used by 
    //the optimiser during the entire run
    private String[] AS; //acceptance strategies to be initialised from an external class
    private final Dictionary ASUpdate = new Hashtable();
    //private Dictionary activated = new Hashtable();
    private int active; //stores the index of the sctive Acceptance Strategy (AS)
    private final ArrayList<String> defaultAS = new ArrayList();

    /**
     * The codes for the acceptance strategies are as follows:
     * <ul>
     * <li>AA1 = Adaptive Acceptance 1</li>
     * <li>FLS = Accept Probabilistic Worse, in FS-ILS paper<li>
     * GDA = Great Deluge Acceptance<li>
     * SA = Simulated Annealing<li>
     * RRT = Record-to-Record Travel<li>
     * MC = Monte Carlo Acceptance<li>
     * TA = Threshold Acceptance<li>
     * IE = Improving or Equal Acceptance<li>
     * NA = Naive Acceptance<li>
     * OI = Only Improving
     * </ul><br>Use the listed codes as values for the 'args' vector, you can pass in as many as possible.
     * In case your hyper-heuristic employs multiple acceptance strategies<br>Example:
     * <b>Acceptance_Mechanism objA = new Acceptance_Mechanism(380, new String[]{"FLS", "SA"})</b><br>
     * The following means that the hyper-heuristic will run for 380 secs and then Accept Probabilistic Worse and
     * Simulated Annealing will be employed as the acceptance strategies
     * <p>
     * For 'FLS', append a temperature value like this "FLS,1.0", the value after the comma represents the temperature, if the
     * value doesn't exist (only "FLS" was passed in as the argument), the default value of 0.5 will be used.<br>
     * For 'GDA' or 'SA', use "GDA,25" or "SA,25" if the search is to be segmented into 25 phases. If the second value of the
     * argument doesn't exist, then a default value of 45 will be used.
     * @args initialises the array AS, which contains the list of acceptance strategies for the optimiser.
     * @param time total execution time for the optimiser
     * @param args a list of Acceptance Strategies that would be employed by the optimiser during problem solving
     */
    public Acceptance_Mechanism(long time, String[] args){
        execTime = time;
        e_best = Double.MAX_VALUE;
        defaultAS.add("FLS"); defaultAS.add("TA"); defaultAS.add("RRT"); defaultAS.add("AA1"); defaultAS.add("GDA");
        defaultAS.add("OI"); defaultAS.add("IE"); defaultAS.add("MC"); defaultAS.add("NA"); defaultAS.add("SA"); defaultAS.add("MTA");
        defaultAS.add("SA-2");
        String parameterisedAS = "GDA,SA,TA,MTA,FLS,SA-2";
        for(int i=0; i<args.length; i++) {
            String arg = args[i];
            String argv = arg.split(",")[0];
            if (parameterisedAS.contains(argv)) {
                switch(argv){
                    case "SA":
                    case "GDA":
                        try{
                            if(arg.split(",").length > 1){
                                String arg2 = arg.split(",")[1];
                                int number_phases = Integer.parseInt(arg2);
                                setParam_GD_SA(number_phases);
                            }
                            else
                                setParam_GD_SA(45);
                            ASUpdate.put(argv, 'y');
                            args[i] = argv;
                        }
                        catch(NumberFormatException ex){
                            System.out.println(ex.getMessage());
                            System.out.println("Improper format of the arguments listing, program will exit");
                            System.exit(1);
                        }
                        catch(Exception ex){
                            System.out.println(ex.getMessage());
                            System.out.println("Wrong input/Improper code(s) used for the submitted acceptance strategies");
                            System.exit(1);
                        }
                        break;
                    case "MTA":
                        T_int = 30;
                    case "FLS":
                        try{
                            if(!defaultAS.contains(argv)) throw new Exception("Invalid arguments");
                            if(arg.split(",").length > 1){
                                String arg2 = arg.split(",")[1];
                                double T = Double.parseDouble(arg2);
                                setParamFS_ILS_accept(T);
                            }
                            else
                                setParamFS_ILS_accept(0.5);
                            ASUpdate.put(argv, 'y');
                            args[i] = argv;
                        }
                        catch(NumberFormatException ex){
                            System.out.println(ex.getMessage());
                            System.out.println("Improper format of the arguments listing, program will exit");
                            System.exit(1);
                        }
                        catch(Exception ex){
                            System.out.println(ex.getMessage());
                            System.out.println("Wrong input/Improper code(s) used for the submitted acceptance strategies");
                            System.exit(1);
                        }
                        break;
                    case "SA-2":
                        try{
                            if(!defaultAS.contains(argv)) throw new Exception("Invalid arguments");
                            if(arg.split(",").length > 1){
                                String arg2 = arg.split(",")[1];
                                double T = Double.parseDouble(arg2);
                                setParamSA_accept(T);
                            }
                            else
                                setParamSA_accept(1.0);
                            ASUpdate.put(argv, 'y');
                            args[i] = argv;
                        }
                        catch(NumberFormatException ex){
                            System.out.println(ex.getMessage());
                            System.out.println("Improper format of the arguments listing, program will exit");
                            System.exit(1);
                        }
                        catch(Exception ex){
                            System.out.println(ex.getMessage());
                            System.out.println("Wrong input/Improper code(s) used for the submitted acceptance strategies");
                            System.exit(1);
                        }
                        break;
                    case "TA":
                        try{
                            ASUpdate.put(arg, 'n');
                            int arg3 = (int)Math.round((execTime * 15.0) / 346.0);
                            setParamtThresholdAcc(30, 2, arg3);
                        }
                        catch(Exception ex){
                            System.out.println(ex.getMessage());
                            System.out.println("Wrong input/Improper code(s) used for the submitted acceptance strategies");
                            System.exit(1);
                        }
                        break;
                    default:
                        System.out.println("Wrong input/Improper code(s) used for the submitted acceptance strategies");
                        System.exit(1);
                        break;
                }
            }
            else{
                try{
                    if(!defaultAS.contains(arg)) throw new Exception("Invalid arguments");
                    if(arg.equals("AA1"))
                        Counter = 1;
                    ASUpdate.put(arg, 'n');
                }
                catch(Exception ex){
                    System.out.println(ex.getMessage());
                    System.out.println("Wrong input/Improper code(s) used for the submitted acceptance strategies");
                    System.exit(1);
                }
            }
        }

        AS = args;
        mode = (AS.length > 1) ? 1 : 0;
        active = (mode==0) ? 0 : -1;

        RAND = new Random();
    }

    /**
     * This method determines the acceptance of a solution based on the current active acceptance strategy
     * @param eval the evaluation of the proposed solution
     * @param eval_0 the evaluation of the incumbent solution
     * @param bestEval the evaluation of the best solution
     * @param eTime the current elapsed time
     * @return
     */
    public boolean accept(double eval, double eval_0, double bestEval, long eTime){
        boolean val = false;
        e_received = eval;
        this.elapsedTime = eTime / 1000;
        try{
            if(active == -1)
                throw new Exception("There is no active acceptance strategy");
            String AM = AS[active]; //get the code of the active AS
            switch(AM)
            {
                case "AA1":
                    val = adaptive_acceptance1(eval, eval_0);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    if(NCC%10000==0 && NCC > 0){
                        updateCounter();
                        prevCounter = NCC;
                    }
                    else if(NCC==0 && Counter > 1){
                        resetCounter();
                        prevCounter = NCC;
                    }
                    break;
                case "FLS":
                    val = FS_ILS_accept(eval, eval_0);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "SA-2":
                    val = SA_accept(eval, eval_0);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "GDA":
                    val = GreatDeluge(eval, eval_0, bestEval);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    /*if(elapsedTime >= endPhaseTime){
                        phase++;
                        endPhaseTime = phaseTime * phase;
                    }*/
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "MC":
                    val = Monte_Carlo(eval, eval_0);
                    if(e_received < e_best){
                        NCC = 0;
                        imprv_wT++;
                        prevCounter = 0;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "RRT":
                    val = RRT(eval, eval_0, bestEval);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "SA":
                    /*if(elapsedTime >= endPhaseTime){
                        phase++; //breakpoint
                        endPhaseTime = phaseTime * phase;
                    }*/
                    val = SA(eval, eval_0, bestEval);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "TA":
                    val = ThresholdAcc(eval, eval_0, bestEval);
                    if(e_received < e_best){
                        imprv_wT++;
                        NCC = 0;
                        prevCounter = 0;
                        e_best = e_received; //BP
                    }
                    else NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT == 0){
                            T -= t_delta; //BP
                            if(T<0) T = 0; //reset back to zero
                        }
                        else
                            imprv_wT = 0;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "MTA":
                    val = ModifiedTA(eval, eval_0, bestEval);
                    if(e_received < e_best){
                        imprv_wT++;
                        NCC = 0;
                        prevCounter = 0;
                        e_best = e_received; //BP
                    }
                    else NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT == 0){
                            T -= t_delta; //BP
                            if(T<0) T = 0; //reset back to zero
                        }
                        else
                            imprv_wT = 0;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "IE":
                    val = improving_equal(eval, eval_0);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "NA":
                    val = naive_acceptance(eval, eval_0);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        e_best = e_received;
                        imprv_wT++;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
                case "OI":
                    val = only_improving(eval, eval_0);
                    if(e_received < e_best){
                        NCC = 0;
                        prevCounter = 0;
                        imprv_wT++;
                        e_best = e_received;
                    }
                    else
                        NCC++;
                    if(elapsedTime - runTime_ == allowT){
                        if(imprv_wT > 0){
                            imprv_wT = 0;
                            time_elapsed_since_global_update = 0;
                            lastTime_update = 0;
                        }
                        else
                            time_elapsed_since_global_update += allowT;
                        runTime_ = elapsedTime;
                    }
                    break;
            }
        }

        catch(Exception ex){
            System.err.println(ex.getMessage());
        }

        return val;
    }

    /**
     * This method sets an active AS, which will be used for accepting solutions during its lifespan. The method must be called
     * when multiple AS are being used, (may be) during the start of search and when the optimiser is to switch to another 
     * alternative
     * @param arg the index of the acceptance strategy. <em><b>Note: </b>pass an integer representing the index of the AS in the 
     * array of strings used to initialise the object of the Acceptance_Mechanism class and NOT the AS string code
     * </em>
     * @param elapsedTime the current elapsed time during optimization
     * @param curBest the evaluation of the global best solution as @ when an active acceptance strategy is set
     */
    public void setActiveAS(int arg, double elapsedTime, double curBest){
        e_best = curBest;
        int eTime = (int)Math.floor(elapsedTime / 1000); //elapsed time in seconds
        String accept_m;
        try{
            if(arg >= AS.length) throw new Exception("Invalid argument, the value of arg is greater than the maximum index of the AS vector");
            else{
                active = arg;
                accept_m = AS[active];
                switch(accept_m){
                    /*case "SA":
                    case "GDA":
                        if(eTime <= phaseTime)
                            phase = 1; //bp
                        else
                            phase = (int)Math.ceil(eTime / phaseTime); //bp
                        endPhaseTime = phaseTime * phase;
                        break;*/
                    case "TA":
                        if(time_elapsed_since_global_update >= allowT){
                            double diff = time_elapsed_since_global_update - lastTime_update;
                            lastTime_update = time_elapsed_since_global_update; //bp
                            int factor = (int)(diff / allowT);
                            T -= factor * t_delta;
                            if(T < 0) T = 0;
                        }
                        break;
                    case "AA1":
                        if(NCC >= 10000){
                            int diff = NCC - prevCounter; //bp
                            int factor = (int)Math.floor(diff / 10000);
                            Counter += factor;
                            if(Counter<=0)
                                System.out.println("A problem because the value of Counter is " + Counter); //bp
                            prevCounter = NCC;
                        }
                        else
                            Counter = 1; //bp
                        break;
                    default:
                        break;
                }
            }
        }
        catch(Exception ex){
            System.err.println(ex.getMessage());
        }
    }

    /**
     * sets the threshold value of the Threshold acceptance mechanism
     * @param T the threshold value
     * @param delta the amount by which the parameter T is reduced
     * @param allowT the total time (in secs) permitted for an HH to run without updating its best solution
     */
    private void setParamtThresholdAcc(double T, double delta, double allowT){
        this.T = T;
        t_delta = delta;
        this.allowT = allowT;
    }

    /**
     * sets the parameters for the FS_ILS acceptance mechanism
     * @param T the value for temperature
     */
    public void setParamFS_ILS_accept(double T){
        temp = T;
        n_impr = 0;
        avg_impr = 0;
    }

    private void setParamSA_accept(double T){
        SA_temp = T;
    }

    /**
     * sets the parameter for the maximum number of phases for SA and GD acceptance mechanisms
     * @param value sets the number of total number of phases for the search 
     */
    private void setParam_GD_SA(int value){
        phase = 1;
        maxPhase = value;
        phaseTime = execTime / maxPhase;
        endPhaseTime = phaseTime;
    }
    public boolean only_improving(double eval, double eval_0){
        return(eval < eval_0);
    }
    public boolean improving_equal(double eval, double eval_0){
        return(eval <= eval_0);
    }
    /**
     * Implements the naive acceptance mechanism which accepts all improving moves, non-improving moves are
     * accepted with a 50% probability
     * @param eval proposed solution
     * @param eval_0 current solution
     * @return a boolean value
     */
    public boolean naive_acceptance(double eval, double eval_0){
        if(eval < eval_0)
            return true;
        else
            return(RAND.nextBoolean());
    }
    public boolean ThresholdAcc(double eval, double eval_0, double bestEval){
        //when using this, value for consecutive runs without improvement in best should be set to 20.7 secs (15 secs)
        if(eval < eval_0)
            return true;
        else{
            if(eval < 1){
                double T_norm = T/10000.0;
                return (eval < (bestEval + T_norm));
            }
            else
                return (eval < (bestEval + T));
        }
    }
    public boolean GreatDeluge(double eval, double eval_0, double bestEval){
        if(eval < eval_0)
            return true;
        else{
            double arg = (1 + Math.pow(0.85, phase - 1)) * bestEval;
            return (eval <= arg);
        }
    }
    public void setMTA(double val){
        T_int = val;
    }
    public boolean ModifiedTA(double eval, double eval_0, double bestEval){
        if(eval < eval_0)
            return true;
        else{
            if(eval < 1){
                double T_norm = T_int/10000.0;
                return (eval < (bestEval + T_norm));
            }
            else
                return (eval < (bestEval + T_int));
        }
    }
    /**
     * implements the adaptive acceptance mechanism (AA1), where a set of parameters is adapted to balance between 
     * intensification and getting out of local optima. This version 1 follows that of Marshall et al. (2015)
     * @param eval the proposed solution's objective value
     * @param eval_0 the current solution's objective value
     * @return
     */
    public boolean adaptive_acceptance1(double eval, double eval_0){
        double rand = RAND.nextDouble();
        double prob = 1 - (1/Counter);
        if(eval < eval_0)
            return true;
        else
            return(rand <= prob);
    }
    /**
     * Updates the Counter of AA1 by 1
     */
    private void updateCounter(){
        Counter++;
    }
    /**
     * Sets the Counter value of AA1 back to 1
     */
    private void resetCounter(){
        Counter = 1;
    }

    /**
     * Implements a simulated annealing acceptance criterion of Marshall et al. (2015)
     * @param eval cost of the proposed/new solution
     * @param eval_0 cost of the incumbent solution
     * @param bestEval cost of the best solution found so far
     * @return
     */
    public boolean SA(double eval, double eval_0, double bestEval){
        if(eval < eval_0)
            return true;
        else{
            double delta = eval - eval_0;
            double Temp = 0.5 * bestEval * Math.pow(0.85, phase - 1);
            double arg = -(delta / Temp);
            double rnd = RAND.nextDouble();
            return (rnd < Math.exp(arg));
        }
    }
    /**
     * increase the phase counter representing the current phase of the search
     */
    public void incrementPhase(){
        phase++;
    }
    /**
     * Implements the Metropolis Acceptance Mechanism in FS-ILS hyper-heuristic of Adriaensen et al. (2014)
     * <br> This acceptance strategy is also called Accept Probabilistic Worse
     * @param eval
     * @param eval_0
     * @return
     */
    public boolean FS_ILS_accept(double eval, double eval_0){
        double impr = eval_0 - eval;
        double rnd;
        if(impr > 0){
            n_impr++;
            avg_impr += (impr - avg_impr) / n_impr;
        }
        double arg = impr / (temp * avg_impr);
        rnd = RAND.nextDouble();
        return (rnd < Math.exp(arg));
    }

    public void SA_restart(){
        avg_impr_SA = 0;
        n_impr_SA = 0;
    }

    public boolean SA_accept(double eval, double eval_0){
        double impr = eval_0 - eval;
        double rnd;
        if(impr > 0){
            n_impr_SA++;
            avg_impr_SA += (impr - avg_impr_SA) / n_impr_SA;
        }
        double arg1 = impr / (SA_temp * avg_impr_SA);
        double arg2 = (double)execTime / ((double)execTime - (double)elapsedTime);
        double arg = arg1 * arg2;
        rnd = RAND.nextDouble();
        return (rnd < Math.exp(arg));
    }

    /**
     * Implements the Record-to-Record travel acceptance mechanism implemented in Marshall et al. (2015)
     * @param eval
     * @param eval_0
     * @param bestEval
     * @return
     */
    public boolean RRT(double eval, double eval_0, double bestEval){
        if(eval < eval_0)
            return true;
        else{
            double arg = 1.03 * bestEval;
            return eval <= arg;
        }
    }

    public void FLS_set_temp(double T){ temp = T; }

    public void FLS_restart(){
        avg_impr = 0;
        n_impr = 0;
    }

    public boolean Monte_Carlo(double eval, double eval_0){
        if(eval < 1){
            eval *= 10000;
            eval_0 *= 10000;
        }
        double delta = -(eval - eval_0);
        double rnd = RAND.nextDouble();
        return(rnd < Math.exp(delta));
    }

    public String getActiveAS(){
        int index = this.active;
        return this.AS[index];
    }
}