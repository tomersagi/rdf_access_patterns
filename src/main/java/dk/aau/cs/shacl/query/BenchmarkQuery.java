package dk.aau.cs.shacl.query;

import dk.aau.cs.shacl.Main;
import dk.aau.cs.shacl.utils.ConfigManager;
import dk.aau.cs.shacl.utils.RdfUtils;
import dk.aau.cs.shacl.utils.Tuple5;
import org.apache.jena.ext.com.google.common.base.Functions;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.mgt.Explain;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Objects;

public class BenchmarkQuery {
    final static org.apache.log4j.Logger logger = Logger.getLogger(BenchmarkQuery.class);
    private final File folder = new File(ConfigManager.getProperty("queries_directory"));
    private HashMap<String, Tuple5<Long, Long, Long, Long, Integer>> executionTime = new HashMap<>();
    private HashMap<String, Tuple5<Long, Long, Long, Long, Integer>> executionTimeSD = new HashMap<>();
    private int resultSize = 0;
    private long elapsedTimeJena = 0;
    private long elapsedTimeShape = 0;
    private long planTimeJena = 0;
    private long planningTimeShape = 0;
    private int rsJena = 0;
    private int rsShape = 0;
    Dataset ds = null;

    //private HashMap<String, Tuple4<Long, Long, Long, Long>> executionTimeNew = new HashMap<>();

    public BenchmarkQuery() {
    }

    public void executeBenchmarkForOneQuery(String thisQuery) {
        executeAndSaveCalculationsWithAverage("query", thisQuery);
    }

    public void executeBenchmarkForAllQueries() {
        for (final File fileEntry : this.folder.listFiles()) {
            System.out.println(fileEntry.getPath());
            String queryFile = fileEntry.getPath();
            int pos = queryFile.lastIndexOf("/");
            String queryId = queryFile.substring(pos >= 0 ? (pos + 1) : 0);
            Query query = QueryFactory.read(queryFile);
            System.out.println(query.toString());
            //executeAndSaveCalculations(queryId, query.toString());
            executeAndSaveCalculationsWithAverage(queryId, query.toString());
            executeTheGivenQueryAsItIs(query.toString());
        }
    }

    private void executeAndSaveCalculationsWithAverage(String queryId, String query) {
        //System.out.println(query);
        logger.info("Executing query " + queryId);
        int counter = Integer.parseInt(ConfigManager.getProperty("queryRunnerCounter"));
        logger.info("Running each query " + counter + " times.");
        long avgPlanningShape = 0, avgExecutionShape = 0, avgPlanningJena = 0, avgExecutionJena = 0;
        long[] shapePlanningTimeArray = new long[counter], shapeExecTimeArray = new long[counter], jenaPlanningTimeArray = new long[counter], jenaExecTimeArray = new long[counter];

        for (int i = 0; i < counter; i++) {
            logger.info("-> " + i);
            long[] time = benchmarkShapeAndSPARQLQuery(query, queryId);

            shapePlanningTimeArray[i] = time[0];
            shapeExecTimeArray[i] = time[1];
            jenaPlanningTimeArray[i] = time[2];
            jenaExecTimeArray[i] = time[3];

            avgPlanningShape += time[0];
            avgExecutionShape += time[1];
            avgPlanningJena += time[2];
            avgExecutionJena += time[3];

            long divider = i + 1;
            logger.info(queryId + " -- " + avgPlanningShape / divider + " -- " + avgExecutionShape / divider + " -- " + avgPlanningJena / divider + " -- " + avgExecutionJena / divider);
        }
        //logger.info("Standard Deviation -- " + queryId + " -- " + calculateSD(shapePlanningTimeArray) + " -- " + calculateSD(shapeExecTimeArray) + " -- " + calculateSD(JenaExecTimeArray));
        executionTimeSD.put(queryId, new Tuple5<>(calculateSD(shapePlanningTimeArray), calculateSD(shapeExecTimeArray), calculateSD(jenaPlanningTimeArray), calculateSD(jenaExecTimeArray), resultSize));
        executionTime.put(queryId, new Tuple5<>(avgPlanningShape / counter, avgExecutionShape / counter, avgPlanningJena / counter, avgExecutionJena / counter, resultSize));
    }

    private long[] benchmarkShapeAndSPARQLQuery(String query, String queryId) {

        long[] time = new long[4];

        ds = RdfUtils.getTDBDataset();
        Model gm = ds.getNamedModel(Main.getRdfModelIRI());
        ds.begin(ReadWrite.READ);

        elapsedTimeJena = 0;
        elapsedTimeShape = 0;
        planTimeJena = 0;
        rsJena = 0;
        rsShape = 0;
        planningTimeShape = 0;

        if (ConfigManager.getProperty("jenaExec").equals("true")) {
            executeJenaQuery(query, gm, queryId);
        }

        if (Objects.equals(ConfigManager.getProperty("shapeExec"), "true")) {
            executeShapeApproachQuery(query, gm, queryId);
        }

        time[0] = planningTimeShape;
        time[1] = elapsedTimeShape;
        time[2] = planTimeJena;
        time[3] = elapsedTimeJena;

        ds.end();
        ds.close();

        if (rsJena == rsShape) {
            resultSize = rsJena;
        } else {
            logger.info("MISMATCH_RESULT for " + queryId + " Jena: " + rsJena + " Shape:" + rsShape);
            resultSize = Math.max(rsJena, rsShape);
        }

        return time;
    }

    private void executeJenaQuery(String query, Model gm, String queryId) {
        long initPlanTimeJena = System.currentTimeMillis();
        QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(query), gm);
        setLoggingLevel(qExec);
        ResultSet resultJenaExec = qExec.execSelect();
        planTimeJena = System.currentTimeMillis() - initPlanTimeJena;
        logger.info("Jena Plan Computed in " + planTimeJena + " ms");
        long startTimeExecutionSparql = System.currentTimeMillis();
        rsJena = iterateOverResultsWithTimeOut(resultJenaExec);
        elapsedTimeJena = System.currentTimeMillis() - startTimeExecutionSparql;
        resultToFile("Jena", queryId, resultJenaExec, rsJena, elapsedTimeJena);
        qExec.close();
    }

    private void executeShapeApproachQuery(String query, Model gm, String queryId) {
        EvaluateSPARQLQuery queryEvaluator = new EvaluateSPARQLQuery(query);
        //EvaluateSPARQLQueryDuplicate queryEvaluator = new EvaluateSPARQLQueryDuplicate(query);
        long initPlanningTime = System.currentTimeMillis();

        Op op = queryEvaluator.useShapesToGenerateOptimizedAlgebra();
        //System.out.println(op.toString());
        planningTimeShape = System.currentTimeMillis() - initPlanningTime;
        logger.info("Shape Plan Computed in " + planningTimeShape + " ms");
        long startTime = System.currentTimeMillis();
        if (!ds.isInTransaction())
            ds.begin(ReadWrite.READ);
        QueryIterator queryIterator = Algebra.exec(op, gm.getGraph());
        logger.info("Algebra executed into the query iterator");
        ResultSet resultShapeExec = new ResultSetStream(queryEvaluator.getProjectedVariables(), gm, queryIterator);
        logger.info("Result streamed into ResultSet");



        rsShape = iterateOverResultsWithTimeOut(resultShapeExec);
        queryIterator.close();

        elapsedTimeShape = System.currentTimeMillis() - startTime;
        resultToFile("ShapeApproach", queryId, resultShapeExec, rsShape, elapsedTimeShape);
    }

    public int iterateOverResults(ResultSet resultSet) {
        int iterationCounter = 0;

        while (resultSet.hasNext()) {
            Binding b = resultSet.nextBinding();
            //System.out.println(b.toString());
            iterationCounter++;
        }
        return iterationCounter;
    }


    public int iterateOverResultsWithTimeOut(ResultSet resultSet) {
        long limit = System.currentTimeMillis() + 120000;
        int iterationCounter = 0;

        while (resultSet.hasNext() ) {

            Binding b = resultSet.nextBinding();
            //System.out.println(b.toString());
            if(System.currentTimeMillis() > limit){
                logger.info("TIME OUT ...............................................................");
                break;
            }

            //System.out.println(b.toString());
            iterationCounter++;
        }

        return iterationCounter;
    }

    private void resultToFile(String action, String queryId, ResultSet resultSet, int size, long elapsedTime) {
        if (Objects.equals(ConfigManager.getProperty("outputQueryResultInFile"), "true")) {
            logger.info("START FLUSHING RESULT .... ");
            writeToFile(ResultSetFormatter.asText(resultSet), queryId + "_" + action + ".txt");
            logger.info("FINISH FLUSHING RESULT .... ");
        }
        logger.info(queryId + ": " + action + " took " + elapsedTime + " ms" + " RESULT_SIZE: " + size);
    }

    private void writeToFile(String str, String fileNameAndPath) {
        try {
            String address = ConfigManager.getProperty("queryResultOutputDirectory");
            FileWriter fileWriter = new FileWriter(new File(address, fileNameAndPath));
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(str);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Tuple5<Long, Long, Long, Long, Integer>> getExecutionTime() {
        return executionTime;
    }

    public HashMap<String, Tuple5<Long, Long, Long, Long, Integer>> getExecutionTimeSD() {
        return executionTimeSD;
    }


    private static long calculateSD(long[] numArray) {
        long sum = 0, standardDeviation = 0;
        int length = numArray.length;

        for (long num : numArray) {
            sum += num;
        }

        long mean = sum / length;

        for (long num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return (long) Math.sqrt(standardDeviation / length);
        /*
        Note: This program calculates standard deviation of a sample. If you need to compute S.D. of a population,
        return Math.sqrt(standardDeviation/(length-1)) instead of Math.sqrt(standardDeviation/length) from the calculateSD() method.
         */
    }

    private void setLoggingLevel(QueryExecution qExec) {
        if (ConfigManager.getProperty("showJenaExecutionPlan").equals("true"))
            qExec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL);
    }

    private void executeTheGivenQueryAsItIs(String q) {
        if (Objects.equals(ConfigManager.getProperty("executeQueryAsItIs"), "true")) {
            logger.info("Executing Query as it is....");
            long t1 = System.currentTimeMillis();
            Query query = QueryFactory.create(q);
            Op op = Algebra.compile(query);

            Dataset ds = RdfUtils.getTDBDataset();
            Model gm = ds.getNamedModel(Main.getRdfModelIRI());
            ds.begin(ReadWrite.READ);
            QueryIterator queryIterator = Algebra.exec(op, gm.getGraph());
            ResultSet resultSet = new ResultSetStream(Lists.transform(query.getProjectVars(), Functions.toStringFunction()), gm, queryIterator);
            logger.info("Result set size: " + iterateOverResults(resultSet));
            System.out.println(System.currentTimeMillis() - t1);
            queryIterator.close();
            ds.end();
            ds.close();
        }
    }
}
