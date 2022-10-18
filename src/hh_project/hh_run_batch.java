import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;
import BinPacking.BinPacking;
import FlowShop.FlowShop;
import MAC.MaxCut;
import KP.KnapsackProblem;
import QAP.QAP;
import PersonnelScheduling.PersonnelScheduling;
import SAT.SAT;
import VRP.VRP;
import travelingSalesmanProblem.TSP;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class hh_run_batch {
    static long seed;
    static ProblemDomain problem;
    static long t_allowed;
    static HyperHeuristic algo;
    static String domainName;

    public static void main(String[] args){
        args = new String[]{"1", "5", "6", "31", "530000"};
        if(args.length == 0 || args.length < 5){
            System.out.println("No argument was passed in or incomplete arguments set, program will exit...");
            System.exit(1);
        }
        int batchIter = Integer.parseInt(args[3]); //set the number of times you want each of the competing algorithms to run
        BufferedWriter wr = null;
        BufferedWriter wr2 = null;
        String resultFile, sequenceFile;
        ArrayList<Integer> algos = new ArrayList();
        String[] competingAlgo = args[0].split(",");
        for(String strArg : competingAlgo){
            int arg = Integer.parseInt(strArg);
            algos.add(arg);
        }
        String[] Domains = {"SAT","BP","PS","PFS","TSP","VRP","QAP","KP","MAC"};
        String[] problems = args[1].split(",");
        String[] instances = args[2].split(",");
        int size_of_problems = problems.length;
        if(size_of_problems != instances.length){
            System.out.println("The size of problems does not match the number of instances, the system will exit");
            System.exit(1);
        }
        int problemIndex = 0;
        int Domain = Integer.parseInt(problems[problemIndex].trim());
        int instance = Integer.parseInt(instances[problemIndex].trim());
        resultFile = Domains[Domain] + instance + ".txt";
        sequenceFile = Domains[Domain] + instance + "-sequences.txt";
        try {
            wr = new BufferedWriter(new FileWriter(resultFile));
        } catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
        for(int i=0; i<batchIter; i++){
            seed = new Date().getTime();
            t_allowed = Long.parseLong(args[4]);
            domainName = Domains[Domain] + instance + "";
            Collections.shuffle(algos);
            for(Integer algoH : algos){
                setHH(algoH, i); //instantiate the object of the hyper-heuristic algo for testing
                boolean firstSwitch = false;
                switch(Domain){
                    case 0:
                        problem = new SAT(seed);
                        break;
                    case 1:
                        problem = new BinPacking(seed);
                        break;
                    case 2:
                        problem = new PersonnelScheduling(seed);
                        break;
                    case 3:
                        problem = new FlowShop(seed);
                        break;
                    case 4:
                        problem = new TSP(seed);
                        break;
                    case 5:
                        problem = new VRP(seed);
                        break;
                    case 6:
                        problem = new QAP(seed);
                        break;
                    case 7:
                        problem = new KnapsackProblem(seed);
                        break;
                    case 8:
                        problem = new MaxCut(seed);
                        break;
                    default:
                        System.out.println("Wrong domain entered, only an integer within 0 and 5 is allowed");
                        System.exit(1);
                        break;
                }

                problem.loadInstance(instance);
                algo.setTimeLimit(t_allowed);
                algo.loadProblemDomain(problem);
                System.out.println("Testing "+algo+" for "+t_allowed+" ms on "+problem.getClass().getSimpleName()+
                        "["+instance+"]...");
                algo.run();
                double[] f = algo.getFitnessTrace();
                System.out.println("Fitness Trace");
                for(int fT=0; fT<f.length; fT++)
                    System.out.print(f[fT] + " -> ");
                System.out.println();
                //print out quality of best solution found
                double best_ = algo.getBestSolutionValue();
                String best_solution = problem.bestSolutionToString();
                System.out.println(best_);
                String domain = problem.getClass().getSimpleName()+"["+instance+"]";
                String line = algo + " using seed: " + seed + " on " + domain + ": " + best_;
                String line2 = "Solution as a string: " + best_solution;
                try{
                    wr.write(line); //write the solution quality to the file
                    wr.newLine();
                    //wr.write(line2); //write the solution (as a string) to the file
                    //wr.newLine();
                }
                catch(Exception ex){
                    System.out.println("Error");
                }
            }

            try{
                wr.write("*****************");
                wr.newLine();
                wr2.write("*****************");
                wr2.newLine();
            }

            catch(Exception ex){
                if(wr2 != null) System.out.println("Error");
            }

            if(i == batchIter - 1){
                try {
                    wr.close();
                    if (wr2 != null) wr2.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
                problemIndex++;
                if(problemIndex < size_of_problems) {
                    i = -1; //start solving another problem
                    Domain = Integer.parseInt(problems[problemIndex].trim());
                    instance = Integer.parseInt(instances[problemIndex].trim());
                    resultFile = Domains[Domain] + instance + ".txt";
                    sequenceFile = Domains[Domain] + instance + "-sequences.txt";
                    try {
                        wr = new BufferedWriter(new FileWriter(resultFile));
                        wr2 = new BufferedWriter(new FileWriter(sequenceFile));
                    } catch(IOException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        }
    }

    private static void setHH(int index, int batchIter){
        switch(index){
            case 0:
                algo = new ILS_conf(seed, t_allowed);
                break;
            case 1:
                algo = new EA_ILS_final(seed, t_allowed, domainName, batchIter);
                break;
            default:
                System.err.println("You have entered a wrong algorithm-code");
                System.exit(1);
                break;
        }
    }
}
