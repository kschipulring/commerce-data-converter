package com.migrator;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONObjectArray extends JSONArray {

    public JSONObjectArray put(String key, Object value){
        super.put( new JSONObject().put(key, value) );

        return this;
    }
}
