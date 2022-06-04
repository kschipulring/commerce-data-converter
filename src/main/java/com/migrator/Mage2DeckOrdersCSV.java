package com.migrator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;  
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;


public class Mage2DeckOrdersCSV extends JSONToCSV {

    public Mage2DeckOrdersCSV() throws IOException{
        //signify that this is the Orders section and not something else like customers
        super( "orders" );
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

        List<Map<DeckOrderHeaders, Object>> mdmo = m2doc.mage2DeckMapOrders(mage_orders_items);


        System.out.println( mdmo.get(0).get( DeckOrderHeaders.ORDERNUMBER ) );


        for (DeckOrderHeaders d : DeckOrderHeaders.values()) {
            System.out.println( d + " " + d.value  + " = " + mdmo.get(0).get(d) );
        }

    }

    public String mage2DeckDateTime(String mage_date_str) throws ParseException{

        Date mage_date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(mage_date_str);
        DateFormat deckDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm");
  
        return deckDateFormat.format(mage_date);
    }

    public List<Map<DeckOrderHeaders, Object>> mage2DeckMapOrders( JSONObject mage_orders_items ) throws ParseException
    {
        
        //each "item" represents a Magento order, not a magento 'quote' (or cart) item
        JSONArray mage_orders = mage_orders_items.getJSONArray("items");

        //what gets returned
        List<Map<DeckOrderHeaders, Object>> order_rows = new ArrayList<Map<DeckOrderHeaders, Object>>();

        //loop through the original Magento API orders
        for (int i = 0; i < mage_orders.length(); i++) {
            JSONObject mage_order = mage_orders.getJSONObject(i);

            Map<DeckOrderHeaders, Object> map = new EnumMap<DeckOrderHeaders, Object>(DeckOrderHeaders.class);
         
            map.put(DeckOrderHeaders.ORDERNUMBER, mage_order.get("entity_id") );
            map.put(DeckOrderHeaders.SITECODE,  Config.company_name.replace(" ", "-") );
            map.put(DeckOrderHeaders.CUSTOMERID, mage_order.get("customer_id") );
            map.put(DeckOrderHeaders.CUSTOMERLOCALE, "en-US");

            String deck_date_str = mage2DeckDateTime( mage_order.getString("created_at") );
            map.put(DeckOrderHeaders.ORDERDATE, deck_date_str);

            map.put(DeckOrderHeaders.ORDERSTATUSCODE, "");
            map.put(DeckOrderHeaders.DISCOUNTAMOUNT, mage_order.opt("base_discount_amount").toString() );
            map.put(DeckOrderHeaders.DISCOUNTCODE, "");
            map.put(DeckOrderHeaders.SHIPPINGMETHOD, mage_order.opt("shipping_description") );
            map.put(DeckOrderHeaders.SHIPPINGCOST, mage_order.opt("shipping_incl_tax") );

            map.put(DeckOrderHeaders.SHIPPINGDISCOUNTAMOUNT, mage_order.opt("shipping_discount_amount") );
            map.put(DeckOrderHeaders.SHIPPINGDISCOUNTCODE, "");
            map.put(DeckOrderHeaders.USSHIPPINGTAX, mage_order.opt("shipping_tax_amount") );
            map.put(DeckOrderHeaders.VATSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.GSTSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.HSTSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.PSTSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.GSTVATSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.NETSHIPPINGTAX, "");
            map.put(DeckOrderHeaders.USSALESTAX, mage_order.opt("tax_amount") );
            map.put(DeckOrderHeaders.VATSALESTAX, "");
            map.put(DeckOrderHeaders.GSTSALESTAX, "");
            map.put(DeckOrderHeaders.HSTSALESTAX, "");
            map.put(DeckOrderHeaders.PSTSALESTAX, "");
            map.put(DeckOrderHeaders.GSTVATSALESTAX, "");
            map.put(DeckOrderHeaders.NETSALESTAX, "");
            map.put(DeckOrderHeaders.SUBTOTAL, mage_order.opt("subtotal") );
            map.put(DeckOrderHeaders.ORDERTOTAL, mage_order.opt("total_due") );
            map.put(DeckOrderHeaders.BILLFIRSTNAME, mage_order.optJSONObject("billing_address").opt("firstname") );
            map.put(DeckOrderHeaders.BILLLASTNAME, mage_order.optJSONObject("billing_address").opt("lastname") );
            map.put(DeckOrderHeaders.BILLEMAIL, mage_order.optJSONObject("billing_address").opt("email") );
            map.put(DeckOrderHeaders.BILLPHONE, mage_order.optJSONObject("billing_address").opt("telephone") );
            map.put(DeckOrderHeaders.BILLADDRESS, mage_order.optJSONObject("billing_address").getJSONArray("street").opt(0) );
            map.put(DeckOrderHeaders.BILLADDRESS2, mage_order.optJSONObject("billing_address").getJSONArray("street").opt(1) );
            map.put(DeckOrderHeaders.BILLADDRESS3, mage_order.optJSONObject("billing_address").getJSONArray("street").opt(2) );
            map.put(DeckOrderHeaders.BILLCITY, mage_order.optJSONObject("billing_address").opt("city") );
            map.put(DeckOrderHeaders.BILLPROVINCE, mage_order.optJSONObject("billing_address").opt("region_code") );
            map.put(DeckOrderHeaders.BILLPOSTALCODE, mage_order.optJSONObject("billing_address").opt("postcode") );
            map.put(DeckOrderHeaders.BILLCOUNTRY, mage_order.optJSONObject("billing_address").opt("country_id") );


            JSONObject shipping_settings = mage_order.optJSONObject("extension_attributes")
                                                    .optJSONArray("shipping_assignments")
                                                    .optJSONObject(0)
                                                    .optJSONObject("shipping")
                                                    .optJSONObject("address");

            map.put(DeckOrderHeaders.SHIPFIRSTNAME, shipping_settings.opt("firstname") );
            map.put(DeckOrderHeaders.SHIPLASTNAME, shipping_settings.opt("lastname") );
            map.put(DeckOrderHeaders.SHIPEMAIL, shipping_settings.opt("email") );
            map.put(DeckOrderHeaders.SHIPPHONE, shipping_settings.opt("telephone") );
            map.put(DeckOrderHeaders.SHIPADDRESS, shipping_settings.getJSONArray("street").opt(0) );
            map.put(DeckOrderHeaders.SHIPADDRESS2, shipping_settings.getJSONArray("street").opt(1) );
            map.put(DeckOrderHeaders.SHIPADDRESS3, shipping_settings.getJSONArray("street").opt(2) );
            map.put(DeckOrderHeaders.SHIPCITY, shipping_settings.opt("city") );
            map.put(DeckOrderHeaders.SHIPPROVINCE, shipping_settings.opt("region") );
            map.put(DeckOrderHeaders.SHIPPOSTALCODE, shipping_settings.opt("postcode") );
            map.put(DeckOrderHeaders.SHIPCOUNTRY, shipping_settings.opt("country_id") );
            map.put(DeckOrderHeaders.ORDERSOURCE, "Web" );
        
            order_rows.add(map);
        }

        return order_rows;
    }

    public static void main( String[] args ) throws IOException, ParseException{

        mageOrderLoadTester();
    }
}
