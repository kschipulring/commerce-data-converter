package com.migrator;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Mage2DeckOrdersCSV extends JSONToCSV {

    public Mage2DeckOrdersCSV() throws IOException{
        //signify that this is the Orders section and not something else like customersa
        super( "orders" );
    }

    /*
     * DESCRIPTION: converts Magento orders to Deck CSV data.
    */
    public List<List<String>> mage2DeckOrdersCSVRows( JSONObject mage_orders ) throws IOException, ParseException{

        //the JSON form of the Deck Commerce data to export
        Mage2DeckOrdersCSV m2doc = new Mage2DeckOrdersCSV();

        //convert the Magento orders to an object format that can be quickly converted to Deck CSV.
        List<Map<DeckOrderHeaders, String>> mdmo = m2doc.mage2DeckMapOrders(mage_orders);

        //start with blank CSV rows
        List<List<String>> csv_rows = new ArrayList<List<String>>();

        //initial blank CSV headers, or the first row here
        List<String> csv_row_headers = new ArrayList<String>();

        //Populate headers here.
        for (DeckOrderHeaders d : DeckOrderHeaders.values()) {
            csv_row_headers.add( d.value );
        }

        //add the headers row to the overall CSV structure
        csv_rows.add( csv_row_headers );
        
        //now populate the actual CSV data
        for(int i = 0; i < mdmo.size(); i++){
            List<String> csv_row = new ArrayList<String>();

            for (DeckOrderHeaders d : DeckOrderHeaders.values()) {
                csv_row.add( mdmo.get(i).get(d) );
            }

            csv_rows.add( csv_row );
        }

        //ready to be saved to file
        return csv_rows;
    }

    public static void mageOrderLoadTester() throws IOException, ParseException{
        
        Mage2SFOrders mage2SFOrders = new Mage2SFOrders();
        
        String[] json_folder_arr = { Config.json_save_dir, mage2SFOrders.section };
        String json_folder = String.join(File.separator, json_folder_arr) + File.separator;

        System.out.println("json_folder = "+ json_folder);

        //String json_filename = json_folder + "sample_mcstaging_orders.json";
        //String json_filename = json_folder + "orders_pageSize-10_currentPage-1_2019-01-03_08-50-22.json";
        String json_filename = json_folder + "orders_pageSize-10_currentPage-500_2019-11-30_22-24-20.json";

        //get the data from the saved .json file of Magento orders.
        String json_data = ReadFromFile.contents( json_filename );

        JSONObject mage_orders_items = new JSONObject(json_data);

        JSONArray mage_orders = mage_orders_items.getJSONArray("items");

        //the JSON form of the Deck Commerce data to export
        Mage2DeckOrdersCSV m2doc = new Mage2DeckOrdersCSV();

        List<Map<DeckOrderHeaders, String>> mdmo = m2doc.mage2DeckMapOrders(mage_orders_items);

        List<List<String>> csv_rows = m2doc.deckItems2CSVRows( mdmo, DeckOrderHeaders.values() );

        //get the first order timestamp.
        String start_ts = mage_orders.getJSONObject(0).getString("created_at");

        //prepare and then save the CSV file
        m2doc.prepareDeckCSVFile( start_ts, csv_rows );

    }

    /*
     * DESCRIPTION: converts the Magento order JSON to a List of Maps that use 
     * Deck order CSV header as key. Will be used for the final conversion to 
     * CSV data.
    */
    public List<Map<DeckOrderHeaders, String>> mage2DeckMapOrders( JSONObject mage_orders_items ) throws ParseException
    {
        
        //each "item" represents a Magento order, not a magento 'quote' (or cart) item
        JSONArray mage_orders = mage_orders_items.getJSONArray("items");

        //what gets returned
        List<Map<DeckOrderHeaders, String>> order_rows = new ArrayList<Map<DeckOrderHeaders, String>>();

        //loop through the original Magento API orders
        for (int i = 0; i < mage_orders.length(); i++) {
            JSONObject mage_order = mage_orders.getJSONObject(i);

            Map<DeckOrderHeaders, String> map = new EnumMap<DeckOrderHeaders, String>(DeckOrderHeaders.class);
         
            map.put(DeckOrderHeaders.ORDERNUMBER, mage_order.get("entity_id").toString() );
            map.put(DeckOrderHeaders.SITECODE,  Config.company_name.replace(" ", "-") );
            map.put(DeckOrderHeaders.CUSTOMERID, mage_order.get("customer_id").toString() );
            map.put(DeckOrderHeaders.CUSTOMERLOCALE, "en-US");

            String deck_date_str = mage2DeckDateTime( mage_order.getString("created_at") );
            map.put(DeckOrderHeaders.ORDERDATE, deck_date_str);

            //Special handling for the OrderStatusCode column
            String order_status_raw = mage_order.optString("status");

            String[] c_statuses = {"closed", "canceled"};
            boolean c_found = Arrays.stream(c_statuses).anyMatch(s -> s.equals(order_status_raw));

            //if the order status is cancelled or closed, it is "C". Otherwise, it is "Z".
            String OrderStatusCode = c_found? "C" : "Z";

            map.put(DeckOrderHeaders.ORDERSTATUSCODE, OrderStatusCode);
            map.put(DeckOrderHeaders.DISCOUNTAMOUNT, mage_order.optString("base_discount_amount").toString() );
            map.put(DeckOrderHeaders.DISCOUNTCODE, "");
            map.put(DeckOrderHeaders.SHIPPINGMETHOD, mage_order.optString("shipping_description") );
            map.put(DeckOrderHeaders.SHIPPINGCOST, mage_order.optString("shipping_incl_tax") );

            map.put(DeckOrderHeaders.SHIPPINGDISCOUNTAMOUNT, mage_order.optString("shipping_discount_amount") );
            map.put(DeckOrderHeaders.SHIPPINGDISCOUNTCODE, "");
            map.put(DeckOrderHeaders.USSHIPPINGTAX, mage_order.optString("shipping_tax_amount") );
            map.put(DeckOrderHeaders.VATSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.GSTSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.HSTSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.PSTSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.GSTVATSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.NETSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.USSALESTAX, mage_order.optString("tax_amount") );
            map.put(DeckOrderHeaders.VATSALESTAX, "");
            map.put(DeckOrderHeaders.GSTSALESTAX, "");
            map.put(DeckOrderHeaders.HSTSALESTAX, "");
            map.put(DeckOrderHeaders.PSTSALESTAX, "");
            map.put(DeckOrderHeaders.GSTVATSALESTAX, "");
            map.put(DeckOrderHeaders.NETSALESTAX, "");
            map.put(DeckOrderHeaders.SUBTOTAL, mage_order.optString("subtotal") );
            map.put(DeckOrderHeaders.ORDERTOTAL, mage_order.optString("total_due") );
            map.put(DeckOrderHeaders.BILLFIRSTNAME, mage_order.optJSONObject("billing_address").optString("firstname") );
            map.put(DeckOrderHeaders.BILLLASTNAME, mage_order.optJSONObject("billing_address").optString("lastname") );
            map.put(DeckOrderHeaders.BILLEMAIL, mage_order.optJSONObject("billing_address").optString("email") );
            map.put(DeckOrderHeaders.BILLPHONE, mage_order.optJSONObject("billing_address").optString("telephone") );
            map.put(DeckOrderHeaders.BILLADDRESS, mage_order.optJSONObject("billing_address").getJSONArray("street").optString(0) );
            map.put(DeckOrderHeaders.BILLADDRESS2, mage_order.optJSONObject("billing_address").getJSONArray("street").optString(1) );
            map.put(DeckOrderHeaders.BILLADDRESS3, mage_order.optJSONObject("billing_address").getJSONArray("street").optString(2) );
            map.put(DeckOrderHeaders.BILLCITY, mage_order.optJSONObject("billing_address").optString("city") );
            map.put(DeckOrderHeaders.BILLPROVINCE, mage_order.optJSONObject("billing_address").optString("region_code") );
            map.put(DeckOrderHeaders.BILLPOSTALCODE, mage_order.optJSONObject("billing_address").optString("postcode") );
            map.put(DeckOrderHeaders.BILLCOUNTRY, mage_order.optJSONObject("billing_address").optString("country_id") );


            JSONObject shipping_settings = mage_order.optJSONObject("extension_attributes")
                                                    .optJSONArray("shipping_assignments")
                                                    .optJSONObject(0)
                                                    .optJSONObject("shipping")
                                                    .optJSONObject("address");

            map.put(DeckOrderHeaders.SHIPFIRSTNAME, shipping_settings.optString("firstname") );
            map.put(DeckOrderHeaders.SHIPLASTNAME, shipping_settings.optString("lastname") );
            map.put(DeckOrderHeaders.SHIPEMAIL, shipping_settings.optString("email") );
            map.put(DeckOrderHeaders.SHIPPHONE, shipping_settings.optString("telephone") );
            map.put(DeckOrderHeaders.SHIPADDRESS, shipping_settings.getJSONArray("street").optString(0) );
            map.put(DeckOrderHeaders.SHIPADDRESS2, shipping_settings.getJSONArray("street").optString(1) );
            map.put(DeckOrderHeaders.SHIPADDRESS3, shipping_settings.getJSONArray("street").optString(2) );
            map.put(DeckOrderHeaders.SHIPCITY, shipping_settings.optString("city") );
            map.put(DeckOrderHeaders.SHIPPROVINCE, shipping_settings.optString("region") );
            map.put(DeckOrderHeaders.SHIPPOSTALCODE, shipping_settings.optString("postcode") );
            map.put(DeckOrderHeaders.SHIPCOUNTRY, shipping_settings.optString("country_id") );
            map.put(DeckOrderHeaders.ORDERSOURCE, "Web" );
        
            order_rows.add(map);
        }

        return order_rows;
    }

    public static void main( String[] args ) throws IOException, ParseException
    {
        //just testing
        mageOrderLoadTester();
    }
}
