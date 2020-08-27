package dk.aau.cs.learnRDF;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class contains all information required to annotate a BGP
 */
public class BGP {

    private final HashMap<TriplePath, Set<Var>> triplePaths;
    private final Set<Triple> body;
    private final PrefixMapping p;
    private final List<Var> projectedVariables;
    private final boolean distinct;
    private final boolean aggregate;
    private final Set<Expr> filters;

    public BGP(Set<Triple> body, Set<TriplePath> triplePaths, PrefixMapping p, List<Var> projectedVariables, boolean distinct, boolean aggregate, Set<Expr> filters) {
        this.triplePaths = new HashMap<>(triplePaths.size());
        this.filters = filters;
        for (TriplePath tp : triplePaths) {
            CoveredPathVisitor visitor =  new CoveredPathVisitor();
            tp.getPath().visit(visitor);
            this.triplePaths.put(tp, visitor.covered_vars);
        }

        this.body = body;
        this.p = p;
        this.projectedVariables = projectedVariables;
        this.distinct = distinct;
        this.aggregate = aggregate;
    }

    public PrefixMapping getPrefixMapping() {
        return p;
    }

    public int getNumberTriples() {
        return body.size();
    }

    public Set<Triple> getBody() {
        return this.body;
    }

    public List<Var> getProjectedVariables() {

        return projectedVariables;
    }

    public boolean isDistinct() {
        return distinct; 
    }

    public String toString() {

        StringBuilder s = new StringBuilder();
        for (Triple t : body) {
            s.append(t.toString()).append(". ");
        }
        if (body.size() > 0) {
            s = new StringBuilder(s.substring(0, s.length() - 2));
        }
        return s.toString();
    }

    public HashMap<TriplePath,Set<Var>> getTriplePaths() {
        return triplePaths;
    }

    public boolean isAggregate() {
        return aggregate;
    }

    public Set<Expr> getFilters() {
        return this.filters;
    }

    /**
     * Recursively Collects variables from a Path
     */
    private static class CoveredPathVisitor implements PathVisitor {
        private final Set<Var> covered_vars = new HashSet<>(3);

        public Set<Var> getCoveredVars() {return this.covered_vars;}

        @Override
        public void visit(P_Link pathNode) {
            Node node = pathNode.getNode();
            if (node.isVariable())
                covered_vars.add((Var)node);
        }

        @Override
        public void visit(P_ReverseLink pathNode) {
            Node node = pathNode.getNode();
            if (node.isVariable())
                covered_vars.add((Var)node);

        }

        @Override
        public void visit(P_NegPropSet pathNotOneOf) {
            //noop, not one of doesn't cover the variable since it is not retained
        }

        @Override
        public void visit(P_Inverse inversePath) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            inversePath.getSubPath().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_Mod pathMod) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            pathMod.getSubPath().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_FixedLength pFixedLength) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            pFixedLength.visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_Distinct pathDistinct) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            pathDistinct.visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_Multi pathMulti) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            pathMulti.visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_Shortest pathShortest) {
            //noop, no actual predicates or variables here
        }

        @Override
        public void visit(P_ZeroOrOne path) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            path.getSubPath().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_ZeroOrMore1 path) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            path.getSubPath().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_ZeroOrMoreN path) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            path.getSubPath().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_OneOrMore1 path) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            path.getSubPath().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_OneOrMoreN path) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            path.getSubPath().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_Alt pathAlt) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            pathAlt.getLeft().visit(visitor);
            pathAlt.getRight().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }

        @Override
        public void visit(P_Seq pathSeq) {
            CoveredPathVisitor visitor = new CoveredPathVisitor();
            pathSeq.getLeft().visit(visitor);
            pathSeq.getRight().visit(visitor);
            covered_vars.addAll(visitor.getCoveredVars());
        }
    }
}
