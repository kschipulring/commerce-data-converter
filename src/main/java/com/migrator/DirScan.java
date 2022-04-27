package com.migrator;

import java.io.File;
import java.io.FilenameFilter;

public class DirScan
{

    public static void main(String[] args){
        //testing only
        System.out.println( fileStartsWithFullName("orders/orders_") );
    }

    /*
    find out if a file starting with a certain pattern already exists.
    If so, return the full path name of the first match.
    */
    public static String fileStartsWithFullName(String fname_starts_with)
    {
        
        //start with the root folder of this Java app.
        String abs_path = new java.io.File("").getAbsolutePath();
        
        File root = new File(abs_path + "/" + Config.base_save_dir + "/");
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
