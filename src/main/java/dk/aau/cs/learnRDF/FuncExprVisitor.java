package dk.aau.cs.learnRDF;

import org.apache.jena.sparql.expr.*;

import java.util.HashSet;
import java.util.Set;

public class FuncExprVisitor extends ExprVisitorBase {
    private boolean isSpecial = false;
    private final Set<ExprFunction2> rangeFunctions = new HashSet<>();
    private final Set<String> specialFunction1s = new HashSet<>();
    private final Set<String> rangeFunction2s = new HashSet<>();
    public FuncExprVisitor() {
        super();
        specialFunction1s.add(E_IsBlank.class.getName());
        specialFunction1s.add(E_IsIRI.class.getName());
        specialFunction1s.add(E_IsLiteral.class.getName());
        specialFunction1s.add(E_IsNumeric.class.getName());
        specialFunction1s.add(E_Datatype.class.getName());
        specialFunction1s.add(E_Lang.class.getName());
        rangeFunction2s.add(E_GreaterThan.class.getName());
        rangeFunction2s.add(E_GreaterThanOrEqual.class.getName());
        rangeFunction2s.add(E_LessThan.class.getName());
        rangeFunction2s.add(E_LessThanOrEqual.class.getName());
    }



    @Override
    public void visit(ExprFunction0 func) {
        super.visit(func);
    }

    @Override
    public void visit(ExprFunction1 func) {
        super.visit(func);
        if (specialFunction1s.contains(func.getClass().getName()))
            this.isSpecial=true;
        if (func.getArg().isFunction()) {
            FuncExprVisitor subVisitor = new FuncExprVisitor();
            func.getArg().visit(subVisitor);
            if (subVisitor.isSpecial)
                this.isSpecial=true;
            rangeFunctions.addAll(subVisitor.getRangeFunctions());
        }
    }

    @Override
    public void visit(ExprFunction2 func) {
        super.visit(func);
        if (func instanceof E_LangMatches)
            this.isSpecial=true;
        else if (rangeFunction2s.contains(func.getClass().getName()))
            rangeFunctions.add(func);
        if (func.getArg1().isFunction()) {
            FuncExprVisitor subVisitor = new FuncExprVisitor();
            func.getArg1().visit(subVisitor);
            if (subVisitor.isSpecial)
                this.isSpecial=true;
            rangeFunctions.addAll(subVisitor.getRangeFunctions());
        }
        if (func.getArg2().isFunction()) {
            FuncExprVisitor subVisitor = new FuncExprVisitor();
            func.getArg2().visit(subVisitor);
            if (subVisitor.isSpecial)
                this.isSpecial=true;
            rangeFunctions.addAll(subVisitor.getRangeFunctions());
        }
    }

    @Override
    public void visit(ExprFunction3 func) {
        super.visit(func);
        if (func.getArg1().isFunction()) {
            FuncExprVisitor subVisitor = new FuncExprVisitor();
            func.getArg1().visit(subVisitor);
            if (subVisitor.isSpecial)
                this.isSpecial=true;
            rangeFunctions.addAll(subVisitor.getRangeFunctions());
        }
        if (func.getArg2().isFunction()) {
            FuncExprVisitor subVisitor = new FuncExprVisitor();
            func.getArg2().visit(subVisitor);
            if (subVisitor.isSpecial)
                this.isSpecial=true;
            rangeFunctions.addAll(subVisitor.getRangeFunctions());
        }
        if (func.getArg3().isFunction()) {
            FuncExprVisitor subVisitor = new FuncExprVisitor();
            func.getArg3().visit(subVisitor);
            if (subVisitor.isSpecial)
                this.isSpecial=true;
            rangeFunctions.addAll(subVisitor.getRangeFunctions());
        }
    }

    @Override
    public void visit(ExprFunctionN func) {
        super.visit(func);
        for (Expr arg :func.getArgs()) {
            if (arg.isFunction()) {
                FuncExprVisitor subVisitor = new FuncExprVisitor();
                arg.visit(subVisitor);
                if (subVisitor.isSpecial)
                    this.isSpecial=true;
                rangeFunctions.addAll(subVisitor.getRangeFunctions());
            }
        }
    }

    @Override
    public void visit(ExprFunctionOp op) {
        super.visit(op);
        // System.out.println(op);
    }

    @Override
    public void visit(NodeValue nv) {
        super.visit(nv);
        // System.out.println(nv);
    }

    @Override
    public void visit(ExprVar nv) {
        super.visit(nv);
    }

    @Override
    public void visit(ExprAggregator eAgg) {
        super.visit(eAgg); //Probably not relevant in filters.
        // System.out.println("Unexpected visit in " + eAgg);
    }


    public boolean isSpecial() {
        return isSpecial;
    }

    public Set<ExprFunction2> getRangeFunctions() {
        return rangeFunctions;
    }
}
