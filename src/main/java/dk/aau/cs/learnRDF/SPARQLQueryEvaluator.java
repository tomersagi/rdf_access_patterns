package dk.aau.cs.learnRDF;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction2;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.syntax.*;

import java.util.*;


public class SPARQLQueryEvaluator {

    private final String posedQuery;
    private final boolean verbose;

    SPARQLQueryEvaluator(String query) {
        this(query, true);
    }
    SPARQLQueryEvaluator(String query, boolean verbose) {
        this.posedQuery = query;
        this.verbose = verbose;
    }


    /**
     * Main entry point for class.
     * Extract BGPs from the query, count the number and type of access patterns in the query
     *
     * @return list of access pattern, count strings
     */
    public boolean[] evaluateQuery() throws QueryParseException {
        boolean[] res = new boolean[AccessPattern.values().length];
        Arrays.fill(res, false);
        try {
            Query query = QueryFactory.create(posedQuery);
            if(query.isDescribeType()){
                if(this.verbose) {
                    System.err.println("Found Describe query");
                }
                return res;
            }
            if (query.hasOrderBy() && !query.hasGroupBy())
                res[AccessPattern.RETURN_SORTED.ordinal()]=true;
            ArrayList<BGP> bgps = getBGPs(query);
            for (BGP bgp : bgps) {
                apCheckReturn(bgp, res);
                apCheckConstants(bgp, res);
                apCheckRange(bgp, res);
                apCheckTraversals(bgp, res);
                apCheckPivot(bgp, res);
                //System.out.println(t.toString());
            }
            return res;
        } catch(Exception e) {
            if(verbose){
                System.err.println(String.format("Could not parse query\n%s\n", this.posedQuery ));
            }
            throw(e);
        }
    }

    /**
     * Checks for the existence of range filters over the bgp and subtypes them
     * @param bgp to check
     * @param res to flag in
     */
    private void apCheckRange(BGP bgp, boolean[] res) {
        Set<Expr> filters = bgp.getFilters();


        if (!filters.isEmpty()){
            Map<String, List<Expr>> byVar = new HashMap<>(bgp.getProjectedVariables().size());
            for (Expr expr : filters) {
                FuncExprVisitor visitor = new FuncExprVisitor();
                expr.visit(visitor);
                if (visitor.isSpecial())
                    res[AccessPattern.RANGE_S.ordinal()]=true;

                for (ExprFunction2 x : visitor.getRangeFunctions()) {
                    if (x.getArg1().isVariable()) {
                        String v = x.getArg1().asVar().getVarName();
                        List<Expr> expList = byVar.containsKey(v) ? byVar.get(v) : new ArrayList<>();
                        expList.add(x);
                        byVar.put(v, expList);
                    }
                    if (x.getArg2().isVariable()){
                        String v = x.getArg2().asVar().getVarName();
                        List<Expr> expList = byVar.containsKey(v) ? byVar.get(v) : new ArrayList<>();
                        expList.add(x);
                        byVar.put(v, expList);
                    }
                }

            }
            for (List<Expr> clusteredExpressions : byVar.values()) {
                if(clusteredExpressions.size()==1)
                    res[AccessPattern.RANGE_O.ordinal()]=true;
                if(clusteredExpressions.size()>1)
                    res[AccessPattern.RANGE_C.ordinal()]=true;
            }
        }
    }

    private ArrayList<BGP> getBGPs(Query query) {
        ArrayList<BGP> res = new ArrayList<>();
        List<Query> subQueries = getSubQueries(query);
        if (!subQueries.isEmpty()) {
            for (Query q : subQueries) {
                res.addAll(getBGPs(q));
            }
        }
        Op op = (new AlgebraGenerator()).compile(query);
        BGPVisitor bgpv = new BGPVisitor();
        OpWalker.walk(op, bgpv);

        //parse EXIST/NOTEXIST
        ArrayList<BGP> existBGPs = new ArrayList<>();
        for (BGPVisitor b : bgpv.getBgps()) {
            existBGPs.add(new BGP(b.getTriples(), b.getTriplePaths(), query.getPrefixMapping(), new ArrayList<>(), false, false, new HashSet<>()));
        }

        Set<Triple> triples = bgpv.getTriples();
        Set<TriplePath> triplePaths =  bgpv.getTriplePaths();

        //bind all subject and object variables that have a simple bind statement (BIND ?v const)
        Map<Triple,Triple> to_replace = new HashMap<>();
        for (Map.Entry<Var, NodeValue> entry : bgpv.getToBind().entrySet()) {
            Var v = entry.getKey();
            for (Triple t : triples){
                if (t.objectMatches(v))
                    to_replace.put(t,new Triple(t.getSubject(),t.getPredicate(), entry.getValue().asNode()));
                if (t.subjectMatches(v))
                    to_replace.put(t,new Triple(entry.getValue().asNode(),t.getPredicate(), t.getObject()));
            }
        }
        for (Map.Entry<Triple, Triple> entry : to_replace.entrySet()) {
            triples.remove(entry.getKey());
            triples.add(entry.getValue());
        }
        Map<TriplePath,TriplePath> to_replace2 = new HashMap<>();
        for (Map.Entry<Var, NodeValue> entry : bgpv.getToBind().entrySet()) {
            Var v = entry.getKey();
            for (TriplePath t : triplePaths){
                if (t.getObject().equals(v))
                    to_replace2.put(t,new TriplePath(t.getSubject(),t.getPath(), entry.getValue().asNode()));
                if (t.getSubject().equals(v))
                    to_replace2.put(t,new TriplePath(entry.getValue().asNode(),t.getPath(), t.getObject()));
            }
        }
        for (Map.Entry<TriplePath, TriplePath> entry : to_replace2.entrySet()) {
            triplePaths.remove(entry.getKey());
            triplePaths.add(entry.getValue());
        }

        //Remove triplepaths and triples already present in subqueries
        for (BGP subQueryBGP : res)
            triplePaths.removeAll(subQueryBGP.getTriplePaths().keySet());
        for (BGP subQueryBGP : res)
            triples.removeAll(subQueryBGP.getBody());


        BGP bgp = new BGP(triples, triplePaths, query.getPrefixMapping(), query.getProjectVars(), query.isDistinct(), query.hasAggregators(), bgpv.getFilters());
        res.add(bgp);
        res.addAll(existBGPs);
        return res;
    }

    private List<Query> getSubQueries(Query query) {
        List<Query> subQueries = new ArrayList<>();
        if (!(query.getQueryPattern() instanceof ElementGroup)){
            if (query.getQueryPattern() instanceof ElementSubQuery) {
                subQueries.add(((ElementSubQuery)query.getQueryPattern()).getQuery());
                return subQueries;
            }
        }

        for (Element e : ((ElementGroup)query.getQueryPattern()).getElements()) {
            if (e instanceof ElementSubQuery){
                subQueries.add(((ElementSubQuery) e).getQuery());
            }
        }
        return subQueries;
    }

    /**
     * Checks for pivot access patterns by comparing each triple to all others
     * @param bgp basic graph pattern to examine
     * @param res boolean array to set the results in
     */
    private void apCheckPivot(BGP bgp, boolean[] res) {
        Set<Triple> allTriplesOrPaths = new HashSet<>(bgp.getBody());
        for (TriplePath tp : bgp.getTriplePaths().keySet()) {
            Node p = tp.getPredicate()!=null ? tp.getPredicate() : new NullNode();
            allTriplesOrPaths.add(new Triple(tp.getSubject(), p, tp.getObject()));
        }

        for (Triple t : allTriplesOrPaths) {
            for (Triple t2 : allTriplesOrPaths) {
                if (t.equals(t2))
                    continue;
                try {
                    if (!t.getSubject().isConcrete() && t.subjectMatches(t2.getSubject())) {
                        res[AccessPattern.PIVOT_S.ordinal()] = true;
                        res[test_n_way(t, t2, allTriplesOrPaths, AccessPattern.PIVOT_S).ordinal()] = true;
                    }
                    if (!t.getObject().isConcrete() && t.objectMatches(t2.getSubject())) {
                        res[AccessPattern.PIVOT_OS.ordinal()] = true;
                        res[test_n_way(t, t2, allTriplesOrPaths, AccessPattern.PIVOT_OS).ordinal()] = true;
                    }
                    if (!t.getObject().isConcrete() && t.objectMatches(t2.getObject())) {
                        res[AccessPattern.PIVOT_O.ordinal()] = true;
                        res[test_n_way(t, t2, allTriplesOrPaths, AccessPattern.PIVOT_O).ordinal()] = true;
                    }
                    if (!t.getSubject().isConcrete() && t.subjectMatches(t2.getPredicate())) {
                        res[AccessPattern.PIVOT_SP.ordinal()] = true;
                        res[test_n_way(t, t2, allTriplesOrPaths, AccessPattern.PIVOT_SP).ordinal()] = true;
                    }
                    if (!t.getObject().isConcrete() && t.objectMatches(t2.getPredicate())) {
                        res[AccessPattern.PIVOT_OP.ordinal()] = true;
                        res[test_n_way(t, t2, allTriplesOrPaths, AccessPattern.PIVOT_OP).ordinal()] = true;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Given two tuples, a bgp and an access pattern from the pivot dimension, returns if the BGP contains
     * an n-way access pattern, either star or arbitrary.
     * @param t first tuple
     * @param t2 second tuple
     * @param allTriples all triples and triples paths from the bgp
     * @param pivot 2-way access pattern that got us here. If it is a single position pattern, it may
     *              lead to an n-way star or an n-way arbitrary. If it is a 2-position pivot
     * @return n-way pivot if such exists or the incoming pvot pattern otherwise
     */
    private AccessPattern test_n_way(Triple t, Triple t2, Set<Triple> allTriples, AccessPattern pivot) {
        boolean star = false;
        boolean arbitrary = false;
        for (Triple t3 : allTriples) {
            if (!t.equals(t3) && !t2.equals(t3)) {
                //other triple
                switch (pivot){
                    case PIVOT_O:
                    {
                        if (t3.objectMatches(t2.getObject())) {
                            star = true;
                        }
                        if (t3.subjectMatches(t2.getObject()) || t3.predicateMatches(t2.getObject())) {
                            arbitrary = true;
                        }
                        break;
                    }
                    case PIVOT_S:
                    {
                        if (t3.subjectMatches(t2.getSubject())) {
                            star = true;
                        }
                        if (t3.objectMatches(t2.getSubject()) || t3.predicateMatches(t2.getSubject())) {
                            arbitrary = true;
                        }
                        break;
                    }
                    case PIVOT_OS: //Already two-position pivot
                    {
                        if (t.subjectMatches(t2.getObject()) && (t3.objectMatches(t2.getObject()) ||
                                t3.subjectMatches(t2.getObject()) || t3.predicateMatches(t2.getObject()))) //t.S->t2.O + t2.O->t3.Any
                            arbitrary = true;
                        if (t.objectMatches(t2.getSubject()) && (t3.objectMatches(t2.getSubject()) ||
                                t3.subjectMatches(t2.getSubject()) || t3.predicateMatches(t2.getSubject()))) //t.O->t2.S + t2.S->t3.Any
                            arbitrary = true;
                    }
                    break;
                    case PIVOT_OP: //Already two-position pivot
                    {
                        if (t.predicateMatches(t2.getObject()) && (t3.objectMatches(t2.getObject()) ||
                                t3.subjectMatches(t2.getObject()) || t3.predicateMatches(t2.getObject()))) //t.P->t2.O + t2.O->t3.Any
                            arbitrary = true;
                        if (t.objectMatches(t2.getPredicate()) && (t3.objectMatches(t2.getPredicate()) ||
                                t3.subjectMatches(t2.getPredicate()) || t3.predicateMatches(t2.getPredicate()))) //t.O->t2.P + t2.P->t3.Any
                            arbitrary = true;
                    }
                    break;
                    case PIVOT_SP: //Already two-position pivot
                    {
                        if (t.predicateMatches(t2.getSubject()) && (t3.objectMatches(t2.getSubject()) ||
                                t3.subjectMatches(t2.getSubject()) || t3.predicateMatches(t2.getSubject()))) //t.P->t2.S + t2.S->t3.Any
                            arbitrary = true;
                        if (t.subjectMatches(t2.getPredicate()) && (t3.objectMatches(t2.getPredicate()) ||
                                t3.subjectMatches(t2.getPredicate()) || t3.predicateMatches(t2.getPredicate()))) //t.S->t2.P + t2.P->t3.Any
                            arbitrary = true;
                    }
                    break;
                    //TBD PO and PS pivots in the github repo
                }

            }
        }
        if (arbitrary)
            return AccessPattern.PIVOT_NA;
        else if (star)
            return AccessPattern.PIVOT_NS;
        else
            return pivot;
    }

    /**
     * Checks which return access patterns exist in the bgp
     * @param bgp to check
     * @param res boolean array to set the results in
     */
    private void apCheckReturn(BGP bgp, boolean[] res) {
        if (bgp.isDistinct())
            res[AccessPattern.RETURN_DISTINCT.ordinal()]=true;
        else
            if(bgp.isAggregate())
                res[AccessPattern.RETURN_AGG.ordinal()]=true;
            else {
                if (!bgp.getProjectedVariables().isEmpty()) //test for ask
                    res[AccessPattern.RETURN_VAL.ordinal()]=true;
            }

        //check exist pattern for triples
        for (Triple t : bgp.getBody()) {
            Set<Var> to_check = new HashSet<>(3);
            extractVariablesT(to_check, new HashSet<>(Collections.singleton(t))); //generate to check

            //check if fully instantiated, if yes, shortcut finish
            if (to_check.isEmpty()) {//all constants / literals
                res[AccessPattern.RETURN_EXISTS.ordinal()] = true;
                return;
            }

            //Collect all other covered variables
            Set<Triple> others =  new HashSet<>(bgp.getBody());
            others.remove(t);
            Map<TriplePath,Set<Var>> triplePaths = new HashMap<>(bgp.getTriplePaths());
            Set<Var> covered_vars = new HashSet<>(others.size());
            extractVariablesT(covered_vars, others); //add variables from other triples
            extractVariablesTP(covered_vars, triplePaths); //add variables from triplePaths

            //Check complete containment
            if (covered_vars.containsAll(to_check))
                res[AccessPattern.RETURN_EXISTS.ordinal()] = true;
        }

        //check existence pattern for triplePaths
        for (Map.Entry<TriplePath,Set<Var>> entry : bgp.getTriplePaths().entrySet()) {
            Set<Var> to_check =  new HashSet<>(3); //generate to check
            extractVariablesTP(to_check, new HashMap<>(Collections.singletonMap(entry.getKey(),entry.getValue())));

            //check if fully instantiated, if yes, shortcut finish
            if (to_check.isEmpty()) {//all constants / literals
                res[AccessPattern.RETURN_EXISTS.ordinal()] = true;
                return;
            }

            //Collect all other covered variables
            Map<TriplePath,Set<Var>> others =  new HashMap<>(bgp.getTriplePaths());
            others.remove(entry.getKey());
            Set<Var> covered_vars = new HashSet<>(others.size());
            extractVariablesT(covered_vars,new HashSet<>(bgp.getBody())); //add variables from triples
            extractVariablesTP(covered_vars,others); //add variables from other triplePaths

            //Check complete containment
            if (covered_vars.containsAll(to_check))
                res[AccessPattern.RETURN_EXISTS.ordinal()] = true;

        }

    }


    /**
     * Extracts variables from a set of triples into the given set of variables
     * @param return_set into which to extract
     * @param triples form which to extract
     */
    private void extractVariablesT(Set<Var> return_set, Set<Triple> triples) {
        for (Triple other_t : triples) {
            if (other_t.getSubject().isVariable()) return_set.add((Var)other_t.getSubject());
            if (other_t.getPredicate().isVariable()) return_set.add((Var)other_t.getPredicate());
            if (other_t.getObject().isVariable()) return_set.add((Var)other_t.getObject());
        }
    }

    /**
     * Extract variables from a set of TriplePaths into the given set of variables
     * @param return_set into which to extract
     * @param triplePaths from which to extract
     */
    private void extractVariablesTP(Set<Var> return_set, Map<TriplePath,Set<Var>> triplePaths) {
        for (Map.Entry<TriplePath,Set<Var>> e : triplePaths.entrySet()) {
            if (e.getKey().getSubject().isVariable()) return_set.add((Var)e.getKey().getSubject());
            if (e.getKey().getObject().isVariable()) return_set.add((Var)e.getKey().getObject());
            return_set.addAll(e.getValue());
        }
    }

    /**
     * Checks for the existence of traversal access patterns in the given BGP by creating chains starting
     * from concrete subject or object and checking their lengths.
     * @param bgp Basic graph pattern in which to check
     * @param res Boolean result array to be used to update the result of the check
     */
    private void apCheckTraversals(BGP bgp, boolean[] res) {
        /*
         TRAVERSAL_out1("Traversal", "1-hop s->o"),
         TRAVERSAL_in1("Traversal", "1-hop o->s"),
         TRAVERSAL_outK("Traversal", "K-hop s->o"), // K>1
         TRAVERSAL_inK("Traversal", "K-hop o->s"), // K>1
         TRAVERSAL_outP_STAR("Traversal", "P* s->o"),
         TRAVERSAL_inP_STAR("Traversal", "P* o->s"),
         TRAVERSAL_outSTAR("Traversal", "* s->o"),
         TRAVERSAL_inSTAR("Traversal", "* o->s"),
         */
        //test for Triples Paths and collect 1-hop triple paths as pseudo-triples
        for (TriplePath tp : bgp.getTriplePaths().keySet()) {
            CountingPathVisitor pVisitor = new CountingPathVisitor();
            tp.getPath().visit(pVisitor);
            //Collect all other covered variables
            Map<TriplePath,Set<Var>> others =  new HashMap<>(bgp.getTriplePaths());
            others.remove(tp);
            Set<Var> covered_vars = new HashSet<>(others.size());
            extractVariablesT(covered_vars,new HashSet<>(bgp.getBody())); //add variables from triples
            extractVariablesTP(covered_vars,others); //add variables from other triplePaths

            //check the traversal lengths
            Node s = tp.getSubject();
            Node o = tp.getObject();
            if ((s.isConcrete() || (s.isVariable() && covered_vars.contains(s))) && !o.isConcrete()) {
                if (pVisitor.getLength()>1)
                    res[AccessPattern.TRAVERSAL_outK.ordinal()]=true;
                if (pVisitor.isHasStar())
                    res[AccessPattern.TRAVERSAL_outP_STAR.ordinal()]=true;
                if (pVisitor.getLength()==1) {
                    res[AccessPattern.TRAVERSAL_out1.ordinal()] = true;
                }
            }
            if (!s.isConcrete() && (o.isConcrete() || (o.isVariable() && covered_vars.contains(o)))) {
                if (pVisitor.getLength()>1)
                    res[AccessPattern.TRAVERSAL_inK.ordinal()]=true;
                if (pVisitor.isHasStar())
                    res[AccessPattern.TRAVERSAL_inP_STAR.ordinal()]=true;
                if (pVisitor.getLength()==1) {
                    res[AccessPattern.TRAVERSAL_in1.ordinal()]=true;
                }


            }
        }

        //test regular triple chains + TODO pseudo triplePaths
        for (Triple t : bgp.getBody()) {
            Set<Triple> outer_others =  new HashSet<>(bgp.getBody());
            outer_others.remove(t);
            Map<TriplePath,Set<Var>> triplePaths = new HashMap<>(bgp.getTriplePaths());
            Set<Var> covered_vars = new HashSet<>(outer_others.size());
            extractVariablesT(covered_vars, outer_others); //add variables from other triples
            extractVariablesTP(covered_vars, triplePaths); //add variables from triplePaths

            if ((t.getSubject().isConcrete() || covered_vars.contains(t.getSubject())) && !t.getObject().isConcrete()) {
                res[AccessPattern.TRAVERSAL_out1.ordinal()] = true;
                Set<Triple> others = new HashSet<>(bgp.getBody());
                others.remove(t);
                Triple[] longestChain = makeLongestChain(new Triple[] {t}, others, true, bgp.getProjectedVariables());
                if (longestChain.length > 1)
                    res[AccessPattern.TRAVERSAL_outK.ordinal()] = true;
            } else { //check incoming direction
                Node subj = t.getSubject();
                Node obj = t.getObject();
                if (!subj.isConcrete() && (obj.isConcrete() || (obj.isVariable() && covered_vars.contains(obj)))) {
                    res[AccessPattern.TRAVERSAL_in1.ordinal()] = true;
                    Set<Triple> others = new HashSet<>(bgp.getBody());
                    others.remove(t);
                    Triple[] longestChain = makeLongestChain(new Triple[] {t}, others, false, bgp.getProjectedVariables());
                    if (longestChain.length > 1){
                        res[AccessPattern.TRAVERSAL_inK.ordinal()] = true;
                    }
                }
            }
        }
    }

    /**
     * Recursive function to count length of chains starting from the given triple traversing intermediate
     * variables that are not returned.
     * @param current longest chain found
     * @param others remaining unchained triples
     * @param outgoing if true, will try to construct s-->o chains otherwise will try to construct o-->s chains
     * @param projectedVariables needed to see if the chain can continue through a variable or if it is needed
     *
     */
    private Triple[] makeLongestChain(Triple[] current, Set<Triple> others, boolean outgoing, List<Var> projectedVariables) {
        Triple[] newLongest = current;
        if (outgoing) {
            Triple link = current[current.length-1];
            Var linking_var =  (Var)link.getObject();
            if (projectedVariables.contains(linking_var)){
                return current;
            }

            for (Triple candidate : others) {
                if (link.objectMatches(candidate.getSubject()) && candidate.getObject().isVariable()) {
                    HashSet<Triple> newOthers = new HashSet<>(others);
                    newOthers.remove(candidate);
                    Triple[] candidateChain = makeLongestChain(ArrayUtils.add(current,candidate),newOthers, true, projectedVariables);
                    if (candidateChain.length>newLongest.length){
                        newLongest = candidateChain;
                    }

                }
            }
        } else { //incoming
            Triple link = current[0];
            Var linking_var = (Var)link.getSubject();
            if (projectedVariables.contains(linking_var))
                return current;
            for (Triple candidate : others) {
                if (link.subjectMatches(candidate.getObject()) && candidate.getSubject().isVariable()) {
                    HashSet<Triple> newOthers = new HashSet<>(others);
                    newOthers.remove(candidate);
                    Triple[] candidateChain = ArrayUtils.addAll(makeLongestChain(ArrayUtils.insert(0,
                            current,candidate),newOthers, false, projectedVariables),current);
                    if (candidateChain.length>newLongest.length){
                        newLongest = candidateChain;
                    }
                }
            }

        }
        return newLongest;
    }

    /**
     * Checks for the existence of Constant access patterns (e.g, AccessPattern.CONSTANTS_SPO)
     * in the given BGP by reviewing all tuples.
     * @param bgp Basic graph pattern in which to check
     * @param res Boolean result array to be used to update the result of the check
     */
    private void apCheckConstants(BGP bgp, boolean[] res) {
        for (Triple t : bgp.getBody()) {
            setConstantPatternByTripleOrTriplePath(res, t.getSubject().isConcrete(), t.getPredicate().isConcrete(), t.getObject().isConcrete());
        }

        for (Map.Entry<TriplePath,Set<Var>> entry : bgp.getTriplePaths().entrySet()) {
            boolean c_sub = entry.getKey().getSubject().isConcrete();
            boolean c_pre = entry.getValue().isEmpty();
            boolean c_obj = entry.getKey().getObject().isConcrete();
            setConstantPatternByTripleOrTriplePath(res, c_sub, c_pre, c_obj);
        }
    }

    /**
     * Encapsulates the logic for flagging the constant acess patterns for a given triple or triple path state
     * @param res result array to flag the appropriate constant ordinal positions
     * @param c_sub is there a constant in the subject
     * @param c_pre is there a constant in the predicate (triple)/ are there no variables in the path (triple path)
     * @param c_obj is there a constant in the object
     */
    private void setConstantPatternByTripleOrTriplePath(boolean[] res, boolean c_sub, boolean c_pre, boolean c_obj) {
        if (c_sub) {
            if (c_pre) {
                if (c_obj){
                    res[AccessPattern.CONSTANTS_SPO.ordinal()] = true;
                } else {
                    res[AccessPattern.CONSTANTS_SP.ordinal()] = true;
                }
            } else { //Subject is concrete but predicate is not
                if (c_obj){
                    res[AccessPattern.CONSTANTS_SO.ordinal()] = true;
                } else {
                    res[AccessPattern.CONSTANTS_S.ordinal()] = true;
                }
            }
        } else { //Subject is not concrete
            if (c_pre) {
                if (c_obj) {
                    res[AccessPattern.CONSTANTS_PO.ordinal()] = true;
                }  else {
                    res[AccessPattern.CONSTANTS_P.ordinal()] = true;
                }

            } else { //Subject and predicate are not concrete
                if (c_obj){
                    res[AccessPattern.CONSTANTS_O.ordinal()] = true;
                }
            }
        }
    }
}
