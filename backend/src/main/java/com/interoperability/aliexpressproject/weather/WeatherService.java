package com.interoperability.aliexpressproject.weather;

import java.util.Map;


public interface WeatherService {

    Map<String, String> getTemperature(String cityTerm) throws Exception;
}
