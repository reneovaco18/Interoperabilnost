// src/main/java/com/interoperability/aliexpressproject/tools/DataSeeder.java
package com.interoperability.aliexpressproject.tools;

import com.fasterxml.jackson.databind.*;
import com.interoperability.aliexpressproject.model.Aliproduct;
import com.interoperability.aliexpressproject.util.XmlValidationUtil;
import jakarta.xml.bind.*;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Called from /admin/seed or from main(). */
@Component
public class DataSeeder {

    private static final String RAPID_HOST =
            "ecommdatahub-aliexpress-datahub-v1.p.rapidapi.com";
    private static final String KEY = System.getenv("RAPID_KEY");   // <‑ put your key here
    private static final Path   DIR = Paths.get("data", "aliproducts");

    private static final List<String> IDS = List.of(
            // first 10 → XSD, last 10 → RNG
            "1005005244562338","1005005067759019","1005004870304739",
            "1005004529878893","1005004030872043","1005005012309013",
            "1005005110995725","1005004876770172","1005005138589063",
            "1005005050738982",
            "1005005073331165","1005004584152406","1005004603017744",
            "1005004739991317","1005004693253120","1005004590443815",
            "1005004711161819","1005004632305551","1005004332202136",
            "1005004961132711"
    );

    private final ObjectMapper mapper = new ObjectMapper();
    private final JAXBContext  jaxb   = JAXBContext.newInstance(Aliproduct.class);
    private final XmlValidationUtil validator = new XmlValidationUtil();

    public DataSeeder() throws Exception { Files.createDirectories(DIR); }

    /** returns the list of product‑IDs that were **actually** stored */
    public List<String> seed() throws Exception {
        List<String> saved = new ArrayList<>();

        int idx = 0;
        for (String id : IDS) {
            idx++;
            boolean useXsd = idx <= 10;
            System.out.println("▶ Fetch "+id+" ("+(useXsd?"XSD":"RNG")+")");

            String json = Request.get("https://"+RAPID_HOST+"/item_detail_2?itemId="+id)
                    .addHeader("X-RapidAPI-Key",  KEY)
                    .addHeader("X-RapidAPI-Host", RAPID_HOST)
                    .addHeader("Accept", "application/json")
                    .connectTimeout(Timeout.ofSeconds(15))
                    .responseTimeout(Timeout.ofSeconds(15))
                    .execute().returnContent().asString();

            Aliproduct p = mapJson(json);

            // marshal → byte[]
            var bout = new java.io.ByteArrayOutputStream();
            Marshaller m = jaxb.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(p, bout);
            byte[] xml = bout.toByteArray();

            boolean ok;
            try (var in = new java.io.ByteArrayInputStream(xml)) {
                ok = useXsd
                        ? validator.validateAgainstXsd(in, new ArrayList<>())
                        : validator.validateAgainstRng(in, new ArrayList<>());
            }
            if (!ok) { System.out.println("  💥 validation failed"); continue; }

            Files.write(DIR.resolve(p.getId()+".xml"), xml);
            saved.add(p.getId());
            System.out.println("  ✅ saved");
        }
        return saved;
    }

    /* helper ─────────────────────────────────────────────── */
    private Aliproduct mapJson(String json) throws Exception {
        JsonNode item = mapper.readTree(json).path("item");

        Aliproduct p      = new Aliproduct();
        long idFromApi    = item.path("itemId").asLong(0);
        if (idFromApi != 0) p.setId(String.valueOf(idFromApi));

        p.setTitle(item.path("title").asText());

        String img = item.path("images").get(0).asText("");
        if (img.startsWith("//")) img = "https:"+img;
        p.setImageUrl(img);

        String price = item.at("/sku/def/promotionPrice").asText();
        if (price.contains("-")) price = price.split("-")[0].trim();
        p.setPrice(new BigDecimal(price));

        p.setRating(BigDecimal.valueOf(item.path("rating").asDouble(0)));
        return p;
    }

    /* optional CLI run */
    public static void main(String[] a) throws Exception {
        System.out.println(new DataSeeder().seed());
    }
}
