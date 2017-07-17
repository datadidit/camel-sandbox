package org.datadidit.camel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

/**
 * Take in JSON and enhance the JSON w/ 
 * GEO data
 *
 */
public class GeoEnrichmentProcessor implements Processor{
	private String apiKey; 
	
	private String fields; 
	
	private String geoJsonKey;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private static Map<String, GeocodingResult[]> cache = new HashMap<>();
	
	private static GeoApiContext context;

	public GeoEnrichmentProcessor(String apiKey, String fields, String geoJsonKey){
		setApiKey(apiKey);
		setFields(fields);
		this.setGeoJsonKey(geoJsonKey);
		context = new GeoApiContext().setApiKey(apiKey);
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		InputStream stream = exchange.getIn().getBody(InputStream.class);
	
		//Convert Input into Data Structure representing Json
		Object jsonDS = this.stringToJson(stream);
		
		List<Map<String, Object>> output = new ArrayList<>();
		
		if(jsonDS instanceof List){
			List<Map<String,Object>> incomingData = (List<Map<String, Object>>) jsonDS;
			
			for(Map<String, Object> entry : incomingData){
				output.add(this.addGeo(entry, fields, this.geoJsonKey));
			}
		}else{
			Map<String, Object> temp = (Map<String, Object>) jsonDS;
			output.add(this.addGeo(temp, fields, this.getGeoJsonKey()));
		}
		
		exchange.getIn().setBody(output);
	}
	
	public Object stringToJson(InputStream stream) throws IOException{        
        Map<String,Object> map = new HashMap<>();
		
        String input = IOUtils.toString(stream, Charset.defaultCharset());
        
        /*
         * Dealing with Map
         */
        if(input.startsWith("[")){
        	List<Map<String, Object>> convertList = new ArrayList<>();
        	
        	return mapper.readValue(input, convertList.getClass());
        }
        
        return mapper.readValue(input, map.getClass());
	}
	
	public Map<String, Object> addGeo(Map<String, Object> json) throws GeoException{
		return addGeo(json, this.fields, this.geoJsonKey);
	}
	
	/**
	 * 
	 * @param json
	 * @param fields
	 * 	Fields need to be in order
	 * @return
	 * @throws GeoException 
	 */
	private Map<String, Object> addGeo(Map<String,Object> json, String fields, String geokey) throws GeoException{
		String[] addressKeys = fields.split(",");
		String address; 
		StringBuilder build = new StringBuilder(); 
		
		/*
		 * Loop through fields 
		 */
		for(String addresskey : addressKeys) {
			if(json.containsKey(addresskey))
				build.append(json.get(addresskey));
			
			build.append(" ");
		}
		
		address = build.toString().trim();
		
		try {
			if(cache.containsKey(address)) {
				json.put(geokey, cache.get(address));
			}else {
				GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
				cache.put(address, results);
				json.put(geokey, results);	
			}
		} catch (ApiException | InterruptedException | IOException e) {
			throw new GeoException("Issue accessing Coordinates ", e);
		}
		
		return json;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getFields() {
		return fields;
	}

	public void setFields(String fields) {
		this.fields = fields;
	}

	public String getGeoJsonKey() {
		return geoJsonKey;
	}

	public void setGeoJsonKey(String geoJsonKey) {
		this.geoJsonKey = geoJsonKey;
	}

}
