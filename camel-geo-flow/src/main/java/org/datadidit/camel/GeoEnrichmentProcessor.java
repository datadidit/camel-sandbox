package org.datadidit.camel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

/**
 * Take in JSON and enhance the JSON w/ 
 * GEO data
 *
 */
public class GeoEnrichmentProcessor implements Processor{
	private Logger logger = Logger.getLogger(GeoEnrichmentProcessor.class);
	
	private String apiKey; 
	
	private String fields; 
	
	private String geoJsonKey = "geometry";
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private Map<String, Object> cache = new HashMap<>();
	
	private static GeoApiContext context;

	public GeoEnrichmentProcessor(String apiKey, String fields, String geoJsonKey) throws ConfigurationException{
		if(apiKey==null || fields==null)
			throw new ConfigurationException("Unable to configure processor apiKey and fields must be set. Fields: "+fields+" API Key: "+apiKey);
		
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
				try {
					output.add(this.addGeo(entry, fields, this.geoJsonKey));
				}catch(Exception ex) {
					logger.error("Issue getting geo for entry: "+entry);
				}
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
				logger.debug("Hit cache "+address);
				json.put(geokey, cache.get(address));
			}else {
				GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
				
				/*
				 * Follow on processors like elastic search may only handle Native objects at ingest so convert 
				 * georesult to String rep.
				 * 
				 * Note: String rep didn't work going to try Map<String, Object>
				 */
				List<Map<String, Object>> objRep = new ArrayList<>();
				for(GeocodingResult result : results) {
					Map<String, Object> obj = new HashMap<>();
					
					obj.put("formattedAddress", result.formattedAddress);
					obj.put("postcodeLocalities", result.postcodeLocalities);
					obj.put("placeId", result.placeId);
					
					//Add Geo
					Map<String, Object> bounds = new HashMap<>();
					Map<String, Object> viewport = new HashMap<>();
					
					if(result.geometry.bounds!=null) {
						bounds.put("northeast", result.geometry.bounds.northeast.toString());
						bounds.put("southwest", result.geometry.bounds.southwest.toString());
						obj.put("bounds", bounds);						
					}
					
					if(result.geometry.viewport!=null) {
						viewport.put("northeast", result.geometry.viewport.northeast.toString());
						viewport.put("southwest", result.geometry.viewport.southwest.toString());
						obj.put("viewport", viewport);
					}
					
					obj.put("location", result.geometry.location.toString());
					obj.put("locationType", result.geometry.locationType);
					
					//Add Address Component
					List<Map<String, Object>> addressComponents = new ArrayList<>();
					for(AddressComponent comp : result.addressComponents) {
						Map<String,Object> addressComp = new HashMap<>();
						addressComp.put("long_name", comp.longName);
						addressComp.put("short_name", comp.shortName);
						String[] types = new String[comp.types.length];
						for(int i=0; i<comp.types.length; i++) {
							types[i] = comp.types[i].toString();
						}
						addressComp.put("types", types);
						addressComponents.add(addressComp);
					}
					
					obj.put("addressComponents", addressComponents);
					objRep.add(obj);
				}
				
				//TODO: Make this work with the actual list in case there is more than 
				//one but for now just have work with 1st result
				logger.debug("Updating cache "+address);
				cache.put(address, objRep.get(0));
				json.put(geokey, objRep.get(0));	
			}
		} catch (ApiException | InterruptedException | IOException e) {
			throw new GeoException("Issue accessing Coordinates ", e);
		}
		
		return json;
	}

	/**
	 * Follow on systems may be looking for lat, lon as keys instead of 
	 * lat, lng. Just normalize to standard (latitude, longitude)
	 * 
	 * @param coordinates
	 * @return
	 */
	private String normalizeLatLng(LatLng coordinates) {
		StringBuilder build = new StringBuilder();
		
		build.append("(");
		build.append(coordinates.lat);
		build.append(",");
		build.append(coordinates.lng);
		build.append(")");
		
		return build.toString();
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
	
	public void setCache(Map<String, Object> cache) {
		this.cache = cache;
	}
	
	public Map<String, Object> getCache(){
		return cache;
	}

}
