package com.migrator;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command Line entry point
 *
 */
public class App 
{

    /*
    gets Magento orders from the specified environment via REST, then saves them
     as .json files in specified destination.
    */
    public static List<JSONArray> getMageOrders(
        @Nullable HashMap<String, String> props,
        @Nullable Boolean load_file_if_exist
    ) throws IOException, InterruptedException
    {
        
        //the return value. will have all the CSV rows plus the headers as the first row.
        List<JSONArray> mage_order_groups = new ArrayList<JSONArray>();
        
        // default value for the command line props, which is empty HashMap.
        props = props != null ? props : new HashMap<String, String>();

        load_file_if_exist = load_file_if_exist != null ? load_file_if_exist : true;

        /*
        which page of results? Best off starting with '1' as it is the same as zero.
        But starting with zero may cause repeat results problems in iteration calls.
        */
        Integer page_start = props.containsKey("page_start") ? 
                            Integer.parseInt( props.get("page_start") ) : 1;

        // optional page end for multiple json saves
        Integer page_end = props.containsKey("page_end") ? 
                            Integer.parseInt( props.get("page_end") ) : page_start;

        //how many results per page?
        Integer max_per_page = props.containsKey("max_per_page") ? 
                            Integer.parseInt( props.get("max_per_page") ) : 10;

        //which Magento environment is being targeted for data retrieval?
        String env = props.containsKey("env") ? props.get("env") : null;

        //start date for how far back to search the API. No records created before this date (optional)
        String date_from = props.containsKey("date_from") ? props.get("date_from") : null;

        //start date for how far back to search the API. No records created before this date (optional)
        String date_to = props.containsKey("date_to") ? props.get("date_to") : null;

        //lets go get some orders now...
        MagentoOrderGetter mog = new MagentoOrderGetter(max_per_page, env);

        //save the JSON files after downloading content from Magento API?
        mog.save_file = true;

        //rely on existing saved JSON files if available?
        mog.load_file_if_exist = load_file_if_exist;

        //which page of results to start off with?
        mog.current_page = page_start;

        mog.date_from = date_from;
        mog.date_to = date_to;

        //now have the start and end dates to pull data from the API actually apply.
        mog.setEndpointExtras();

        /*
        how many total potential Magento API items (like orders, customers,
        products) can be called with the given parameters
        */
        Integer total_orders = mog.getOrdersJson(page_start).optInt("total_count");

        //how many pages are potentially allowable from the Magento API call with given parameters
        Integer max_page_end = (int)Math.ceil((float)total_orders / (float)max_per_page);

        /*
        how many pages are used in this session is the lesser of the stated 
        desire for number of pages called vs the maximum potential for the
        number of pages
        */
        Integer final_page_end = Math.min(page_end, max_page_end);

        M2SSystem.println( "final_page_end = " + final_page_end );

        for(int i = page_start; i < final_page_end + 1; i++){

            /*
            page_start is fed as a method parameter, because I find that easier 
            for when the method is in an iteration block.
            */
            JSONObject orders_json = mog.getOrdersJson(i);

            JSONArray temp_order_items = null;

            if( orders_json != null ){
                temp_order_items = orders_json.optJSONArray("items");

                if( temp_order_items != null ){
                    mage_order_groups.add(temp_order_items);
                }
            }
        }

        return mage_order_groups;
    }

    public static void saveSFOrders(List<JSONArray> mage_orders_list) throws IOException
    {

        String first_order_created_at = "";

        //going through the List of order groups
        for(int i = 0; i < mage_orders_list.size(); i++){
            JSONArray mage_orders = mage_orders_list.get(i);

            //get the first order timestamp. Shall be used in part of the file name.
            String start_ts = mage_orders.optJSONObject(0).optString("created_at");

            JSONObject mage_order_obj = new JSONObject();
            
            mage_order_obj.put("items", mage_orders);

            //to convert the Magento orders to Salesforce storefront orders
            Mage2SFOrders mage2SFOrders = new Mage2SFOrders();
            JSONObject sf_data = mage2SFOrders.mage2SFObjOrders( mage_order_obj );

            //save the XML to a file
            mage2SFOrders.prepareSFXMLFile(start_ts, sf_data);

            if(i == 0){
                first_order_created_at = mage2SFOrders.first_order_created_at;
            }
        }

        //which folder has all the new .xml files in it?
        String files_source_folder = Config.xml_save_dir + "orders" + File.separator;

        Map<String, FileContentsProcessedFuncInterface> funcMap = new HashMap<String, FileContentsProcessedFuncInterface>();

        Map<String, String> paramsMap = new HashMap<String, String>();

        //we want to remove all the parent '<orders>' tags from the source xml files before merging them together
        FileContentsProcessedFuncInterface perFileFunc = new FileContentsProcessedFuncInterface() {
            @Override
            public String process(String... file_contents) {
                return file_contents[0].replaceAll("(?i)(<\\/?orders([^>]+)?>)", "");
            }
        };

        funcMap.put("perFileFunc", perFileFunc);


        //we want to remove all the parent '<orders>' tags from the source xml files before merging them together
        FileContentsProcessedFuncInterface endFilesMergedFunc = new FileContentsProcessedFuncInterface() {      
            @Override
            public String process(String... params) {
                String out_str = "<orders xmlns=\"https://www.demandware.com/xml/impex/order/" + params[1] + "\">";
                out_str += params[0] + "</orders>";

                return out_str;
            }
        };

        funcMap.put("endFilesMergedFunc", endFilesMergedFunc);

        //extra params for above closure
        paramsMap.put("endFilesMergedFunc", first_order_created_at);

        //combine those XML files from this session into master for orders and also order items
        MergerFiles.folderFiles2MergedFiles(files_source_folder, "finals",
        false, funcMap, paramsMap, true, "Orders_" + Config.company_name.replace(" ", "-") + "-US_");
    }

    public static void saveDeckOrders(List<JSONArray> mage_orders_list) throws IOException, ParseException 
    {

        for(int i = 0; i < mage_orders_list.size(); i++){

            JSONArray mage_orders = mage_orders_list.get(i);
        
            //convert the Magento orders to Deck Commerce Orders. First, get the right class instance.
            Mage2DeckOrdersCSV m2d = new Mage2DeckOrdersCSV();

            //take the opened Magento JSON orders, covert them to Deck Orders CSV and save it.
            m2d.saveDeckFileFromMageJSONOrders(mage_orders);
        }

        //which folder has all the new .csv files in it?
        String files_source_folder = Config.csv_save_dir + "orders" + File.separator;

        //combine those CSV files from this session into master for orders and also order items
        MergerFiles.folderFiles2MergedFiles(files_source_folder, "finals",
        true, null, null, false, "LegacyOrder_", "LegacyOrderItem_");
    }

    /*
    get the command line parameters (assuming that there are any)

    The Format for the args. All parameters are optional and do not need to be in any order.
    env:mcstaging,max_per_page:234,page_start:5,page_end:35,date_from:2020-01-02,date_to:2022-03-01
    mode:get|getconvertxml|getconvertcsv|convertxml|convertcsv|sftpxml|sftpcsv

    NOTE: parameter 'mode' value sample above is only one of the possibilites 
    seperated by the bar character.
    */
    protected static HashMap<String, String> getParams(String[] args){

        HashMap<String, String> cl_props = new HashMap<String, String>();

        String line = null;

        if( args.length > 0 ){
            line = args[0];
        }

        //only process if there are actual command line parameters
        if(line != null && line.length() > 3 ) {

            //split the key/value pairs by commas
            String str[] = line.split(",");

            for(int i=0; i<str.length; i++){
                String arr[] = str[i].split(":|=");
                
                cl_props.put( arr[0], arr[1] );
            }
        }

        return cl_props;
    }

    public static void main( String[] args ) throws IOException, InterruptedException, ParseException
    {

        // get the command line parameters (assuming that there are any)
        HashMap<String, String> cl_props = getParams( args );

        //mode=get|getconvertxml|convertxml|getconvertcsv|convertcsv|sftpxml|sftpcsv
        String mode = "";

        System.out.println( cl_props );

        if( cl_props.containsKey("mode") ){
            mode = cl_props.get("mode");
        }

        //real world use will be batches of groups of orders
        List<JSONArray> mage_orders = null;

        System.out.println( "mode = " +  mode);

        switch (mode) {
            case "get":
                mage_orders = getMageOrders( cl_props, false );
            break;
            case "convertxml":
                mage_orders = getMageOrders( cl_props, true );

                saveSFOrders(mage_orders);
            break;
            case "getconvertxml":
                mage_orders = getMageOrders( cl_props, false );

                saveSFOrders(mage_orders);
            break;
            case "convertcsv":
                mage_orders = getMageOrders( cl_props, true );

                saveDeckOrders(mage_orders);
            break;
            case "getconvertcsv":
                mage_orders = getMageOrders( cl_props, false );

                saveDeckOrders(mage_orders);
            break;
            default:
                mage_orders = getMageOrders( cl_props, false );
            break;
        }
    }
}
