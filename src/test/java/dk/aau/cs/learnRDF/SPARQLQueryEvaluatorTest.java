package dk.aau.cs.learnRDF;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static dk.aau.cs.learnRDF.AccessPattern.*;
import static org.junit.Assert.*;

public class SPARQLQueryEvaluatorTest {

    @Test
    public void evaluateQuery() throws IOException {
        String[] filenames = new String[]{
                "po_pivot_ex.txt",
                "query_16", "ps_pivot_ex.txt", "query_0685_2774.rq","query_10.rq","query_11.rq","WebQTest-1458.rq"
                ,"query_42.rq", "query_2.rq","F5.txt", "query_32.rq", "query_37.rq", "query_01", "ask.sparql"
                };
        AccessPattern[][] patterns = new AccessPattern[][]{
                {
                    CONSTANTS_P, PIVOT_S, PIVOT_O, PIVOT_OP, RETURN_EXISTS, RETURN_AGG, TRAVERSAL_out1
                },
                { CONSTANTS_PO,
                        CONSTANTS_P,
                        TRAVERSAL_in1,
                        TRAVERSAL_out1,
                        TRAVERSAL_outK,
                        TRAVERSAL_inK,
                        RETURN_EXISTS,
                        RETURN_DISTINCT,
                        RETURN_AGG,
                        PIVOT_OS,
                        PIVOT_S

                },
                {
                        CONSTANTS_PO, PIVOT_S, PIVOT_SP, RETURN_DISTINCT, TRAVERSAL_in1, RETURN_EXISTS, TRAVERSAL_out1
                },
                {RETURN_DISTINCT, CONSTANTS_P, CONSTANTS_PO, PIVOT_S, TRAVERSAL_inK,TRAVERSAL_out1, TRAVERSAL_in1, PIVOT_OS
                        , RETURN_EXISTS},
                {CONSTANTS_S,CONSTANTS_O, RANGE_S,
                        TRAVERSAL_out1,TRAVERSAL_in1,
                        RETURN_DISTINCT},
                {CONSTANTS_PO,
                CONSTANTS_P,
                RANGE_S,
                TRAVERSAL_in1,TRAVERSAL_out1,
                RETURN_DISTINCT,RETURN_EXISTS,
                PIVOT_S},
                {RETURN_EXISTS,RETURN_DISTINCT,RANGE_S, RANGE_O,TRAVERSAL_outK
                        , TRAVERSAL_out1, CONSTANTS_P, CONSTANTS_SP, PIVOT_OS},
                {
                CONSTANTS_P, CONSTANTS_PO,
                TRAVERSAL_outK, TRAVERSAL_in1, TRAVERSAL_out1, TRAVERSAL_inP_STAR, RETURN_EXISTS,//TRAVERSAL_outP_STAR reversed - awaiting matteo's approval
                RETURN_DISTINCT, PIVOT_S, PIVOT_OS
                },
                { CONSTANTS_P, CONSTANTS_PO,
                        RANGE_C, RANGE_S,
                        TRAVERSAL_in1,TRAVERSAL_out1,
                        TRAVERSAL_inP_STAR,
                        RETURN_VAL,RETURN_EXISTS,
                        PIVOT_S
                },
                { CONSTANTS_P,
                  CONSTANTS_SP,
                  TRAVERSAL_out1,
                  RETURN_VAL,
                  RETURN_EXISTS,
                  PIVOT_S,
                  PIVOT_OS
                },
                {
                CONSTANTS_PO,
                CONSTANTS_P,RANGE_S,
                TRAVERSAL_in1, TRAVERSAL_out1,
                RETURN_AGG, RETURN_EXISTS,
                PIVOT_OS,
                PIVOT_S
    },{
                CONSTANTS_P, CONSTANTS_PO,
                TRAVERSAL_inK, TRAVERSAL_out1,RETURN_EXISTS,
                RETURN_AGG,
                PIVOT_S
        }, {CONSTANTS_P,
                CONSTANTS_PO,
                TRAVERSAL_out1,
                TRAVERSAL_in1,
                TRAVERSAL_outK,
                RETURN_VAL,
                RETURN_EXISTS,
                PIVOT_S,
                PIVOT_OS},
                {
                        CONSTANTS_P,
                        CONSTANTS_PO,
                        TRAVERSAL_in1,TRAVERSAL_out1,TRAVERSAL_outK,
                        RETURN_EXISTS,
                        PIVOT_S,
                        PIVOT_OS,
                }

        };

        ClassLoader classLoader = this.getClass().getClassLoader();
        int i = 0;
        for (String filename : filenames) {
            //Get query for evaluation
            File file = new File(Objects.requireNonNull(classLoader.getResource(filename)).getFile());
            assertTrue(file.exists());
            String testQ = Files.readString(Paths.get(file.getPath()), StandardCharsets.UTF_8);

            //Evaluate query
            SPARQLQueryEvaluator rsq = new SPARQLQueryEvaluator(testQ);
            boolean[] res = rsq.evaluateQuery();
            assertNotNull(res);
            assertEquals(AccessPattern.values().length, res.length);

            //Build expected access patterns array
            boolean[] expected = new boolean[AccessPattern.values().length];
            Arrays.fill(expected, false);
            for (AccessPattern p : patterns[i])
                expected[p.ordinal()] = true;

            //Check that access patterns returned match expected
            for (AccessPattern ap : AccessPattern.values())
                if (expected[ap.ordinal()])
                    assertTrue("In ".concat(filename).concat(", expected ").concat(ap.toString()), res[ap.ordinal()]);
                else
                    assertFalse("In ".concat(filename).concat(", unexpected ").concat(ap.toString()), res[ap.ordinal()]);
            i++;
        }
    }
 }