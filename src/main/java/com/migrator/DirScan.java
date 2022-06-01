package com.migrator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class DirScan
{

    public static void main(String[] args) throws IOException
    {
        
        //testing only
        String[] file_folder_arr = {"json_src", "orders"};
        String file_root_str = String.join(File.separator, file_folder_arr) + File.separator;
        
        System.out.println( fileStartsWithFullName(file_root_str + "orders_") );
    }

    /*
    find out if a file starting with a certain pattern already exists.
    If so, return the full path name of the first match.
    */
    public static String fileStartsWithFullName(String fname_starts_with) throws IOException
    {

        Config.getInstance();

        //find out if fname_starts_with has a parent directory...
        File file = new File( fname_starts_with );
        String fname_parent = file.getParent();
        String fname = file.getName();
        
        //start with the save folder of this Java app.
        String[] file_root_arr = {Config.abs_path, Config.base_save_dir, fname_parent};
        String file_root_str = String.join(File.separator, file_root_arr) + File.separator;
        
        //loop through the file names in the supposed file name directory...
        File root = new File(file_root_str);
        FilenameFilter beginswithm = new FilenameFilter()
        {
            public boolean accept(File directory, String filename) {
                return filename.startsWith(fname);
            }
        };

        File[] files = root.listFiles(beginswithm);
        for (File f: files)
        {

            //we only care about the first match
            return f.toString();
        }

        return null;
    }
}
