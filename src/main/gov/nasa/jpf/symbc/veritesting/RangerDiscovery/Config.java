package gov.nasa.jpf.symbc.veritesting.RangerDiscovery;

import gov.nasa.jpf.symbc.VeritestingListener;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Statistics.AllMutationStatistics;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.mutation.MutationResult;
import gov.nasa.jpf.symbc.veritesting.VeritestingUtil.Pair;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import jkind.lustre.Ast;
import jkind.lustre.BoolExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Program;
import jkind.lustre.parsing.LustreParseUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.mutation.MutationUtils.createSpecMutants;
import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.mutation.ProcessMutants.processMutants;

public class Config {
    public static String counterExPropertyName = "fail";
    public static String folderName = "../src/DiscoveryExamples/";
    public static String symVarName;
    public static int lastMaxSteps; //this is the last maximum steps used in the last BMC
    public static ThreadInfo ti;
    public static ArrayList<ElementInfo> objrefs = new ArrayList<>();

    // atom synthesized
    static String tFileName;
    static String holeRepairFileName = folderName + "holeRepair";
    public static String TNODE = "T_node"; // also refers to the R_prime in the refinement loop.
    public static String RNODE = "Ranger_node";
    public static String WRAPPERNODE = "Ranger_wrapper";
    public static String CHECKSPECNODE = "Check_spec";
    public static String H_discovery = "H_discovery";
    public static String FIXED_T = "Fixed_T";
    public static String CAND_T_PRIME = "Candidate_T_Prime";
    public static String specPropertyName = "ok";
    public static String wrapperOutpuName = "out";
    // of finding a different R.

    public static String tnodeSpecPropertyName;
    public static String candidateSpecPropertyName = "discovery_out";

    public static Ast defaultHoleValBool = new BoolExpr(false);
    public static Ast defaultHoleValInt = new IntExpr(1);

    public static boolean useInitialSpecValues = true;
    public static String genericRepairNodeName = "repairNode";
    //this boolean toggles between equation based repair and whole spec repair.
    public static boolean specLevelRepair;// = false;
    //this contains specific equations we would like to repair, instead of repairing the whole thing. This is now used for testing only.
    public static Integer[] equationNumToRepair = {1};
    public static boolean allEqRepair = true;


    /***** configurations needs consideration for each run in .jpf file *********/
    public static RepairScopeType repairScope = RepairScopeType.ENCLOSED_TERMS; //the default configuration.
    public static boolean randZ3Seed = false;
    public static String spec;
    public static String currFaultySpec;
    public static boolean printMutantDir = false;
    public static boolean mutationEnabled = true;
    public static boolean repairMutantsOnly = false;
    public static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.RepairMode repairMode;
    public static String[] faultySpecs;
    public static int[] repairDepth;
    public static boolean[] perfectMutantFlags;
    public static boolean z3Solver = true;
    public static int repairNodeDepth = 1; //defines the depth of the repair node. A depth 0 means a single boolean
    public static boolean depthFixed = false;
    public static boolean rangeValueAnalysis = true;

    public static boolean evaluationMode = false;
    public static int timeOut = 600; //in seconds - time out of every jkind query
    public static int mutantTimeOut = 3600;  // in seconds time out for a mutant repair
    public static boolean mac = true;

    public static final int OUTERLOOP_MAXLOOPCOUNT = 5;
    public static final int MINIMALLOOP_MAXLOOPCOUNT = 30; //found 378 iteration, then we find a repair.

    /*
    //testing
        public static int timeOut = 3; //in seconds
        public static boolean mac = true;

        public static final int OUTERLOOP_MAXLOOPCOUNT = 2;
        public static final int MINIMALLOOP_MAXLOOPCOUNT = 3; //found 378 iteration, then we find a repair.
    //end testing
     */
    public static int faultySpecIndex = 0;

    public static boolean repairInitialValues = true;

    private static boolean firstTime = true;

    static String mutationDir = "../src/DiscoveryExamples/mutants";

    public static int milliSecondSimplification = 1000; // must be in thousands, since it is usually used for simplification and to also check if we exceeded the timeout which is also in seconds.

    public static AllMutationStatistics allMutationStatistics;

    public static OperationMode operationMode = OperationMode.NORMAL;
    public static boolean randomSample = true;
    public static int goalMutantNum;


    public static int prop; //name of the property, which is used in conjunction with the spec and rundomSampleMutants on to populate the right number of mutants to operate on for this property provided the maximum sample we would have is in maxSampleMutants.
    private static int maxRandForProp; // maximum number of mutants that we are going to sample
    private static int samplesSoFar = 0; //this is the number of samples that we have finished so far. We shoul stop when they read the maxRandforProp.
    public static boolean regressionTestOn = false;


    public static boolean canSetup() throws IOException {

        if (firstTime) {
            DiscoverContract.contract = new Contract();
            allMutationStatistics = new AllMutationStatistics();
            firstTime = false;
            if (mutationEnabled) {
                tFileName = folderName + currFaultySpec;
                Program origSpec = LustreParseUtil.program(new String(Files.readAllBytes(Paths.get(tFileName)), "UTF-8"));
                ArrayList<MutationResult> mutationResults = createSpecMutants(origSpec, mutationDir, DiscoverContract.contract.tInOutManager);
                Pair<Pair<String[], int[]>, boolean[]> triple = processMutants(mutationResults, origSpec, currFaultySpec, operationMode);
                faultySpecs = triple.getFirst().getFirst();
                repairDepth = triple.getFirst().getSecond();
                perfectMutantFlags = triple.getSecond();
                System.out.println("OperationMode is " + operationMode.name());
            }
            if (randomSample) {
                computeUniformPropDistribution();
                System.out.println("maxMutants for Random Sampling is =" + goalMutantNum + "benchmark Sample = " + maxRandForProp);
                faultySpecIndex = new Random().nextInt(faultySpecs.length);
                ++samplesSoFar;
            }
        }

        assert faultySpecs.length != 0;

        if (randomSample && samplesSoFar > maxRandForProp) return false;
        if ((faultySpecIndex) >= faultySpecs.length) return false;

        currFaultySpec = faultySpecs[faultySpecIndex];

        if (mutationEnabled)
            repairNodeDepth = repairDepth[faultySpecIndex];

        if (!randomSample) ++faultySpecIndex;
        else {
            faultySpecIndex = new Random().nextInt(faultySpecs.length);
            ++samplesSoFar;
        }

        tFileName = folderName + currFaultySpec;
        if (!mutationEnabled) { //sanity check
            Program origSpec = LustreParseUtil.program(new String(Files.readAllBytes(Paths.get(tFileName)), "UTF-8"));
            if (!regressionTestOn && origSpec.repairNodes.size() == 0) {
                System.out.println("repair nodes can not be zero if we are not using mutation. The user needs to specify a repair node and repair expr");
                assert false;
            }
        }
        tnodeSpecPropertyName = "T_node~0.p1";

        //make a new directory for the output of that spec
        new File(folderName + "/output/" + Config.currFaultySpec).mkdirs();

        VeritestingListener.simplify = false; //forcing simplification to be false for now
        return true;
    }

    /**
     * used to divide the maxMutants used for the expirement among the benchmarks. The division is dependent on the number of mutants that can be generated for each property, and it is precomputed. Alarm = 22%, Infusion = 58%, TCAS = 9% and WBS = 10%. These percentages are also divided by properties for every benchmark where
     * ALARM	Prop1	2
     * Prop2	2
     * Prop3	4
     * Prop4	1
     * Prop5	1
     * Prop6	6
     * Prop7	3
     * Prop8	3
     * Prop9	1
     * Prop10	0
     * 22
     * INFUSION MGR	Prop1	26
     * Prop2	1
     * Prop3	1
     * Prop5	1
     * Prop6	13
     * Prop7	1
     * Prop8	3
     * Prop9	5
     * Prop10	2
     * Prop11	2
     * Prop12	2
     * Prop13	1
     * 58
     * TCAS	Prop1	3
     * Prop2	3
     * Prop4	3
     * 9
     * WBS	Prop1	6
     * Prop3	4
     */
    private static void computeBenchmarkMaxSample() {
        if (spec.equals("gpca")) {
            if (prop == 1) {
                maxRandForProp = (int) (goalMutantNum * 0.02);
            } else if (prop == 2) {
                maxRandForProp = (int) (goalMutantNum * 0.02);
            } else if (prop == 3) {
                maxRandForProp = (int) (goalMutantNum * 0.04);
            } else if (prop == 4) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 5) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 6) {
                maxRandForProp = (int) (goalMutantNum * 0.06);
            } else if (prop == 7) {
                maxRandForProp = (int) (goalMutantNum * 0.03);
            } else if (prop == 8) {
                maxRandForProp = (int) (goalMutantNum * 0.03);
            } else if (prop == 9) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 10) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else {
                System.out.println("unknown property for spec. cannot sample. aborting");
                assert false;
            }
        } else if (spec.equals("infusion")) {
            if (prop == 1) {
                maxRandForProp = (int) (goalMutantNum * 0.26);
            } else if (prop == 2) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 3) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 5) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 6) {
                maxRandForProp = (int) (goalMutantNum * 0.13);
            } else if (prop == 7) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 8) {
                maxRandForProp = (int) (goalMutantNum * 0.03);
            } else if (prop == 9) {
                maxRandForProp = (int) (goalMutantNum * 0.05);
            } else if (prop == 10) {
                maxRandForProp = (int) (goalMutantNum * 0.02);
            } else if (prop == 11) {
                maxRandForProp = (int) (goalMutantNum * 0.02);
            } else if (prop == 12) {
                maxRandForProp = (int) (goalMutantNum * 0.02);
            } else if (prop == 13) {
                maxRandForProp = (int) (goalMutantNum * 0.01);
            } else if (prop == 4 || prop == 14) {
                System.out.println("Invalid Props are not part of the sampling");
                assert false;
            } else {
                System.out.println("unknown property for spec. cannot sample. aborting");
                assert false;
            }
        } else if (spec.equals("tcas")) {
            if (prop == 1) {
                maxRandForProp = (int) (goalMutantNum * 0.03);
            } else if (prop == 2) {
                maxRandForProp = (int) (goalMutantNum * 0.03);
            } else if (prop == 4) {
                maxRandForProp = (int) (goalMutantNum * 0.03);
            } else if (prop == 3) {
                System.out.println("Invalid Props are not part of the sampling");
                assert false;
            } else {
                System.out.println("unknown property for spec. cannot sample. aborting");
                assert false;
            }
        } else if (spec.equals("wbs")) {
            if (prop == 1) {
                maxRandForProp = (int) (goalMutantNum * 0.06);
            } else if (prop == 3) {
                maxRandForProp = (int) (goalMutantNum * 0.04);
            } else if (prop == 2 || prop == 4 || prop == 5) {
                System.out.println("Invalid Props are not part of the sampling");
                assert false;
            } else {
                System.out.println("unknown property for spec. cannot sample. aborting");
                assert false;
            }
        } else {
            System.out.println("cannot sample from unknown benchmark");
        }
    }

    private static void computeUniformPropDistribution() {
        maxRandForProp = 69;
        goalMutantNum = 69 * 29;
    }

    public static boolean isCurrMutantPerfect() {
        /*if (randomSample)
            if (perfectMutantFlags[faultySpecIndex])
                return true;
            else return false;
        else if (perfectMutantFlags[faultySpecIndex - 1])
            return true;
        else return false;*/
        return false;
    }
}
