package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.ast.def.*;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.AstMapVisitor;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprVisitor;
import za.ac.sun.cs.green.expr.Expression;

import java.util.HashSet;

public class ContractInputSetDefValueVisitor extends AstMapVisitor {
    public HashSet<String> defVarsSet;

    public ContractInputSetDefValueVisitor(ExprVisitor<Expression> exprVisitor, HashSet<String> defVarsSet) {
        super(exprVisitor);
        this.defVarsSet = defVarsSet;
    }


    public static HashSet<String>  execute(Stmt dynStmt){
        HashSet<String> defSet = new HashSet<>();
        ContractInputSetDefValExpVisitor contractInputSetDefValExpVisitor = new ContractInputSetDefValExpVisitor(defSet);
        ContractInputSetDefValueVisitor contractInputSetDefValueVisitor = new ContractInputSetDefValueVisitor(contractInputSetDefValExpVisitor, defSet);
        dynStmt.accept(contractInputSetDefValueVisitor);
        return contractInputSetDefValueVisitor.defVarsSet;
    }
}
