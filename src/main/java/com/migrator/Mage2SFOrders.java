package com.migrator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

public class Mage2SFOrders extends JSONToXML {

    public Mage2SFOrders() throws IOException{
        super( "orders" );
    }

    public static void main(String...s) throws IOException{

        Mage2SFOrders mage2SFOrders = new Mage2SFOrders();
        
        String[] json_folder_arr = { Config.json_save_dir, mage2SFOrders.section };
        String json_folder = String.join(File.separator, json_folder_arr) + File.separator;

        System.out.println("json_folder = "+ json_folder);

        //String json_folder = abs_path + "/" + Config.base_save_dir + "/" + Config.json_save_subdir + "/orders/";
        //String[] xml_folder_arr = {Config.xml_save_subdir, mage2SFOrders.section};
        //String xml_folder = String.join(File.separator, xml_folder_arr) + File.separator;

        String json_filename = json_folder + "sample_mcstaging_orders.json";
        
        //String xml_filename = xml_folder + "sample_mcstaging_orders.xml";

        //get the data from the saved .json file of Magento orders.
        String json_data = ReadFromFile.contents( json_filename );

        JSONObject obj = new JSONObject(json_data);

        //the JSON form of the Salesforce data to export
        JSONObject sfData = mage2SFOrders.mage2SFObjOrders(obj);

        
        String xml_data = XML.toString(sfData);

        //hack to get rid of unwanted attributes in the closing tags
        String pattern = "(?i)(<\\/([\\w-]+)(.*?)>)";
        String xml_data_updated = xml_data.replaceAll(pattern, "</" + "$2" + ">");

        System.out.println(xml_data_updated);
    }

    public static JSONObject getCustomerName( JSONObject mage_order ){

        JSONObject customer_name = new JSONObject();

        String customer_firstname = "";
        String customer_lastname = "";

        JSONObject mage_shipping_assignment = null;

        //first, go for the default customer name set
        if( mage_order.has("customer_firstname") ){
            customer_firstname = mage_order.getString("customer_firstname");
        }

        if( mage_order.has("customer_lastname") ){
            customer_lastname = mage_order.getString("customer_lastname");
        }

        //if these attempts fail, then try the customer names from shipping
        if( customer_firstname == "" || customer_lastname == "" ){
            mage_shipping_assignment = mage_order
                .getJSONObject( "extension_attributes" )
                .getJSONArray( "shipping_assignments" )
                .getJSONObject(0)
                .getJSONObject( "shipping" )
                .getJSONObject( "address" );

            
            System.out.println( "mage_shipping_assignment = " );
            System.out.println( mage_shipping_assignment );

            if( customer_firstname == "" ){
                customer_firstname = mage_shipping_assignment.optString("firstname");
            }

            if( customer_lastname == "" ){
                customer_lastname = mage_shipping_assignment.optString("lastname");
            }
        }
        
        customer_name.put("firstname", customer_firstname)
                    .put("lastname", customer_lastname);

        return customer_name;
    }
    
    //populates order -> 'customer' tag for SF order
    public static JSONObject getSFCustomer( JSONObject mage_order ){

        //customer sub object
        JSONObject sf_order_customer = new JSONObject();

        JSONObjectArray soc_sorter = new JSONObjectArray();

        //customer id is not always there, like seemingly when it is a guest.
        String customer_id = mage_order.has("customer_id") ?
            mage_order.get("customer_id").toString() : "0";

        //sf_order_customer.put("customer-no", customer_id );
        soc_sorter.put( new JSONObject().put("customer-no", customer_id ) );

        JSONObject customer_name_obj = getCustomerName( mage_order );

        String customer_first_name = customer_name_obj.getString("firstname");
        String customer_last_name = customer_name_obj.getString("lastname");
        
        String customer_name = customer_first_name + " " + customer_last_name;

        soc_sorter.put( "customer-name", customer_name )
                .put( "customer-email", mage_order.get("customer_email") );


        /* ORDER -> CUSTOMER -> BILLING-ADDRESS SUB-SECTION */
        JSONObject sf_order_customer_billingAddress = new JSONObject();
        JSONObjectArray soc_ba_sorter = new JSONObjectArray();

        soc_ba_sorter.put( "first-name", customer_first_name )
                    .put( "last-name", customer_last_name );

        JSONObject temp_mage_ba = mage_order.getJSONObject("billing_address");
        JSONArray temp_mage_ba_street = temp_mage_ba.getJSONArray("street");

        soc_ba_sorter.put( "address1", temp_mage_ba_street.opt(0) )
                     .put( "city", temp_mage_ba.opt("city") )
                     .put( "postal-code", temp_mage_ba.opt("postcode") )
                     .put( "state-code", temp_mage_ba.opt("region_code") )
                     .put( "country-code", temp_mage_ba.opt("country_id") )
                     .put( "phone", temp_mage_ba.opt("telephone") );

        sf_order_customer_billingAddress.put("sorter", soc_ba_sorter );

        soc_sorter.put( "billing-address", sf_order_customer_billingAddress );

        sf_order_customer.put( "sorter", soc_sorter );

        /* END ORDER -> CUSTOMER -> BILLING-ADDRESS SUB-SECTION */

        return sf_order_customer;
    }

    //populates order -> 'product-lineitems' tag
    public static JSONObject getSFProductLineItems( JSONArray mage_order_product_items ){

        JSONArray sf_order_cart_items = new JSONArray();

        for(int j=0; j < mage_order_product_items.length(); j++){

            JSONObject mage_order_product_item = mage_order_product_items.getJSONObject(j);

            /*
            we only want actual ordered items. So when someone orders a 
            configurable product, there will then be 2 entries in the orders 
            items per configurable product.  But we only want one.

            So, then how is this achieved?  By only using items that do not both
            have a parent_item_id and also are not with a 'row_total' value of 
            zero.
            */
            double row_total = mage_order_product_item.getDouble("row_total");

            if( mage_order_product_item.has("parent_item_id") && row_total < 1){

                //move to the next iteration, because this quote item is a duplicate of another
                continue;
            }

            JSONObject sf_order_cart_item = new JSONObject();

            JSONObjectArray sf_order_cart_item_sorter = new JSONObjectArray();

            sf_order_cart_item_sorter.put( "net-price", mage_order_product_item.get("base_price") )
                      .put( "tax", mage_order_product_item.get("tax_amount") )
                      .put( "gross-price", mage_order_product_item.get("base_price_incl_tax") )
                      .put( "base-price", mage_order_product_item.get("base_price") )
                      .put( "lineitem-text", mage_order_product_item.get("name") )
                      .put( "tax-basis", mage_order_product_item.get("base_price") )
                      .put( "position", j+1 )
                      .put( "product-id", mage_order_product_item.get("product_id") )
                      .put( "product-name", mage_order_product_item.get("name") )
                      .put( "quantity", mage_order_product_item.get("qty_ordered") );

            double temp_tax_rate = mage_order_product_item.getDouble("base_tax_amount") /
                                   mage_order_product_item.getDouble("base_price");

            sf_order_cart_item_sorter.put( "tax-rate", temp_tax_rate );

            sf_order_cart_item.put("sorter", sf_order_cart_item_sorter);

            //add this cart item to the product line items array
            sf_order_cart_items.put(sf_order_cart_item );
        }

        //add the product line items array to the product line item object
        JSONObject sf_order_productLineItems = new JSONObject();
        sf_order_productLineItems.put("product-lineitem", sf_order_cart_items );

        return sf_order_productLineItems;
    }

    public static JSONObject getSFShippingLineItems( JSONArray mage_shipping_assignments ){
        
        JSONArray sf_order_shipping_items = new JSONArray();

        for(int j=0; j < mage_shipping_assignments.length(); j++){
            JSONObject mage_shipping_assignment = mage_shipping_assignments.getJSONObject(j);

            //where most of the data for this section comes from
            JSONObject mage_shipping = mage_shipping_assignment.getJSONObject("shipping");
            JSONObject mage_shipping_total = mage_shipping.getJSONObject("total");

            JSONObject first_product_item = mage_shipping_assignment
                                            .getJSONArray("items")
                                            .getJSONObject(0);

            String delivery_type = first_product_item
                                    .getJSONObject("extension_attributes")
                                    .getString("delivery_type");
            
            JSONObject sf_shipping_line_item = new JSONObject();

            JSONObjectArray ssli_sorter = new JSONObjectArray();

            ssli_sorter.put( "net-price", mage_shipping_total.get("base_shipping_incl_tax") )
                        .put( "tax", mage_shipping_total.get("shipping_tax_amount") )
                        .put( "gross-price", mage_shipping_total.get("shipping_amount") )
                        .put( "base-price", mage_shipping_total.get("base_shipping_amount") )
                        .put( "lineitem-text", delivery_type )
                        .put( "tax-basis", mage_shipping_total.get("base_shipping_tax_amount") )
                        .put( "item-id", mage_shipping.get("method") );

            //sometimes this is zero. Since we all know that dividing by zero causes disasters, we do this...
            Double tax_rate = mage_shipping_total.getDouble("shipping_tax_amount") == 0 ?
                0 :
                mage_shipping_total.getDouble("shipping_tax_amount") / 
                mage_shipping_total.getDouble("shipping_amount");

            ssli_sorter.put( "tax-rate", tax_rate );

            sf_shipping_line_item.put("sorter", ssli_sorter );

            //add the shipping line item to its array
            sf_order_shipping_items.put(sf_shipping_line_item );
        }

        JSONObject sf_order_shippingLineItems = new JSONObject();
        sf_order_shippingLineItems.put("shipping-lineitem", sf_order_shipping_items );

        return sf_order_shippingLineItems;
    }

    public static JSONObject getShipments(JSONObject mage_order){

        JSONObject sf_order_shipments = new JSONObject();

        JSONObjectArray sos_sorter = new JSONObjectArray();

        String order_status = mage_order.getString("status");
        String shipping_status_str = "{'shipping-status':'" + order_status + "'}}";

        JSONObject shipping_status = new JSONObject(shipping_status_str);

        String shipping_description = mage_order.getString("shipping_description");

        sos_sorter.put("status", shipping_status )
                .put("shipping-method", shipping_description );

        
        String shipment_id = null;

        if( shipping_description.contains("Delivery") ){
            shipment_id = "00000001";
        }else{
            shipment_id = "00000002";
        }

        JSONObject mage_shipping_assignment = mage_order
                             .getJSONObject( "extension_attributes" )
                             .getJSONArray( "shipping_assignments" )
                             .getJSONObject(0);

        sos_sorter.put("shipping-address", getShippingAddress(mage_shipping_assignment) )
                    .put("totals", getTotals(mage_order) );

          
        sf_order_shipments.put("sorter", sos_sorter).put("shipment-id", shipment_id);

        return sf_order_shipments;
    }

    public static JSONObject getShippingAddress( JSONObject mage_shipping_assignment ){

        JSONObject sf_shipping_address = new JSONObject();

        JSONObjectArray ssa_sorter = new JSONObjectArray();

        JSONObject mage_shipping_address = mage_shipping_assignment
                                            .getJSONObject("shipping")
                                            .getJSONObject("address");

        ssa_sorter.put("first-name", mage_shipping_address.get("firstname"))
                  .put("last-name", mage_shipping_address.get("lastname"))
                  .put("address1", mage_shipping_address.getJSONArray("street").get(0) )
                  .put("city", mage_shipping_address.get("city"))
                  .put("postal-code", mage_shipping_address.get("postcode"))
                  .put("state-code", mage_shipping_address.get("region_code"))
                  .put("country-code", mage_shipping_address.get("country_id"))
                  .put("phone", mage_shipping_address.get("telephone"));

        sf_shipping_address.put("sorter", ssa_sorter);

        return sf_shipping_address;
    }

    public static JSONObject getTotals(JSONObject mage_order){

        JSONObjectArray sf_order_totals_sorter = new JSONObjectArray();
        JSONObject sf_order_totals = new JSONObject();

        JSONObjectArray merchandizeTotal_sorter = new JSONObjectArray();
        JSONObject merchandizeTotal = new JSONObject();

        JSONObjectArray shippingTotal_sorter = new JSONObjectArray();
        JSONObject shippingTotal = new JSONObject();

        JSONObjectArray orderTotal_sorter = new JSONObjectArray();
        JSONObject orderTotal = new JSONObject();

        merchandizeTotal_sorter.put("net-price", mage_order.opt("base_subtotal") )
                            .put("tax", mage_order.opt("base_tax_amount") )
                            .put("gross-price", mage_order.opt("base_subtotal_incl_tax") );

        merchandizeTotal.put("sorter", merchandizeTotal_sorter);
        
        
        shippingTotal_sorter.put("net-price", mage_order.opt("base_shipping_amount") )
                            .put("tax", mage_order.opt("base_shipping_tax_amount") )
                            .put("gross-price", mage_order.opt("base_shipping_incl_tax") );

        shippingTotal.put("sorter", shippingTotal_sorter);


        orderTotal_sorter.put("net-price", mage_order.opt("subtotal") )
                        .put("tax", mage_order.opt("tax_amount") )
                        .put("gross-price", mage_order.opt("total_due") );

        orderTotal.put("sorter", orderTotal_sorter);

        sf_order_totals_sorter.put( "merchandize-total", merchandizeTotal)
                        .put( "shipping-total", shippingTotal)
                        .put( "order-total", orderTotal);

        sf_order_totals.put("sorter", sf_order_totals_sorter);


        return sf_order_totals;
    }

    public static JSONObject getPayments(JSONObject mage_order){

        JSONArray payment_additional_info = mage_order
                                    .getJSONObject("extension_attributes")
                                    .getJSONArray("payment_additional_info");

        JSONObject mage_payment = mage_order.getJSONObject("payment");

        String card_type = null;
        String card_number = "";

        String card_holder = mage_order
            .getJSONObject("billing_address")
            .optString("firstname");

        card_holder += " " + mage_order
            .getJSONObject("billing_address")
            .optString("lastname");


        Iterator<Object> iterator = payment_additional_info.iterator();
        while (iterator.hasNext()) {
            JSONObject ix = (JSONObject)iterator.next();

            String k = ix.getString("key");
            String v = ix.getString("value");

            //get the payment method type
            if( k.contains( "score_card_scheme" ) ){
                card_type = v;
            }

            //failsafe 1
            if( card_type == null && k.contains( "afsReply_cardScheme" ) ){
                card_type = v;
            }

            //failsafe 2
            if( card_type == null && k.contains( "score_card_account_type" ) ){
                card_type = v;
            }

            //failsafe 3
            if( card_type == null && k.contains( "afsReply_cardAccountType" ) ){
                card_type = v;
            }

            /*
            failsafe 4. Probably best for when customer used check / money 
            order or something that is not standard credit card.
            */
            if( card_type == null && k.contains( "method_title" ) ){
                card_type = v;
            }

            //get the last 4 of credit card number if available
            if( k.contains( "last4" ) ){
                try {
                    double d = Double.parseDouble(v);

                    card_number = "" + d;
                } catch (NumberFormatException nfe) {
                    card_number = null;
                }
            }

            //or get it this way if available
            if( k.contains( "cardNumber" ) ){
                card_number = v == null ? null : v;
            }
        }

        JSONObjectArray sf_payments_sorter = new JSONObjectArray();
        JSONObject sf_payments = new JSONObject();

        JSONObjectArray credit_card_sorter = new JSONObjectArray();
        JSONObject credit_card = new JSONObject();

        //the credit card number can actually come from more than one possible place in the source data.
        String final_card_number = card_number == null || card_number == "" ?
                                    mage_payment.optString("cc_last4") :
                                    card_number;

        //for when the card number does not have enough characters, like when it is the last 4 of the actual CC
        if( final_card_number.length() < 8 ){
            final_card_number = "XXXX-XXXX-XXXX-" + final_card_number;
        }

        credit_card_sorter.put("card-type", card_type)
                .put("card-number", final_card_number)
                .put("card-holder", card_holder)
                .put("expiration-month", mage_payment.opt("cc_exp_month") )
                .put("expiration-year", mage_payment.opt("cc_exp_year") );


        credit_card.put("sorter", credit_card_sorter);

        sf_payments_sorter.put("credit-card", credit_card )
                    .put("amount", mage_payment.opt("amount_ordered") )
                    .put("processor-id", mage_payment.opt("method") )
                    .put("transaction-id", mage_payment.opt("cc_trans_id") );

        
        sf_payments.put("sorter", sf_payments_sorter);

        JSONObject payments = new JSONObject().put("payment", sf_payments);

        return payments;
    }

    /*
    The Magento data has a different schema than Salesforce, even in spite of 
    differing formats.  So it must first be converted to SF.
    */
    public JSONObject mage2SFObjOrders( JSONObject obj ) throws IOException{
        
        //final output object
        JSONObject sfData = new JSONObject();

        //an array where each immediate child element is an 'order' element. final output object.
        JSONObject sfData_orders = new JSONObject();

        //each "item" represents an order, not a SF 'product line item' or magento 'quote' item
        JSONArray items = obj.getJSONArray("items");

        String first_order_created_at = "";

        //loop through the original Magento API order items list
        for (int i = 0; i < items.length(); i++) {
            JSONObject mage_order = items.getJSONObject(i);

            JSONObject sf_order = new JSONObject();

            JSONObjectArray sorter = new JSONObjectArray();

            //get the creation date of the first order, then retain for later
            if(i == 0){
                first_order_created_at = mage_order.getString("created_at");

                //can not have spaces in XML 'xmlns' attributes
                first_order_created_at = first_order_created_at.replace(" ", "_");
            }

            sorter.put( "order-date", mage_order
                                            .getString("created_at")
                                            .replace(" ", "T") + "Z"
                )
                .put( "created-by", "storefront" )
                .put( "original-order-no", mage_order.get("entity_id") )
                .put( "currency", mage_order.get("base_currency_code") )
                .put( "customer-locale", "default" )
                .put( "taxation", "net" )
                .put( "invoice-no", mage_order.get("increment_id") );

            //customer sub-section of the order
            JSONObject sf_order_customer = getSFCustomer( mage_order );
            sorter.put( "customer", sf_order_customer );

 
            /* status sub-section of the order */
            JSONObject sf_order_status = new JSONObject();
            JSONObjectArray sf_order_status_sorter = new JSONObjectArray();

            sf_order_status_sorter.put( "order-status", mage_order.get("status") );


            String[] complete_arr = { "complete", "pending", "sent_to_fulfillment" };

            Object cs_val = null;

            if( Arrays.asList(complete_arr).contains( mage_order.getString("status") ) ){ 
                cs_val = "CONFIRMED";
            }else{
                cs_val = mage_order.getString("status");
            }

            sf_order_status_sorter.put("confirmation-status", cs_val )
                                .put( "export-status", "EXPORTED" );

            sf_order_status.put( "sorter", sf_order_status_sorter );

            sorter.put( "status", sf_order_status );
            /* end status sub-section of the order */

            sorter.put( "current-order-no", mage_order.get("entity_id") );

            /* ORDER -> PRODUCT LINE ITEMS SUB-SECTION */
            JSONArray mage_order_product_items = mage_order.getJSONArray("items");

            JSONObject sf_order_productLineItems = getSFProductLineItems( mage_order_product_items );

            sorter.put( "product-lineitems", sf_order_productLineItems );
            /* END ORDER -> PRODUCT LINE ITEMS SUB-SECTION */

            /* ORDER -> SHIPPING LINE ITEMS SUB-SECTION */
            //String shipping_description = mage_order.get("shipping_description").toString();

            JSONArray sa = mage_order
                           .getJSONObject("extension_attributes")
                           .getJSONArray("shipping_assignments");

            JSONObject sf_order_shippingLineItems = getSFShippingLineItems(sa);

            sorter.put( "shipping-lineitems", sf_order_shippingLineItems );
            /* END ORDER -> SHIPPING LINE ITEMS SUB-SECTION */

            JSONObject mage_shipments = getShipments(mage_order);

            String shipment_id = mage_shipments.getString( "shipment-id" );

            //now remove the shipment-id property as it is no longer needed or wanted for the overall xml
            mage_shipments.remove("shipment-id");

            //hack needed for XML attributes, as I did not use a fancy XML library like Jackson
            String shipment_keyval = "shipment shipment-id=\""+ shipment_id +"\"";

            JSONObject sf_shipments = new JSONObject().put(shipment_keyval, mage_shipments);
            sorter.put( "shipments", sf_shipments )
                    .put( "totals", getTotals(mage_order) )
                    .put( "payments", getPayments(mage_order) );


            sf_order.put( "sorter", sorter );

            //attach the order to its array
            sfData_orders.put( "order order-no=\"" + mage_order.get("entity_id") + "\"", sf_order );
        }

        //for the orders main tag
        String order_xmlns = "https://www.demandware.com/xml/impex/order/" + first_order_created_at;
        sfData.put("orders xmlns=\""+ order_xmlns +"\"", sfData_orders);

        return sfData;
    }

}
