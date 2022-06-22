package com.migrator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.JSONArray;
import org.json.JSONObject;

public class Mage2DeckOrdersCSV extends JSONToCSV {

    public Mage2DeckOrdersCSV() throws IOException{
        //signify that this is the Orders section and not something else like customersa
        super( "orders" );
    }

    public List<List<String>> deckItems2CSVRows(
        List<Map<?, String>> deck_items,
        CSVHeaderInterface[] enum_vals
    ){

        //the return value. will have all the CSV rows plus the headers as the first row.
        List<List<String>> csv_rows = new ArrayList<List<String>>();

        //the first row are the CSV headers. They come from an enum
        List<String> csv_row_headers = new ArrayList<String>();

        for (CSVHeaderInterface e : enum_vals) {
            csv_row_headers.add( e.value() );
        }

        //make it the first row
        csv_rows.add( csv_row_headers );

        for(int i = 0; i < deck_items.size(); i++){
            List<String> csv_row = new ArrayList<String>();

            for (CSVHeaderInterface d : enum_vals ) {
                csv_row.add( deck_items.get(i).get(d) );
            }

            csv_rows.add( csv_row );
        }

        return csv_rows;
    }

    public List<List<String>> mage2DeckOrdersCSVRows( JSONObject mage_orders ) throws IOException, ParseException{

        //the JSON form of the Deck Commerce data to export
        Mage2DeckOrdersCSV m2doc = new Mage2DeckOrdersCSV();

        List<Map<DeckOrderHeaders, String>> mdmo = m2doc.mage2DeckMapOrders(mage_orders);


        List<List<String>> csv_rows = new ArrayList<List<String>>();

        List<String> csv_row_headers = new ArrayList<String>();

        //the first row is headers
        for (DeckOrderHeaders d : DeckOrderHeaders.values()) {
            csv_row_headers.add( d.value );
        }

        csv_rows.add( csv_row_headers );
        
        for(int i = 0; i < mdmo.size(); i++){
            List<String> csv_row = new ArrayList<String>();

            for (DeckOrderHeaders d : DeckOrderHeaders.values()) {
                csv_row.add( mdmo.get(i).get(d) );
            }

            csv_rows.add( csv_row );
        }

        return csv_rows;
    }

    public static void mageOrderLoadTester() throws IOException, ParseException{
        
        Mage2SFOrders mage2SFOrders = new Mage2SFOrders();
        
        String[] json_folder_arr = { Config.json_save_dir, mage2SFOrders.section };
        String json_folder = String.join(File.separator, json_folder_arr) + File.separator;

        System.out.println("json_folder = "+ json_folder);

        String json_filename = json_folder + "sample_mcstaging_orders.json";

        //get the data from the saved .json file of Magento orders.
        String json_data = ReadFromFile.contents( json_filename );

        JSONObject mage_orders_items = new JSONObject(json_data);

        //the JSON form of the Deck Commerce data to export
        Mage2DeckOrdersCSV m2doc = new Mage2DeckOrdersCSV();

        //List<Map<CSVHeaderInterface, String>> mdmo = m2doc.mage2DeckMapOrders(mage_orders_items);
        List<Map<DeckOrderHeaders, String>> mdmo = m2doc.mage2DeckMapOrders(mage_orders_items);

        //List<Map<CSVHeaderInterface, String>> mdmo2 = (List<Map<?, String>>)mdmo;

        List<List<String>> csv_rows = m2doc.deckItems2CSVRows( mdmo, DeckOrderHeaders.values() );


        String SAMPLE_CSV_FILE = "./sample_mcstaging_orders.csv";
        
        try (
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(SAMPLE_CSV_FILE));

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT );
        ) {

            //csvPrinter.printRecord( csv_row_headers );

            for(int i = 0; i < csv_rows.size(); i++){
                csvPrinter.printRecord( csv_rows.get(i) );
            }

            csvPrinter.flush();
        }
    }

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

            map.put(DeckOrderHeaders.ORDERSTATUSCODE, "");
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

    public static void enumMerator( CSVHeaderInterface[] enum_vals ){

        for (CSVHeaderInterface e : enum_vals) {
            System.out.println( e + " " + e.value() );
        }
    }

    public static void main( String[] args ) throws IOException, ParseException
    {

        enumMerator( DeckOrderHeaders.values() );

        /*
        for (DeckOrderHeaders d : DeckOrderHeaders.values()) {
            System.out.println( d + " " + d.value );
        }*/

        /*
        CSVHeaderInterface[] y = DeckOrderHeaders.values();

        System.out.println( y );


        enumMerator( y );
        */

        //System.out.println( DeckOrderHeaders.values() );

        //List<?> xyz = (ArrayList<?>)DeckOrderHeaders.values();

        //mageOrderLoadTester();
    }
}
