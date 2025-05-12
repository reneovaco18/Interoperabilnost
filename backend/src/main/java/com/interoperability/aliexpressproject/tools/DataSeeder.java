package com.interoperability.aliexpressproject.tools;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interoperability.aliexpressproject.model.Aliproduct;
import com.interoperability.aliexpressproject.util.XmlValidationUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String API_HOST = "aliexpress-datahub.p.rapidapi.com";
    private static final List<String> XSD_IDS = List.of(
            "1005004854674331", "1005002405661482", "1005005772936829",
            "1005001869705960", "1005002674284663"
    );
    private static final List<String> RNG_IDS = List.of(
            "32994977920", "1005005834510026", "1005007385669783",
            "1005007323672451", "1005007299962523"
    );

    private final ObjectMapper mapper = new ObjectMapper();
    private final JAXBContext jaxbCtx;
    private final XmlValidationUtil validator = new XmlValidationUtil();
    private final AsyncHttpClient client;

    @Value("${rapidapi.key}")
    private String apiKey;

    public DataSeeder() throws Exception {
        this.client = new DefaultAsyncHttpClient();
        this.jaxbCtx = JAXBContext.newInstance(Aliproduct.class);
    }

    @PostConstruct
    public void init() throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RapidAPI key missing – please set rapidapi.key");
        }
        Files.createDirectories(Paths.get("data", "aliproducts"));
    }

    @PreDestroy
    public void shutdown() throws IOException {
        client.close();
    }

    public int seed() {
        seedAll();
        return XSD_IDS.size() + RNG_IDS.size();
    }

    private void seedAll() {
        log.info("Seeding {} XSD-validated products…", XSD_IDS.size());
        fetchAndStore(XSD_IDS, true);
        log.info("Seeding {} RNG-validated products…", RNG_IDS.size());
        fetchAndStore(RNG_IDS, false);
    }

    private void fetchAndStore(List<String> ids, boolean useXsd) {
        for (var id : ids) {
            log.info("▶ Fetching {} (validate via {})", id, useXsd ? "XSD" : "RNG");
            try {
                String json = fetchDetailJson(id);
                if (json == null) {
                    log.warn("   • no detail JSON → skipped");
                    continue;
                }

                Aliproduct p = mapJson(json);
                byte[] xml = marshal(p);

                if (!validateXml(xml, useXsd)) {
                    log.warn("   • XML validation failed → skipped");
                    continue;
                }

                Path out = Paths.get("data", "aliproducts", p.getId() + ".xml");
                Files.write(out, xml);
                log.info("   ✅ saved {}", out);
            } catch (Exception e) {
                log.error("   ✖ error processing " + id, e);
            }

            try {
                Thread.sleep(1200);
            } catch (InterruptedException ignored) {}
        }
    }

    private String fetchDetailJson(String id) {
        for (var ep : List.of("item_detail_2", "item_detail")) {
            String url = String.format("https://%s/%s?itemId=%s", API_HOST, ep, id);

            try {
                var response = client.prepare("GET", url)
                        .addHeader("X-RapidAPI-Key", apiKey.trim())
                        .addHeader("X-RapidAPI-Host", API_HOST)
                        .addHeader("Accept", "application/json")
                        .execute()
                        .toCompletableFuture()
                        .join();

                int status = response.getStatusCode();
                String body = response.getResponseBody(StandardCharsets.UTF_8);

                if (status == 200) {
                    JsonNode root = mapper.readTree(body);
                    JsonNode itemNode = root.has("item")
                            ? root.get("item")
                            : root.path("result").path("item");

                    if (!itemNode.isMissingNode()) {
                        ObjectNode wrapper = mapper.createObjectNode();
                        wrapper.set("item", itemNode);
                        return mapper.writeValueAsString(wrapper);
                    }

                } else if (status == 403) {
                    log.error("   • HTTP 403 Forbidden – key/host combo rejected. URL: {}\nResponse body: {}", url, body);
                    return null;
                } else if (status == 429) {
                    log.warn("   • HTTP 429 Too Many Requests – try increasing delay or upgrade plan");
                    return null;
                } else {
                    log.warn("   • HTTP {} on GET {}. Response body: {}", status, url, body);
                }

            } catch (Exception e) {
                log.warn("   • call to {} failed: {}", ep, e.getMessage());
            }
        }

        return null;
    }

    private Aliproduct mapJson(String json) throws Exception {
        JsonNode item = mapper.readTree(json).path("item");
        if (item.isMissingNode()) throw new IllegalStateException("no 'item' node");

        Aliproduct p = new Aliproduct();
        p.setId(item.path("itemId").asText());
        p.setTitle(item.path("title").asText("<no title>"));

        JsonNode imgs = item.path("images");
        String img = imgs.isArray() && imgs.size() > 0
                ? imgs.get(0).asText()
                : "";
        if (img.startsWith("//")) img = "https:" + img;
        p.setImageUrl(img);

        String price = item.at("/sku/def/promotionPrice").asText("0");
        if (price.contains("-")) price = price.split("-")[0].trim();
        p.setPrice(new BigDecimal(price));
        p.setRating(BigDecimal.valueOf(item.path("rating").asDouble(0)));

        return p;
    }

    private byte[] marshal(Aliproduct p) throws Exception {
        try (var bout = new ByteArrayOutputStream()) {
            Marshaller m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(p, bout);
            return bout.toByteArray();
        }
    }

    private boolean validateXml(byte[] xml, boolean useXsd) throws Exception {
               try (var bin = new ByteArrayInputStream(xml)) {

                                List<String> errors = new ArrayList<>();
                        boolean ok = useXsd
                                        ? validator.validateAgainstXsd(bin, errors)
                                       : validator.validateAgainstRng(bin, errors);
                        if (!ok) {
                              errors.forEach(e -> log.warn("   • XML validation error: {}", e));
                           }
                       return ok;
               }
           }
}