package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.ast.def.*;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprMapVisitor;
import za.ac.sun.cs.green.expr.*;

import java.util.HashSet;

public class ContractInputSetDefValExpVisitor extends ExprMapVisitor {

    private final HashSet<String> defVarsSet;

    public ContractInputSetDefValExpVisitor(HashSet<String> defVarsSet) {
        this.defVarsSet = defVarsSet;
    }

    @Override
    public Expression visit(IntVariable expr) {
        defVarsSet.add(expr.toString());
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



}
