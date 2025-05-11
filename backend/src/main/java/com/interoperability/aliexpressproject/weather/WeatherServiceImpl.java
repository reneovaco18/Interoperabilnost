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

        /* 1) fetch */
        String xml = web.get()
                .uri(DHMZ_URL)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        /* 2) parse */
        Document doc  = sax.build(new StringReader(xml));
        Element  root = doc.getRootElement();              // <Hrvatska>

        Map<String,String> results  = new LinkedHashMap<>();
        String             needle   = cityTerm.toLowerCase();

        /* 3) iterate every <Grad> node */
        for (Element grad : root.getChildren("Grad")) {

            /* 3a) city name — DHMZ changed tags twice, handle both */
            String name = grad.getChildText("GradIme");      // current tag
            if (name == null) name = grad.getChildText("Grad"); // legacy tag
            if (name == null || !name.toLowerCase().contains(needle)) continue;

            /* 3b) temperature */
            Element podaci = grad.getChild("Podatci");       // correct name
            if (podaci == null) continue;

            String temp = podaci.getChildText("Temp");       // current tag
            if (temp == null) temp = podaci.getChildText("Temp1"); // fallback
            if (temp == null) continue;

            results.put(name, temp.trim() + "°C");
        }

        return results;
    }
}
