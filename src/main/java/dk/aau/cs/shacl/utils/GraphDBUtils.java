//package dk.aau.cs.shacl.utils;
//
//import org.apache.jena.query.ResultSet;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.eclipse.rdf4j.model.IRI;
//import org.eclipse.rdf4j.query.*;
//import org.eclipse.rdf4j.repository.Repository;
//import org.eclipse.rdf4j.repository.RepositoryConnection;
//
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Set;
//
//public class GraphDBUtils {
//    private static Logger logger = LogManager.getLogger(GraphDBUtils.class);
//    private KBManagement kbManager = new KBManagement();
//    private Repository repository = kbManager.initGraphDBRepository();
//    private RepositoryConnection repositoryConnection = repository.getConnection();
//
//    public void updateQueryExecutor(String query) {
//        try {
//            System.out.println(query);
//            repositoryConnection.begin();
//            Update updateOperation = repositoryConnection.prepareUpdate(QueryLanguage.SPARQL, query);
//            updateOperation.execute();
//            repositoryConnection.commit();
//            //repositoryConnection.close();
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//            if (repositoryConnection.isActive())
//                repositoryConnection.rollback();
//        }
//    }
//
//    public List<BindingSet> runSelectQuery(String query){
//        List<BindingSet> result = new ArrayList<>();
//        try {
//            TupleQuery tupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, query);
//            TupleQueryResult classesQueryResult = tupleQuery.evaluate();
//            classesQueryResult.forEach(result::add);
//            classesQueryResult.close();
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//            if (repositoryConnection.isActive())
//                repositoryConnection.rollback();
//        }
//        return result;
//    }
//
//    public void selectQueryExecutor(String query) {
//        try {
//            repositoryConnection.begin();
//
//            String classesQuery = "SELECT DISTINCT ?className  WHERE { ?s rdf:type ?className. }";
//
//
//            List<BindingSet> classesQueryResultBindings = runSelectQuery(classesQuery);
//            List<String> queries = new ArrayList<>();
//
//
//            classesQueryResultBindings.forEach(triple -> {
//                String countPropClassQuery = "SELECT ?property (Count(*) AS ?count) WHERE " + "{ [] a <" + triple.getValue("className").stringValue() + "> ; ?property [] .} GROUP BY ?property ";
//                //queries.add(countPropClassQuery);
//
//                List<BindingSet> countPropClassQueryResultBindings = runSelectQuery(countPropClassQuery);
//
//                countPropClassQueryResultBindings.forEach(t -> {
//                    System.out.println(t);
//                });
//            });
//
////            queries.forEach(countQuery -> {
////                List<BindingSet> countPropClassQueryResultBindings = runSelectQuery(countQuery);
////
////                countPropClassQueryResultBindings.forEach(t -> {
////
////                });
////            });
//
//
//
////                TupleQuery countPropClassTupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, countPropClassQuery);
////                TupleQueryResult countPropClassTupleQueryResult = countPropClassTupleQuery.evaluate();
////
////
////                countPropClassTupleQueryResult.forEach(t -> {
////                    //countPropClassQueryResultBindings.add(t);
////                    String maxCountQuery = "SELECT (MAX(?Y) AS ?maxCount) WHERE {\n" +
////                            "SELECT (COUNT( DISTINCT ?x) AS ?Y ) \n" +
////                            "WHERE {\n" +
////                            "    [] a <" + triple.getValue("className").stringValue() + "> ; <" + t.getValue("property").toString() + "> ?x .\n" +
////                            "}\n" +
////                            "    GROUP BY ?x \n" +
////                            "}";
////
////                    System.out.println(maxCountQuery);
////                });
////                countPropClassTupleQueryResult.close();
//
//
//
//
////            System.out.println(query);
////            repositoryConnection.begin();
////
////
////            TupleQuery tupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, query);
////
////            TupleQueryResult res = tupleQuery.evaluate();
////
////            while (res.hasNext()) {
////                System.out.println(res.next());
////                //String pred = res.next().getValue("p").toString();
////            }
//
//            //repositoryConnection.commit();
//            //repositoryConnection.close();
//        }
//        catch (Exception e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//            if (repositoryConnection.isActive())
//                repositoryConnection.rollback();
//        }
//    }
//
//}
