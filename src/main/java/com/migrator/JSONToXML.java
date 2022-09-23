package com.migrator;

import java.io.IOException;
import java.io.File;

import org.json.JSONObject;
import org.json.XML;

import javax.annotation.*;
 
public class JSONToXML {

    //default
    public String section = "orders";

    public Integer mage_max_per_page = 5;

    public JSONToXML( @Nullable String section ) throws IOException{

        section = section != null ? section : this.section;

        this.section = section;
        
        //make sure the Config instance is loaded
        Config.getInstance();
    }

    public static void main(String...s) throws IOException{

        JSONToXML jsonToXML = new JSONToXML(null);

        String[] json_folder_arr = { Config.json_save_dir, jsonToXML.section };
        String json_folder = String.join(File.separator, json_folder_arr) + File.separator;


        String[] xml_folder_arr = { Config.xml_save_subdir, jsonToXML.section };
        String xml_folder = String.join(File.separator, xml_folder_arr) + File.separator;

        String json_filename = json_folder + "test1.json";
        
        String xml_filename = xml_folder + "test1.xml";

        System.out.println(xml_filename);

        System.out.println( "json_filename = " + json_filename );

        String json_data = ReadFromFile.contents( json_filename );

        JSONObject obj = new JSONObject(json_data);

        //test output
        saveXMLFile(xml_filename, obj);

        M2SSystem.println( "contents saved to: " + xml_filename );
    }

    public void prepareSFXMLFile(String timestamp, JSONObject jsonObject) throws IOException{

        //which folder for the XML file?
        String[] xml_folder_arr = { Config.xml_save_dir, this.section };
        String xml_folder = String.join(File.separator, xml_folder_arr) + File.separator;

        M2SSystem.println("xml_folder = "+ xml_folder);

        String section_capped = this.section.substring(0,1).toUpperCase();
        section_capped += section.substring(1).toLowerCase();
        
        //file name to save to
        String xml_filename = xml_folder + section_capped + "_" + Config.company_name.replace(" ", "-") + "-US_";
        xml_filename += timestamp.replace(" ", "_")
                            .replace(":", "-");

        xml_filename += ".xml";

        M2SSystem.println( "xml_filename = " + xml_filename );

        //Orders_site-id_yyyy-MM-dd_HH-mm-ss.xml
        saveXMLFile( xml_filename, jsonObject );
    }

    //write out the XML file, but before doing so, fix any closing tags with attributes
    public static void saveXMLFile( String xml_filename, JSONObject jsonObject ) throws IOException
    {

        //converting json to xml
        String xml_data = XML.toString(jsonObject);

        //hack to get rid of unwanted attributes in the closing XML tags. Not detrimental when not applicable.
        String pattern = "(?i)(<\\/([\\w-]+)(.*?)>)";
        String xml_data_updated = xml_data
                                    .replaceAll(pattern, "</" + "$2" + ">")
                                    .replaceAll("(?i)(<\\/?sorter>)", "");

        //System.out.println(xml_data_updated);

        M2SSystem.println( "xml_filename = " + xml_filename );

        WriteToFile.write(xml_filename, xml_data_updated, true);
    }
}
