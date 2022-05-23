package com.migrator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class DirScan
{

    public static void main(String[] args) throws IOException
    {
        //testing only
        System.out.println( fileStartsWithFullName("orders/orders_") );
    }

    /*
    find out if a file starting with a certain pattern already exists.
    If so, return the full path name of the first match.
    */
    public static String fileStartsWithFullName(String fname_starts_with) throws IOException
    {

        Config.getInstance();
        
        //start with the save folder of this Java app.
        String[] file_root_arr = {Config.abs_path, Config.base_save_dir};
        String file_root_str = String.join(File.separator, file_root_arr) + File.separator;
        
        File root = new File(file_root_str);
        FilenameFilter beginswithm = new FilenameFilter()
        {
            public boolean accept(File directory, String filename) {
                return filename.startsWith(fname_starts_with);
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
