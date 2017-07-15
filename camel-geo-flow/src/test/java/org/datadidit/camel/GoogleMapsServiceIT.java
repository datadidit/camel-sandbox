package org.datadidit.camel;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

public class GoogleMapsServiceIT {
	private static GeoApiContext context;

	private static String apiKey;

	@BeforeClass
	public static void setupGeo() {
		apiKey = System.getenv("apiKey");
		System.out.println("API Key: " + apiKey);
		context = new GeoApiContext().setApiKey(apiKey);
	}

	@Test
	public void test() {
		GeocodingResult[] results;
		try {
			results = GeocodingApi.geocode(context, "1600 Amphitheatre Parkway Mountain View, CA 94043").await();
			System.out.println("Address " + results[0].formattedAddress);
			System.out.println("Geometry " + results[0].geometry.location);

			// See if Jackson can Output
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(results[0].geometry));
		} catch (ApiException | InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}
}
