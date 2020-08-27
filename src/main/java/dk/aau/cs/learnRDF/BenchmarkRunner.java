package dk.aau.cs.learnRDF;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.expr.ExprEvalException;

import static org.junit.Assert.assertTrue;


/**
 * This class provides method to iterate over a banchmark and produce counting statistics
 */
public class BenchmarkRunner {

    private final boolean stopOnError;
    private final int verbose;
    public BenchmarkRunner(boolean stopOnError, int verbose){
        this.verbose = verbose;
        this.stopOnError = stopOnError;
    }

    /**
     * adds 1 to every element in arr where toAdd=true.
     *
     * @param arr   base array to add to
     * @param toAdd boolean array to examine
     */
    private void addBoolToInt(int[] arr, boolean[] toAdd) {
        for (int i = 0; i < arr.length; i++){
            arr[i] += (toAdd[i] ? 1 : 0);
        }
    }

    public void evaluateQuerysets(List<QuerySet> toEvaluate, Path outputFilePath) {
        Map<QuerySet, int[]> evaluationResults = new HashMap<>(toEvaluate.size());

        for (QuerySet qs : toEvaluate) {
            int countSuccess =0;
            int countFail =0;
            int countParsed = 0;
            System.out.println(String.format("Parsing: %s",  qs.getFolderPath() ));
            int[] res = new int[AccessPattern.values().length];
            Arrays.fill(res, 0);
            Map<String, String> queries = qs.listQueries();
            for (Map.Entry<String, String> q : queries.entrySet()) {
                try {
                    SPARQLQueryEvaluator evaluation =new SPARQLQueryEvaluator(q.getValue(), this.verbose>1);
                    countParsed++;
                    addBoolToInt(res, evaluation.evaluateQuery());
                    countSuccess++;

                } catch (QueryParseException| ExprEvalException | IllegalStateException qpe) {
                    countFail++;
                    if(this.verbose > 0) {
                        System.err.println("Failed to parse query " + q.getKey());
                    }
                    if(this.verbose > 1) {
                        System.err.println(qpe.getMessage());
                    }
                    if(stopOnError){
                        System.exit(3);
                    }
                    // qpe.printStackTrace();
                }
            }
            qs.setCorrectQueries(countSuccess);
            qs.setFailedQueries(countFail);
            evaluationResults.put(qs, res); //@Tomer Note: you were using an object has key without defining equals and hashvalue !?
            System.out.println(String.format("Parsed %s /  %s :\tsuccess %s\tfail to read %s ", countParsed, queries.size(), countSuccess, countFail ));
        }


        try {
            toCSV(evaluationResults, outputFilePath);
        } catch (IOException e) {
            System.err.println("Completed evaluation but failed to write it out to a csv file");
            e.printStackTrace();
        }
    }

    /**
     * Writes the evaluation results to a csv file
     *
     * @param evaluationResults map of query set and results
     * @param outputFilePath    nio path to file to be writting to
     */
    private void toCSV(Map<QuerySet, int[]> evaluationResults, Path outputFilePath) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath.toFile()))) {
            // adding header to csv
            String[] header = {"QuerySet", "Queries"};
            for (AccessPattern ap : AccessPattern.values()){
                header = ArrayUtils.add(header, ap.toString());
            }


            writer.writeNext(header);
            for (Map.Entry<QuerySet, int[]> p : evaluationResults.entrySet()) {
                QuerySet qs = p.getKey();
                int[] stats = p.getValue();
                String[] data1 = {qs.getName(), Integer.toString(qs.getCorrectQueries())}; //Some queries contain mistakes, this makes sure that numbers take into account only those we managed to parse
                for (int i : stats){
                    data1 = ArrayUtils.add(data1, Integer.toString(i)); // TODO: this really generates so many arrays to just add 1 value. Why are you not using a Good'ol'ArrayList?
                }
                data1 = ArrayUtils.add(data1, Integer.toString(qs.getFailedQueries()));

                data1 = ArrayUtils.add(data1, Integer.toString(qs.listQueries().size())); // @Tomer: you where parsing the folder structures every time to get the list of queries!

                writer.writeNext(data1);
            }
        }
    }

    /**
     * run evaluation
     *
     * @param args 0: query folder path, 1: out path
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Expected 2 arguments (1. query folder path and 2. output folder path. Received "
                    + args.length + " arguments. ");
            System.exit(-1);
        }

        File queryFolder = new File(args[0]);
        File outFolder = new File(args[1]);

        if (!queryFolder.exists() || !queryFolder.isDirectory()) {
            System.err.println("Expected 2 arguments (1. query folder path and 2. output folder path. First argument "
                    + args[0] + " is not a valid folder path. ");
            System.exit(-1);
        }

        if (!outFolder.exists() || !outFolder.isDirectory()) {
            System.err.println("Expected 2 arguments (1. query folder path and 2. output folder path. Second argument "
                    + args[1] + " is not a valid folder path. ");
            System.exit(-1);
        }


        int verbose = 0;
        boolean stopOnError = true;

        HashSet<String> enabledQuerySets = new HashSet<>();
        try {
            Optional<Path> confFile = Files.walk(Paths.get("./"))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().endsWith("configuration.properties")).findFirst();


            if(confFile.isEmpty()){
                System.err.println("No config file found.");
            } else {
                try (InputStream input = new FileInputStream(confFile.get().toFile())) {

                    Properties prop = new Properties();

                    // load a properties file
                    prop.load(input);

                    // get the property value and print it out
                    verbose = Integer.parseInt(prop.getProperty("verbose", "1").trim());
                    stopOnError = Boolean.parseBoolean(prop.getProperty("stopOnError", "true").trim());

                    // For now I just use the config file as a list of enabled query set, but is better is the toEvaluate is instantiated directly
                    String querySetList = prop.getProperty("querySets", "");
                    for(String s : querySetList.split(",")){
                        if (!s.isEmpty() || !s.isBlank()){
                            enabledQuerySets.add(s.trim());
                        }
                    }
                    System.out.println("Configuration set contains " + String.join(", ", enabledQuerySets));

                } catch (IOException e) {
                    System.err.println("Cannot read configuration file. Reason "+e.getLocalizedMessage());
                }
            }
        }  catch (IOException e) {
            System.err.println("Cannot search directory for configuration file. Reason "+e.getLocalizedMessage());
        }


        BenchmarkRunner br = new BenchmarkRunner(stopOnError, verbose); //TODO: handle either to stop at first error or to ignore errors
        List<QuerySet> toEvaluate = new ArrayList<>();
        //TODO: This is better if is parsed and instantiated from a config file,  I will try to use `configuration.properties`


        toEvaluate.add(new QuerySet("DBpedia", new File(queryFolder, "DBpedia"), new String[]{}));
        toEvaluate.add(new QuerySet("LDBC", new File(queryFolder, "LDBC"), new String[]{"queries.txt"}));
        toEvaluate.add(new QuerySet("LUBM", new File(queryFolder, "LUBM"), new String[]{}));
        toEvaluate.add(new QuerySet("WATDIV", new File(queryFolder, "WATDIV"), new String[]{"README"}));
        toEvaluate.add(new QuerySet("WikiData", new File(queryFolder, "WikiData"), new String[]{}));
        toEvaluate.add(new QuerySet("BioBench", new File(queryFolder, "BioBench"), new String[]{"queries.txt"}));
        toEvaluate.add(new QuerySet("FeasibleDBpedia", new File(queryFolder, "Feasible-DBpedia"), new String[]{}));
        toEvaluate.add(new QuerySet("SP2Bench", new File(queryFolder, "SP2Bench"), new String[]{}));
        toEvaluate.add(new QuerySet("SWDF", new File(queryFolder, "SWDF"), new String[]{}));
        toEvaluate.add(new QuerySet("ComplexQuestions", new File(queryFolder, "ComplexQuestions"), new String[]{"ComplexWebQuestions_dev.json","ComplexWebQuestions_test.json","ComplexWebQuestions_train.json","export.py"}));

        try {
            File out = new File(outFolder, "br_test_out.csv");
            if (!out.exists() || out.delete()) { //check if exists and delete if does //TODO: Silent delete of folders/files is dangerous business
                assertTrue("Failed to create output file", out.createNewFile()); //create outfile //TODO: I don't think this is how one should use assert :)
            }
            toEvaluate.removeIf(q -> !enabledQuerySets.isEmpty() && !enabledQuerySets.contains(q.getName()));
            System.out.println("Parsing "+ String.join(", ", toEvaluate.stream().map(QuerySet::getName).collect(Collectors.toCollection(ArrayList::new))));
            br.evaluateQuerysets(toEvaluate, out.toPath());
            Desktop desktop = Desktop.getDesktop();
            desktop.open(outFolder);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
