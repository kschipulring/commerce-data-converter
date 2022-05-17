package com.migrator;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
 
public class JSONToXML {
    public static void main(String...s) throws IOException{

        //make sure the Config instance is loaded
        Config.getInstance();

        //which is the base folder of this app?
        String abs_path = new java.io.File("").getAbsolutePath();


        String json_folder = abs_path + "/" + Config.base_save_dir + "/" + Config.json_save_subdir + "/orders/";
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