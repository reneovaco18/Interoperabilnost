package com.interoperability.aliexpressproject.service;

import com.interoperability.aliexpressproject.model.Aliproduct;
import com.interoperability.aliexpressproject.util.XmlValidationUtil;
import jakarta.xml.bind.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@Service
public class AliproductService {

    private static final Path DIR = Paths.get("data", "aliproducts");
    private final JAXBContext       jaxbCtx;
    private final XmlValidationUtil validator;

    public AliproductService(XmlValidationUtil validator)
            throws JAXBException, IOException {
        this.validator = validator;
        this.jaxbCtx   = JAXBContext.newInstance(Aliproduct.class);
        Files.createDirectories(DIR);
    }

    /* ---------- public API ---------- */

    public List<String> validateAndSaveXsd(MultipartFile f) throws IOException {
        return validateThenPersist(f, true);
    }

    public List<String> validateAndSaveRng(MultipartFile f) throws IOException {
        return validateThenPersist(f, false);
    }

    /* ---------- internals ---------- */

    private List<String> validateThenPersist(MultipartFile f, boolean xsd) throws IOException {
        List<String> errors = new ArrayList<>();
        try (InputStream in = f.getInputStream()) {
            boolean ok = xsd
                    ? validator.validateAgainstXsd(in, errors)
                    : validator.validateAgainstRng(in, errors);
            if (!ok) return errors;            // validation failed – just return errs
        }
        return unmarshalAndStore(f);
    }

    private List<String> unmarshalAndStore(MultipartFile f) throws IOException {
        try (InputStream in = f.getInputStream()) {
            Unmarshaller um  = jaxbCtx.createUnmarshaller();
            Aliproduct p     = (Aliproduct) um.unmarshal(in);

            if (p.getId() == null || p.getId().isBlank()) {
                p.setId(UUID.randomUUID().toString());
            }

            Path out = DIR.resolve(p.getId() + ".xml");
            Marshaller m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(p, out.toFile());
            return Collections.emptyList();
        } catch (JAXBException ex) {
            return List.of("JAXB error: " + ex.getMessage());
        }
    }
}
