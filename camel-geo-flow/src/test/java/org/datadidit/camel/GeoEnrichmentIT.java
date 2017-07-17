package org.datadidit.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

import datadidit.helpful.hints.camel.CSVToJsonProcessor;

public class GeoEnrichmentIT {
	private static GeoApiContext context;

	private static File file; 

	private static String apiKey;
	
	private static Map<String, GeocodingResult[]> cache = new HashMap<>();

	@BeforeClass
	public static void setup() {
		file = new File("src/test/resources/data/POBDataSnippet.csv");
		apiKey = System.getenv("apiKey");
		System.out.println("API Key: " + apiKey);
		context = new GeoApiContext().setApiKey(apiKey);
	}
	
	@Test
	public void test() {
		try {
			CSVToJsonProcessor processor = new CSVToJsonProcessor(true, null);
			ObjectMapper mapper = new ObjectMapper();

			List<Map<?,?>> json = processor.readObjectsFromCsv(new FileInputStream(file)); 
		
			for(Map<?, ?> entry : json) {
				this.addGeo((Map<String, Object>) entry, "Birthplace,State,Country", "geometry");
				System.out.println(mapper.writeValueAsString(entry));
			}
		} catch (ConfigurationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGeo() {
		String city = "Silver Spring";
		String state = "MD";
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			GeocodingResult[] results = GeocodingApi.geocode(context, city+" "+state).await();
			
			for(GeocodingResult result : results) {
				System.out.println(mapper.writeValueAsString(result));
			}
		} catch (ApiException | InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGeoCity() {
		String address1 = "Greenbelt MD US";
		String address2 = "Annapolis MD US";
		
		try {
			GeocodingResult[] resultAddress1 = GeocodingApi.geocode(context, address1).await();
			GeocodingResult[] resultAddress2 = GeocodingApi.geocode(context, address2).await();
			
			assertEquals(1, resultAddress1.length);
			assertEquals(1, resultAddress2.length);

			System.out.println(resultAddress1[0].formattedAddress);
			System.out.println(resultAddress1[0].geometry.location);
			
			System.out.println(resultAddress2[0].formattedAddress);
			System.out.println(resultAddress2[0].geometry.location);
		} catch (ApiException | InterruptedException | IOException e) {
			fail("Error trying to geo code "+e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param json
	 * @param fields
	 * 	Fields need to be in order
	 * @return
	 * @throws GeoException 
	 */
	public Map<String, Object> addGeo(Map<String,Object> json, String fields, String geokey) throws GeoException{
		String[] addressKeys = fields.split(",");
		String address; 
		StringBuilder build = new StringBuilder(); 
		
		/*
		 * Loop through fields 
		 */
		for(String addresskey : addressKeys) {
			build.append(json.get(addresskey));
			build.append(" ");
		}
		
		address = build.toString().trim();
		
		try {
			if(cache.containsKey(address)) {
				System.out.println("Hit cache "+address);
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
}
