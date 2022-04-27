package com.migrator;

import java.io.IOException;
//import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
//import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

//import org.json.JSONArray;
import org.json.JSONObject;


abstract public class MagentoDataGetter {

    protected final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    
    protected String mage_api_base_url = null;
    protected String mage_auth_token = null;
    
    public MagentoDataGetter( ) throws IOException
    {

        Config.getInstance();
        
        this.mage_api_base_url = Config.mage_api_base_url;
        this.mage_auth_token = Config.mage_auth_token;
    }

    public String getRequest(String endpoint) throws IOException, InterruptedException
    {

        //which URL provides the data?
        String request_url = mage_api_base_url + endpoint;
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(request_url))
                .setHeader("User-Agent", "Java 11 Magento API order reader with HttpClient")
                .setHeader("Authorization", "Bearer " + this.mage_auth_token)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print response headers
        /*HttpHeaders headers = response.headers();
        headers.map().forEach((k, v) -> System.out.println(k + ":" + v));*/

        // print status code
        //System.out.println(response.statusCode());

        // print response body
        //System.out.println(response.body());
       
        return response.body();
    }
}
