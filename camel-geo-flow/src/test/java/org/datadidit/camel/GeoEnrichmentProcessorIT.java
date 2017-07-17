package org.datadidit.camel;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class GeoEnrichmentProcessorIT {
	@Test
	public void testAddGeo() {
		String apiKey = System.getenv("apiKey");
		String geoKey = "geometry";
		System.out.println("Api Key: "+apiKey);
		GeoEnrichmentProcessor processor = new GeoEnrichmentProcessor(apiKey, "City,State,Country", geoKey);

		Map<String, Object> geoInfo = new HashMap<>();
		
		geoInfo.put("State", "MD");
		geoInfo.put("City", "Ocean City");
		geoInfo.put("Country", "US");
		
		try {
			Map<String, Object> enhancedMap = processor.addGeo(geoInfo);
			
			//Geo Key should be in returned map
			assertTrue(enhancedMap.containsKey(geoKey));
		} catch (GeoException e) {
			e.printStackTrace();
			fail("Unable to geoEnhance "+e.getMessage());
		}
	}
}
