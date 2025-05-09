package com.interoperability.aliexpressproject.weather;

import java.util.Map;

/**
 * XML-RPC interface: clients call getTemperature(cityTerm)
 * and receive a map from full city name → temperature string.
 */
public interface WeatherService {
    /**
     * @param cityTerm full or partial city name (case-insensitive)
     * @return map of matching cityName → temperature (e.g. "Zagreb" → "15°C")
     */
    Map<String, String> getTemperature(String cityTerm) throws Exception;
}
