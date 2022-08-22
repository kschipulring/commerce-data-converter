package com.migrator;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

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
        
        MagentoOrderGetter mog = new MagentoOrderGetter(max_per_page, env);

        //save the JSON files after downloading content from Magento API?
        mog.save_file = true;

        //rely on existing saved JSON files if available?
        mog.load_file_if_exist = load_file_if_exist;

        //which page of results to start off with?
        mog.current_page = page_start;


        for(int i=page_start; i < page_end + 1; i++){

            /*
            page_start is fed as a method parameter, because I find that easier for 
            when the method is in an iteration block.
            */
            JSONObject orders_json = mog.getOrdersJson(i);

            JSONArray temp_order_items_arr = null;

            if( orders_json != null ){
                temp_order_items_arr = orders_json.optJSONArray("items");

                if( temp_order_items_arr != null ){
                    mage_order_groups.add(temp_order_items_arr);
                }
            }
        }

        return mage_order_groups;
    }

    public static void saveSFOrders(List<JSONArray> mage_orders_list) throws IOException
    {

        //going through the List of order groups
        for(int i = 0; i < mage_orders_list.size(); i++){
            JSONArray mage_orders = mage_orders_list.get(i);

            //get the first order timestamp. Shall be used in part of the file name.
            String start_ts = mage_orders.getJSONObject(0).getString("created_at");

            JSONObject mage_order_obj = new JSONObject();

            //System.out.println( "start_ts = " + start_ts );
            
            mage_order_obj.put("items", mage_orders);

            //to convert the Magento orders to Salesforce storefront orders
            Mage2SFOrders mage2SFOrders = new Mage2SFOrders();
            JSONObject sf_data = mage2SFOrders.mage2SFObjOrders( mage_order_obj );

            //save the XML to a file
            mage2SFOrders.prepareSFXMLFile(start_ts, sf_data);
        }
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
    }

    /*
    get the command line parameters (assuming that there are any)

    The Format for the args. All parameters are optional and do not need to be in any order.
    env:mcstaging,max_per_page:234,page_start:5,page_end:35,
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
