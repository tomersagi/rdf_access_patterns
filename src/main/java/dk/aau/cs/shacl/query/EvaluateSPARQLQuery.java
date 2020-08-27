package dk.aau.cs.shacl.query;

import dk.aau.cs.shacl.Main;
import dk.aau.cs.shacl.shapes.ShapesDetector;
import dk.aau.cs.shacl.shapes.Statistics;
import dk.aau.cs.shacl.utils.RdfUtils;
import dk.aau.cs.shacl.utils.Tuple3;
import org.apache.jena.ext.com.google.common.base.Functions;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.join.Join;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.vocabulary.RDF;
import org.decimal4j.util.DoubleRounder;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import java.util.*;


public class EvaluateSPARQLQuery {
    private final RanksCalculator ranksCalculator = new RanksCalculator(this);
    private String posedQuery = "";
    private List<Var> projectedVariables = null;
    private static Expr filterExpression;

    public EvaluateSPARQLQuery(String query) {
        this.posedQuery = query;
    }

    Op useShapesToGenerateOptimizedAlgebra() {
        Op algebraOp = null;
        Query query = QueryFactory.create(posedQuery);
        projectedVariables = query.getProjectVars();
        ArrayList<HashSet<Triple>> bgps = getBGPs(query);

        //for every Basic Graph Pattern
        for (HashSet<Triple> triples : bgps) {

            HashMap<Triple, List<Tuple3<String, String, String>>> triplesToShapesMapping = new ShapesDetector(triples).getBgpShapes();

            HashMap<Triple, List<Statistics>> triplesWithStats = new HashMap<>();

            // Get statistics
            getStatisticsFromShapes(triplesToShapesMapping, triplesWithStats);

            // Get Stars
            Vector<SubQuery> stars = getStars(triples, triplesWithStats);

            //Iterate over stars to estimate the cardinality
            BasicPattern bp = subQueryIterator(stars, query);

            algebraOp = makeQueryAlgebra(query, bp);

            System.out.println(algebraOp);

        }
        return algebraOp;
    }


    private static ArrayList<HashSet<Triple>> getBGPs(Query query) {
        ArrayList<HashSet<Triple>> bgps = null;
        try {
            ElemVisitor elemVisitor = new ElemVisitor();
            ElementWalker.walk(query.getQueryPattern(), elemVisitor);
            filterExpression = elemVisitor.getFilterExpression();

            Op op = (new AlgebraGenerator()).compile(query);
            BGPVisitor bgpv = new BGPVisitor();
            OpWalker.walk(op, bgpv);
            bgps = bgpv.getBGPs();
        } catch (Exception e) {
            e.printStackTrace();
            //System.exit(1);
        }
        return bgps;
    }

    private void getStatisticsFromShapes(HashMap<Triple, List<Tuple3<String, String, String>>> triplesToShapesMapping, HashMap<Triple, List<Statistics>> triplesWithStats) {
        triplesToShapesMapping.forEach((triple, list) -> {
            //List<Tuple3<String, Integer, Integer>> temp = new ArrayList<>();
            List<Statistics> tempStatList = new ArrayList<>();
            list.forEach(tuple -> {

                if (triple.getPredicate().toString().equals(RDF.type.toString())) {

                    //In this case we treat <?x a :Composition> where :Composition is a class and there is no min & max count associated with the Node Shape.
                    // There is no shape property for RDF.type predicate

                    int subjectCount = (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "count", Main.getShapesModelIRI())).getInt();
                    Statistics statistics = new Statistics(triple, tuple._3,
                            1, 1, // default values for min and max count is 1 (Assumption)
                            subjectCount,
                            (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "distinctCount", Main.getShapesModelIRI())).getInt(), tuple._2);
                    statistics.setSubject(tuple._1);
                    statistics.setSubjectCount(subjectCount);
                    tempStatList.add(statistics);
                } else {
                    Statistics statistics = new Statistics(
                            triple,
                            tuple._3,
                            (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "minCount", Main.getShapesModelIRI())).getInt(),
                            (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "maxCount", Main.getShapesModelIRI())).getInt(), // need a check on maxCount
                            (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "count", Main.getShapesModelIRI())).getInt(),
                            (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "distinctCount", Main.getShapesModelIRI())).getInt(), tuple._2
                    );
                    statistics.setSubject(tuple._1);
                    statistics.setSubjectCount((RdfUtils.getFromModel(Main.getShapesPrefixURL() + tuple._2 + "Shape", Main.getShapesStatsPrefixURL() + "count", Main.getShapesModelIRI())).getInt());
                    if (tuple._2.equals("RDFGraph")) {
                        statistics.setDistinctSubjectCount((RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "distinctSubjectCount", Main.getShapesModelIRI())).getInt());
                        statistics.setDistinctObjectCount((RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "distinctObjectCount", Main.getShapesModelIRI())).getInt());
                    }

                    tempStatList.add(statistics);
                }
            });
            triplesWithStats.put(triple, tempStatList);
            //triplesToShapesMappingWithStats.put(triple, temp);
        });
    }

    private static Vector<SubQuery> getStars(HashSet<Triple> triples, HashMap<Triple, List<Statistics>> relevantShapes) {

        Vector<SubQuery> stars = new Vector<>();
        List<Triple> ts = new LinkedList<Triple>(triples);
        HashMap<Node, HashSet<Triple>> bySubject = new HashMap<Node, HashSet<Triple>>();

        //HashMap<Node, HashSet<Triple>> byObject = new HashMap<Node, HashSet<Triple>>();

        /*for (Triple t : ts) {
            Node o = t.getObject();
            HashSet<Triple> ots = byObject.get(o);
            if (ots == null) {
                ots = new HashSet<Triple>();
            }
            ots.add(t);
            byObject.put(o, ots);
        }*/
        for (Triple t : ts) {
            Node s = t.getSubject();
            HashSet<Triple> sts = bySubject.get(s);
            if (sts == null) {
                sts = new HashSet<Triple>();
            }
            sts.add(t);
            bySubject.put(s, sts);
        }

        for (Node s : bySubject.keySet()) {
            HashSet<Triple> starSubj = bySubject.get(s);
            SubQuery sq = new SubQuery(starSubj);
            sq.setSubjectStar();

            HashMap<Triple, List<Statistics>> shapesOfStar = new HashMap<>();

            //sq.getPredicates();
            //Identifying triples with shapes for the relevant star query. Every star should contain the shapes stat for its relevant triples
            starSubj.forEach(triple -> {
                if (relevantShapes.containsKey(triple)) {
                    List<Statistics> x = relevantShapes.get(triple);
                    shapesOfStar.put(triple, x);
                }

            });
            sq.setShapes(shapesOfStar);
            stars.add(sq);
        }
        return stars;
    }

    private BasicPattern subQueryIterator(Vector<SubQuery> stars, Query query) {
        List<Star> starList = new ArrayList<>();

        for (SubQuery sq : stars) {
            //System.out.println("Parsing SubQuery " + sq.toString());

            List<TripleWithStats> tripleWithStatsArrayList = new ArrayList<>();
            // Go through each triple of star and compute ranking
            for (Triple triple : sq.getTriples()) {
                //System.out.println(triple);
                double rank = 0;

                // ************************ CASE A *******************************
                if (!triple.getObject().isVariable()) {
                    rank = ranksCalculator.getRankWhenObjIsNotVar(sq, triple, rank);
                }
                // ************************  CASE B *******************************
                if (triple.getObject().isVariable()) {
                    rank = ranksCalculator.getRankWhenObjIsVar(sq, triple, rank);
                }
                double dsc = ranksCalculator.getCountDistinctSubjects();
                double doc = ranksCalculator.getCountDistinctObjects();
                //System.out.println("dsc: " + dsc + "  doc: " + doc);
                tripleWithStatsArrayList.add(new TripleWithStats(DoubleRounder.round(rank, 5), dsc, doc, triple));
            }

            //calculate cardinality using new formulas
            double estimatedStarCardinality;
            double nom = 1D;
            double maxDistinctSubjectCount = 1D;

            for (TripleWithStats tripleWithStats : tripleWithStatsArrayList) {
                if (tripleWithStats.cardinality != 0) {
                    nom = nom * tripleWithStats.cardinality;
                }
                if (tripleWithStats.distinctSubjects > maxDistinctSubjectCount) {
                    maxDistinctSubjectCount = tripleWithStats.distinctSubjects;
                }
            }

            if (tripleWithStatsArrayList.size() == 1) {
                estimatedStarCardinality = nom;
            } else {
                estimatedStarCardinality = nom / maxDistinctSubjectCount;
            }

            //System.out.println(estimatedStarCardinality + " - " + tripleWithStatsArrayList.toString());
            starList.add(new Star(estimatedStarCardinality, tripleWithStatsArrayList));

        }

        // Number of Subject Stars = Size of the List of stars;
        // For each star we need to estimate the intermediate result size of join with the other star
        HashMap<String, Double> joinPairCost = new HashMap<>();

        int starsCount = starList.size();
        int[][] index = new int[starsCount][starsCount];
        int i = 0;

        //System.out.println(starList.toString());

        //iterate over list of stars
        //outer loop
        for (Star starA : starList) {

            int j = 0;
            for (Star starB : starList) {
                if (i != j) {
                    if (index[j][i] == 0) {
                        //main logic here

                        //System.out.println("\nSTAR [" + (i + 1) + "][" + (j + 1) + "]  -->  ");
                        boolean joinFlag = false;
                        double cost = 0;
                        // StarA JOIN StarB
                        for (TripleWithStats starATriple : starA.triples) {
                            for (TripleWithStats starBTriple : starB.triples) {
                                //if(starATriple.triple.getSubject().equals(starBTriple.triple.getSubject())) {
                                //    System.out.println(starATriple.triple.toString() + " SS_JOIN " + starBTriple.triple.toString());
                                //}

                                if (starATriple.triple.getSubject().toString().equals(starBTriple.triple.getObject().toString())) {
                                    //System.out.println(starATriple.triple.toString() + " SO_JOIN " + starBTriple.triple.toString());
                                    cost = estimateIntermediateResultSize(starA.starCardinality, starATriple.distinctSubjects, starB.starCardinality, starBTriple.distinctObjects);

                                    joinFlag = true;
                                }

                                if (starATriple.triple.getObject().toString().equals(starBTriple.triple.getSubject().toString())) {
                                    //System.out.println(starATriple.triple.toString() + " OS_JOIN " + starBTriple.triple.toString());
                                    cost = estimateIntermediateResultSize(starA.starCardinality, starATriple.distinctObjects, starB.starCardinality, starBTriple.distinctSubjects);

                                    joinFlag = true;
                                }

                                if (starATriple.triple.getObject().toString().equals(starBTriple.triple.getObject().toString())) {
                                    //System.out.println(starATriple.triple.toString() + " OO_JOIN " + starBTriple.triple.toString());
                                    cost = estimateIntermediateResultSize(starA.starCardinality, starATriple.distinctObjects, starB.starCardinality, starBTriple.distinctObjects);

                                    joinFlag = true;
                                }
                            }

                        }
                        if (!joinFlag) {
                            cost = starA.starCardinality * starB.starCardinality;
                        }
                        //System.out.println("Cost " + cost);
                        joinPairCost.put((i + 1) + "." + (j + 1), cost);
                        index[i][j] = -1;
                    }
                }
                j++;
            }
            i++;
        }
        //System.out.println(joinPairCost);
        String order = new CostMapper().decider(joinPairCost);
        System.out.println(order);

        BasicPattern bp = new BasicPattern();
        List<Op> opList = new ArrayList<>();

        if (starList.size() > 2) {
            int starNumber = 0;
            for (String indexNumber : order.split("\\.")) {
                BasicPattern basicPattern = new BasicPattern();
                //Get the list of triples of the current star
                List<TripleWithStats> ListOfTriplesOfTheCurrentStar = starList.get(Integer.parseInt(indexNumber) - 1).triples;

                List<TripleWithStats> copy = ListOfTriplesOfTheCurrentStar;

                //System.out.println("star: " + starNumber + " order: " + indexNumber + " Triples: " + ListOfTriplesOfTheCurrentStar.size() + " Current Size of BP: " + bp.size());


                boolean flag = false;
                // loop through all the triples and add them to bp
                for (int loopIndex = 0; loopIndex < ListOfTriplesOfTheCurrentStar.size(); loopIndex++) {

                    TripleWithStats cTriple = ListOfTriplesOfTheCurrentStar.get(loopIndex);

                    if (starNumber >= 1 && !flag) {
                        //check the last triple added in the bp, get its variables, try to match them with the current triple in
                        //the loop, if it matches, go ahead, otherwise reserve it and try to match with the other triple

                        Triple lastTripleOfThePrevStar = bp.get(bp.size() - 1);
                        Triple currentTripleInTheLoop = cTriple.triple;

                        //System.out.println(lastTripleOfThePrevStar + " --> " + currentTripleInTheLoop);

                        flag = isJoinVariable(flag, lastTripleOfThePrevStar, currentTripleInTheLoop);

                        if (flag && ListOfTriplesOfTheCurrentStar.size() > 1) {
                            //There is no join variable, now we have to check with the next triple
                            //System.out.println("Swapping ...");
                            Collections.swap(copy, 0, loopIndex);
                        }
                    }

                }

                for (TripleWithStats cTriple : copy) {
                    bp.add(cTriple.triple);
                    basicPattern.add(cTriple.triple);
                }

                Op algebraOpBasicPatter = new OpBGP(basicPattern);
                opList.add(algebraOpBasicPatter);
                starNumber++;
            }
        } else {
            //for stars less than 2
            for (Star star : starList) {
                for (TripleWithStats triples : star.triples) {
                    bp.add(triples.triple);
                }
            }
        }


        //customJoinExecutor(opList, query);


        return bp;
    }

    private boolean isJoinVariable(boolean flag, Triple lastTripleOfThePrevStar, Triple currentTripleInTheLoop) {
        if (lastTripleOfThePrevStar.objectMatches(currentTripleInTheLoop.getObject())) {
            //System.out.println("Object Object Match Found");
            flag = true;
        }

        if (lastTripleOfThePrevStar.objectMatches(currentTripleInTheLoop.getSubject())) {
            //System.out.println("obj Subj Match Found");
            flag = true;
        }

        if (lastTripleOfThePrevStar.subjectMatches(currentTripleInTheLoop.getObject())) {
            //System.out.println("Subj Obj Match Found");
            flag = true;
        }
        return flag;
    }

    private void customJoinExecutor(List<Op> opList, Query query) {
        //System.out.println(opList);
        //System.out.println(opList.size());

        Dataset ds = RdfUtils.getTDBDataset();
        Model gm = ds.getNamedModel(Main.getRdfModelIRI());
        ds.begin(ReadWrite.READ);

        QueryIterator queryIterator = null;

        queryIterator = Join.nestedLoopJoin(Algebra.exec(opList.get(0), gm.getGraph()), Algebra.exec(opList.get(1), gm.getGraph()), null);
        queryIterator = Join.nestedLoopJoin(queryIterator, Algebra.exec(opList.get(2), gm.getGraph()), null);
        queryIterator = Join.nestedLoopJoin(queryIterator, Algebra.exec(opList.get(3), gm.getGraph()), null);

 /*       if (opList.size() == 1) {
            System.out.println(Algebra.optimize(opList.get(0)));
            queryIterator = Algebra.exec(opList.get(0), gm.getGraph());
        } else {

            if (opList.size() >= 2) {
                System.out.println(Algebra.optimize(opList.get(0)));
                System.out.println(Algebra.optimize(opList.get(1)));

                queryIterator = Join.nestedLoopJoin(Algebra.exec(opList.get(0), gm.getGraph()), Algebra.exec(opList.get(1), gm.getGraph()), null);

                for (int index = 2; index < opList.size(); index++) {
                    System.out.println(Algebra.optimize(opList.get(index)));
                    queryIterator = Join.nestedLoopJoin(queryIterator, Algebra.exec(opList.get(index), gm.getGraph()), null);
                }
            }
        }*/

        ResultSet resultSet = new ResultSetStream(Lists.transform(query.getProjectVars(), Functions.toStringFunction()), gm, queryIterator);
        System.out.println("Result");
        System.out.println(ResultSetFormatter.consume(resultSet));

        ds.end();
        ds.close();
    }

    private double estimateIntermediateResultSize(double starACardinality, double v1, double starBCardinality,
                                                  double v2) {
        double nom = starACardinality * starBCardinality;
        double den = Math.max(v1, v2);
        return nom / den;
    }

    private Op makeQueryAlgebra(Query query, BasicPattern bp) {
        Op algebraOp;
        algebraOp = new OpBGP(bp);
        algebraOp = new OpProject(algebraOp, projectedVariables);
        algebraOp = new OpSlice(algebraOp, query.getOffset(), query.getLimit());

        if (filterExpression != null)
            algebraOp = OpFilter.filter(filterExpression, algebraOp);

        if (query.getOrderBy() != null)
            algebraOp = new OpOrder(algebraOp, query.getOrderBy());

        return algebraOp;
    }

    //Getter Functions
    List<String> getProjectedVariables() {
        return Lists.transform(projectedVariables, Functions.toStringFunction());
    }

    public List<Var> getVarProjectedVariables() {
        return projectedVariables;
    }

    // In case need to execute a query on an endpoint
    public Long executeAndEvaluateQueryOverEndpoint(String q) {
        SPARQLRepository repo = new SPARQLRepository(Main.getEndPointAddress());
        RepositoryConnection conn = repo.getConnection();
        long durationIs = 0l;
        try {
            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
            long start = System.currentTimeMillis();
            TupleQueryResult res = query.evaluate();
            //return Long.parseLong(rs.next().getValue("objs").stringValue());
            int n = 0;
            while (res.hasNext()) {
                res.next();
                //System.out.println(res.next());
                n++;
            }
            long duration = System.currentTimeMillis() - start;
            durationIs = duration;
            //System.out.println("Done query \n" + q + ": \n duration=" + duration + "ms, results=" + n);
        } finally {
            conn.close();
            repo.shutDown();
        }
        //System.out.println(durationIs + " ms");
        return durationIs;
    }
}