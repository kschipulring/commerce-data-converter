package com.migrator;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Mage2DeckOrdersItemsCSV extends JSONToCSV {

     //part of the CSV file name.
     public String created_at = null;

     //this will be a large batch of CSV rows for a instance of this class.
     public List<Map<CSVHeaderInterface, String>> deckMapOrderItems = null;

     //to determine if this batch can finally be saved. False means not yet.
     public boolean is_last_iteration = false;

     public Mage2DeckOrdersItemsCSV() throws IOException{
          //signify that this is the Order items (products) section and not something else like customers
          super( "orders", "items" );
     }

     //helper method to convert Magento Order Items to Deck Commerce CSV counterparts
     @SuppressWarnings("unchecked")
     public List<Map<CSVHeaderInterface, String>> mage2DeckMapOrderItems(
          String order_number, String OrderStatusCode, JSONArray mage_order_product_items
     ) throws ParseException
     {
          
          //what gets returned
          List<Map<CSVHeaderInterface, String>> order_items_rows = new ArrayList<Map<CSVHeaderInterface, String>>();
          
          for (int i = 0; i < mage_order_product_items.length(); i++) {
               JSONObject mage_order_product_item = mage_order_product_items.getJSONObject(i);
               
               EnumMap map = new EnumMap<DeckOrderItemHeaders, String>(DeckOrderItemHeaders.class);
               
               //the order number comes from the parent order
               map.put(DeckOrderItemHeaders.ORDERNUMBER, order_number);
               
               // ItemStatusCode.  Default is "IX" or 'Not Available'
               String item_status_code = "IX";
               
               if(mage_order_product_item.getInt("qty_shipped") >= 1){
                    item_status_code = "IZ";
               }else if( OrderStatusCode == "C" ){
                    item_status_code = "IV";
               }
               
               map.put(DeckOrderItemHeaders.ITEMSTATUSCODE, item_status_code);
               map.put(DeckOrderItemHeaders.UPC, mage_order_product_item.optString("sku"));
               map.put(DeckOrderItemHeaders.PRODUCTCODE, mage_order_product_item.optString("name"));
               map.put(DeckOrderItemHeaders.SKU, mage_order_product_item.optString("sku"));
               map.put(DeckOrderItemHeaders.QUANTITY, mage_order_product_item.optString("qty_ordered").toString() );
               map.put(DeckOrderItemHeaders.NETPRICE, mage_order_product_item.optString("base_price").toString() );
               map.put(DeckOrderItemHeaders.GROSSPRICE, mage_order_product_item.optString("base_price_incl_tax").toString() );

               String discount_amount = mage_order_product_item.optFloat("discount_amount") > 0 ? 
                    mage_order_product_item.optString("discount_amount").toString() : null;
               
               map.put(DeckOrderItemHeaders.DISCOUNTAMOUNT, discount_amount);

               map.put(DeckOrderItemHeaders.USSALESTAX, mage_order_product_item.optString("tax_amount").toString());

               order_items_rows.add(map);
          }

          return order_items_rows;
     }
 
     public void saveDeckFileFromMageJSONOrderItems(
          String order_number, String OrderStatusCode, JSONArray mage_order_product_items
     ) throws IOException, ParseException
     {
 
         //the Map form of the Deck Commerce data to export
         List<Map<CSVHeaderInterface, String>> deckMapOrderItems =
          this.mage2DeckMapOrderItems(order_number, OrderStatusCode, mage_order_product_items);
 
         //a usable format for org.apache.commons.csv classes to turn into a CSV file
         List<List<String>> csv_rows = this.deckItems2CSVRows(deckMapOrderItems, DeckOrderItemHeaders.values());

         System.out.println( csv_rows );
 
         //prepare and then save the CSV file
         this.prepareDeckCSVFile( this.created_at, csv_rows );
     }
}
