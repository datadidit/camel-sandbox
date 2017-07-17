package org.datadidit.camel;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.maps.model.GeocodingResult;

public class GeoEnrichmentProcessorIT {
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
			GeocodingResult[] resultAddress1 = (GeocodingResult[]) enhancedMap1.get(geoKey);
			GeocodingResult[] resultAddress2 = (GeocodingResult[]) enhancedMap2.get(geoKey);

			System.out.println(resultAddress1[0].formattedAddress);
			System.out.println(resultAddress1[0].geometry.location);

			System.out.println(resultAddress2[0].formattedAddress);
			System.out.println(resultAddress2[0].geometry.location);
		} catch (GeoException e) {
			fail("Error trying to geo code "+e.getMessage());
		}
	}
}
