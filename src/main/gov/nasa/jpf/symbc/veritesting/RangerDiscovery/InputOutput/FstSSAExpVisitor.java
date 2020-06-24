package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.ast.def.*;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprMapVisitor;
import za.ac.sun.cs.green.expr.*;

import java.util.HashSet;

public class FstSSAExpVisitor extends ExprMapVisitor {

    private final HashSet<String> defVarsSet;

    public FstSSAExpVisitor(HashSet<String> defVarsSet) {
        this.defVarsSet = defVarsSet;
    }

    @Override
    public Expression visit(IntConstant expr) {
        return null;
    }

    @Override
    public Expression visit(IntVariable expr) {
        defVarsSet.add(expr.toString());
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
    public Expression visit(ArrayRefVarExpr expr) {
        return null;
    }

    @Override
    public Expression visit(WalaVarExpr expr) {
        defVarsSet.add(expr.toString());
        return null;
    }

    @Override
    public Expression visit(FieldRefVarExpr expr) {
        defVarsSet.add(expr.toString());
        return null;
    }


    @Override
    public Expression visit(AstVarExpr expr) {
        defVarsSet.add(expr.toString());
        return null;
    }
}
