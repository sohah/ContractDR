package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.ast.def.*;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprMapVisitor;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprVisitor;
import za.ac.sun.cs.green.expr.*;

import java.util.HashSet;

public class LastSSAExpVisitor extends ExprMapVisitor {

    private String interestingInVar;
    private boolean found = false;

    public LastSSAExpVisitor(String interestingInVar) {
        this.interestingInVar = interestingInVar;
    }


    public boolean isFound() {
        return found;
    }

    public void resetFound() {
        found = false;
    }

    public void setInterestingInVar(String interestingInVar) {
        this.interestingInVar = interestingInVar;
    }

    @Override
    public Expression visit(IntConstant expr) {
        return null;
    }

    @Override
    public Expression visit(IntVariable expr) {
        if (expr.toString().equals(interestingInVar))
            found = true;
        return null;
    }

    @Override
    public Expression visit(Operation expr) {
        return null;
    }

    @Override
    public Expression visit(RealConstant expr) {
        return null;
    }

    @Override
    public Expression visit(RealVariable expr) {
        return null;
    }

    @Override
    public Expression visit(StringConstantGreen expr) {
        return null;
    }

    @Override
    public Expression visit(StringVariable expr) {
        return null;
    }

    @Override
    public Expression visit(IfThenElseExpr expr) {
        return null;
    }

    @Override
    public Expression visit(ArrayRefVarExpr expr) {
        if (expr.toString().equals(interestingInVar))
            found = true;
        return null;
    }

    @Override
    public Expression visit(WalaVarExpr expr) {
        if (expr.toString().equals(interestingInVar))
            found = true;
        return null;
    }

    @Override
    public Expression visit(FieldRefVarExpr expr) {
        if (expr.toString().equals(interestingInVar))
            found = true;
        return null;
    }


    @Override
    public Expression visit(AstVarExpr expr) {
        if (expr.toString().equals(interestingInVar))
            found = true;
        return null;
    }
}
