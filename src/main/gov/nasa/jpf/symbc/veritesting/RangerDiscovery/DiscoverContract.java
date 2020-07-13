package gov.nasa.jpf.symbc.veritesting.RangerDiscovery;

import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.ThereExistsQuery;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Statistics.QueryType;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Statistics.RepairStatistics;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Statistics.TerminationResult;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Util.DiscoveryUtil;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.LustreExtension.LustreAstMapExtnVisitor;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.LustreExtension.RemoveRepairConstructVisitor;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.MinimalRepair.MinimalRepairDriver;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.ARepair.CounterExampleQuery;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.ARepair.repair.HolePlugger;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.sketchRepair.FlattenNodes;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.sketchRepair.SketchVisitor;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.ARepair.synthesis.*;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.mutation.MutationType;
import gov.nasa.jpf.symbc.veritesting.VeritestingUtil.Pair;
import gov.nasa.jpf.symbc.veritesting.ast.transformations.Environment.DynamicRegion;
import gov.nasa.jpf.vm.ThreadInfo;
import jkind.JKindException;
import jkind.api.results.JKindResult;
import jkind.lustre.*;
import jkind.lustre.parsing.LustreParseUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Config.*;
import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Util.DiscoveryUtil.callJkind;
import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Util.DiscoveryUtil.writeToFile;

public class DiscoverContract {
    /**
     * name of the method we want to extract its contract.
     */
    public static boolean contractDiscoveryOn = false;
    public static boolean discoveryAttempted = false;
    public static RepairStatistics repairStatistics;

    public static LinkedHashSet<Pair> z3QuerySet = new LinkedHashSet();

    //TODO: These needs to be configured using the .jpf file.

    public static List<String> userSynNodes = new ArrayList<>();

    //public static HoleRepair holeRepairHolder = new HoleRepair();
    public static HoleRepairState holeRepairState = new HoleRepairState();

    public static int loopCount = 0;
    public static int permutationCount = 0;

    public static int outerLoopRepairNum = -1;

    public static Contract contract;
    public static DynamicRegion dynRegion;

    public static boolean specAlreadyMatching = false;

/***** begin of unused vars***/
    /**
     * currently unused because we assume we have a way to find the input and output.
     * This later needs to be changed to generalize it by looking only at the method
     * and the class of interest.
     */
    public static String className;
    private static boolean repaired;
    private static String innerDirectory; // directory under ../src/DiscoveryExample
    public static long executionTime = 0;

    /***** end of unused vars***/

    public static void updateRegion(DynamicRegion dynRegion){
        DiscoverContract.dynRegion = dynRegion;
    }

    public static final void discoverLusterContract(ThreadInfo ti, DynamicRegion dynRegion) {
        Config.ti = ti;
        DiscoverContract.dynRegion = dynRegion;
        fillUserSynNodes();
        try {
            while (!specAlreadyMatching && Config.canSetup()) {

                System.out.println("-|-|-|-|-|  resetting state and trying repairing: " + currFaultySpec);
                resetState();
                assert (userSynNodes.size() > 0);
                if (Config.specLevelRepair)
                    try {
                        executionTime = System.currentTimeMillis();
                        repairSpec();
                    } catch (JKindException jkindExp) {
                        System.out.println("jkind exception encountered aborting specification" + jkindExp);
                        repairStatistics.terminationResult = TerminationResult.OTHER_JKIND_EXCEPTION;
                        repairStatistics.lastQueryType = QueryType.UNKONWN;
                        repairStatistics.printSpecStatistics();
                        assert false;
                    }
                else
                    assert false; //removed definition repair for now.
                //repairDef(dynRegion);
                //executionTime = (System.currentTimeMillis() - executionTime) / milliSecondSimplification;
                allMutationStatistics.doneAllMutants();
                System.out.println("The overall time for : " + currFaultySpec + "= " + executionTime + " sec");
            }
        } catch (IOException e) {
            System.out.println("Unable to read specification file.! Aborting");
            assert false;
            e.printStackTrace();
        }
    }


    public static void resetState() {
        loopCount = 0;
        permutationCount = 0;
        outerLoopRepairNum = 0;
        repaired = false;
        //userSynNodes = new ArrayList<>(); //stop resetting that, now it is entered manually.

        CounterExampleQuery.resetState();
        ThereExistsQuery.resetState();
        MinimalRepairDriver.resetState();
        TestCaseManager.resetState();
        LustreAstMapExtnVisitor.resetState();
    }

    private static void repairSpec() throws IOException {
        String fileName;

        /*if (Config.repairInitialValues)
            System.out.println("Repair includes initial values");
        else
            System.out.println("Repair does NOT include initial values");
*/
        System.out.println("Running References on mac?   ---> " + mac);
        System.out.println("Running Evaluation Mode?   ---> " + evaluationMode);

        System.out.println("Outer loop max count:   ---> " + OUTERLOOP_MAXLOOPCOUNT);
        System.out.println("Minimal loop max count:   ---> " + MINIMALLOOP_MAXLOOPCOUNT);
        repairStatistics = new RepairStatistics(tFileName, MutationType.UNKNOWN);

        //print out the translation once, for very first time we hit linearlization for the method of
        // interest.
        //contract = new Contract();

        //this holds a repair which we might find, but it might not be a tight repair, in which case we'll have to
        // call on the other pair of thereExists and forAll queries for finding minimal repair.
        ARepairSynthesis aRepairSynthesis = null;
        HolePlugger holePlugger = new HolePlugger();
        Program originalProgram, flatExtendedPgm = null;
        Program inputExtendedPgm = null; // holds the original program with the extended lustre feature of the
        // "repair" construct

        NodeRepairKey originalNodeKey;

        if (Config.repairMode == RepairMode.LIBRARY) {

            inputExtendedPgm = LustreParseUtil.program(new String(Files.readAllBytes(Paths.get(tFileName)),
                    "UTF-8"));


            originalNodeKey = defineNodeKeys(inputExtendedPgm);

            flatExtendedPgm = FlattenNodes.execute(inputExtendedPgm);

            originalProgram = RemoveRepairConstructVisitor.execute(flatExtendedPgm);

            /* //this has moved to Config.java
            String mutationDir = "../src/DiscoveryExamples/mutants";
            ArrayList<MutationResult> mutationResults = createSpecMutants(originalProgram, mutationDir, contract.tInOutManager);
            System.out.println("wrote " + mutationResults.size() + " mutants into the " + mutationDir + " folder");*/

        } else {
            originalProgram = LustreParseUtil.program(new String(Files.readAllBytes(Paths.get(tFileName)),
                    "UTF-8"));

            originalNodeKey = defineNodeKeys(originalProgram);

        }

        CounterExampleQuery counterExampleQuery = new CounterExampleQuery(originalProgram);
        String counterExampleQueryStrStr = counterExampleQuery.toString();

        do {
            System.out.println("Forall outer matching query.");
            if ((evaluationMode) && (loopCount < OUTERLOOP_MAXLOOPCOUNT)) //use only a single file in the evaluation mode and when we have not reached the limit of the loop count.
                fileName = currFaultySpec + ".lus";
            else
                fileName = currFaultySpec + "_" + loopCount + "_forall.lus";

            writeToFile(fileName, counterExampleQueryStrStr, false, false);
            long singleQueryTime = System.currentTimeMillis();

            JKindResult counterExResult = callJkind(fileName, true, -1, false, false);
            singleQueryTime = (System.currentTimeMillis() - singleQueryTime);
            System.out.println("TIME = " + DiscoveryUtil.convertTimeToSecond(singleQueryTime));

            repairStatistics.printCandStatistics(String.valueOf(loopCount), false, -1, QueryType.FORALL, singleQueryTime);
            switch (counterExResult.getPropertyResult(tnodeSpecPropertyName).getStatus()) {
                case VALID: //valid match
                    System.out.println("^-^Starting Minimal Loop ^-^");
                    repairStatistics.advanceTighterLoop(true);
                    if (loopCount > 0) {// we had at least a single repair/synthesis, at that point we want to find
                        // minimal repair.
                        outerLoopRepairNum = loopCount;
                        System.out.println("Initial repair found, in iteration #: " + outerLoopRepairNum);
                        System.out.println("Trying minimal repair.");
                        Program minimalRepair = MinimalRepairDriver.execute(counterExampleQuery.getCounterExamplePgm
                                        (), originalProgram,
                                aRepairSynthesis, flatExtendedPgm);
                        repairStatistics.printSpecStatistics();
                    } else {
                        System.out.println("Contract Matching! Printing repair and aborting!");
                        repairStatistics.terminationResult = TerminationResult.ALREADY_MATCHING;
                        repairStatistics.lastQueryType = QueryType.FORALL;
                        repairStatistics.advanceTighterLoop(false);
                        repairStatistics.printSpecStatistics();
                    }
                    //System.out.println(getTnodeFromStr(fileName));
                    DiscoverContract.repaired = true;
                    //repairStatistics.printSpecStatistics();
                    return;
                case INVALID: //synthesis is needed
                    if (aRepairSynthesis == null) {
                        Program holeProgram = null;
                        ArrayList<Hole> holes = null;
                        switch (Config.repairMode) {
                            case CONSTANT:
                                holeProgram = SpecConstHoleVisitor.executeMain(LustreParseUtil.program(originalProgram.toString()), originalNodeKey);
                                holes = new ArrayList<>(SpecConstHoleVisitor.getHoles());
                                break;
                            case PRE:
                                holeProgram = SpecPreHoleVisitor.executeMain(LustreParseUtil.program(originalProgram.toString()), originalNodeKey);
                                holes = new ArrayList<>(SpecPreHoleVisitor.getHoles());
                                break;
                            case LIBRARY:
                                holeProgram = LustreAstMapExtnVisitor.execute(flatExtendedPgm);
                                holes = new ArrayList<>(LustreAstMapExtnVisitor.getHoles());
                                break;
                            default:
                                assert false;
                        }
                        aRepairSynthesis = new ARepairSynthesis(contract, holeProgram, holes, counterExResult, originalNodeKey);
                    } else
                        aRepairSynthesis.collectCounterExample(counterExResult);

                    if (loopCount == 0) //first loop, then setup initial repair values
                        holeRepairState.createEmptyHoleRepairValues();

                    String synthesisContractStr = aRepairSynthesis.toString();
                    if ((evaluationMode) && (loopCount < OUTERLOOP_MAXLOOPCOUNT)) //only a single file is used if the loop bound has not been exceeded.
                        fileName = currFaultySpec + "_" + "hole.lus";
                    else
                        fileName = currFaultySpec + "_" + loopCount + "_" + "exists.lus";

                    writeToFile(fileName, synthesisContractStr, false, false);
                    System.out.println("repairing holes for iteration#:" + loopCount);

                    singleQueryTime = System.currentTimeMillis();
                    JKindResult synthesisResult = callJkind(fileName, false, aRepairSynthesis
                            .getMaxTestCaseK() - 1, false, false);
                    singleQueryTime = (System.currentTimeMillis() - singleQueryTime);

                    System.out.println("TIME = " + DiscoveryUtil.convertTimeToSecond(singleQueryTime));
                    repairStatistics.printCandStatistics(String.valueOf(loopCount), false, -1, QueryType.THERE_EXISTS, singleQueryTime);

                    switch (synthesisResult.getPropertyResult(counterExPropertyName).getStatus()) {
                        case VALID:
                            System.out.println("^-^ Ranger Discovery Result ^-^");
                            System.out.println("Cannot find a synthesis");
                            DiscoverContract.repaired = false;
                            repairStatistics.advanceTighterLoop(false);
                            repairStatistics.terminationResult = TerminationResult.NO_VALID_SYNTHESIS_FOR_GRAMMAR;
                            repairStatistics.lastQueryType = QueryType.THERE_EXISTS;
                            repairStatistics.advanceTighterLoop(false);
                            repairStatistics.printSpecStatistics();
                            return;
                        case INVALID:
                            if (Config.repairMode != RepairMode.LIBRARY) {
                                holeRepairState.plugInHoles(synthesisResult);
                                holePlugger.plugInHoles(synthesisResult, counterExampleQuery
                                        .getCounterExamplePgm
                                                (), aRepairSynthesis.getSynthesizedProgram(), aRepairSynthesis.getSynNodeKey());
                                counterExampleQueryStrStr = holePlugger.toString();
                                DiscoveryUtil.appendToFile(holeRepairFileName, holeRepairState.toString());
                                break;
                            } else {
                                inputExtendedPgm = SketchVisitor.execute(flatExtendedPgm, synthesisResult, false);
                                originalProgram = RemoveRepairConstructVisitor.execute(inputExtendedPgm);
                                /*if (!evaluationMode) {
                                    fileName = currFaultySpec + "_Extn" + loopCount + 1 + ".lus";
                                    writeToFile(fileName, inputExtendedPgm.toString(), false, false);
                                }*/

                                counterExampleQuery = new CounterExampleQuery(originalProgram);
                                counterExampleQueryStrStr = counterExampleQuery.toString();
                                break;
                            }
                        default:
                            System.out.println("unexpected property status for synthesis query " + synthesisResult.getPropertyResult(counterExPropertyName).getStatus().toString());
                            DiscoverContract.repaired = false;
                            /*if (singleQueryTime >= timeOut)
                                repairStatistics.terminationResult = TerminationResult.OUTERLOOP_TIMED_OUT;
                            else*/
                            repairStatistics.terminationResult = TerminationResult.OUTERLOOP_EXISTS_UNKNOWN;
                            repairStatistics.lastQueryType = QueryType.THERE_EXISTS;
                            repairStatistics.advanceTighterLoop(false);
                            repairStatistics.printSpecStatistics();
                            //assert false;
                            return;
                    }
                    break;
                default:
                    System.out.println("Outerloop unexpected property status for the counter example query: " + counterExResult.getPropertyResult(tnodeSpecPropertyName).getStatus().toString());
                    DiscoverContract.repaired = false;
                    /*if (singleQueryTime >= timeOut)
                        repairStatistics.terminationResult = TerminationResult.OUTERLOOP_TIMED_OUT;
                    else*/
                    repairStatistics.terminationResult = TerminationResult.OUTERLOOP_FORALL_UKNOWN;
                    repairStatistics.lastQueryType = QueryType.FORALL;
                    repairStatistics.advanceTighterLoop(false);
                    repairStatistics.printSpecStatistics();
                    return;
            }
            ++loopCount;
            if (loopCount == OUTERLOOP_MAXLOOPCOUNT + 3) {
                repairStatistics.terminationResult = TerminationResult.OUTERLOOP_MAX_LOOP_REACHED;
                repairStatistics.lastQueryType = QueryType.THERE_EXISTS;
                repairStatistics.advanceTighterLoop(false);
                repairStatistics.printSpecStatistics();
                return;
            }
        }
        while (true);
    }



/*
    public static Program getLustreNoExt(Program origLustreExtPgm) {
        return RemoveRepairConstructVisitor.execute(origLustreExtPgm);

    }*/

    private static Node getTnodeFromStr(String tFileName) throws IOException {
        Program program = LustreParseUtil.program(new String(Files.readAllBytes(Paths.get(folderName + "/" + tFileName)), "UTF-8"));

        List<Node> nodes = program.nodes;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id.equals(TNODE))
                return nodes.get(i);
        }

        return null;
    }

    /**
     * Initiall node keys are defined on the original program, where the "main" is the only node that needs repair, as well as any other nodes that the user wants to define in userSynNodes
     *
     * @param program
     * @return
     */
    private static NodeRepairKey defineNodeKeys(Program program) {
        NodeRepairKey nodeRepairKey = new NodeRepairKey();
        nodeRepairKey.setNodesKey("main", NodeStatus.REPAIR);
        nodeRepairKey.setNodesKey(userSynNodes, NodeStatus.REPAIR);

        for (int i = 0; i < program.nodes.size(); i++) {
            Node node = program.nodes.get(i);
            if (!node.id.equals("main"))
                nodeRepairKey.setNodesKey(node.id, NodeStatus.DONTCARE_SPEC);
        }

        return nodeRepairKey;
    }

    private static void fillUserSynNodes() {
        userSynNodes.add("main");
    }


    //ToDo: not sure if this works, I need to test the change.
    public static String toSMT(String solver, HashSet z3FunDecl) {
        return Z3Format.toSMT(solver, z3FunDecl);
    }


    public static boolean isRepaired() {
        return repaired;
    }
}
