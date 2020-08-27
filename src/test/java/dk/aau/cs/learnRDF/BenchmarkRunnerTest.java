package dk.aau.cs.learnRDF;

import com.opencsv.CSVReader;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import static org.junit.Assert.*;

public class BenchmarkRunnerTest {

    @Test
    public void evaluateQuerysets() {
        BenchmarkRunner br = new BenchmarkRunner(false, 0);
        List<QuerySet> toEvaluate = new ArrayList<>();
        QuerySet LDBC = new QuerySet("LDBC", new File("C:\\Users\\OT48ZK\\Dropbox\\Research\\projects\\LearnedIndexesFor_KG\\system\\queries\\LDBC"),new String[] {"queries.txt"});
        toEvaluate.add(LDBC);
        URI url = null;
        try {
            url = Objects.requireNonNull(BenchmarkRunner.class.getClassLoader().getResource("F5.txt")).toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        assert url != null;
        File resourceFolder = new File(url).getParentFile();
        try {
            File out = new File(resourceFolder, "br_test_out.csv");
            if (!out.exists() || out.delete()) //check if exists and delete if does
                assertTrue("Failed to create output file", out.createNewFile()); //create outfile

            br.evaluateQuerysets(toEvaluate,out.toPath());
            try (CSVReader reader = new CSVReader(new FileReader(out))) {
                String[] header = reader.readNext();
                assertEquals("Unexpected header length", AccessPattern.values().length+2, header.length);
                String[] ldbcEval = reader.readNext();
                assertNotNull("No result on LDBC eval",ldbcEval);
            } catch(Exception e) {
                e.printStackTrace();
            }
        } catch(IOException e){
            e.printStackTrace();
        }


    }
}