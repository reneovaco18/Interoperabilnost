package com.interoperability.aliexpressproject.service;

import com.interoperability.aliexpressproject.model.Aliproduct;
import jakarta.xml.bind.*;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamResult;
import java.io.*;
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
    }

    /** Returns XML string containing all products. */
    public String buildXml() throws IOException, JAXBException {

        List<Path> files = Files.list(DIR)
                .filter(p -> p.toString().endsWith(".xml"))
                .collect(Collectors.toList());

        StringWriter out = new StringWriter();
        out.write("<products xmlns=\"http://interoperability.com/aliproduct\">\n");

        Unmarshaller u = jaxb.createUnmarshaller();
        for (Path p : files) {
            Aliproduct prod = (Aliproduct) u.unmarshal(p.toFile());
            Marshaller m = jaxb.createMarshaller();
            m.marshal(prod, new StreamResult(out));
            out.write("\n");
        }
        out.write("</products>");
        return out.toString();
    }
}
