package com.interoperability.aliexpressproject.service;

import com.interoperability.aliexpressproject.model.Aliproduct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds one XML document <products>…</products>
 * from every file in data/aliproducts/.
 */
@Service
public class ProductXmlAggregator {

    private static final Path DIR = Paths.get("data", "aliproducts");
    private final JAXBContext jaxb;

    public ProductXmlAggregator() throws JAXBException {
        this.jaxb = JAXBContext.newInstance(Aliproduct.class);
        // ensure the directory exists
        try {
            Files.createDirectories(DIR);
        } catch (IOException e) {
            throw new RuntimeException("Could not create data directory", e);
        }
    }

    /** Returns XML string containing all products. */
    public String buildXml() throws IOException, JAXBException {
        List<Path> files = Files.list(DIR)
                .filter(p -> p.toString().endsWith(".xml"))
                .collect(Collectors.toList());

        StringWriter out = new StringWriter();
        out.write("<products xmlns=\"http://interoperability.com/aliproduct\">\n");

        Unmarshaller u = jaxb.createUnmarshaller();
        Marshaller   m = jaxb.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);

        for (Path p : files) {
            Aliproduct prod = (Aliproduct) u.unmarshal(p.toFile());
            m.marshal(prod, new StreamResult(out));
            out.write("\n");
        }

        out.write("</products>");
        return out.toString();
    }
}
