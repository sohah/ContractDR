package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.VeritestingUtil.Pair;
import gov.nasa.jpf.symbc.veritesting.ast.def.ArrayRefVarExpr;
import gov.nasa.jpf.symbc.veritesting.ast.def.FieldRefVarExpr;
import gov.nasa.jpf.symbc.veritesting.ast.def.WalaVarExpr;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprMapVisitor;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprVisitor;
import jkind.lustre.BoolExpr;
import jkind.lustre.NamedType;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;

import java.util.ArrayList;
import java.util.List;

public class PutDefaultValuesExpr extends ExprMapVisitor {
    public List<String> varList;

    @Override
    public Expression visit(IntVariable expr) {
        return returnDefaultOrExpr(expr);
    }

    @Override
    public Expression visit(WalaVarExpr expr) {
        return returnDefaultOrExpr(expr);
    }


    @Override
    public Expression visit(FieldRefVarExpr expr) {
        return returnDefaultOrExpr(expr);
    }


    @Override
    public Expression visit(ArrayRefVarExpr expr) {
        return returnDefaultOrExpr(expr);
    }


    public Expression returnDefaultOrExpr(Expression exp){
        if(varList.contains(exp.toString())) //assuming that all expressions in dynRegions are of type int.
            return new IntConstant(0);
        else return exp;
    }



}
