package com.migrator;

import java.io.IOException;

import java.util.HashMap;

import javax.annotation.Nullable;

import org.json.JSONObject;
import org.json.JSONArray;

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
    public static JSONArray getMageOrders(
        @Nullable HashMap<String, String> props,
        @Nullable Boolean load_file_if_exist
    ) throws IOException, InterruptedException
    {
        
        // default value for the command line props, which is empty HashMap.
        props = props != null ? props : new HashMap<String, String>();

        load_file_if_exist = load_file_if_exist != null ? load_file_if_exist : true;

        /*
        which page of results? Best off starting with '1' as it is the same as zero.
        But starting with zero may cause repeat results problems in iteration calls.
        */
        Integer page_start = props.containsKey("page_start") ? 
                            Integer.parseInt( props.get("page_start") ) : 1;

        //how many results per page?
        Integer max_per_page = props.containsKey("max_per_page") ? 
                            Integer.parseInt( props.get("max_per_page") ) : 10;
        
        MagentoOrderGetter mog = new MagentoOrderGetter(max_per_page);

        //save the JSON files after downloading content from Magento API?
        mog.save_file = true;

        //rely on existing saved JSON files if available?
        mog.load_file_if_exist = load_file_if_exist;

        //which page of results to start off with?
        mog.current_page = page_start;

        /*
        page_start is fed as a method parameter, because I find that easier for 
        when the method is in an iteration block.
        */
        JSONObject orders_json = mog.getOrdersJson(page_start);

        JSONArray jsonArray = orders_json.getJSONArray("items");

        System.out.println( jsonArray.getJSONObject(0).get("created_at") );

        return jsonArray;
    }

    public static void saveSForders(JSONArray mage_orders) throws IOException{

        //System.out.println( mage_orders );
        String start_ts = mage_orders.getJSONObject(0).getString("created_at");

        JSONObject mage_order_obj = new JSONObject();


        System.out.println( "start_ts = " + start_ts );
        

        mage_order_obj.put("items", mage_orders);


        Mage2SFOrders mage2SF = new Mage2SFOrders();

        JSONObject sf_data = mage2SF.mage2SFObj( mage_order_obj );

        mage2SF.prepareSFXMLFile(start_ts, sf_data);
/**/
        //System.out.println( sf_data );
    }

    /*
    get the command line parameters (assuming that there are any)

    The Format for the args. All parameters are optional and do not need to be in any order.
    env:mcstaging,page_size:234,page_start:5,page_end:35,mode:get|getconvertxml|getconvertcsv|convertxml|convertcsv|sftpxml|sftpcsv

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

            //split the key/value pairs by either commas or ampersands
            String str[] = line.split(",|&");

            for(int i=0; i<str.length; i++){
                String arr[] = str[i].split(":|=");
                
                cl_props.put( arr[0], arr[1] );
            }
        }

        return cl_props;
    }

    public static void main( String[] args ) throws IOException, InterruptedException
    {

        // get the command line parameters (assuming that there are any)
        HashMap<String, String> cl_props = getParams( args );

        //mode=get|getconvertxml|getconvertcsv|convertxml|convertcsv|sftpxml|sftpcsv
        String mode = "";
        String xyz =  null;

        System.out.println( cl_props );

        if( cl_props.containsKey("mode") ){
            System.out.println( cl_props.containsKey("mode")  );
        }
        
        if( cl_props.containsKey("mode") ){
            mode = cl_props.get("mode");
        }

        JSONArray mage_orders = null;

        switch (mode) {
            case "get":
                mage_orders = getMageOrders( cl_props, false );
            break;
            case "convertxml":
                mage_orders = getMageOrders( cl_props, true );

                saveSForders(mage_orders);
            break;
            case "getconvertxml":
                mage_orders = getMageOrders( cl_props, false );

                saveSForders(mage_orders);
            default:
                mage_orders = getMageOrders( cl_props, false );
            break;
        }

        /*for (String s: args) {
            System.out.println(s);
        }*/

        //items.map().forEach((k, v) -> System.out.println(k + ":" + v));
    }
}
