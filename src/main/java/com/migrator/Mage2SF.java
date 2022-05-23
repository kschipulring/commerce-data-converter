package com.migrator;

import java.io.File;
import java.io.IOException;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

public class Mage2SF extends JSONToXML {

    public Mage2SF() throws IOException{
        super( "orders" );
    }

    public static void main(String...s) throws IOException{

        Mage2SF mage2SF = new Mage2SF();
        
        String[] json_folder_arr = { Config.json_save_dir, mage2SF.section };
        String json_folder = String.join(File.separator, json_folder_arr) + File.separator;

        System.out.println("json_folder = "+ json_folder);

        //String json_folder = abs_path + "/" + Config.base_save_dir + "/" + Config.json_save_subdir + "/orders/";
        String[] xml_folder_arr = {Config.xml_save_subdir, mage2SF.section};
        String xml_folder = String.join(File.separator, xml_folder_arr) + File.separator;

        String json_filename = json_folder + "sample_mcstaging_orders.json";
        
        String xml_filename = xml_folder + "sample_mcstaging_orders.xml";

        //get the data from the saved .json file of Magento orders.
        String json_data = ReadFromFile.contents( json_filename );

        JSONObject obj = new JSONObject(json_data);

        //the JSON form of the Salesforce data to export
        JSONObject sfData = mage2SF.mage2SFObj(obj);

        
        String xml_data = XML.toString(sfData);

        //hack to get rid of unwanted attributes in the closing tags
        String pattern = "(?i)(<\\/([\\w-]+)(.*?)>)";
        String xml_data_updated = xml_data.replaceAll(pattern, "</" + "$2" + ">");

        System.out.println(xml_data_updated);
    }
    
    //populates order -> 'customer' tag for SF order
    public static JSONObject getSFCustomer( JSONObject mage_order ){

        //customer sub object
        JSONObject sf_order_customer = new JSONObject();

        //customer id is not always there, like seemingly when it is a guest.
        String customer_id = mage_order.has("customer_id") ?
            mage_order.get("customer_id").toString() : "0";

        sf_order_customer.put("customer-no", customer_id );

        String customer_first_name = mage_order.has("customer_firstname") ? 
            mage_order.get("customer_firstname").toString() : "";
        
        String customer_last_name = mage_order.has("customer_lastname") ? 
            mage_order.get("customer_lastname").toString() : "";
        
        String customer_name = customer_first_name + " " + customer_last_name;

        sf_order_customer.put("customer-name", customer_name );
        sf_order_customer.put("customer-email", mage_order.get("customer_email") );


        /* ORDER -> CUSTOMER -> BILLING-ADDRESS SUB-SECTION */
        JSONObject sf_order_customer_billingAddress = new JSONObject("{}");

        sf_order_customer_billingAddress.put("first-name", customer_first_name );
        sf_order_customer_billingAddress.put("last-name", customer_last_name );

        JSONObject temp_mage_ba = mage_order.getJSONObject("billing_address");
        JSONArray temp_mage_ba_street = temp_mage_ba.getJSONArray("street");

        sf_order_customer_billingAddress.put("address1", temp_mage_ba_street.get(0) );

        sf_order_customer_billingAddress.put("city", temp_mage_ba.get("city") );
        sf_order_customer_billingAddress.put("postal-code", temp_mage_ba.get("postcode") );
        sf_order_customer_billingAddress.put("state-code", temp_mage_ba.get("region_code") );
        sf_order_customer_billingAddress.put("country-code", temp_mage_ba.get("country_id") );
        sf_order_customer_billingAddress.put("phone", temp_mage_ba.get("telephone") );

        sf_order_customer.put("billing-address", sf_order_customer_billingAddress);

        /* END ORDER -> CUSTOMER -> BILLING-ADDRESS SUB-SECTION */

        return sf_order_customer;
    }

    //populates order -> 'product-lineitems' tag
    public static JSONObject getSFProductLineItems( JSONArray mage_order_cart_items ){

        JSONArray sf_order_cart_items = new JSONArray();

        for(int j=0; j < mage_order_cart_items.length(); j++){

            JSONObject mage_order_cart_item = mage_order_cart_items.getJSONObject(j);

            /*
            we only want actual ordered items. So when someone orders a 
            configurable product, there will then be 2 entries in the orders 
            items per configurable product.  But we only want one.

            So, then how is this achieved?  By only using items that do not both
            have a parent_item_id and also are not with a 'row_total' value of 
            zero.
            */
            double row_total = mage_order_cart_item.getDouble("row_total");

            if( mage_order_cart_item.has("parent_item_id") && row_total < 1){

                //move to the next iteration, because this quote item is a duplicate of another
                continue;
            }

            JSONObject sf_order_cart_item = new JSONObject();

            sf_order_cart_item.put( "net-price", mage_order_cart_item.get("base_price") );
            sf_order_cart_item.put( "tax", mage_order_cart_item.get("tax_amount") );
            sf_order_cart_item.put( "gross-price", mage_order_cart_item.get("base_price_incl_tax") );
            sf_order_cart_item.put( "base-price", mage_order_cart_item.get("base_price") );
            sf_order_cart_item.put( "lineitem-text", mage_order_cart_item.get("name") );
            sf_order_cart_item.put( "tax-basis", mage_order_cart_item.get("base_price") );
            sf_order_cart_item.put( "position", j+1 );
            sf_order_cart_item.put( "product-id", mage_order_cart_item.get("product_id") );
            sf_order_cart_item.put( "product-name", mage_order_cart_item.get("name") );
            sf_order_cart_item.put( "quantity", mage_order_cart_item.get("qty_ordered") );

            double temp_tax_rate = mage_order_cart_item.getDouble("base_tax_amount") /
                                   mage_order_cart_item.getDouble("base_price");

            sf_order_cart_item.put("tax-rate", temp_tax_rate);

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

            JSONObject first_product_item = mage_shipping_assignment.getJSONArray("items").getJSONObject(0);

            String delivery_type = (String)first_product_item.getJSONObject("extension_attributes").get("delivery_type");
            
            JSONObject sf_shipping_line_item = new JSONObject();


            sf_shipping_line_item.put( "net-price", mage_shipping_total.get("base_shipping_incl_tax") );
            sf_shipping_line_item.put( "tax", mage_shipping_total.get("shipping_tax_amount") );
            sf_shipping_line_item.put( "gross-price", mage_shipping_total.get("shipping_amount") );
            sf_shipping_line_item.put( "base-price", mage_shipping_total.get("base_shipping_amount") );

            sf_shipping_line_item.put( "lineitem-text", delivery_type );

            sf_shipping_line_item.put( "tax-basis", mage_shipping_total.get("base_shipping_tax_amount") );
            sf_shipping_line_item.put( "item-id", mage_shipping.get("method") );

            //sometimes this is zero. Since we all know that dividing by zero causes disasters, we do this...
            Double tax_rate = mage_shipping_total.getDouble("shipping_tax_amount") == 0 ?
                0 :
                mage_shipping_total.getDouble("shipping_tax_amount") / 
                mage_shipping_total.getDouble("shipping_amount");

            sf_shipping_line_item.put( "tax-rate", tax_rate );

            //add the shipping line item to its array
            sf_order_shipping_items.put(sf_shipping_line_item );
        }

        JSONObject sf_order_shippingLineItems = new JSONObject();
        sf_order_shippingLineItems.put("shipping-lineitem", sf_order_shipping_items );

        return sf_order_shippingLineItems;
    }

    public static JSONObject getShipments(JSONObject mage_order){

        JSONObject sf_order_shipments = new JSONObject();

        String order_status = mage_order.getString("status");
        String shipping_status_str = "{'shipping-status':'" + order_status + "'}}";

        JSONObject shipping_status = new JSONObject(shipping_status_str);

        sf_order_shipments.put("status", shipping_status );
        sf_order_shipments.put("shipping-method", mage_order.getString("shipping_description") );

        JSONObject mage_shipping_assignment = mage_order
                             .getJSONObject( "extension_attributes" )
                             .getJSONArray( "shipping_assignments" )
                             .getJSONObject(0);

        sf_order_shipments.put("shipping-address", getShippingAddress(mage_shipping_assignment) );

        sf_order_shipments.put("totals", getTotals(mage_order) );

        return sf_order_shipments;
    }

    public static JSONObject getShippingAddress( JSONObject mage_shipping_assignment ){

        JSONObject sf_shipping_address = new JSONObject();

        JSONObject mage_shipping_address = mage_shipping_assignment
                                            .getJSONObject("shipping")
                                            .getJSONObject("address");

        sf_shipping_address.put("first-name", mage_shipping_address.get("firstname"));
        sf_shipping_address.put("last-name", mage_shipping_address.get("lastname"));
        sf_shipping_address.put("address1", mage_shipping_address.getJSONArray("street").get(0) );
        sf_shipping_address.put("city", mage_shipping_address.get("city"));
        sf_shipping_address.put("postal-code", mage_shipping_address.get("postcode"));
        sf_shipping_address.put("state-code", mage_shipping_address.get("region_code"));
        sf_shipping_address.put("country-code", mage_shipping_address.get("country_id"));
        sf_shipping_address.put("phone", mage_shipping_address.get("telephone"));

        return sf_shipping_address;
    }

    public static JSONObject getTotals(JSONObject mage_order){

        JSONObject sf_order_totals = new JSONObject();

        JSONObject merchandizeTotal = new JSONObject();
        JSONObject shippingTotal = new JSONObject();
        JSONObject orderTotal = new JSONObject();

        merchandizeTotal.put("net-price", mage_order.get("base_subtotal") );
        merchandizeTotal.put("tax", mage_order.get("base_tax_amount") );
        merchandizeTotal.put("gross-price", mage_order.get("base_subtotal_incl_tax") );

        shippingTotal.put("net-price", mage_order.get("base_shipping_amount") );
        shippingTotal.put("tax", mage_order.get("base_shipping_tax_amount") );
        shippingTotal.put("gross-price", mage_order.get("base_shipping_incl_tax") );

        orderTotal.put("net-price", mage_order.get("subtotal") );
        orderTotal.put("tax", mage_order.get("tax_amount") );
        orderTotal.put("gross-price", mage_order.get("total_due") );

        sf_order_totals.put( "merchandize-total", merchandizeTotal);
        sf_order_totals.put( "shipping-total", shippingTotal);
        sf_order_totals.put( "order-total", orderTotal);

        return sf_order_totals;
    }

    public static JSONObject getPayments(JSONObject mage_order){

        JSONArray payment_additional_info = mage_order
        .getJSONObject("extension_attributes")
        .getJSONArray("payment_additional_info");

        JSONObject mage_payment = mage_order.getJSONObject("payment");

        String card_type = "";
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
            if( k.contains( "method_title" ) ){
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

        JSONObject sf_payments = new JSONObject();

        JSONObject credit_card = new JSONObject();

        //the credit card number can actually come from more than one possible place in the source data.
        String final_card_number = card_number == null || card_number == "" ?
                                    mage_payment.optString("cc_last4") :
                                    card_number;

        //for when the card number does not have enough characters, like when it is the last 4 of the actual CC
        if( final_card_number.length() < 8 ){
            final_card_number = "XXXX-XXXX-XXXX-" + final_card_number;
        }

        credit_card.put("card-type", card_type);
        credit_card.put("card-number", final_card_number);
        credit_card.put("card-holder", card_holder);
        credit_card.put("expiration-month", mage_payment.opt("cc_exp_month") );
        credit_card.put("expiration-year", mage_payment.opt("cc_exp_year") );

        sf_payments.put("credit-card", credit_card );
        sf_payments.put("amount", mage_payment.get("amount_ordered") );
        sf_payments.put("processor-id", mage_payment.get("method") );

        JSONObject payments = new JSONObject();
        payments.put("payment", sf_payments);

        return payments;
    }

    /*
    The Magento data has a different schema than Salesforce, even in spite of 
    differing formats.  So it must first be converted to SF.
    */
    public JSONObject mage2SFObj( JSONObject obj ) throws IOException{

        //final output object
        JSONObject sfData = new JSONObject();

        //an array where each immediate child element is an 'order' element
        JSONObject sfData_orders = new JSONObject();

        //each "item" represents an order, not a SF 'product line item' or magento 'quote' item
        JSONArray items = obj.getJSONArray("items");

        String first_order_created_at = "";

        //loop through the original Magento API order items list
        for (int i = 0; i < items.length(); i++) {
            JSONObject mage_order = items.getJSONObject(i);

            JSONObject sf_order = new JSONObject();

            //get the creation date of the first order, then retain for later
            if(i == 0){
                first_order_created_at = mage_order.getString("created_at");

                //can not have spaces in XML 'xmlns' attributes
                first_order_created_at = first_order_created_at.replace(" ", "_");
            }

            sf_order.put("order-date", mage_order.get("created_at") );
            sf_order.put("original-order-no", mage_order.get("entity_id") );
            sf_order.put("currency", mage_order.get("base_currency_code") );
            sf_order.put("invoice-no", mage_order.get("increment_id") );
            sf_order.put("current-order-no", mage_order.get("entity_id") );

            //customer sub-section of the order
            JSONObject sf_order_customer = getSFCustomer( mage_order );
            sf_order.put("customer", sf_order_customer );


            /* status sub-section of the order */
            JSONObject sf_order_status = new JSONObject();
            sf_order_status.put("order-status", mage_order.get("status") );

            sf_order.put("status", sf_order_status );


            /* ORDER -> PRODUCT LINE ITEMS SUB-SECTION */
            JSONArray mage_order_cart_items = mage_order.getJSONArray("items");

            JSONObject sf_order_productLineItems = getSFProductLineItems( mage_order_cart_items );

            sf_order.put("product-lineitems", sf_order_productLineItems );
            /* END ORDER -> PRODUCT LINE ITEMS SUB-SECTION */

            /* ORDER -> SHIPPING LINE ITEMS SUB-SECTION */
            //String shipping_description = mage_order.get("shipping_description").toString();

            JSONArray sa = mage_order
                           .getJSONObject("extension_attributes")
                           .getJSONArray("shipping_assignments");

            JSONObject sf_order_shippingLineItems = getSFShippingLineItems(sa);

            sf_order.put("shipping-lineitems", sf_order_shippingLineItems );
            /* END ORDER -> SHIPPING LINE ITEMS SUB-SECTION */


            JSONObject sf_shipments = getShipments(mage_order);
            sf_order.put("shipments", sf_shipments );

            sf_order.put("totals", getTotals(mage_order) );

            sf_order.put("payments", getPayments(mage_order) );

            //attach the order to its array
            sfData_orders.put( "order order-no=\"" + mage_order.get("entity_id") + "\"", sf_order );
        }

        //for the orders main tag
        String order_xmlns = "https://www.demandware.com/xml/impex/order/" + first_order_created_at;
        sfData.put("orders xmlns=\""+ order_xmlns +"\"", sfData_orders);

        return sfData;
    }
}
