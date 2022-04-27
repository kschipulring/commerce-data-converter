package com.migrator;

import java.io.IOException;

import io.github.cdimascio.dotenv.Dotenv;
//import io.github.cdimascio.dotenv.DotenvException;

public class Config {
    // Static variable reference of single_instance of type Config
    private static Config single_instance = null;

    // for connecting to the Magento API
    public static String mage_auth_token;

    //Magento API endpoint base URL. NOTE: I use everything up to and including "/index.php/rest/V1/".
    public static String mage_api_base_url;

    /*
    how many records per page? For the orders call, will be the number of orders
    that could return. Ditto for customers, etc.
    */
    public static int mage_max_per_page;

    //what is the parent directory for all saved files?
    public static String base_save_dir;

    //which child directory does the JSON from the Magento API get saved in?
    public static String json_save_subdir = "json_src";

    //where do the XML files that will be sent to FTP be saved for now?
    public static String xml_save_subdir = "xml_dest";

    public static int http_duration_wait;

    // Constructor
    // Here we will be creating private constructor restricted to this class itself
    private Config() throws IOException
    {

        //Where is the .env file?
        //String env_dir = System.getProperty("user.dir");
        String env_dir = new java.io.File("").getAbsolutePath();
        
        //load the .env file
        Dotenv dotenv = Dotenv.configure()
            .directory( env_dir )
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

        //Magento REST API security basic auth token
        mage_auth_token = dotenv.get("MAGE_AUTH_TOKEN");

        //Magento REST API base URL
        mage_api_base_url = dotenv.get("MAGE_API_BASE_URL");

        base_save_dir = dotenv.get("BASE_SAVE_DIR", "saved_files");

        //how many results per page maximum?
        mage_max_per_page = Integer.parseInt( dotenv.get("MAGE_MAX_PER_PAGE", "10") );

        //how long should the API be waited for?
        http_duration_wait = Integer.parseInt( dotenv.get("HTTP_DURATION_WAIT", "10") );
    }

    // Static method to create instance of Singleton class
    public static Config getInstance() throws IOException
    {
        if (single_instance == null)
            single_instance = new Config();
 
        return single_instance;
    }
}
