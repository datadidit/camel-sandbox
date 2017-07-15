package org.datadidit.camel;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Take in JSON and enhance the JSON w/ 
 * GEO data
 *
 */
public class GeoEnhancementProcessor implements Processor{
	private String apiKey; 
	
	private String fields; 
	
	private String geoJsonKey;
	
	private ObjectMapper mapper = new ObjectMapper(); 
	
	@Override
	public void process(Exchange exchange) throws Exception {
		InputStream stream = exchange.getIn().getBody(InputStream.class);
	
		//Each line contains a Json string
		
	
	}
	
	private Map<String,Object> stringToJson(InputStream stream) throws IOException{        
        Map<String,Object> map = new HashMap<>();
		
        return mapper.readValue(stream, map.getClass());
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
