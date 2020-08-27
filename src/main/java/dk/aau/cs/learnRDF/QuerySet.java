package dk.aau.cs.learnRDF;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Provides access to a set of queries
 */

public class QuerySet {

    private final String name;
    private final Set<String> exclusion;
    private final File mainFolder;
    private final Map<String, String> queries;

    private int correctQueries = 0;
    private int failedQueries =0;


    /**
     * Constructor for a QuerySet, see params
     * @param name Display name for output
     * @param mainFolder folder from which files will be recursively read
     * @param filesToExclude list of absolute filenames to exclude from those attempted to be parsed
     */
    public QuerySet(String name, File mainFolder, String [] filesToExclude) {
        this.mainFolder = mainFolder;
        this.name = name;
        this.exclusion = new HashSet<>(Arrays.asList(filesToExclude));

        this.queries =  new HashMap<>();
        try {
            Files.walk(Paths.get(this.mainFolder.toURI()))
                    .filter(Files::exists)
                    .filter(Files::isRegularFile)
                    .filter(path -> !exclusion.contains(path.getFileName().getName(0).toString()))
                    .filter(path -> path.getFileName().getName(0).toString().endsWith(".rq") || path.getFileName().getName(0).toString().endsWith(".txt"))
                    .map(Path::toAbsolutePath)
                    .forEach(p -> {
                        try {
                            queries.put(p.toString(), FileUtils.readFileToString(p.toFile(), "utf8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Map<String, String> listQueries() {
        return this.queries;
    }

    public String getFolderPath(){
        return this.mainFolder.getAbsolutePath();
    }

    public String getName() {
        return name;
    }

    public void setCorrectQueries(int correctQueries) {
        this.correctQueries = correctQueries;
    }

    public void setFailedQueries(int failedQueries) {
        this.failedQueries = failedQueries;
    }

    public int getCorrectQueries() {
        return correctQueries;
    }

    public int getFailedQueries() {
        return failedQueries;
    }

    @Override
    public int hashCode() {
        return this.mainFolder.getAbsolutePath().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof QuerySet){
            return ((QuerySet)other).mainFolder.equals(this.mainFolder);
        }
        return false;
    }
}
