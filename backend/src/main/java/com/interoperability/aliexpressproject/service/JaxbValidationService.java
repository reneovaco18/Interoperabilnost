package com.interoperability.aliexpressproject.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JaxbValidationService {

    private final JAXBContext jaxbContext;
    private final Schema schema;

    public JaxbValidationService() {
        try {

            this.jaxbContext = JAXBContext.newInstance(
                    com.interoperability.aliexpressproject.model.Aliproduct.class
            );


            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            InputStream xsdStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("xsd/aliproduct.xsd");

            if (xsdStream == null) {
                throw new IllegalStateException(
                        "Could not load XSD schema from classpath: xsd/aliproduct.xsd"
                );
            }

            this.schema = sf.newSchema(new StreamSource(xsdStream));
            xsdStream.close();

        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize JAXBContext", e);
        } catch (org.xml.sax.SAXException e) {
            throw new IllegalStateException("Failed to compile XSD schema", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Error closing XSD stream", e);
        }
    }


    public List<String> validate(String xml) {
        List<String> errors = new ArrayList<>();
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);


            List<ValidationEvent> events = new ArrayList<>();
            ValidationEventHandler handler = event -> {
                events.add(event);
                return true;
            };
            unmarshaller.setEventHandler(handler);


            unmarshaller.unmarshal(new StreamSource(new StringReader(xml)));


            for (ValidationEvent event : events) {
                ValidationEventLocator loc = event.getLocator();
                String message = String.format(
                        "Line %d:%d – %s",
                        loc.getLineNumber(),
                        loc.getColumnNumber(),
                        event.getMessage()
                );
                errors.add(message);
            }
        } catch (JAXBException e) {

            errors.add(e.getMessage());
        }

        return errors;
    }
}
