package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.MinimalRepair;

import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Config;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.DiscoverContract;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.LustreExtension.RemoveRepairConstructVisitor;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.LustreTranslation.ToLutre;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.ARepair.synthesis.ARepairSynthesis;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Queries.sketchRepair.SketchVisitor;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Statistics.QueryType;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Statistics.TerminationResult;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Util.DiscoveryUtil;
import gov.nasa.jpf.symbc.veritesting.VeritestingUtil.Pair;
import jkind.api.results.JKindResult;
import jkind.api.results.PropertyResult;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.results.UnknownProperty;

import java.util.ArrayList;

import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Config.*;
import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.DiscoverContract.executionTime;
import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.DiscoverContract.repairStatistics;
import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Util.DiscoveryUtil.callJkind;
import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Util.DiscoveryUtil.writeToFile;

public class MinimalRepairDriver {

    private static Program laskKnwnGoodRepairPgm; // last repair the matches the implementation.
    //private static ARepairSynthesis lastSynthizedContract; // last query used to find the above repaired program

    //private static Program counterExamplePgm;

    public static int candidateLoopCount = 0; //counts the candidates attempted inside every tightness loop.

    public static int knownRepairLoopCount = 0; // counts how many tight repairs we needed to do.

    public static int successfulCandidateNum = -1;

    public static int lastKnownRepairLoopCount = -1;

    public static ArrayList<Node> repairs = new ArrayList<>();

    private static long thereExistsTime;

    private static long forAllTime;

    public static void resetState() {
        laskKnwnGoodRepairPgm = null;
        candidateLoopCount = 0;
        knownRepairLoopCount = 0;
        successfulCandidateNum = -1;
        lastKnownRepairLoopCount = -1;
        repairs = new ArrayList<>();
        thereExistsTime = 0;
        forAllTime = 0;
    }

    /**
     * This method initiates the discovery of finding minimal repair enclosed in the repairedProgram. It starts by
     * finding the there exist part of finding some R', then proceeds by calling the forall part to ensure that
     * indeed R' is inclosed in R and still matches the implementation.
     *
     * @param counterExamplePgm
     * @param repairedProgram       this is the specification after repair, which we want to find some inner, enclosed
     *                              program.
     * @param lastSynthizedContract This is the last query used for the repaired program.
     * @param flatExtendedPgm       This is the program we started with that has the sketch extendion, repair construct, arround places we want to repair.    @return
     */

    public static Program execute(Program counterExamplePgm, Program repairedProgram, ARepairSynthesis lastSynthizedContract, Program flatExtendedPgm) {


        repairs.add(repairedProgram.getMainNode());

        //MinimalRepairDriver.counterExamplePgm = counterExamplePgm;
        MinimalRepairDriver.laskKnwnGoodRepairPgm = repairedProgram;

        /*//removing the repair expression keeping only the repair value included
        laskKnwnGoodRepairPgm = RemoveRepairConstructVisitor.execute(repairedProgram);
*/


        boolean canFindMoreTighterRepair = true;
        boolean tighterRepairFound = false;

        long singleQueryTime1;


        MinimalRepairSynthesis tPrimeExistsQ = new MinimalRepairSynthesis(lastSynthizedContract, laskKnwnGoodRepairPgm.getMainNode());

        while (canFindMoreTighterRepair) {//we are still trying to discover a minimal repair, thus there is a potiential tighter repair that we haven't discovered so far.

            System.out.println("trying minimal good repair iteration #: " + knownRepairLoopCount);

            while (!tighterRepairFound && canFindMoreTighterRepair) { //while we haven't found a tighter repair and we know that we can find a tighter repair.
                if (candidateLoopCount == (MINIMALLOOP_MAXLOOPCOUNT + 3)) {//exit if we tried max candidates number.
                    canFindMoreTighterRepair = false;
                    repairStatistics.terminationResult = TerminationResult.MINIMAL_MAX_LOOP_REACHED;
                    repairStatistics.lastQueryType = QueryType.FORALL;
                    System.out.println("Minimum Loop Max Count Reached. Aborting");
                } else if (DiscoveryUtil.convertTimeToSecond(System.currentTimeMillis() - executionTime) >= mutantTimeOut) { // aborting if we ever hit the overall mutant timeout.
                    canFindMoreTighterRepair = false;
                    repairStatistics.terminationResult = TerminationResult.MUTANT_TIME_OUT;
                    repairStatistics.lastQueryType = QueryType.FORALL;
                } else {
                    System.out.println("Trying candidate #: " + candidateLoopCount);
                    String fileName;
                    if ((evaluationMode) && (candidateLoopCount < MINIMALLOOP_MAXLOOPCOUNT)) //use the same name if we are in the evaluation mode and we have not exceeded the number of loops
                        fileName = currFaultySpec + "_" + "exists.lus";
                    else
                        fileName = currFaultySpec + "_" + knownRepairLoopCount + "_" + candidateLoopCount + "_" + "exists.lus";

                    writeToFile(fileName, tPrimeExistsQ.toString(), true, false);

                    System.out.println("ThereExists Query of : " + fileName);

                    singleQueryTime1 = System.currentTimeMillis();
                    JKindResult synthesisResult = callJkind(fileName, false, (tPrimeExistsQ.getMaxTestCaseK() - 1), true, true);

                    singleQueryTime1 = (System.currentTimeMillis() - singleQueryTime1);

                    //System.out.println("TIME of ThereExists Query of : " + fileName + "= " + singleQueryTime);
                    System.out.println("TIME = " + DiscoveryUtil.convertTimeToSecond(singleQueryTime1));
                    repairStatistics.printCandStatistics(String.valueOf(knownRepairLoopCount), true, candidateLoopCount, QueryType.THERE_EXISTS, singleQueryTime1);

                    PropertyResult propResult = synthesisResult.getPropertyResult(counterExPropertyName);
                    switch (synthesisResult.getPropertyResult(counterExPropertyName).getStatus()) {
                        case VALID:
                            System.out.println("^-^ Ranger Discovery Result ^-^");
                            System.out.println("No more R' can be found, last known good repair was found at, outer loop # = " + DiscoverContract.outerLoopRepairNum + " minimal repair loop # = " + lastKnownRepairLoopCount);
                            canFindMoreTighterRepair = false;
                            repairStatistics.terminationResult = TerminationResult.TIGHTEST_REACHED;
                            repairStatistics.lastQueryType = QueryType.THERE_EXISTS;
                            break;
                        case INVALID:
                            Pair<Boolean, Boolean> loopCond = runForallQuery(flatExtendedPgm, synthesisResult, counterExamplePgm, tPrimeExistsQ);
                            tighterRepairFound = loopCond.getFirst();
                            canFindMoreTighterRepair = loopCond.getSecond();
                            break;
                        default:
                            System.out.println("^-^ Ranger Discovery Result ^-^");
                            canFindMoreTighterRepair = false;
//                            if (singleQueryTime1 >= timeOut) {
//                                repairStatistics.terminationResult = TerminationResult.MINIMAL_TIMED_OUT;
//                                System.out.println("Property unexpected output (synthesis Query MINIMAL_TIMED_OUT):");
//                            } else {
                            if ((propResult.getProperty() instanceof UnknownProperty) && (((UnknownProperty) propResult.getProperty()).getTrueFor() == lastMaxSteps)) {
                                repairStatistics.terminationResult = TerminationResult.TRUE_FOR_MAX_STEPS;
                                repairStatistics.lastQueryType = QueryType.THERE_EXISTS;
                                System.out.println("Property uknown but TRUE_FOR_Max_STEPS (synthesis Query):" + synthesisResult.getPropertyResult(counterExPropertyName).getStatus().toString());
                            } else {
                                repairStatistics.terminationResult = TerminationResult.MINIMAL_EXISTS_UKNOWN;
                                repairStatistics.lastQueryType = QueryType.THERE_EXISTS;
                                System.out.println("Property unexpected output (synthesis Query):" + synthesisResult.getPropertyResult(counterExPropertyName).getStatus().toString());
                            }
//                            }
                            System.out.println(" No more R' can be found, returning last known good repair.");
                            break;
                    }
                }
            }
            //there are two conditions where this can be reached, either we have found a tighter repair, in which can
            // we want to find the minimal, or we have found out that there is no more tighter repairs could be found
            // and so we'd just abort the whole thing.
            if (tighterRepairFound) {
                repairStatistics.advanceTighterLoop(true);
                tPrimeExistsQ.changeFixedR(laskKnwnGoodRepairPgm.getMainNode());
                tighterRepairFound = false;
                ++knownRepairLoopCount;
                candidateLoopCount = 0;
            }
        }
        if (!tighterRepairFound) repairStatistics.advanceTighterLoop(false);

        //   repairStatistics.printSpecStatistics();

        System.out.println("Minimal repair finished with the following result, outer loop # = " + DiscoverContract.outerLoopRepairNum + " minimal repair loop # = " + lastKnownRepairLoopCount + " the LAST candidate repair loop # = " + successfulCandidateNum);
        return laskKnwnGoodRepairPgm;
    }


    private static Pair<Boolean, Boolean> runForallQuery(Program flatExtendedPgm, JKindResult synthesisResult, Program counterExamplePgm, MinimalRepairSynthesis tPrimeExistsQ) {
        boolean tighterRepairFound = false;
        boolean canFindMoreTighterRepair = true;
        Program forAllQ;
        long singleQueryTime2;
        String fileName;

        Program candTPrimePgm = RemoveRepairConstructVisitor.execute(SketchVisitor.execute(flatExtendedPgm, synthesisResult, true));

        if ((evaluationMode) && (candidateLoopCount < MINIMALLOOP_MAXLOOPCOUNT)) //use the same name if we are in the evaluation mode and we have not exceeded the number of loops
            fileName = currFaultySpec + "_" + "rPrimeCandidate.lus";
        else
            fileName = currFaultySpec + "_" + knownRepairLoopCount + "_" + candidateLoopCount + "_" + "rPrimeCandidate.lus";

        writeToFile(fileName, candTPrimePgm.toString(), true, false);

        forAllQ = MinimalRepairCheck.execute(DiscoverContract.contract, counterExamplePgm, laskKnwnGoodRepairPgm.getMainNode(), candTPrimePgm.getMainNode());

        if ((evaluationMode) && (candidateLoopCount < MINIMALLOOP_MAXLOOPCOUNT)) //use the same name if we are in the evaluation mode and we have not exceeded the number of loops
            fileName = currFaultySpec + "_" + "forall" + ".lus";
        else
            fileName = currFaultySpec + "_" + knownRepairLoopCount + "_" + candidateLoopCount + "_" + "forall" + ".lus";

        writeToFile(fileName, ToLutre.lustreFriendlyString(forAllQ.toString()), true, false);


        singleQueryTime2 = System.currentTimeMillis();

        System.out.println("ForAll Query of : " + fileName);

        JKindResult counterExampleResult = callJkind(fileName, true, -1, true, false);

        singleQueryTime2 = (System.currentTimeMillis() - singleQueryTime2);

        //System.out.println("TIME of forAll Query of : " + fileName + "= " + singleQueryTime);
        System.out.println("TIME = " + DiscoveryUtil.convertTimeToSecond(singleQueryTime2));
        repairStatistics.printCandStatistics(String.valueOf(knownRepairLoopCount), true, candidateLoopCount, QueryType.FORALL, singleQueryTime2);

        switch (counterExampleResult.getPropertyResult(candidateSpecPropertyName).getStatus()) {
            case VALID:
                laskKnwnGoodRepairPgm = candTPrimePgm;
                successfulCandidateNum = candidateLoopCount; //storing the current loop count where a
                lastKnownRepairLoopCount = knownRepairLoopCount; // storing the loop number at which
                if (!containsNode(repairs, candTPrimePgm.getMainNode())) {
                    repairs.add(candTPrimePgm.getMainNode());
                    // the last good tight repair was found.
                    System.out.println("Great! a tighter repair was found at, outer loop # = " + DiscoverContract.outerLoopRepairNum + " minimal repair loop # = " + lastKnownRepairLoopCount + " successful candidate # = " + successfulCandidateNum);

                    // minimal repair was found.
                    tighterRepairFound = true;
                    break;
                } else {
                    System.out.println("encountering the same repair, aborting.");
                    canFindMoreTighterRepair = false;
                    assert false;
                    break;
                }
            case INVALID:
                tPrimeExistsQ.collectCounterExample(counterExampleResult, tPrimeExistsQ.getSynthesizedProgram().getMainNode());
                ++candidateLoopCount;
                break;
            default:
                System.out.println("^-^ Ranger Discovery Result ^-^");
                canFindMoreTighterRepair = false;
//                                    if (singleQueryTime2 >= timeOut) {
//                                        repairStatistics.terminationResult = TerminationResult.MINIMAL_TIMED_OUT;
//                                        System.out.println("Property unexpected output (forall Query MINIMAL_TIMED_OUT):");
//                                    } else {
                repairStatistics.terminationResult = TerminationResult.MINIMAL_FORALL_UNKNOWN;
                repairStatistics.lastQueryType = QueryType.FORALL;
                System.out.println(" Property unexpected output (for all Query):" + counterExampleResult.getPropertyResult(candidateSpecPropertyName).getStatus().toString());
//                                    }

                System.out.println(" No more R' can be found, returning last known good repair.");
                break;
        }
        return new Pair<Boolean, Boolean>(tighterRepairFound, canFindMoreTighterRepair);
    }


    //unfortungely we will do string comparision since node does not implement isEqual method.
    private static boolean containsNode(ArrayList<Node> repairs, Node mainNode) {
        for (Node node : repairs) {
            if (node.toString().equals(mainNode.toString())) return true;
        }
        return false;
    }
}
