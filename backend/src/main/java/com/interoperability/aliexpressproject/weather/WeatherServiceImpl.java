// src/main/java/com/interoperability/aliexpressproject/weather/WeatherServiceImpl.java
package com.interoperability.aliexpressproject.weather;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("weatherService")
public class WeatherServiceImpl implements WeatherService {

    private static final String DHMZ_URL = "https://vrijeme.hr/hrvatska_n.xml";
    private final WebClient web = WebClient.create();
    private final SAXBuilder saxBuilder = new SAXBuilder();

    @Override
    public Map<String, String> getTemperature(String cityTerm) throws Exception {
        // 1) fetch XML
        String xml = web.get()
                .uri(DHMZ_URL)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 2) parse
        Document doc = saxBuilder.build(new StringReader(xml));
        Element root = doc.getRootElement();  // <Hrvatska>

        Map<String, String> results = new LinkedHashMap<>();
        String lowerTerm = cityTerm.toLowerCase();

        // 3) each <Grad>…
        for (Element grad : root.getChildren("Grad")) {
            String name = grad.getChildText("GradIme");    // correct tag
            if (name == null || !name.toLowerCase().contains(lowerTerm)) {
                continue;
            }
            Element podaci = grad.getChild("Podatci");     // correct tag spelling
            if (podaci == null) {
                continue;
            }
            String temp = podaci.getChildText("Temp");     // correct tag
            if (temp != null) {
                results.put(name, temp.trim() + "°C");
            }
        }

        return results;
    }
}
