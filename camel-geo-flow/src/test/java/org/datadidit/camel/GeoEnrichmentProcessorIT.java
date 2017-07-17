package org.datadidit.camel;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datadidit.camel.util.TestUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeoEnrichmentProcessorIT {
	private ObjectMapper mapper = new ObjectMapper(); 
	
	@Test
	public void testAddGeo() {
		String apiKey = System.getenv("apiKey");
		String geoKey = "geometry";
		System.out.println("Api Key: " + apiKey);
		GeoEnrichmentProcessor processor = new GeoEnrichmentProcessor(apiKey, "City,State,Country", geoKey);

		Map<String, Object> geoInfo = new HashMap<>();

		geoInfo.put("State", "MD");
		geoInfo.put("City", "Ocean City");
		geoInfo.put("Country", "US");

		try {
			Map<String, Object> enhancedMap = processor.addGeo(geoInfo);

			// Geo Key should be in returned map
			assertTrue(enhancedMap.containsKey(geoKey));
		} catch (GeoException e) {
			e.printStackTrace();
			fail("Unable to geoEnhance " + e.getMessage());
		}
	}

	@Test
	public void testAddGeoCity() {
		String apiKey = System.getenv("apiKey");
		String geoKey = "geometry";
		GeoEnrichmentProcessor processor = new GeoEnrichmentProcessor(apiKey, "City,State,Country", geoKey);
		Map<String, Object> geoInfo = new HashMap<>();
		Map<String, Object> geoInfo2 = new HashMap<>();
		
		geoInfo.put("State", "MD");
		geoInfo.put("City", "Ocean City");
		geoInfo.put("Country", "US");
		
		geoInfo2.put("State", "MD");
		geoInfo2.put("City", "Hyattsville");
		geoInfo.put("Country", "US");
		
		try {


			Map<String, Object> enhancedMap1 = processor.addGeo(geoInfo);


			Map<String, Object> enhancedMap2 = processor.addGeo(geoInfo2);
			List<Map<String,Object>> resultAddress1 = TestUtils.convertToGeo((List<String>) enhancedMap1.get(geoKey));
			List<Map<String,Object>> resultAddress2 = TestUtils.convertToGeo((List<String>) enhancedMap2.get(geoKey));

			for(Map<String, Object> entry : resultAddress1) {
				System.out.println(entry);
			}
			
			//TODO: Add some asserts in here
			//GeocodingResult[] resultAddress1 = enhancedMap1.get(geoKey);
			//GeocodingResult[] resultAddress2 = enhancedMap2.get(geoKey);

			//System.out.println(resultAddress1[0].formattedAddress);
			//System.out.println(resultAddress1[0].geometry.location);

			//System.out.println(resultAddress2[0].formattedAddress);
			//System.out.println(resultAddress2[0].geometry.location);
		} catch (GeoException | IOException e) {
			fail("Error trying to geo code "+e.getMessage());
		}
	}
	

}
