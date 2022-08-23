package com.migrator;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.annotation.Nullable;

import org.json.JSONObject;

public class MagentoOrderGetter extends MagentoDataGetter{

    protected JSONObject jsonObject = null;
    
    //constructor
    public MagentoOrderGetter(Integer mage_max_per_page, @Nullable String env) throws IOException {
        super( mage_max_per_page, env);
    }

    public void setEndpointExtras(){
        String endpoint_extras = "";

        //don't want guests, as they have no customer_id
        endpoint_extras += "&searchCriteria[filter_groups][0][filters][0][field]=customer_is_guest";
        endpoint_extras += "&searchCriteria[filter_groups][0][filters][0][value]=0";
        endpoint_extras += "&searchCriteria[filter_groups][0][filters][0][condition_type]=eq";

        //must at also have a customer id
        endpoint_extras += "&searchCriteria[filter_groups][1][filters][0][field]=customer_id";
        endpoint_extras += "&searchCriteria[filter_groups][1][filters][0][value]=null";
        endpoint_extras += "&searchCriteria[filter_groups][1][filters][0][condition_type]=neq";

        //must at least have a populated increment_id field
        endpoint_extras += "&searchCriteria[filter_groups][2][filters][0][field]=increment_id";
        endpoint_extras += "&searchCriteria[filter_groups][2][filters][0][value]=null";
        endpoint_extras += "&searchCriteria[filter_groups][2][filters][0][condition_type]=neq";

        //assuming that these are populated from the command line or perhaps elsewhere. By default, they are not.
        String date_from = this.date_from;
        String date_to = this.date_to;

        M2SSystem.println("date_from is " + date_from);
        M2SSystem.println("date_to is " + date_to);

        //to potentially create new date strings
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        //used to get todays actual date. Also usable later for a past date relative to this.
        Date today = new Date();

        //the if block here normally goes unless the 'date_from' command line parameter is populated 
        if( date_from == null || date_from.length() < 4 ){

            //date from 18 months before present
            Calendar cal = new GregorianCalendar();
            cal.setTime(today);
            cal.add(Calendar.MONTH, -18);
            Date prior18months = cal.getTime();
            M2SSystem.println(prior18months);

            //18 months ago date string
            date_from = dateFormat.format(prior18months);
        }
        
        if( date_to == null || date_to.length() < 4 ){
            //today string
            date_to = dateFormat.format(today);
        }

        M2SSystem.println("date_from is " + date_from);
        M2SSystem.println("date_to is " + date_to);

        endpoint_extras += "&searchCriteria[filter_groups][3][filters][0][field]=created_at";
        endpoint_extras += "&searchCriteria[filter_groups][3][filters][0][value]=" + date_from;
        endpoint_extras += "&searchCriteria[filter_groups][3][filters][0][condition_type]=from";

        endpoint_extras += "&searchCriteria[filter_groups][4][filters][0][field]=created_at";
        endpoint_extras += "&searchCriteria[filter_groups][4][filters][0][value]=" + date_to;
        endpoint_extras += "&searchCriteria[filter_groups][4][filters][0][condition_type]=to";

        //whitelist of returned fields for orders
        endpoint_extras += "&items[entity_id,base_currency_code,base_discount_amount,created_at,";
        endpoint_extras += "increment_id,customer_id,customer_firstname,customer_lastname,";
        endpoint_extras += "customer_email,billing_address[firstname,lastname,street,city,postcode,";
        endpoint_extras += "region_code,country_id,telephone],status,items[base_price,tax_amount,";
        endpoint_extras += "base_tax_amount,base_price_incl_tax,extension_attributes[delivery_type,";
        endpoint_extras += "is_lcp,delivery_date,product_options],name,product_id,sku,qty_ordered],";
        endpoint_extras += "extension_attributes[sameday_fee,shipping_assignments[shipping[address,";
        endpoint_extras += "method,total[base_shipping_incl_tax,shipping_tax_amount,";
        endpoint_extras += "shipping_amount,base_shipping_amount,base_shipping_tax_amount,";
        endpoint_extras += "shipping_amount]]],payment_additional_info],base_subtotal,";
        endpoint_extras += "base_tax_amount,base_subtotal_incl_tax,base_shipping_amount,";
        endpoint_extras += "base_shipping_tax_amount,base_shipping_incl_tax,subtotal,";
        endpoint_extras += "tax_amount,total_due,payment_additional_info,";
        endpoint_extras += "payment[cc_exp_month,cc_exp_year,amount_ordered,method],shipping_description]";

        //want to know how many possible records are available, across all possible pages of results
        endpoint_extras += ",total_count";

        this.setEndpointExtras(endpoint_extras);
    }

    public JSONObject getOrdersJson(int... cp) throws IOException, InterruptedException
    {
        
        //if no specified current page, start it with zero
        int current_page = cp.length > 0 ? cp[0] : this.current_page;

        //overridden from the parameter
        this.current_page = current_page;

        //from either the REST API or the 'saved_files' cache
        String json_str = this.getJSONstring("orders");

        JSONObject jsonObject = new JSONObject( json_str );

        return jsonObject;
    }
}
