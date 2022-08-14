package com.migrator;

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

    //for batch processes of order saves
    Mage2DeckOrdersItemsCSV mi = null;

    //constructor, for Java newbies
    public Mage2DeckOrdersCSV() throws IOException{
        //signify that this is the Orders section and not something else like customers
        super( "orders" );

        this.mi = new Mage2DeckOrdersItemsCSV();
    }

    //get Magento JSON orders from a saved JSON file
    public JSONArray getMageOrdersFromFile(String base_json_filename) throws IOException, ParseException
    {
        Mage2SFOrders mage2SFOrders = new Mage2SFOrders();

        String json_folder = this.getJsonFolder(mage2SFOrders.section);
        String json_filename = json_folder + base_json_filename;

        //get the data from the saved .json file of Magento orders.
        String json_str = ReadFromFile.contents( json_filename );

        //The actual source orders to convert from
        return this.getMageOrdersFromString(json_str);
    }

    //can be from a static JSON file or a REST API call.  Called from above method.
    public JSONArray getMageOrdersFromString(String json_orders_string)
    {
        JSONObject mage_orders_items = new JSONObject(json_orders_string);

        //The actual source orders to convert from
        return mage_orders_items.getJSONArray("items");
    }

    public void saveDeckFileFromMageJSONOrders(String json_filename) throws IOException, ParseException
    {

        JSONArray mage_orders = this.getMageOrdersFromFile(json_filename);

        //the Map form of the Deck Commerce data to export
        List<Map<CSVHeaderInterface, String>> deckMapOrders = this.mage2DeckMapOrders(mage_orders);

        //a usable format for org.apache.commons.csv classes to turn into a CSV file
        List<List<String>> csv_rows = this.deckItems2CSVRows(deckMapOrders, DeckOrderHeaders.values());

        //get the first order timestamp. Used for part of the CSV file name.
        String start_ts = mage_orders.getJSONObject(0).getString("created_at");

        //prepare and then save the CSV file
        this.prepareDeckCSVFile( start_ts, csv_rows );
    }

    /*
     * DESCRIPTION: converts the Magento order JSON to a List of Maps that use 
     * Deck order CSV header as key. Will be used for the final conversion to 
     * CSV data. This occurs with mage2DeckOrdersCSVRows(...)
    */
    @SuppressWarnings("unchecked")
    public List<Map<CSVHeaderInterface, String>> mage2DeckMapOrders(JSONArray mage_orders) throws ParseException, IOException
    {

        //what gets returned
        List<Map<CSVHeaderInterface, String>> order_rows = new ArrayList<Map<CSVHeaderInterface, String>>();

        //loop through the original Magento API orders
        for (int i = 0; i < mage_orders.length(); i++) {
            JSONObject mage_order = mage_orders.getJSONObject(i);

            //Map<CSVHeaderInterface, String> map = new EnumMap<DeckOrderHeaders, String>(DeckOrderHeaders.class);
            EnumMap map = new EnumMap<DeckOrderHeaders, String>(DeckOrderHeaders.class);

            String order_number = mage_order.get("entity_id").toString();
         
            map.put(DeckOrderHeaders.ORDERNUMBER, order_number );
            map.put(DeckOrderHeaders.SITECODE,  Config.company_name.replace(" ", "-") );
            map.put(DeckOrderHeaders.CUSTOMERID, mage_order.get("customer_id").toString() );
            map.put(DeckOrderHeaders.CUSTOMERLOCALE, "en-US");

            String created_at = mage_order.getString("created_at");

            String deck_date_str = mage2DeckDateTime( created_at );
            map.put(DeckOrderHeaders.ORDERDATE, deck_date_str);

            //Special handling for the OrderStatusCode column
            String order_status_raw = mage_order.optString("status");

            //find out if the order is closed, canceled or fraud...
            String[] c_statuses = {"closed", "canceled", "fraud"};
            boolean c_found = Arrays.stream(c_statuses).anyMatch(s -> s.equals(order_status_raw));

            //if the order status is cancelled or closed, it is "C". Otherwise, it is "Z".
            String OrderStatusCode = c_found? "C" : "Z";

            // process the Order Items
            JSONArray orderItems = mage_order.getJSONArray("items");

            if( i == 0 ){
                this.mi.created_at = created_at;
            }

            this.mi.saveDeckFileFromMageJSONOrderItems(order_number, OrderStatusCode, orderItems);


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

    public void mageOrderLoadTester() throws IOException, ParseException
    {

        //String json_filename = "sample_mcstaging_orders.json";
        //String json_filename = "orders_pageSize-10_currentPage-1_2019-01-03_08-50-22.json";
        String json_filename = "orders_pageSize-10_currentPage-500_2019-11-30_22-24-20.json";

        this.saveDeckFileFromMageJSONOrders(json_filename);
    }

    public static void main( String[] args ) throws IOException, ParseException
    {
        Mage2DeckOrdersCSV m2d = new Mage2DeckOrdersCSV();
        
        //just testing
        m2d.mageOrderLoadTester();
    }
}
