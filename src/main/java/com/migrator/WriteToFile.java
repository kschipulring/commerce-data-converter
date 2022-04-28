package com.migrator;

import java.io.File;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

public class WriteToFile {
    public static void write(String f_name, String f_contents) throws IOException
    {

        //the filename must not be empty or null.
        if( f_name == null || f_name.isEmpty() || f_contents == null || f_contents.isEmpty() )
        {
            return;
        }

        //make sure the Config instance is loaded
        Config.getInstance();

        //which is the base folder of this app?
        String abs_path = new java.io.File("").getAbsolutePath();

        String directory = abs_path + "/" + Config.base_save_dir + "/";
        String file_write_name = directory + "/" + f_name;

        try {

            //Java only plays nice when the desired directory for the file already exists
            File myFile = new File(file_write_name);
            if(!myFile.getParentFile().exists()) {
                myFile.getParentFile().mkdirs();
            }
            
            FileWriter myWriter = new FileWriter(file_write_name);
            myWriter.write(f_contents);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
