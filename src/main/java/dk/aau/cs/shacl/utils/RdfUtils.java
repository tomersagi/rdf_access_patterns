package dk.aau.cs.shacl.utils;


import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.mgt.Explain;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateAction;
import dk.aau.cs.shacl.Main;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RdfUtils {
    final static Logger logger = Logger.getLogger(RdfUtils.class);
    public static Dataset copyOfTheDataset = null;

    public static Dataset getTDBDataset() {
        if (copyOfTheDataset == null) {
            try {
                return TDBFactory.createDataset(ConfigManager.getProperty("db_path") + ConfigManager.getProperty("db_name"));
            } catch (Exception e) {
                System.out.println("An error has occurred obtaining TDB dataset");
                e.printStackTrace();
            }
        }
        return copyOfTheDataset;
    }

    public static String getSizeOfModel(String uri) {
        long val = 0;
        Dataset dataset = getTDBDataset();
        dataset.begin(ReadWrite.READ);
        try {

            Model modelGraph = dataset.getNamedModel(uri);
            val = modelGraph.size();

        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
        } finally {
            dataset.end();
            dataset.close();
        }
        return String.valueOf(val);
    }

    public static Map<String, String> getPrefixes(String modelUri) {

        Dataset dataset = getTDBDataset();
        dataset.begin(ReadWrite.READ);
        Model modelGraph = dataset.getNamedModel(modelUri);

        Map<String, String> prefixMap = modelGraph.getNsPrefixMap();

        if (dataset.isInTransaction()) {
            dataset.end();
            dataset.close();
        }
        return prefixMap;
    }


    public static String extractPrefixMappingsForQuery(String rdfModel, String shapeModel) {
        Map<String, String> allPrefixes = new HashMap<>();
        allPrefixes.putAll(getPrefixes(rdfModel));
        allPrefixes.putAll(getPrefixes(shapeModel));

        StringBuilder prefixes = new StringBuilder();

        //"PREFIX sim-methods: <http://dbtune.org/sim-methods/resource/>\n" +

        for (Map.Entry<String, String> entry : allPrefixes.entrySet()) {
            //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            prefixes.append("PREFIX ").append(entry.getKey()).append(": <").append(entry.getValue()).append("> \n");
        }
        return prefixes.toString();
    }

    public static void loadTDBData(String modelUri, String fileAddress) {

        ProgressBarBuilder pbb = new ProgressBarBuilder()
                .setTaskName("Reading")
                .setUnit("MB", 1048576) // setting the progress bar to use MB as the unit
                .setConsumer(new DelegatingProgressBarConsumer(logger::info))
                .showSpeed().setStyle(ProgressBarStyle.ASCII);
        Dataset dataset = getTDBDataset();
        dataset.begin(ReadWrite.WRITE);
        Model modelGraph = dataset.getNamedModel(modelUri);

        try (Reader reader = new BufferedReader(new InputStreamReader(ProgressBar.wrap(new FileInputStream(fileAddress), pbb)))) {
            modelGraph.read(reader, "", "TTL");
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println(modelUri + " contains " + modelGraph.size() + " triples");
        modelGraph.commit();

        if (dataset.isInTransaction()) {
            dataset.commit();
            dataset.end();
            dataset.close();
        }

    }

    public static ResultSet runAQuery(String sparqlQuery, Dataset ds) {
        try (QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(sparqlQuery), ds)) {

            System.out.println(qExec.getQuery().getQueryPattern().toString());
            //ResultSet results = qExec.execSelect();
            //ResultSetFormatter.out(System.out, results);
            ResultSetRewindable results = ResultSetFactory.copyResults(qExec.execSelect());

            qExec.close();
            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ResultSet runAQuery(String sparqlQuery, Model model) {
        try (QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(sparqlQuery), model)) {
            //qExec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL);
            ResultSetRewindable results = ResultSetFactory.copyResults(qExec.execSelect());
            qExec.close();
            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ResultSet runAQuery(String sparqlQuery, String graphModelIRI) {
        if (RdfUtils.getTDBDataset().isInTransaction()) {
            RdfUtils.getTDBDataset().end();
            RdfUtils.getTDBDataset().close();
        }
        Dataset dataset = RdfUtils.getTDBDataset();
        dataset.begin(ReadWrite.READ);
        ResultSet returnThisResult = null;
        Model graphModel = dataset.getNamedModel(graphModelIRI);
        try (QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(sparqlQuery), graphModel)) {
            ARQ.getContext().set(ARQ.optimization, true);
            //qExec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL) ;
            //ResultSet results = qExec.execSelect();
            returnThisResult = ResultSetFactory.copyResults(qExec.execSelect());
            //ResultSetFormatter.out(System.out, returnThisResult);
            //return returnThisResult;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataset.end();
            dataset.close();
        }
        return returnThisResult;
    }

    public static void executeAlgebra(Op op) {
        Dataset dataset = RdfUtils.getTDBDataset();
        dataset.begin(ReadWrite.READ);

        Model graphModel = dataset.getNamedModel(Main.getRdfModelIRI());
        // ---- Execute expression
        QueryIterator qIter = Algebra.exec(op, graphModel.getGraph());

        for (; qIter.hasNext(); ) {
            Binding b = qIter.nextBinding();
            System.out.println(b);
        }
        qIter.close();

        dataset.end();
        dataset.close();
    }


    public static ResultSet runAQueryWithPlan(String sparqlQuery, String graphModelIRI) {
        long start = System.currentTimeMillis();
        if (RdfUtils.getTDBDataset().isInTransaction()) {
            RdfUtils.getTDBDataset().end();
            RdfUtils.getTDBDataset().close();
        }
        Dataset dataset = RdfUtils.getTDBDataset();
        dataset.begin(ReadWrite.READ);
        dataset.getContext().set(ARQ.optimization, false);
        Model graphModel = dataset.getNamedModel(graphModelIRI);

        // Get the standard one.
        //StageGenerator orig = (StageGenerator) ARQ.getContext().get(ARQ.stageGenerator);
        // Create a new one
        //StageGenerator myStageGenerator = new MyStageGenerator(orig);
        // Register it
        //StageBuilder.setGenerator(ARQ.getContext(), myStageGenerator);
        //ARQ.getContext().set(ARQ.stageGenerator, myStageGenerator);
        //StageBuilder.setGenerator(ARQ.getContext(), StageBuilder.standardGenerator());

        try (QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(sparqlQuery), graphModel)) {
            //ARQ.getContext().set(ARQ.optimization, false);

            System.out.println(qExec.getContext().get(ARQ.optimization).toString());
            Context context = qExec.getContext();
            qExec.getContext().set(ARQ.strictSPARQL, true);
            qExec.getContext().set(ARQ.optimization, false);
            //qExec.getContext().set(ARQ.stageGenerator, myStageGenerator);

            qExec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL);
            //ResultSet results = qExec.execSelect();

            ResultSetRewindable results = ResultSetFactory.copyResults(qExec.execSelect());
            ResultSetFormatter.out(System.out, results);
            qExec.close();
            return results;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataset.end();
            dataset.close();
            long duration = System.currentTimeMillis() - start;
            System.out.println(duration + " ms - query was running with Plan");
        }

        return null;
    }

    public static Statement getFromModel(String s, String p, String graphModelIRI) {
        //logger.info("FUNC: RdfUtils.getFromModel(s,p,graphModelIRI)");
        //logger.info("s:" + s + ", p:" + p + ", " + graphModelIRI);
        Dataset dataset = RdfUtils.getTDBDataset();
        dataset.begin(ReadWrite.READ);
        Model graphModel = dataset.getNamedModel(graphModelIRI);
        Statement returnedObject = graphModel.getProperty(new ResourceImpl(s), new PropertyImpl(p));
        if(returnedObject == null){
            //setting default to 0
            returnedObject = graphModel.createStatement(new ResourceImpl(s), new PropertyImpl(p), ResourceFactory.createTypedLiteral(0));
        }
        dataset.end();
        dataset.close();
        return returnedObject;
    }

    public static List<Resource> getSubjectsFromModel(String p, String o, String graphModelIRI) {
        //logger.info("FUNC: RdfUtils.getSubjectsFromModel(p,o,graphModelIRI)");
        //logger.info("p:" + p + ", o:" + o + ", " + graphModelIRI);
        Dataset dataset = RdfUtils.getTDBDataset();
        List<Resource> returnResourceList = null;
        try {
           if(!dataset.isInTransaction())
                dataset.begin(ReadWrite.READ);
            Model graphModel = dataset.getNamedModel(graphModelIRI);
            ResIterator subjectsIterator = graphModel.listSubjectsWithProperty(new PropertyImpl(p), new ResourceImpl(o));
            returnResourceList = subjectsIterator.toList();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataset.end();
            dataset.close();
        }
        return returnResourceList;
    }

    public static boolean runAskQuery(String sparqlQuery, Model model) {
        boolean result = false;
        try (QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(sparqlQuery), model)) {
            //qExec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL) ;
            result = qExec.execAsk();
            //ResultSetFormatter.out(System.out, result);
            //qExec.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean runAskQuery(String sparqlQuery, String graphModelIRI) {
        boolean result = false;
        if (RdfUtils.getTDBDataset().isInTransaction()) {
            RdfUtils.getTDBDataset().end();
            RdfUtils.getTDBDataset().close();
        }
        Dataset dataset = RdfUtils.getTDBDataset();
        dataset.begin(ReadWrite.READ);
        Model graphModel = dataset.getNamedModel(graphModelIRI);
        try (QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(sparqlQuery), graphModel)) {
            //qExec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.ALL) ;
            result = qExec.execAsk();
            //ResultSetFormatter.out(System.out, result);
            //qExec.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataset.end();
            dataset.close();
        }
        return result;
    }

    public static void runInsertQuery(String sparqlInsertQuery, String graphModelIRI) {
        try {
            //System.out.println(sparqlInsertQuery);
            Dataset dataset = RdfUtils.getTDBDataset();
            dataset.begin(ReadWrite.WRITE);
            Model graphModel = dataset.getNamedModel(graphModelIRI);

            UpdateAction.parseExecute(sparqlInsertQuery, graphModel);
            dataset.commit();
            dataset.end();
            dataset.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertPrefix(String modelIri, String prefix, String prefixValue) {
        Dataset dataset = RdfUtils.getTDBDataset();
        dataset.begin(ReadWrite.WRITE);
        Model graphModel = dataset.getNamedModel(modelIri);
        graphModel.setNsPrefix(prefix, prefixValue);
        dataset.commit();
        dataset.end();
        dataset.close();
    }

    public static void execSelectQueryAndPrint(String serviceURI, String query) {
        QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query);
        System.out.println(q.toString());
        ResultSet results = q.execSelect();
        System.out.println(results);


        ResultSetFormatter.out(System.out, results);

        /*while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            //RDFNode x = soln.get("x");
            System.out.println(soln.toString());
        }*/
    }

    public static boolean isNamedGraphAlreadyExists(String namedGraph) {
        boolean flag = false;
        Dataset ds = RdfUtils.getTDBDataset();
        ds.begin(ReadWrite.WRITE);
        if (ds.containsNamedModel(namedGraph)) {
            flag = true;
        }
        ds.commit();
        ds.end();
        ds.close();
        return flag;
    }

    public static boolean removeNamedGraph(String namedGraph) {
        System.out.println("removeNamedGraph is invoked ");
        boolean flag;
        Dataset ds = RdfUtils.getTDBDataset();
        ds.begin(ReadWrite.WRITE);
        try {
            ds.removeNamedModel(namedGraph);
            System.out.println("Named graph " + namedGraph + " - Size after : " + ds.getNamedModel(namedGraph).size());

            flag = true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            flag = false;
        }
        ds.commit();
        ds.end();
        ds.close();
        return flag;
    }

    public static void printTriplesOnConsole(String namedGraphIri) {
        System.out.println("printTriplesOnConsole");
        String getClassProperties = " SELECT * WHERE { GRAPH <" + namedGraphIri + "> { ?s ?p ?o .}} ";
        runAQuery(getClassProperties, namedGraphIri).forEachRemaining(triple -> {
            System.out.print(triple.get("s") + "\t" + triple.get("p") + "\t" + triple.get("o") + "\n");
        });
    }

    public static void writeGraphModelToFile(String graphModelIRI, String fileName) {
        try {
            Dataset dataset = RdfUtils.getTDBDataset();
            dataset.begin(ReadWrite.READ);
            Model graphModel = dataset.getNamedModel(graphModelIRI);
            graphModel.write(new FileOutputStream(fileName + ".ttl"), "TTL");
            graphModel.close();
            dataset.commit();
            dataset.end();
            dataset.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

}

