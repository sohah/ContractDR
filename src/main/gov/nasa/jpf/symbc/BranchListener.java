package gov.nasa.jpf.symbc;


import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.WalaException;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.symbc.bytecode.branchchoices.util.IFInstrSymbHelper;
import gov.nasa.jpf.symbc.veritesting.VeritestingUtil.SpfUtil;
import gov.nasa.jpf.symbc.veritesting.branchcoverage.BranchCoverage;
import gov.nasa.jpf.symbc.veritesting.branchcoverage.CoverageStatistics;
import gov.nasa.jpf.symbc.veritesting.branchcoverage.RunMode;
import gov.nasa.jpf.symbc.veritesting.branchcoverage.obligation.CoverageUtil;
import gov.nasa.jpf.symbc.veritesting.branchcoverage.obligation.Obligation;
import gov.nasa.jpf.symbc.veritesting.branchcoverage.obligation.ObligationMgr;
import gov.nasa.jpf.symbc.veritesting.branchcoverage.obligation.ObligationSide;
import gov.nasa.jpf.vm.*;

import java.io.*;

import static gov.nasa.jpf.symbc.veritesting.branchcoverage.obligation.ObligationMgr.printCoverage;
import static gov.nasa.jpf.symbc.veritesting.branchcoverage.obligation.ObligationMgr.printReachability;

public class BranchListener extends PropertyListenerAdapter implements PublisherExtension {

    boolean firstTime = true;

    public static String targetClass;
    public static String targetAbsPath;
    public static RunMode coverageMode = RunMode.GUIDED_SPF; //1 for vanilla spf mode, 2 for Branch Coverage mode, 3 for guided SPF

    // used to flag that the executed branch instruction is symbolic or not. In which case the "instructionExecuted" should let the "firstStepInstruction" check in place, i.e., to return to spf to create
    // the appropriate set of choices, otherwise if it isn't symbolic then it will only invoke "instructionExecuted" only once, and thus we shouldn't return then, and we should check and/or collect obligations then
    public static boolean isSymBranchInst = false;

    public static boolean newCoverageFound = false;
    private boolean allObligationsCovered = false;
    private CoverageStatistics coverageStatistics = new CoverageStatistics();

    public BranchListener(Config conf, JPF jpf) {
        jpf.addPublisherExtension(ConsolePublisher.class, this);

        if ((conf.getString("targetAbsPath") == null)) {
            System.out.println("target class or its absolute path is undefined in jpf file for coverage. Aborting");
            assert false;
        }
        targetClass = conf.getString("target");
        targetAbsPath = conf.getString("targetAbsPath");

        if (conf.hasValue("runMode")) if (conf.getInt("runMode") == 1) coverageMode = RunMode.VANILLA_SPF;
        else if (conf.getInt("runMode") == 2) coverageMode = RunMode.CHECK_COVERAGE_SPF;
        else if (conf.getInt("runMode") == 3) coverageMode = RunMode.GUIDED_SPF;
        else {
            System.out.println("unknown mode. Failing");
            assert false;
        }

        if ((conf.hasValue("GuideBranchExploration")))
            BranchSymInstructionFactory.GuideBranchExploration = conf.getBoolean("GuideBranchExploration");
    }

    public void executeInstruction(VM vm, ThreadInfo ti, Instruction instructionToExecute) {
        if (allObligationsCovered) {
            ti.getVM().getSystemState().setIgnored(true);
            return;
        }

        try {
            if (firstTime) {
                BranchCoverage.createObligations(ti);
                ObligationMgr.finishedCollection();
                firstTime = false;
                printCoverage();
                printReachability();
                System.out.println("|-|-|-|-|-|-|-|-|-|-|-|-finished obligation collection|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-");
            } else {
                if (instructionToExecute instanceof IfInstruction) {
                    isSymBranchInst = SpfUtil.isSymCond(ti, instructionToExecute);
                    if ((isSymBranchInst) && (coverageMode == RunMode.GUIDED_SPF)) {
                        guideSPF(ti, instructionToExecute);
                    }
                }
            }
        } catch (ClassHierarchyException e) {
            e.printStackTrace();
            assert false;
        } catch (IOException | CallGraphBuilderCancelException | WalaException e) {
            e.printStackTrace();
        }
    }

    private void guideSPF(ThreadInfo ti, Instruction instructionToExecute) {
        Obligation oblgThen = CoverageUtil.createOblgFromIfInst((IfInstruction) instructionToExecute, ObligationSide.THEN);
        Obligation oblgElse = CoverageUtil.createOblgFromIfInst((IfInstruction) instructionToExecute, ObligationSide.ELSE);

        //only the then obligation is stored in the reachability map since it is the same in both the then and the else side of a given node.
        Obligation[] uncoveredReachThenOblg = ObligationMgr.isReachableOblgsCovered(oblgThen);
        Obligation[] uncoveredReachElseOblg = ObligationMgr.isReachableOblgsCovered(oblgElse);

        //uncoveredReachThenOblg <=> uncoveredReachElseOblg; iff relation
        assert ((!(uncoveredReachThenOblg == null) || uncoveredReachElseOblg == null) && (!(uncoveredReachElseOblg == null)) || uncoveredReachThenOblg == null);

        if ((uncoveredReachThenOblg == null))//indicating an obligation that we do not care about covering, i.e., not an application code.
            return;

        System.out.println("before execution of  instruction: " + instructionToExecute);

        if ((uncoveredReachElseOblg.length == 0) && (uncoveredReachThenOblg.length == 0) && !newCoverageFound) {//no new obligation can be reached
            ti.getVM().getSystemState().setIgnored(true);
            System.out.println("path is ignored");
        } else {//this is where we have something uncovered and we want to create choices to guide spf - this is not needed in concrete branches
            //default setting is "else" exploration then the "then" exploration. flip if needed
            if ((uncoveredReachThenOblg.length > uncoveredReachElseOblg.length) // if then has more reachable obligations
                    || ((ObligationMgr.isOblgCovered(oblgElse) && !ObligationMgr.isOblgCovered(oblgThen))) //if "else" side has been already covered but the "then" is not covered yet
            ) {
                if (!ti.isFirstStepInsn()) { // first time around
                    IFInstrSymbHelper.flipBranchExploration = true;
                    System.out.println("flipping then and else sides.");
                } else
                    IFInstrSymbHelper.flipBranchExploration = false;
            }
        }
//        BranchCovPCChoiceGenerator.execute(ti, (IfInstruction) instructionToExecute, flip);
    }


    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
        if (coverageMode == RunMode.VANILLA_SPF) runVanillaSPF(executedInstruction, currentThread);
        else if ((coverageMode == RunMode.GUIDED_SPF) || (coverageMode == RunMode.CHECK_COVERAGE_SPF))
            collectCoverage(executedInstruction, currentThread);
        else {
            System.out.println("cannot run in mode:" + coverageMode);
            assert false;
        }
    }

    //executes all paths and all obligations without any side effect of checking covered obligations,
    // it just tracks the order of execution of interesting user code.
    // it skips printing visiting symbolic instructions, with two choices feasible, for the first time only. Since at that point SPF creates the choices but does not
    // execute the semantics of the bytecode instruction yet.
    private void runVanillaSPF(Instruction executedInstruction, ThreadInfo currentThread) {
//        System.out.println("after inst: " + executedInstruction);
        if (executedInstruction instanceof IfInstruction) {
            //used to check if we are in the case of symbolic instruction and we are hitting for the first time. As we want to only intercept
            //either symbolic instruction after isFirstStepInsn has finished or a concerte instruction
            Instruction nextInst = currentThread.getNextPC();
            if (!currentThread.isFirstStepInsn() && (nextInst == executedInstruction)) { // the second condition indicates that the spf listener have not recognoized a single choice being possible, because if it does then the nextPc would have been either its target or its nextInst
                return;
            }
            System.out.println("after execution of  instruction: " + executedInstruction);


            ObligationSide oblgSide;
            if (((IfInstruction) executedInstruction).getTarget() == nextInst)
                oblgSide = ObligationSide.THEN;
            else {
                oblgSide = ObligationSide.ELSE;
                assert (executedInstruction).getNext() == nextInst;
            }
            Obligation oblg = CoverageUtil.createOblgFromIfInst((IfInstruction) executedInstruction, oblgSide);
            if (ObligationMgr.oblgExists(oblg)) System.out.println("Executing obligation" + oblg);

            isSymBranchInst = false;
        }
    }

    private void collectCoverage(Instruction executedInstruction, ThreadInfo currentThread) {
        if (allObligationsCovered) {
            return;
        }

        if (executedInstruction instanceof IfInstruction) {

            //used to check if we are in the case of symbolic instruction and we are hitting for the first time. As we want to only intercept
            //either symbolic instruction after isFirstStepInsn has finished or a concrete instruction
            Instruction nextInst = currentThread.getNextPC();
            if (!currentThread.isFirstStepInsn() && (nextInst == executedInstruction)) { // the second condition indicates that the spf listener have not recognoized a single choice being possible, because if it does then the nextPc would have been either its target or its nextInst
                return;
            }

            ObligationSide oblgSide;
            if (((IfInstruction) executedInstruction).getTarget() == nextInst)
                oblgSide = ObligationSide.THEN;
            else {
                oblgSide = ObligationSide.ELSE;
                assert (executedInstruction).getNext() == nextInst;
            }

            Obligation oblg = CoverageUtil.createOblgFromIfInst((IfInstruction) executedInstruction, oblgSide);
            if (ObligationMgr.oblgExists(oblg)) {
                System.out.println("after execution of  instruction: " + executedInstruction);
                System.out.println("whose obligation is: " + oblg);

                if (ObligationMgr.isNewCoverage(oblg)) {
                    coverageStatistics.recordObligationCovered(oblg);
                    if (!newCoverageFound) {
                        newCoverageFound = true;
                    }
                }
//                System.out.println(ObligationMgr.printCoverage());
                printCoverage();
            }
            isSymBranchInst = false;
        }
    }

    public void threadTerminated(VM vm, ThreadInfo terminatedThread) {
        System.out.println("end of thread");
        newCoverageFound = false;
        allObligationsCovered = ObligationMgr.isAllObligationCovered();
    }


    // -------- the publisher interface
    @Override
    public void publishFinished(Publisher publisher) {
        PrintWriter pw = publisher.getOut();
        publisher.publishTopicStart("Branch Coverage report:");

        printCoverage();
    }
}
