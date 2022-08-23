package com.migrator;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Mage2DeckOrdersItemsCSV extends JSONToCSV {

     //part of the CSV file name.
     public String created_at = null;

     //this will be a large batch of CSV rows for a instance of this class.
     public List<List<String>> deckMapOrderItems = null;

     //to determine if this is the first iteration for the parent order.
     public boolean is_first_iteration = false;

     //to determine if this batch can finally be saved. False means not yet.
     public boolean is_last_iteration = false;

     public Mage2DeckOrdersItemsCSV() throws IOException{
          //signify that this is the Order items (products) section and not something else like customers
          super( "orders", "items" );

          M2SSystem.println( "Mage2DeckOrdersItemsCSV initiated" );

          this.deckMapOrderItems = new ArrayList<List<String>>();
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

               String product_code = mage_order_product_item.optString("name").replace(",", "");
               map.put(DeckOrderItemHeaders.PRODUCTCODE, product_code);
               map.put(DeckOrderItemHeaders.SKU, mage_order_product_item.optString("sku"));
               map.put(DeckOrderItemHeaders.QUANTITY, mage_order_product_item.optString("qty_ordered").toString() );

 
               //take the size from the end of the SKU (if available)
               String[] sku_arr = mage_order_product_item.optString("sku").split("-", 4);
               String size = sku_arr.length > 1 ? sku_arr[1] : "";

               map.put(DeckOrderItemHeaders.SIZE, size );

               map.put(DeckOrderItemHeaders.NETPRICE, mage_order_product_item.optString("base_price").toString() );
               map.put(DeckOrderItemHeaders.GROSSPRICE, mage_order_product_item.optString("base_price_incl_tax").toString() );

               String discount_amount = mage_order_product_item.optFloat("discount_amount") > 0 ? 
                    mage_order_product_item.optString("discount_amount").toString() : null;
               
               map.put(DeckOrderItemHeaders.DISCOUNTAMOUNT, discount_amount);

               
               //multiple uses, including scanning for LCP
               JSONObject joei = mage_order_product_item.optJSONObject("extension_attributes");

               //for testing downstream. Not for directly setting in the CSV
               Boolean is_lcp = joei.optBoolean("is_lcp");

               map.put(DeckOrderItemHeaders.IS_LCP, Boolean.toString(is_lcp) );
               map.put(DeckOrderItemHeaders.LCP_CHECK, joei.optString("lcp_check"));

               //which product is this a Care Plan for? (only for LCP items)
               String lcp_for_item_id = "";

               if(is_lcp){
                    lcp_for_item_id = joei.optString("lcp_for_item_id");
               }

               map.put(DeckOrderItemHeaders.LCP_FOR_ITEM_ID, lcp_for_item_id);
               map.put(DeckOrderItemHeaders.LCP_TYPE, joei.optString("lcp_type"));

               String lcp_add = "";

               if(!is_lcp){
                    //now for the lcp_add section
                    JSONObject product_options = new JSONObject( joei.optString("product_options") );

                    if(product_options.optJSONObject("info_buyRequest") != null){
                         lcp_add = product_options.optJSONObject("info_buyRequest").optString( "lcp_add" );
                    }
     
                    //if there is no apparent 'lcp_add' property, try looking for 'has_lcp'
                    if(lcp_add == null || lcp_add == ""){
                         // yes, a plain old string search
                         if(joei.optString("product_options").contains( "\"has_lcp\":1" ) ){
                              lcp_add = "1";
                         }
                    }
               }

               map.put(DeckOrderItemHeaders.LCP_ADD, lcp_add);
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
          List<List<String>> csv_rows = this.deckItems2CSVRows(deckMapOrderItems, DeckOrderItemHeaders.values(), this.is_first_iteration);

          if(!this.is_last_iteration){
               this.deckMapOrderItems = Stream.concat(this.deckMapOrderItems.stream(), csv_rows.stream())
               .collect(Collectors.toList());
          }else{
               //prepare and then save the CSV file
               this.prepareDeckCSVFile( this.created_at, this.deckMapOrderItems );
          }
     }
}
