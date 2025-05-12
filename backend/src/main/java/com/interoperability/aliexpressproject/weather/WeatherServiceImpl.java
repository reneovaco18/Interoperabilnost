package com.interoperability.aliexpressproject.weather;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("weatherService")
public class WeatherServiceImpl implements WeatherService {

    private static final String DHMZ_URL = "https://vrijeme.hr/hrvatska_n.xml";

    private final WebClient   web        = WebClient.create();
    private final SAXBuilder  sax        = new SAXBuilder();

    @Override
    public Map<String, String> getTemperature(String cityTerm) throws Exception {


        String xml = web.get()
                .uri(DHMZ_URL)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        Document doc  = sax.build(new StringReader(xml));
        Element  root = doc.getRootElement();

        Map<String,String> results  = new LinkedHashMap<>();
        String             needle   = cityTerm.toLowerCase();


        for (Element grad : root.getChildren("Grad")) {


            String name = grad.getChildText("GradIme");
            if (name == null) name = grad.getChildText("Grad");
            if (name == null || !name.toLowerCase().contains(needle)) continue;


            Element podaci = grad.getChild("Podatci");
            if (podaci == null) continue;

            String temp = podaci.getChildText("Temp");
            if (temp == null) temp = podaci.getChildText("Temp1");
            if (temp == null) continue;

            results.put(name, temp.trim() + "°C");
        }

        return results;
    }
}
