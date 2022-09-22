package com.migrator;

import java.io.File;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

public class WriteToFile {
    public static void write(String f_name, String f_contents, Boolean is_absolute_f_name) throws IOException
    {

        //the filename must not be empty or null.
        if( f_name == null || f_name.isEmpty() || f_contents == null || f_contents.isEmpty() )
        {
            
            String error_msg = "Can't write to filename of: " + f_name;
            error_msg += "\n with f_contents of: " + f_contents;
            
            M2SLogger.severe(error_msg);
            
            return;
        }

        //make sure the Config instance is loaded
        Config.getInstance();

        String file_write_name = "";

        //which is the base folder of this app?
        if(is_absolute_f_name){
            file_write_name = f_name;
        }else{
            String abs_path = new java.io.File("").getAbsolutePath();

            String directory = abs_path + File.separator + Config.base_save_dir;
    
            file_write_name = directory + File.separator + f_name;
        }

        try {

            //Java only plays nice when the desired directory for the file already exists
            File myFile = new File(file_write_name);
            if(!myFile.getParentFile().exists()) {
                myFile.getParentFile().mkdirs();
            }
            
            FileWriter myWriter = new FileWriter(file_write_name);
            myWriter.write(f_contents);
            myWriter.close();

            String msg = "Successfully wrote to the file: \n" + file_write_name;

            M2SLogger.info(msg);
        } catch (IOException e) {
            String msg = "An error occurred saving to file: \n" + file_write_name;

            M2SLogger.severe(msg);

            e.printStackTrace();

            M2SLogger.severe(e.getMessage());
        }
    }


    public static void write(String f_name, String f_contents) throws IOException
    {
        write(f_name, f_contents, false);
    }
}
