package com.migrator;

import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

public class WriteToFile {
    public static void write(String f_name, String f_contents) {

        //which is the base folder of this app?
        String abs_path = new java.io.File("").getAbsolutePath();

        try {
            String write_name = abs_path + "/saved_files/" + f_name;
            
            FileWriter myWriter = new FileWriter(write_name);
            myWriter.write(f_contents);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
