//package dk.aau.cs.shacl.shapes;
//
//import dk.aau.cs.shacl.Main;
//import dk.aau.cs.shacl.utils.Constants;
//import dk.aau.cs.shacl.utils.KBManagement;
//import dk.aau.cs.shacl.utils.RdfUtils;
//import org.apache.jena.query.Dataset;
//import org.apache.jena.query.ReadWrite;
//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.Statement;
//import org.apache.jena.rdf.model.impl.PropertyImpl;
//import org.apache.jena.rdf.model.impl.ResourceImpl;
//import org.apache.jena.vocabulary.RDF;
//import org.eclipse.rdf4j.query.QueryLanguage;
//import org.eclipse.rdf4j.query.TupleQuery;
//import org.eclipse.rdf4j.query.TupleQueryResult;
//import org.eclipse.rdf4j.repository.Repository;
//import org.eclipse.rdf4j.repository.RepositoryConnection;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicReference;
//
//public class StatsGeneratorUsingGraphDB {
//    private String graphModelIRI = null;
//    private String shapesModelIRI = null;
//    private List<String> statisticQueries = new ArrayList<>();
//    private final String shapesStatsPrefixURL = Main.getShapesStatsPrefixURL();
//
//    private KBManagement kbManager = new KBManagement();
//    private Repository repository = kbManager.initGraphDBRepository();
//    private RepositoryConnection repositoryConnection = repository.getConnection();
//
//    public StatsGeneratorUsingGraphDB(String graphModelIRI, String shapesModelIRI) {
//        this.graphModelIRI = graphModelIRI;
//        this.shapesModelIRI = shapesModelIRI;
//    }
//
//    public void executeStatsGenerator() {
//        Dataset dataset = RdfUtils.getTDBDataset();
//        dataset.begin(ReadWrite.READ);
//        try {
//            Model graphModel = dataset.getNamedModel(graphModelIRI);
//            captureStats(graphModel);
//
//        } finally {
//            dataset.commit();
//            dataset.end();
//        }
//
//
//        applyStats(shapesModelIRI);
//        dataset.close();
//
//    }
//
//    private void captureStats(Model graphModel) {
//        repositoryConnection.begin();
//
//        String classesQuery = "SELECT DISTINCT ?className  WHERE { ?s rdf:type ?className. }";
//        TupleQuery tupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, classesQuery);
//        TupleQueryResult classesQueryResult = tupleQuery.evaluate();
//
//        classesQueryResult.forEach(triple -> {
//
//            String countPropClassQuery = "SELECT ?property (Count(*) AS ?count) WHERE " +
//                    "{ [] a <" + triple.getValue("className").stringValue() + "> ; ?property [] .} GROUP BY ?property ";
//            System.out.println(countPropClassQuery);
//
//            TupleQuery countPropClassTupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, countPropClassQuery);
//            TupleQueryResult countPropClassTupleQueryResult = countPropClassTupleQuery.evaluate();
//
//
//            countPropClassTupleQueryResult.forEach(t -> {
//                String maxCountQuery = "SELECT (MAX(?Y) AS ?maxCount) WHERE {\n" +
//                        "SELECT (COUNT( DISTINCT ?x) AS ?Y ) \n" +
//                        "WHERE {\n" +
//                        "    [] a <" + triple.getValue("className").stringValue() + "> ; <" + t.getValue("property").toString() + "> ?x .\n" +
//                        "}\n" +
//                        "    GROUP BY ?x \n" +
//                        "}";
//
//                System.out.println(maxCountQuery);
//            });
//
//
//
//        });
//
//        // Get Classes
//        RdfUtils.runAQuery(classesQuery, graphModel).forEachRemaining(triple -> {
//
//            String countPropClassQuery = "SELECT ?property (Count(*) AS ?count) WHERE " +
//                    "{ [] a <" + triple.get("className") + "> ; ?property [] .} GROUP BY ?property ";
//
//            // Get Properties with their count
//            RdfUtils.runAQuery(countPropClassQuery, graphModel).forEachRemaining(t -> {
//                // Some shape properties have missing sh:maxCount; therefore to avoid errors, we take care of it here.
//
//                String maxCountQuery = "SELECT (MAX(?Y) AS ?maxCount) WHERE {\n" +
//                        "SELECT (COUNT( DISTINCT ?x) AS ?Y ) \n" +
//                        "WHERE {\n" +
//                        "    [] a <" + triple.get("className") + "> ; <" + t.get("property") + "> ?x .\n" +
//                        "}\n" +
//                        "    GROUP BY ?x \n" +
//                        "}";
//                AtomicReference<String> maxCountPropResult = new AtomicReference<>("");
//
//                RdfUtils.runAQuery(maxCountQuery, graphModel).forEachRemaining(val -> {
//                    maxCountPropResult.set(val.get("maxCount").asLiteral().getInt() + "\"" + "^^xsd:integer");
//                });
//
//                //Query to get the distinct number of triples for the given property
//                String countDistinctPropQuery = "SELECT (COUNT( DISTINCT ?prop) AS ?distinctCount )  " +
//                        "WHERE { [] a <" + triple.get("className") + "> ; <" + t.get("property") + "> ?prop . }";
//                // Initializing atomic variable to refer in the lambda expression
//                AtomicReference<String> distinctCountPropResult = new AtomicReference<>("");
//
//                //Get the answer i.e., count of distinct properties for the above query
//                RdfUtils.runAQuery(countDistinctPropQuery, graphModel).forEachRemaining(dct -> {
//                    distinctCountPropResult.set(dct.get("distinctCount").asLiteral().getInt() + "\"" + "^^xsd:integer");
//                });
//
//                //  Differentiate between RDF Type properties and the rest
//                //  An insert query is prepared based on all the information collected so far
//                if (t.getResource("property").equals(RDF.type)) {
//                    String shapeCountQuery = "\n" +
//                            "INSERT {\n" +
//                            "shape:" + triple.getResource("className").getLocalName() + "Shape" + " stat:count  " + "\"" + t.get("count").asLiteral().getInt() + "\"" + "^^xsd:integer" + " . \n" +
//                            "shape:" + triple.getResource("className").getLocalName() + "Shape" + " stat:distinctCount  " + "\"" + distinctCountPropResult.get() + " .\n" +
//                            "} \n" +
//                            "WHERE {\n" +
//                            "shape:" + triple.getResource("className").getLocalName() + "Shape" + " a sh:NodeShape .\n" +
//                            "}";
//                    //System.out.println(shapeCountQuery);
//                    statisticQueries.add(shapeCountQuery);
//                } else {
//                    String shapePropCountQuery = "\n" +
//                            "INSERT {\n" +
//                            "shape:" + t.getResource("property").getLocalName() + triple.getResource("className").getLocalName() + "ShapeProperty" + " stat:count  " + "\"" + t.get("count").asLiteral().getInt() + "\"" + "^^xsd:integer" + " . \n" +
//                            "shape:" + t.getResource("property").getLocalName() + triple.getResource("className").getLocalName() + "ShapeProperty" + " stat:distinctCount  " + "\"" + distinctCountPropResult.get() + " . \n" +
//                            "shape:" + t.getResource("property").getLocalName() + triple.getResource("className").getLocalName() + "ShapeProperty" + " stat:maxCount  " + "\"" + maxCountPropResult.get() + " . \n" +
//                            "} \n" +
//                            "WHERE {\n" +
//                            "shape:" + triple.getResource("className").getLocalName() + "Shape" + " a sh:NodeShape; sh:property " + "shape:" + t.getResource("property").getLocalName() + triple.getResource("className").getLocalName() + "ShapeProperty  . \n" +
//                            "}";
//
//                    //System.out.println(shapePropCountQuery);
//                    statisticQueries.add(shapePropCountQuery);
//                }
//            });
//        });
//    }
//
//    private void applyStats(String shapesModelIRI) {
//        //Add Prefix stat with shapes stat uri to the model
//        Dataset dataset = RdfUtils.getTDBDataset();
//        dataset.begin(ReadWrite.WRITE);
//        Model graphModel = dataset.getNamedModel(shapesModelIRI);
//        graphModel.setNsPrefix("stat", shapesStatsPrefixURL);
//        dataset.commit();
//        dataset.end();
//        dataset.close();
//
//
//        statisticQueries.forEach((query) -> {
//            //System.out.println(query);
//            RdfUtils.runInsertQuery(Constants.getPREFIXES() + query, shapesModelIRI);
//        });
//    }
//}
