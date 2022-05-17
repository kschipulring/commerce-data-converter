package com.migrator;

import java.io.IOException;

import java.util.HashMap;

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
    public static void getMageOrders( HashMap<String, String> props) throws IOException, InterruptedException
    {
        
        //page_size=234,env=mcstaging,page_start=5,page_range=5-35

        /*
        which page of results? Best off starting with '1' as it is the same as zero.
        But starting with zero may cause repeat results problems in iteration calls.
        */
        Integer page_start = props.containsKey("page_start") ? 
                            Integer.parseInt( props.get("page_start") ) : 1;

        //how many results per page?
        Integer page_size = props.containsKey("page_size") ? 
                            Integer.parseInt( props.get("page_size") ) : 10;
        
        MagentoOrderGetter mog = new MagentoOrderGetter();

        //save the JSON files after downloading content from Magento API?
        mog.save_file = true;

        //rely on existing saved JSON files if available?
        mog.load_file_if_exist = true;

        /*
        page_start is fed as a method parameter, because I find that easier for 
        when the method is in an iteration block.
        */
        JSONObject orders_json = mog.getOrdersJson(page_start);

        JSONArray jsonArray = orders_json.getJSONArray("items");

        System.out.println( jsonArray.getJSONObject(0).get("created_at") );
    }

    public void saveSForders(){}

    /*
    get the command line parameters (assuming that there are any)

    The Format for the args. All parameters are optional and do not need to be in any order.
    env:mcstaging,page_size:234,page_start:5,page_range:5-35,mode=get|convert|sftp|all

    NOTE: parameter 'mode' sample above is only one of the possibilites 
    seperated by the bar character.
    */
    protected static HashMap<String, String> getParams(String[] args){

        HashMap<String, String> cl_props = null;

        String line = null;

        if( args.length > 0 ){
            line = args[0];
        }

        if(line != null && line.length() > 3 ) {
            cl_props = new HashMap<String, String>();

            String str[] = line.split(",");

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

        System.out.println( cl_props );
        
        /*getMageOrders( args );

        for (String s: args) {
            //System.out.println(s);
        }*/

        //items.map().forEach((k, v) -> System.out.println(k + ":" + v));
    }
}
