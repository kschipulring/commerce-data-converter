package com.migrator;
import java.io.IOException;

//import org.json.JSONArray;
import org.json.JSONObject;

public class MagentoOrderGetter extends MagentoDataGetter{

    protected JSONObject jsonObject = null;
    
    //constructor
    public MagentoOrderGetter(Integer mage_max_per_page) throws IOException {
        super( mage_max_per_page );
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
