//package dk.aau.cs.shacl.query;
//
//import dk.aau.cs.shacl.Main;
//import dk.aau.cs.shacl.shapes.ShapesDetector;
//import dk.aau.cs.shacl.shapes.Statistics;
//import dk.aau.cs.shacl.utils.RdfUtils;
//import dk.aau.cs.shacl.utils.Tuple2;
//import dk.aau.cs.shacl.utils.Tuple3;
//import javafx.util.Pair;
//import org.apache.jena.ext.com.google.common.base.Functions;
//import org.apache.jena.ext.com.google.common.collect.Lists;
//import org.apache.jena.graph.Node;
//import org.apache.jena.graph.NodeFactory;
//import org.apache.jena.graph.Triple;
//import org.apache.jena.query.Query;
//import org.apache.jena.query.QueryFactory;
//import org.apache.jena.sparql.algebra.AlgebraGenerator;
//import org.apache.jena.sparql.algebra.JoinType;
//import org.apache.jena.sparql.algebra.Op;
//import org.apache.jena.sparql.algebra.OpWalker;
//import org.apache.jena.sparql.algebra.op.OpBGP;
//import org.apache.jena.sparql.algebra.op.OpProject;
//import org.apache.jena.sparql.algebra.op.OpSlice;
//import org.apache.jena.sparql.core.BasicPattern;
//import org.apache.jena.sparql.core.Var;
//import org.apache.jena.sparql.engine.join.Join;
//import org.apache.jena.vocabulary.RDF;
//import org.eclipse.rdf4j.query.QueryLanguage;
//import org.eclipse.rdf4j.query.TupleQuery;
//import org.eclipse.rdf4j.query.TupleQueryResult;
//import org.eclipse.rdf4j.repository.RepositoryConnection;
//import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//
//public class EvaluateSPARQLQueryDuplicate {
//    private String sparqlQuery = "";
//    private static boolean distinct;
//    private static List<Var> projectedVariables;
//    private boolean rdfTypeFlag = false;
//
//    private static HashMap<Triple, List<Tuple3<String, String, String>>> triplesWithShapesInfo = null;
//
//    public EvaluateSPARQLQueryDuplicate() {
//    }
//
//    public EvaluateSPARQLQueryDuplicate(String query) {
//        this.sparqlQuery = query;
//    }
//
//    public List<String> getProjectedVariables() {
//        return Lists.transform(projectedVariables, Functions.toStringFunction());
//    }
//
//
//    public Op useShapesToGenerateOptimizedAlgebra() {
//        Query query = QueryFactory.create(sparqlQuery);
//        ArrayList<HashSet<Triple>> bgps = getBGPs(query);
//
//        //System.out.println("Count of BGPs " + bgps.size());
//        HashMap<HashSet<Triple>, Vector<Tree<Pair<Integer, Triple>>>> plans = new HashMap<HashSet<Triple>, Vector<Tree<Pair<Integer, Triple>>>>();
//
//        String newQueryIs = "";
//        Op algebraOp = null;
//
//        distinct = query.isDistinct();
//        projectedVariables = query.getProjectVars();
//
//        //for every basic graph pattern
//        for (HashSet<Triple> triples : bgps) {
//            HashMap<HashSet<Node>, Pair<Vector<Tree<Pair<Double, Triple>>>, Pair<Double, Double>>> DPTable = new HashMap<>();
//            HashMap<HashSet<Node>, Pair<HashSet<Node>, HashSet<Node>>> log = new HashMap<HashSet<Node>, Pair<HashSet<Node>, HashSet<Node>>>();
//
//            List<Tuple2<Double, List<Pair<Double, Triple>>>> starCostAndOrderedTriples = new ArrayList<>();
//
//            long initPlanningTime = System.currentTimeMillis();
//            HashMap<Triple, List<Tuple3<String, String, String>>> triplesToShapesMapping = new ShapesDetector(triples).getBgpShapes();
//            long planningTimeShape = System.currentTimeMillis() - initPlanningTime;
//            System.out.println("Time elapsed to get the BGP shapes: " + planningTimeShape + "ms");
//
//            // Triple, List< Tuple <shapeProperty, count, distinct Count>>
//            //HashMap<Triple, List<Tuple3<String, Integer, Integer>>> triplesToShapesMappingWithStats = new HashMap<>();
//
//            HashMap<Triple, List<Statistics>> triplesWithStats = new HashMap<>();
//
//            // Get statistics
//            triplesToShapesMapping.forEach((triple, list) -> {
//                List<Tuple3<String, Integer, Integer>> temp = new ArrayList<>();
//                List<Statistics> tempStatList = new ArrayList<>();
//                list.forEach(tuple -> {
//
//                    if (triple.getPredicate().toString().equals(RDF.type.toString())) {
//
//                        //In this case we treat <?x a :Composition> where :Composition is a class and there is no min & max count associated with the Node Shape.
//                        // There is no shape property for RDF.type predicate
//
//                        int subjectCount = (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "count", Main.getShapesModelIRI())).getInt();
//                        Statistics statistics = new Statistics(triple, tuple._3,
//                                1, 1, // default values for min and max count is 1 (Assumption)
//                                subjectCount,
//                                (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "distinctCount", Main.getShapesModelIRI())).getInt(), tuple._2,0,0);
//                        statistics.setSubject(tuple._1);
//                        statistics.setSubjectCount(subjectCount);
//                        tempStatList.add(statistics);
//                    } else {
//                        Statistics statistics = new Statistics(
//                                triple,
//                                tuple._3,
//                                (RdfUtils.getFromModel(tuple._3, "http://www.w3.org/ns/shacl#minCount", Main.getShapesModelIRI())).getInt(),
//                                (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "maxCount", Main.getShapesModelIRI())).getInt(), // need a check on maxCount
//                                (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "count", Main.getShapesModelIRI())).getInt(),
//                                (RdfUtils.getFromModel(tuple._3, Main.getShapesStatsPrefixURL() + "distinctCount", Main.getShapesModelIRI())).getInt(), tuple._2,0,0);
//                        statistics.setSubject(tuple._1);
//                        statistics.setSubjectCount((RdfUtils.getFromModel(Main.getShapesPrefixURL() + tuple._2 + "Shape", Main.getShapesStatsPrefixURL() + "count", Main.getShapesModelIRI())).getInt());
//                        tempStatList.add(statistics);
//                    }
//                });
//                triplesWithStats.put(triple, tempStatList);
//                //triplesToShapesMappingWithStats.put(triple, temp);
//            });
//
//            // Get Stars
//            Vector<SubQuery> stars = getStars(triples, triplesWithStats);
//            int k = 1;
//
//            //Iterate over stars to estimate the selectivity
//            for (SubQuery sq : stars) {
//                List<Pair<Double, Triple>> pairArrayList = new ArrayList<>();
//                //System.out.println();
//                //System.out.println("For star -->  " + sq.toString());
//                this.rdfTypeFlag = false;
//                List<Triple> independentTriples = new ArrayList<>();
//
//                // Go through each triple of star and compute ranking
//                for (Triple triple : sq.getTriples()) {
//
//                    //TODO ************************ LITERAL CASE *******************************
//                    double rank = 0;
//                    if (triple.getObject().isLiteral()) {
//                        // ?s    ?p   objA          -->4. c_t / c_o  ...  TotalCount / distinct
//                        // subjA ?p   objA          -->6. c_t / c_s . c_o
//                        if (triple.getPredicate().isVariable()) {
//
//                            if (triple.getSubject().isVariable()) {
//                                rank = (double) Main.getAllTriplesCount() / (double) Main.getDistinctObjectsCount();
//                            } else {
//                                rank = (double) Main.getAllTriplesCount() / ((double) Main.getDistinctSubjectsCount() * (double) Main.getDistinctObjectsCount());
//                            }
//                        }
//
//                        if (!triple.getPredicate().isVariable()) {
//                            // ?s     rdf:type objA         -->11. getTotalSubjectCount
//                            // subjA  rdf:type objA         -->12. Assumption here 1
//                            if (triple.getPredicate().getURI().equals(RDF.type.toString())) {
//                                if (triple.getSubject().isVariable()) {
//                                    for (Statistics stats : (sq.getShapes().get(triple))) {
//                                        rank += stats.getSubjectCount().doubleValue();
//                                    }
//                                } else {
//                                    rank = 1;
//                                }
//                            } else {
//                                // ?s predA objA            -->7. getTotalSubjectCount/getDistinctCountPredicate
//                                // subjA predA objA         -->8. getPredicateCount/getSubjectCount
//                                if (triple.getSubject().isVariable()) {
//                                    double nom = 0, den = 0;
//                                    for (Statistics stats : (sq.getShapes().get(triple))) {
//                                        nom += stats.getSubjectCount().doubleValue();
//                                        den += stats.getDistinctCount().doubleValue();
//                                    }
//                                    rank = nom / den;
//                                } else {
//                                    double nom = 0, den = 0;
//                                    for (Statistics stats : (sq.getShapes().get(triple))) {
//                                        nom += stats.getTotalCount().doubleValue();
//                                        den += stats.getSubjectCount().doubleValue();
//                                    }
//                                    rank = nom / den;
//                                }
//                            }
//                        }
//
//
//                    }
//                    //TODO ************************ NOT LITERAL CASE *******************************
//
//                    if (!triple.getObject().isLiteral()) {
//                        // ?s    ?p   ?o          -->1. c_t
//                        // subjA ?p   ?o          -->2. c_t / c_s
//                        if (triple.getPredicate().isVariable()) {
//                            if (triple.getSubject().isVariable()) {
//                                rank = Main.getAllTriplesCount();
//                            } else {
//                                rank = (double) Main.getAllTriplesCount() / (double) Main.getDistinctSubjectsCount();
//                            }
//                        }
//
//                        if (!triple.getPredicate().isVariable()) {
//                            // ?s     rdf:type ?o         -->9. Count of rdf:type triples
//                            // subjA  rdf:type ?0         -->10. countRdfTypeTriples/ No. of subjects in the dataset with predicate rdf:type
//                            if (triple.getPredicate().getURI().equals(RDF.type.toString())) {
//                                if (triple.getSubject().isVariable()) {
//                                    rank = Main.getDistinctRdfTypeCount();
//                                } else {
//                                    rank = (double) Main.getDistinctRdfTypeCount() / Main.getDistinctRdfTypeSubjCount();
//                                    /*for (Statistics stats : (sq.getShapes().get(triple))) {
//                                        rank += stats.getSubjectCount()/ stats.getSubjectCount().doubleValue();
//                                    }*/
//                                }
//
//                            } else {
//                                // ?s predA ?o            -->3. No. of triples in the dataset with predA
//                                // subjA predA ?o         -->5. No. of subjects in the dataset with predA
//                                if (triple.getSubject().isVariable()) {
//
//                                    for (Statistics stats : (sq.getShapes().get(triple))) {
//                                        rank += stats.getTotalCount().doubleValue();
//                                    }
//                                } else {
//                                    double nom = 0, den = 0;
//                                    System.out.println("subjA predA ?o");
//                                    for (Statistics stats : (sq.getShapes().get(triple))) {
//                                        nom += stats.getSubjectCount().doubleValue();
//                                        den += stats.getDistinctCount().doubleValue();
//                                        System.out.println("Nom: "+ nom + "  -  Den: " + den);
//                                    }
//                                    rank = nom / den;
//                                    System.out.println("Rank " + rank);
//
//                                }
//                            }
//                        }
//                        pairArrayList.add(new Pair<>(rank, triple));
//                    }
//
//
//                  /*  if (triple.getObject().isLiteral()) {
//                        double rank = 0;
//                        //System.out.println(sq.getShapes());
//                        double nom = 0, denom = 0;
//                        for (Statistics stats : (sq.getShapes().get(triple))) {
//                            nom += stats.getSubjectCount().doubleValue();
//                            denom += stats.getDistinctCount().doubleValue();
//                        }
//                        rank = nom / denom;
//                        pairArrayList.add(new Pair<Double, Triple>(normalize(rank), triple));
//                        continue;
//                    }
//
//                    if (!triple.getPredicate().isVariable() && !triple.getObject().isVariable()) {
//                        if (triple.getPredicate().toString().equals(RDF.type.toString())) {
//                            this.rdfTypeFlag = true;
//                            double rank = 0;
//                            System.out.println(sq.getShapes().get(triple).size());
//                            if ((sq.getShapes().get(triple)).size() == 1) {
//                                for (Statistics stats : (sq.getShapes().get(triple))) {
//                                    //System.out.println(stats.getShapeOrClassLocalName());
//                                    //System.out.println(stats.getSubjectCount().doubleValue() + " -- " + stats.getTotalCount() );
//                                    rank = stats.getSubjectCount().doubleValue() / stats.getTotalCount();
//                                    pairArrayList.add(new Pair<Double, Triple>(normalize(rank), triple));
//                                }
//                            } else {
//                                //TODO take care now
//                                System.out.println("*********** Multiple Statistics ****************");
//                            }
//                        } else {
//                            independentTriples.add(triple);
//                        }
//                    }
//
//                    if (!triple.getPredicate().isVariable() && triple.getObject().isVariable()) {
//
//                        if (triple.getPredicate().toString().equals(RDF.type.toString())) {
//
//                            System.out.println("Need to think about this case - Currently not handled");
//                            //independentTriples.add(triple);
//                            *//*double card_S_RdfType_O = 0;
//                            if ((sq.getShapes().get(triple)).size() == 1) {
//                                Statistics stats = (sq.getShapes().get(triple)).get(0);
//                                card_S_RdfType_O = stats.getTotalCount();
//                            } else {
//                                //TODO take care now
//                                System.out.println("*********** Multiple Statistics ****************");
//                            }
//                            pairArrayList.add(new Pair<>(normalize(card_S_RdfType_O), triple));*//*
//                        } else {
//                            independentTriples.add(triple);
//                        }
//                    }*/
//
//                }// end of triples for loop
//
//               /* System.out.println("Size of independent triples for this star: " + independentTriples.size());
//                // Go through independent triples
//                for (Triple triple : independentTriples) {
//                    System.out.println("Processing independent triple " + triple);
//                    double computedRank = 0d;
//
//                    for (Statistics stats : (sq.getShapes().get(triple))) {
//
//                        if (triple.getObject().isVariable()) {
//                            // According to the formula form Prof. Katja's Paper i.e. Resource Planning for SPARQL Query Execution on Data Sharing Platforms
//                            // It should be total count of the property
//                            computedRank += stats.getTotalCount();
//                        } else {
//                            computedRank += stats.getSubjectCount().doubleValue() / stats.getDistinctCount().doubleValue();
//                        }
//                    }
//                    pairArrayList.add(new Pair<Double, Triple>(normalize(computedRank), triple));
//                }*/
//
//                //pairArrayList = pairArrayList.stream().sorted(Comparator.comparing(Pair::getKey, Comparator.reverseOrder())).collect(Collectors.toList());
//                pairArrayList = pairArrayList.stream().sorted(Comparator.comparing(Pair::getKey)).collect(Collectors.toList());
//
//                //Calculate Cardinality for this star
//                double cardinality = 0;
//                for (int x = 0; x < pairArrayList.size(); x++) {
//                    if (x == 0) {
//                        cardinality = (pairArrayList.get(x)).getKey();
//                    } else {
//                        cardinality = (cardinality * (pairArrayList.get(x)).getKey()) / Math.max(cardinality, (pairArrayList.get(x)).getKey());
//                    }
//                    //System.out.println("Cardinality is: " + cardinality);
//                }
//
//                //double costSum = pairArrayList.stream().mapToDouble(Pair::getKey).sum();
//                //double costProduct = pairArrayList.stream().mapToDouble(Pair::getKey).reduce(1l, (a, b) -> a * b);
//                starCostAndOrderedTriples.add(new Tuple2<>(cardinality, pairArrayList));
//
//                //TODO Doing Dynamic Programing stuff here...
//
//               /* Vector<Tree<Pair<Double, Triple>>> vTree = new Vector<>();
//                Tree<Pair<Double, Triple>> sortedStar = null;
//                HashSet<Node> nodes = new HashSet<Node>();
//                HashMap<Node, Vector<Tree<Pair<Double, Triple>>>> map = new HashMap<Node, Vector<Tree<Pair<Double, Triple>>>>();
//                Node nn = NodeFactory.createVariable("Star" + k);
//                //sq.getPredicates();
//                //Set<Triple> remainingTriples = sq.getUnboundedPredicateTriples();
//                //System.out.println("Size of Remaining Unbounded Triples: " + remainingTriples.size());
//                if (sq.isSubjectStar) {
//                    for (Pair<Double, Triple> pair : pairArrayList) {
//                        Leaf<Pair<Double, Triple>> leaf = new Leaf<>(new Pair<>(pair.getKey(), pair.getValue()));
//                        if (sortedStar == null) {
//                            sortedStar = leaf;
//                        } else {
//                            sortedStar = new Branch<>(sortedStar, leaf);
//                        }
//                    }
//                    sortedStar.setCard(cardinality);
//                    if (sortedStar.getCard() > 0) {
//                        vTree.add(sortedStar);
//                    }
//                    System.out.println(vTree);
//                }
//                if (vTree.size() == 0) {  // test case missing sources for a star
//                    nodes.clear();
//                    map.clear();
//                    triples.clear();
//                    break;
//                }
//
//                double cost = 0;
//
//                if (sq.isSubjectStar()) {
//                    cost = subjectCssVSTCost(vTree);
//                }
//
//                System.out.println("Cost = " + cost);
//
//                map.put(nn, vTree);
//
//                k++;
//
//                HashSet<Node> ns = new HashSet<Node>();
//                ns.add(nn);
//                nodes.add(nn);
//                triples.removeAll(sq.getTriples());
//                DPTable.put(ns, new Pair<>(vTree, new Pair<Double, Double>(cost, cost)));*/
//
//            }
//
//            BasicPattern bp = new BasicPattern();
//            // Print the cost of each star along with its ordered triple patterns
//            System.out.println();
//
//            starCostAndOrderedTriples.stream().sorted(Comparator.comparing(Tuple2::_1)).forEach(doubleListTuple2 -> {
//                System.out.println(doubleListTuple2._1 + " ---  " + doubleListTuple2._2);
//                doubleListTuple2._2.forEach(doubleTriplePair -> {
//                    bp.add(doubleTriplePair.getValue());
//                });
//            });
//           /* starCostAndOrderedTriples.forEach(doubleListTuple2 -> {
//                System.out.println(doubleListTuple2._1 + " ---  " + doubleListTuple2._2);
//                doubleListTuple2._2.forEach(doubleTriplePair -> {
//                    bp.add(doubleTriplePair.getValue());
//                });
//            });*/
//            algebraOp = new OpBGP(bp);
//            algebraOp = new OpProject(algebraOp, projectedVariables);
//            algebraOp = new OpSlice(algebraOp, 0, query.getLimit());
//            //System.out.println(algebraOp);
//        }
//
//        //System.out.println("----------------------------------");
//        //RdfUtils.runAQueryWithPlan(sparqlQuery, Main.getRdfModelIRI());
//
//        return algebraOp;
//    }
//
//    private static double normalize(double val) {
//
//        while (val > 10) {
//            val = val / 10;
//        }
//        return val;
//    }
//
//    private static ArrayList<HashSet<Triple>> getBGPs(Query query) {
//
//        ArrayList<HashSet<Triple>> bgps = null;
//        try {
//            Op op = (new AlgebraGenerator()).compile(query);
//            BGPVisitor bgpv = new BGPVisitor();
//            OpWalker.walk(op, bgpv);
//            bgps = bgpv.getBGPs();
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//        return bgps;
//    }
//
//    private static Vector<SubQuery> getStars(HashSet<Triple> triples, HashMap<Triple, List<Statistics>> relevantShapes) {
//
//        Vector<SubQuery> stars = new Vector<SubQuery>();
//        List<Triple> ts = new LinkedList<Triple>(triples);
//        HashMap<Node, HashSet<Triple>> bySubject = new HashMap<Node, HashSet<Triple>>();
//        for (Triple t : ts) {
//            Node s = t.getSubject();
//            HashSet<Triple> sts = bySubject.get(s);
//            if (sts == null) {
//                sts = new HashSet<Triple>();
//            }
//            sts.add(t);
//            bySubject.put(s, sts);
//        }
//        for (Node s : bySubject.keySet()) {
//            HashSet<Triple> starSubj = bySubject.get(s);
//
//            HashMap<Triple, List<Statistics>> shapesOfStar = new HashMap<>();
//            SubQuery sq = new SubQuery(starSubj);
//            sq.setSubjectStar();
//            sq.getPredicates();
//            //Identifying triples with shapes for the relevant star query. Every star should contain the shapes stat for its relevant triples
//            starSubj.forEach(triple -> {
//                if (relevantShapes.containsKey(triple)) {
//                    List<Statistics> x = relevantShapes.get(triple);
//                    shapesOfStar.put(triple, x);
//                }
//            });
//            sq.setShapes(shapesOfStar);
//            stars.add(sq);
//        }
//        return stars;
//    }
//
//    public Long executeAndEvaluateQueryOverEndpoint(String q) {
//        SPARQLRepository repo = new SPARQLRepository(Main.getEndPointAddress());
//        RepositoryConnection conn = repo.getConnection();
//        long durationIs = 0l;
//        try {
//            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
//            long start = System.currentTimeMillis();
//            TupleQueryResult res = query.evaluate();
//            //return Long.parseLong(rs.next().getValue("objs").stringValue());
//            int n = 0;
//            while (res.hasNext()) {
//                res.next();
//                //System.out.println(res.next());
//                n++;
//            }
//            long duration = System.currentTimeMillis() - start;
//            durationIs = duration;
//            //System.out.println("Done query \n" + q + ": \n duration=" + duration + "ms, results=" + n);
//        } finally {
//            conn.close();
//            repo.shutDown();
//        }
//        //System.out.println(durationIs + " ms");
//        return durationIs;
//    }
//
//    // precondition: triples in vTree share the same subject and all triples in each tree are evaluated at the same source
//    public static Double subjectCssVSTCost(Vector<Tree<Pair<Double, Triple>>> vTree) {
//        double cost = 0;
//        for (Tree<Pair<Double, Triple>> tmpTree : vTree) {
//            Double tmp = tmpTree.getCard();
//            tmpTree.setCost(tmp);
//            cost += tmp;
//        }
//        return cost;
//    }
//}
//
