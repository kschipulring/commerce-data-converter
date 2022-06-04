package com.migrator;

import java.io.IOException;
import java.io.File;

import javax.annotation.*;

public class JSONToCSV {

    //default
    public String section = "orders";

    public Integer mage_max_per_page = 5;

    public JSONToCSV( @Nullable String section ) throws IOException{

        section = section != null ? section : this.section;

        this.section = section;
        
        //make sure the Config instance is loaded
        Config.getInstance();
    }

    public void prepareDeckCSVFile() throws IOException{

        //which folder for the XML file?
        String[] csv_folder_arr = { Config.csv_save_subdir, this.section };
        String csv_folder = String.join(File.separator, csv_folder_arr) + File.separator;
    }
}
