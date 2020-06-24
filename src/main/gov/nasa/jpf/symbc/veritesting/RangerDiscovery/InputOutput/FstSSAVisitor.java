package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.ast.def.*;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.AstMapVisitor;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprVisitor;
import za.ac.sun.cs.green.expr.Expression;

import java.util.HashSet;

public class FstSSAVisitor extends AstMapVisitor {
    public HashSet<String> defVarsSet;

    public FstSSAVisitor(ExprVisitor<Expression> exprVisitor, HashSet<String> defVarsSet) {
        super(exprVisitor);
        this.defVarsSet = defVarsSet;
    }

    @Override
    public Stmt visit(AssignmentStmt a) {
        if(defVarsSet.contains(a.lhs.toString()))
            defVarsSet.remove(a.lhs.toString());
        eva.accept(a.rhs);
        return null;
    }

    @Override
    public Stmt visit(CompositionStmt a) {
        a.s2.accept(this);
        a.s1.accept(this);
        return null;
    }

    @Override
    public Stmt visit(IfThenElseStmt a) {
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


    public static HashSet<String>  execute(Stmt dynStmt){
        HashSet<String> defSet = new HashSet<>();
        FstSSAExpVisitor fstSSAExpVisitor = new FstSSAExpVisitor(defSet);
        FstSSAVisitor fstSSAVisitor = new FstSSAVisitor(fstSSAExpVisitor, defSet);
        dynStmt.accept(fstSSAVisitor);
        return fstSSAVisitor.defVarsSet;
    }
}
