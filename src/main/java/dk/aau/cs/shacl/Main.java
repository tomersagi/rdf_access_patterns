package dk.aau.cs.shacl;

import dk.aau.cs.shacl.query.BenchmarkQuery;
import dk.aau.cs.shacl.shapes.MetadataShapes;
import dk.aau.cs.shacl.shapes.StatsGenerator;
import dk.aau.cs.shacl.utils.ConfigManager;
import dk.aau.cs.shacl.utils.RdfUtils;
import dk.aau.cs.shacl.utils.Tuple5;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Main {
    final static Logger logger = Logger.getLogger(Main.class);

    public static String configPath;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    //private static int allTriplesCount = 0, distinctObjectsCount = 0, distinctSubjectsCount = 0, distinctRdfTypeCount = 0, distinctRdfTypeSubjCount = 0, distinctRdfTypeObjectsCount = 0;
    // FOR LUBM Dataset
    private static int allTriplesCount = 91108733, distinctObjectsCount = 12253331, distinctSubjectsCount = 10847184, distinctRdfTypeCount = 25101580, distinctRdfTypeSubjCount = 10847184, distinctRdfTypeObjectsCount = 46;

    public static void main(String[] args) throws Exception {
        configPath = args[0];
        logger.info("Main started....");
        loadRDF();
        loadSHACL();
        reloadSHACL();
        annotateShapesWithStats();
        writeToFile();
        benchmark();
    }

    private static void benchmark() {
        if (Objects.equals(ConfigManager.getProperty("benchmarkQuery"), "true")) {
            //prepareAndWarmUp();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String fileNameExecutionTime = ConfigManager.getProperty("csvFileAddress") + "_TIME_" + sdf.format(timestamp) + ".csv";
            String fileNameExecutionTimeSD = ConfigManager.getProperty("csvFileAddress") + "_SD_" + sdf.format(timestamp) + ".csv";

            BenchmarkQuery benchmarkQuery = new BenchmarkQuery();
            String q = "PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>\n" +
                    "SELECT ?x WHERE {\n" +
                    "    ?x a ub:Course . \n" +
                    "}";
            //benchmarkQuery.executeBenchmarkForOneQuery(q);
            benchmarkQuery.executeBenchmarkForAllQueries();
            csv(fileNameExecutionTime, benchmarkQuery.getExecutionTime());
            csv(fileNameExecutionTimeSD, benchmarkQuery.getExecutionTimeSD());
        }
    }

    private static void writeToFile() {
        if (Objects.equals(ConfigManager.getProperty("writeShapeGraphToFile"), "true")) {
            RdfUtils.writeGraphModelToFile(getShapesModelIRI(), ConfigManager.getProperty("shapesGraphFileAddress"));
        }
    }

    private static void annotateShapesWithStats() {
        if (Objects.equals(ConfigManager.getProperty("generateStatistics"), "true")) {
            System.out.println("Before: Size of the shape graph " + getShapesModelIRI() + " = " + RdfUtils.getSizeOfModel(getShapesModelIRI()));
            generateShapesStatistics();
            addMetadataShape();
            System.out.println("After: Size of the shape graph " + getShapesModelIRI() + " = " + RdfUtils.getSizeOfModel(getShapesModelIRI()));
        } else {
            System.out.println("Shapes graph is already annotated with the statistics");
        }
    }

    private static void addMetadataShape(){
        new MetadataShapes().annotate();
    }

    // Replace the shape graph (Sometimes it is required due to URI, IRI's issue)
    private static void reloadSHACL() {
        if (Objects.equals(ConfigManager.getProperty("reloadShapedData"), "true")) {

            if (RdfUtils.removeNamedGraph(getShapesModelIRI())) {
                System.out.println("Named graph " + getShapesModelIRI() + " removed successfully.\n Let's load new one");
                RdfUtils.loadTDBData(getShapesModelIRI(), getShapeRdfFile());
                RdfUtils.insertPrefix(getShapesModelIRI(), "shape", getShapesPrefixURL());
                RdfUtils.insertPrefix(getShapesModelIRI(), "stat", getShapesStatsPrefixURL());
                System.out.println("Size of the new shape model " + getShapesModelIRI() + " = " + RdfUtils.getSizeOfModel(getShapesModelIRI()));
            }
        }
    }

    private static void loadSHACL() {
        if (Objects.equals(ConfigManager.getProperty("loadShapesData"), "true")) {
            RdfUtils.loadTDBData(getShapesModelIRI(), getShapeRdfFile());
            RdfUtils.insertPrefix(getShapesModelIRI(), "shape", getShapesPrefixURL());
            RdfUtils.insertPrefix(getShapesModelIRI(), "stat", getShapesStatsPrefixURL());

        } else {
            System.out.println("Size of the model " + getShapesModelIRI() + " = " + RdfUtils.getSizeOfModel(getShapesModelIRI()));
        }
    }

    private static void loadRDF() {
        if (Objects.equals(ConfigManager.getProperty("loadRdfData"), "true")) {
            RdfUtils.loadTDBData(getRdfModelIRI(), getRdfFile());
        } else {
            System.out.println("Size of the model " + getRdfModelIRI() + " = " + RdfUtils.getSizeOfModel(getRdfModelIRI()));
        }
    }

    //In this method we will get stats about the whole RDF graph
    private static void prepareAndWarmUp() {
        //Distinct objects
        distinctObjectsCount = (RdfUtils.runAQuery("SELECT (COUNT(DISTINCT ?o) as ?distinctObjects) WHERE { ?s ?p ?o }",
                getRdfModelIRI()).next().getLiteral("distinctObjects")).getInt();

        //Distinct subjects
        distinctSubjectsCount = (RdfUtils.runAQuery("SELECT (COUNT(DISTINCT ?s) as ?distinctSubjects) WHERE { ?s ?p ?o }",
                getRdfModelIRI()).next().getLiteral("distinctSubjects")).getInt();


        //All triples
        allTriplesCount = (RdfUtils.runAQuery("SELECT (COUNT(DISTINCT *) as ?allTriples) WHERE { ?s ?p ?o }",
                getRdfModelIRI()).next().getLiteral("allTriples")).getInt();


        //Distinct rdf:type count
        distinctRdfTypeCount = (RdfUtils.runAQuery("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT (COUNT(DISTINCT *) as ?distinctRdfType) WHERE { ?s ?p ?o . FILTER(?p=rdf:type) }",
                getRdfModelIRI()).next().getLiteral("distinctRdfType")).getInt();

        //Distinct rdf:type count
        distinctRdfTypeObjectsCount = (RdfUtils.runAQuery("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT (COUNT(DISTINCT ?o) as ?distinctRDFObjectCount) WHERE { ?s a ?o }",
                getRdfModelIRI()).next().getLiteral("distinctRDFObjectCount")).getInt();

        //Distinct subject rdf:type count
        distinctRdfTypeSubjCount = (RdfUtils.runAQuery("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT (COUNT(DISTINCT ?s) as ?distinctSubj) WHERE { ?s a ?o }",
                getRdfModelIRI()).next().getLiteral("distinctSubj")).getInt();

        System.out.println(distinctObjectsCount + " - " + distinctSubjectsCount + " - " + distinctRdfTypeCount + " - " + allTriplesCount + " - " + distinctRdfTypeSubjCount + " - " + distinctRdfTypeObjectsCount);
    }

    private static void evaluateQuery(String query) {
        //RdfUtils.runAQueryWithPlan(query, Main.getRdfModelIRI());
        //RdfUtils.runAQuery(query, Main.getRdfModelIRI());

        //Query newQuery = OpAsQuery.asQuery(algebraOp);
        //System.out.println(newQuery.serialize());
        //newQueryIs = newQuery.serialize();

        //Generate algebra
        //Op op = Algebra.compile(query);
        //Op opnew = SSE.parseOp(String.valueOf(op)) ;
        //System.out.println(opnew);

        //op = Algebra.optimize(op);
        //System.out.println(op);
    }

    private static void generateShapesStatistics() {
        try {
            StatsGenerator statsGenerator = new StatsGenerator(getRdfModelIRI(), getShapesModelIRI());
            statsGenerator.executeStatsGenerator();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getRdfFile() {
        return ConfigManager.getProperty("rdfFile");
    }

    private static String getShapeRdfFile() {
        return ConfigManager.getProperty("shapesFile");
    }

    public static String getShapesPrefixURL() {
        return ConfigManager.getProperty("shapesPrefixURL");
    }

    public static String getShapesStatsPrefixURL() {
        return ConfigManager.getProperty("shapesStatsPrefixURL");
    }

    public static String getRdfModelIRI() {
        return ConfigManager.getProperty("rdfModelIRI");
    }

    public static String getShapesModelIRI() {
        return ConfigManager.getProperty("shapesModelIRI");
    }

    public static String getEndPointAddress() {
        return ConfigManager.getProperty("fusekiSparqlEndPointAddress");
    }

    private static void csv(String fileName, HashMap<String, Tuple5<Long, Long, Long, Long, Integer>> executionTime) {
        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(fileName);
            csvWriter.append("QueryID");
            csvWriter.append(",");
            csvWriter.append("Shape_Planning_Time_MS");
            csvWriter.append(",");
            csvWriter.append("Shape_Execution_Time_MS");
            csvWriter.append(",");
            csvWriter.append("Jena_Planning_Time_MS");
            csvWriter.append(",");
            csvWriter.append("Jena_Execution_Time_MS");
            csvWriter.append(",");
            csvWriter.append("Result_Size");
            csvWriter.append("\n");

            List<List<String>> rows = new ArrayList<>();

            executionTime.forEach((key, value) -> {
                List<String> temp = new ArrayList<>();
                temp.add(key);
                temp.add(value._1.toString());
                temp.add(value._2.toString());
                temp.add(value._3.toString());
                temp.add(value._4.toString());
                temp.add(value._5.toString());

                rows.add(temp);
            });

            for (List<String> rowData : rows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getAllTriplesCount() {
        return allTriplesCount;
    }

    public static int getDistinctObjectsCount() {
        return distinctObjectsCount;
    }

    public static int getDistinctSubjectsCount() {
        return distinctSubjectsCount;
    }

    public static int getDistinctRdfTypeCount() {
        return distinctRdfTypeCount;
    }

    public static int getDistinctRdfTypeSubjCount() {
        return distinctRdfTypeSubjCount;
    }

    public static int getDistinctRdfTypeObjectsCount() {
        return distinctRdfTypeObjectsCount;
    }
}
