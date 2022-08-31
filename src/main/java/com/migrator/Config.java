package com.migrator;

import java.io.IOException;

import javax.annotation.Nullable;

import java.io.File;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import io.github.cdimascio.dotenv.Dotenv;
//import io.github.cdimascio.dotenv.DotenvException;

public class Config {
    // Static variable reference of single_instance of type Config
    private static Config single_instance = null;

    // is it staging, development or production?
    public static String env = "";

    // for connecting to the Magento API
    public static String mage_auth_token;

    //Magento API endpoint base URL. NOTE: I use everything up to and including "/index.php/rest/V1/".
    public static String mage_api_base_url;

    /*
    how many records maximum per page? For the orders call, will be the number 
    of order records that could returned. Ditto for customers, etc.
    CAN be overriden with command line parameter 'max_per_page'.
    */
    public static int mage_max_per_page;

    //base folder of app
    public static String abs_path = "";

    //your company name
    public static String company_name = "";

    //what is the parent directory for all saved files? Probably child of above.
    public static String base_save_dir;


    //which child directory does the JSON from the Magento API get saved in?
    public static String json_save_subdir = "json_src";

    //what is the full directory for the JSON files? (finalized a bit later)
    public static String json_save_dir = "";


    //where do the XML files that will be sent to FTP be saved for now?
    public static String xml_save_subdir = "xml_dest";

    //what is the full directory for the XML files?
    public static String xml_save_dir = "";

    //what is the full directory for the CSV files?
    public static String csv_save_base_dir = "";

    //CONV-3. where do the CSV files that will be sent to FTP be saved for now?
    public static String csv_save_subdir = "csv_dest";

    //what is the full directory for the CSV files?
    public static String csv_save_dir = "";

    //used to help name the jobfile
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public static String log_dir = "";

    public static int http_duration_wait;

    public static String default_sort_order = "";

    //should the CSV headers render?
    public static Boolean csv_headers_render = false;

    // Constructor
    // Here we will be creating private constructor restricted to this class itself
    private Config(@Nullable String env_specific) throws IOException
    {

        //Where is the .env file?
        //String env_dir = System.getProperty("user.dir");
        String env_dir = new java.io.File("").getAbsolutePath();

        abs_path = env_dir;
        
        //load the .env file. This one is for universal settings. Overrides will occur with load from below, if available.
        Dotenv dotenv_core = Dotenv.configure()
            .directory( env_dir )
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
        
        //load the .env file specific to the environment (if applicable)
        Dotenv dotenv_specific = null;

        if( env_specific != null ){
            dotenv_specific = Dotenv.configure()
                .directory( env_dir )
                .filename(env_specific + ".env")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
        }else{
            //if no specific environment name is provided, use the default core settings.
            dotenv_specific = dotenv_core;
        }

        //Which environment?
        env = dotenv_specific.get("ENV", dotenv_core.get("ENV"));

        //Magento REST API security basic auth token. First, go to the specific dot env config, then the core.
        mage_auth_token = dotenv_specific.get("MAGE_AUTH_TOKEN", dotenv_core.get("MAGE_AUTH_TOKEN"));

        //Magento REST API base URL
        mage_api_base_url = dotenv_specific.get("MAGE_API_BASE_URL", dotenv_core.get("MAGE_API_BASE_URL"));

        //Your company name.
        company_name = dotenv_specific.get("YOUR_COMPANY_NAME", dotenv_core.get("YOUR_COMPANY_NAME"));


        //master directory for all saved files
        base_save_dir = dotenv_specific.get("BASE_SAVE_DIR", dotenv_core.get("BASE_SAVE_DIR", "saved_files"));

        base_save_dir += File.separator + env;

        //todays date. To be used for at least the CSV save directory.
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String job_stamp = "job_" + sdf.format(timestamp);

        /*
        Full folder string for the base JSON save folder. Will have subdirectory
        like 'orders' added on in actual saving/retrieving.
        */
        String[] json_folder_arr = { abs_path, base_save_dir, json_save_subdir };
        json_save_dir = String.join(File.separator, json_folder_arr) + File.separator;

        //full folder string for the base XML save folder.
        String[] xml_folder_arr = { abs_path, base_save_dir, xml_save_subdir };
        xml_save_dir = String.join(File.separator, xml_folder_arr) + File.separator;

        //full folder string for the base CSV save folder.
        String[] csv_folder_base_arr = { abs_path, base_save_dir, csv_save_subdir };
        csv_save_base_dir = String.join(File.separator, csv_folder_base_arr) + File.separator;

        //full folder string for the base CSV save folder.
        String[] csv_folder_arr = { csv_save_base_dir, job_stamp };
        csv_save_dir = String.join(File.separator, csv_folder_arr) + File.separator;

        //M2SSystem.println( "csv_save_dir = " + csv_save_dir );

        //how many results per page maximum?
        mage_max_per_page = Integer.parseInt( dotenv_specific.get("MAGE_MAX_PER_PAGE",
            dotenv_core.get("MAGE_MAX_PER_PAGE", "10")) );

        //how long should the API be waited for?
        http_duration_wait = Integer.parseInt( dotenv_specific.get("HTTP_DURATION_WAIT",
            dotenv_core.get("HTTP_DURATION_WAIT", "10")) );

        //Which directory is for logging?
        log_dir = dotenv_specific.get("LOG_DIR_CUSTOM",
            dotenv_core.get("LOG_DIR_CUSTOM", "logs"));

        //environment perspective
        //log_dir += File.separator + env;

        //what is the sort order for Magento API records?
        default_sort_order = dotenv_specific.get("SORT_ORDER",
            dotenv_core.get("SORT_ORDER", "ASC"));

        //Should the CSV headers render as the top row?
        String csv_headers_render_string = dotenv_specific.get("CSV_HEADERS_RENDER",
            dotenv_core.get("CSV_HEADERS_RENDER", "false"));

        csv_headers_render = Boolean.valueOf(csv_headers_render_string);
    }

    // Static method to create instance of Singleton class
    public static Config getInstance(@Nullable String env_name) throws IOException
    {
        if (single_instance == null){
            single_instance = new Config(env_name);
        }
 
        return single_instance;
    }

    //kind of a lazy default
    public static Config getInstance() throws IOException
    {
        return getInstance(null);
    }
}
