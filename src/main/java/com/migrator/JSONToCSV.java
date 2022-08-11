package com.migrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.File;

import javax.annotation.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class JSONToCSV {

    //default API section to convert from Magento to Deck Commerce.
    public String section = "orders";

    /* used for when making a seperate CSV file from JSON data which is a 
    sub-section of a certain section JSON. e.g., if one wants a items file that 
    comes from orders. */
    public String sub_section = null;

    public Integer mage_max_per_page = 5;

    public JSONToCSV(@Nullable String section) throws IOException{
        section = section != null ? section : this.section;

        this.section = section;
        
        //make sure the Config instance is loaded
        Config.getInstance();
    }

    // used for things like the product items of orders; from the same data source as orders
    public JSONToCSV(@Nullable String section, @Nullable String sub_section) throws IOException{
        this(section);

        this.sub_section = sub_section;
    }

    /* get the folder name for where the JSON source data files are stored, with
    the section variable providing the end subdirectory */
    public String getJsonFolder(String section){
        String[] json_folder_arr = { Config.json_save_dir, section };
        String json_folder = String.join(File.separator, json_folder_arr) + File.separator;

        return json_folder;
    }

    // DESCRIPTION: converts Magento date string into Deck Commerce date string
    public String mage2DeckDateTime(String mage_date_str) throws ParseException{

        Date mage_date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(mage_date_str);
        DateFormat deckDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm");
  
        return deckDateFormat.format(mage_date);
    }

    /*
     * DESCRIPTION: creates and returns the final data format for the CSV before
     * being saved to file.
    */
    public List<List<String>> deckItems2CSVRows(
        List<Map<DeckOrderHeaders, String>> mdmo,
        CSVHeaderInterface[] enum_vals
    ){

        //the return value. will have all the CSV rows plus the headers as the first row.
        List<List<String>> csv_rows = new ArrayList<List<String>>();

        //the first row are the CSV headers. They come from an enum
        List<String> csv_row_headers = new ArrayList<String>();

        for (CSVHeaderInterface e : enum_vals) {
            csv_row_headers.add( e.value() );
        }

        M2SLogger.info("My first Log Message");

        //make it the first row
        csv_rows.add( csv_row_headers );

        for(int i = 0; i < mdmo.size(); i++){
            List<String> csv_row = new ArrayList<String>();

            for (CSVHeaderInterface d : enum_vals ) {
                csv_row.add( mdmo.get(i).get(d) );
            }

            csv_rows.add( csv_row );
        }

        return csv_rows;
    }

    /**
     * DESCRIPTION: finalizes the file name for the Deck Commerce CSV file. Then
     * saves the data to it.
     * 
    */
    public void prepareDeckCSVFile(String timestamp, List<List<String>> csv_rows) throws IOException
    {

        Config.getInstance();

        String section = this.section;

        //which folder for the CSV file?
        String[] csv_folder_arr = { Config.csv_save_dir, section };
        String csv_folder = String.join(File.separator, csv_folder_arr) + File.separator;

        // capitalize first letter for the section
        String Section = section.substring(0, 1).toUpperCase() + section.substring(1);

        //ditto for the sub-section, if applicable
        String SubSection = this.sub_section == null ? "" :
        this.sub_section.substring(0, 1).toUpperCase() + this.sub_section.substring(1);

        String csv_filename = csv_folder + "Legacy" + Section + SubSection + "_";

        csv_filename += timestamp.replace(" ", "_")
                            .replace(":", "-");

        csv_filename += ".csv";

        //LegacyOrder_{TIMESTAMP}.csv
        this.saveCSVFile(csv_filename, csv_rows);
    }

    public void saveCSVFile( String csv_filename, List<List<String>> csv_rows ) throws IOException
    {

        //Java only plays nice when the desired directory for the file already exists
        File myFile = new File(csv_filename);
        if(!myFile.getParentFile().exists()) {
            myFile.getParentFile().mkdirs();
        }
        
        try (
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(csv_filename));

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT );
        ) {

            //write all the CSV rows
            for(int i = 0; i < csv_rows.size(); i++){
                csvPrinter.printRecord( csv_rows.get(i) );
            }

            csvPrinter.flush();

            M2SLogger.info( "successfully wrote CSV data to " + csv_filename );
        } catch (IOException e) {
            //log the error
            M2SLogger.severe(e);
        }
    }
}
