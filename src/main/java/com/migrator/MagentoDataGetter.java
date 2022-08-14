package com.migrator;

import java.io.File;
import java.io.IOException;
//import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
//import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

abstract public class MagentoDataGetter {

    //which page from the Magento API call is currently being scanned?
    protected int current_page = 1;

    //the contents from either the Magento API or the 'saved_files' local directory
    protected String raw_response = null;

    //save the raw JSON output from the Magento API?
    public boolean save_file = false;

    //instead load the raw JSON output from a local save file (when available)
    public boolean load_file_if_exist = false;

    //which page of results should the operation start on?
    public Integer page_start = 1;

    //should be the same name as a Magento API section like "orders" or "customer/search"
    public String api_section = "orders";

    //which field should this be sorted by? Should be an existing field in the top level of the JSON return
    public String sort_order_field = "increment_id";

    //defaults, overriden with settings from the Config class via dotenv
    protected String mage_api_base_url = null;
    protected String mage_auth_token = null;
    public Integer mage_max_per_page = 10;

    //the sort order of rows retrieved from Magento API endpoints. Default.
    protected String sort_order = "ASC";

    //additional parameters sent to the Magento API call.
    protected String endpoint_extras = null;

    protected final HttpClient httpClient;
    
    public MagentoDataGetter(Integer mage_max_per_page ) throws IOException
    {

        //Config is singleton
        Config.getInstance();

        this.mage_api_base_url = Config.mage_api_base_url;
        this.mage_auth_token = Config.mage_auth_token;

        //forcibly override the maximum per page over the DOTEnv value, if above parameter is fed.
        mage_max_per_page = mage_max_per_page != null ? mage_max_per_page : Config.mage_max_per_page;
        this.mage_max_per_page = mage_max_per_page;

        System.out.println( "mage_max_per_page" );
        System.out.println( mage_max_per_page );

        httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout( Duration.ofSeconds(Config.http_duration_wait) )
            .build();
    }
    
    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    protected void saveJsonFile(String file_contents) throws IOException
    {

        if( !isJSONValid(file_contents) ){
            String msg = "Magento JSON is not Valid for:\n" + "-section: " + this.api_section + "\n";
            msg += "-current_page: " + this.current_page;
            msg += "-mage_max_per_page: " + this.mage_max_per_page;

            M2SLogger.warning(msg);
            M2SLogger.warning(file_contents);

            return;
        }

        //the file shall be partially named on when the first record was created. But the timestamp must first be extracted.
        JSONObject jsonObject = new JSONObject( file_contents );

        //the items section is an array of the meaningful records
        JSONArray jsonArray = jsonObject.optJSONArray("items");

        if( jsonArray == null ){
            String msg = "Magento JSON is NULL for:\n" + "-section: " + this.api_section + "\n";
            msg += "-current_page: " + this.current_page;
            msg += "-mage_max_per_page: " + this.mage_max_per_page;

            M2SLogger.warning(msg);
            M2SLogger.warning(file_contents);

            return;
        }

        //we want the timestamp from the first order from this batch
        String start_ts = jsonArray.getJSONObject(0).getString("created_at")
                            .replace(' ', '_')
                            .replace(':', '-');

        //immediate subdirectory of 'saved_files'
        String parent_dir = Config.json_save_subdir + File.separator + this.api_section + File.separator;

        //file name to save to
        String json_filename = parent_dir + this.api_section + "_pageSize-";
        json_filename += this.mage_max_per_page + "_currentPage-";
        json_filename += this.current_page + "_" + start_ts + ".json";

        //save the json file from the Magento API call contents
        WriteToFile.write(json_filename, file_contents);
    }

    //just the raw request carried out. For actual parameters, method 'getJSONAPIContent' is used
    public String getRequest(String endpoint) throws IOException, InterruptedException
    {

        //which URL provides the data?
        String request_url = mage_api_base_url + endpoint;

        System.out.println( "request_url = " + request_url );
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(request_url))
                .setHeader("User-Agent", "Java 11 Magento API order reader with HttpClient")
                .setHeader("Authorization", "Bearer " + this.mage_auth_token)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print response headers
        /*HttpHeaders headers = response.headers();
        headers.map().forEach((k, v) -> System.out.println(k + ":" + v));*/

        // print status code
        //System.out.println(response.statusCode());

        /*
        save the JSON file in the saved_files directory if the class setting is 
        on and there is not yet already a comparably named file in there.
        */
        if( this.save_file ){
            this.saveJsonFile( response.body() );
        }
       
        return response.body();
    }

    //builds the request GET URL, then uses it to call the above method
    public String getJSONAPIContent(String api_section, String sort_order_field)
    throws IOException, InterruptedException
    {
        //Magento API section. Could be for any section, as well as orders.
        String endpoint = api_section + "?searchCriteria[sortOrders][0][field]=";
        endpoint += sort_order_field;
        endpoint += "&searchCriteria[pageSize]=" + this.mage_max_per_page;
        endpoint += "&searchCriteria[currentPage]=" + this.current_page;
        endpoint += "&searchCriteria[sortOrders][0][direction]=" + this.sort_order;

        System.out.println( "this.endpoint_extras = " + this.endpoint_extras );

        //likely additional endpoint portions from child class implementations.
        endpoint += this.endpoint_extras;

        return this.getRequest(endpoint);
    }

    //could use either the local file JSON or the API endpoint in method above.
    public String getJSONstring(String... args) throws IOException, InterruptedException
    {

        //if no specified current page, start with orders, defined above
        String api_section = args.length > 0 ? args[0] : this.api_section;

        //needed later.
        this.api_section = api_section;

        //default field to sort by...
        String sort_order_field = args.length > 1 ? args[1] : this.sort_order_field;


        //file name to check against to see if already existing
        String[] json_folder_arr = { Config.json_save_subdir, this.api_section };
        String json_folder = String.join(File.separator, json_folder_arr) + File.separator;

        String base_json_filename = this.api_section + "_pageSize-";
        base_json_filename += this.mage_max_per_page + "_currentPage-" + this.current_page + "_";

        System.out.println( "this.current_page = " + this.current_page  );

        String json_filename = json_folder + base_json_filename;

        //is the downloaded JSON file in the saved directory?
        String existing_name = DirScan.fileStartsWithFullName(json_filename);

        //must be reset for each iteration of the batch API JSON call and file save
        this.raw_response = null;

        //if so and if the class property 'load_file_if_exist' is true
        if( this.load_file_if_exist && existing_name != null ){
            this.raw_response = ReadFromFile.contents(existing_name);
        }
        
        //if there is no pre-existing file or if loading JSON from file is not desired.
        if( this.raw_response == null ){

            /*
            if not being loaded from a JSON file in the saved file directory, 
            load it directly from Magento Orders API.
            */
            this.raw_response = this.getJSONAPIContent(api_section, sort_order_field);
        }

        return this.raw_response;
    }
}
