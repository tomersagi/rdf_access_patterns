package dk.aau.cs.shacl.utils;

import dk.aau.cs.shacl.Main;

import java.io.FileInputStream;
import java.io.IOException;


public class ConfigManager {

    public static String getProperty(String property) {
        java.util.Properties prop = new java.util.Properties();
        try {
            if (Main.configPath != null) {
                prop.load(new FileInputStream(Main.configPath));
            } else {
                System.out.println("Config Path is not specified in Main Arg");
            }
            return prop.getProperty(property);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
