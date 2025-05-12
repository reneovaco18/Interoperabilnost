package com.interoperability.aliexpressproject.service;

import com.interoperability.aliexpressproject.model.Aliproduct;
import com.interoperability.aliexpressproject.util.XmlValidationUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
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

    public AliproductService(XmlValidationUtil validator) throws JAXBException, IOException {
        this.validator = validator;
        this.jaxbCtx   = JAXBContext.newInstance(Aliproduct.class);

        Files.createDirectories(DIR);
    }


    public List<String> validateAndSaveXsd(MultipartFile f) throws IOException {
        return validateThenPersist(f, true);
    }


    public List<String> validateAndSaveRng(MultipartFile f) throws IOException {
        return validateThenPersist(f, false);
    }


    private List<String> validateThenPersist(MultipartFile f, boolean useXsd) throws IOException {
        List<String> errors = new ArrayList<>();

        try (InputStream in = f.getInputStream()) {
            boolean ok = useXsd
                    ? validator.validateAgainstXsd(in, errors)
                    : validator.validateAgainstRng(in, errors);
            if (!ok) return errors;
        }


        try (InputStream in = f.getInputStream()) {
            Unmarshaller um = jaxbCtx.createUnmarshaller();
            Aliproduct prod = (Aliproduct) um.unmarshal(in);
            if (prod.getId() == null || prod.getId().isBlank()) {
                prod.setId(UUID.randomUUID().toString());
            }

            Path out = DIR.resolve(prod.getId() + ".xml");
            Marshaller m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(prod, out.toFile());
            return Collections.emptyList();
        } catch (JAXBException ex) {
            return List.of("JAXB error: " + ex.getMessage());
        }
    }
}
