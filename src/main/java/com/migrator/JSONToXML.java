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

    public String abs_path = "";

    //your company name
    public static String company_name = "";

    public JSONToXML( @Nullable String section ) throws IOException{

        section = section != null ? section : this.section;

        this.section = section;
        
        //make sure the Config instance is loaded
        Config.getInstance();

        //which is the base folder of this app?
        this.abs_path = new java.io.File("").getAbsolutePath();

        //this.section = section_override != null ? section_override : this.section;

        

        System.out.println( "please allow me to introduce myself with = " + this.section );
    }

    public static void main(String...s) throws IOException{

        JSONToXML jsonToXML = new JSONToXML(null);


        String json_folder = jsonToXML.abs_path + "/" + Config.base_save_dir + "/" + Config.json_save_subdir + "/orders/";
        String xml_folder = Config.xml_save_subdir + "/orders/";

        String json_filename = json_folder + "tester.json";
        
        String xml_filename = xml_folder + "tester.xml";

        //System.out.println(xml_filename);

        ///System.out.println( "json_filename = " + json_filename );

        String json_data = ReadFromFile.contents( json_filename );

        JSONObject obj = new JSONObject(json_data);

        

        //mage2SFObj(obj);

        /*
        JSONObject sf_obj = new JSONObject( mage2SFObj(obj) );
        
        System.out.println( sf_obj );
        
        //converting json to xml
        String xml_data = XML.toString(sf_obj);
        System.out.println(xml_data);*/

        /*WriteToFile.write(xml_filename, xml_data);*/
    }

    public static void saveXMLFile( String xml_filename, JSONObject jsonObject ) throws IOException
    {

        //converting json to xml
        String xml_data = XML.toString(jsonObject);
        System.out.println(xml_data);

        //WriteToFile.write(xml_filename, xml_data);
    }
}