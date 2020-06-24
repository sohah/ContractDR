package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.ast.def.*;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.AstMapVisitor;

import java.util.HashSet;

//tracks a single input ssa variable to find its last definition, which is then considered to be the output
public class LastSSAVisitor extends AstMapVisitor {

    //this is the var we are trying to find its last def, will be updated with later names if found
    private String interestingInVar;

    public String getLastInterestingVar() {
        return interestingInVar;
    }

    public LastSSAVisitor(LastSSAExpVisitor exprVisitor, String interestingInVar) {
        super(exprVisitor);
        this.interestingInVar = interestingInVar;
    }

    @Override
    public Stmt visit(AssignmentStmt a) {
        ((LastSSAExpVisitor) exprVisitor).setInterestingInVar(interestingInVar);
        eva.accept(a.rhs);
        if (((LastSSAExpVisitor) exprVisitor).isFound()){ //if the interestingInVar was found as a use, then the definition becomes the new interestingInVar we will be looking for.
            interestingInVar = a.lhs.toString();
            ((LastSSAExpVisitor) exprVisitor).resetFound();
        }
        return null;
    }

    @Override
    public Stmt visit(CompositionStmt a) {
        a.s2.accept(this);
        a.s1.accept(this);
        return null;
    }


    @Override
    public Stmt visit(SkipStmt a) {
        return null;
    }

    @Override
    public Stmt visit(SPFCaseStmt c) {
        return null;
    }

    @Override
    public Stmt visit(ArrayLoadInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(ArrayStoreInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(SwitchInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(ReturnInstruction c) {

        return null;
    }

    @Override
    public Stmt visit(GetInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(PutInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(NewInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(InvokeInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(ArrayLengthInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(ThrowInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(CheckCastInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(InstanceOfInstruction c) {
        return null;
    }

    @Override
    public Stmt visit(PhiInstruction c) {
        return null;
    }


    public static String execute(Stmt dynStmt, String interestingInVar) {
        LastSSAExpVisitor lastSSAExpVisitor = new LastSSAExpVisitor(interestingInVar);
        LastSSAVisitor lastSSAVisitor = new LastSSAVisitor(lastSSAExpVisitor, interestingInVar);
        dynStmt.accept(lastSSAVisitor);
        return lastSSAVisitor.getLastInterestingVar();
    }
}
