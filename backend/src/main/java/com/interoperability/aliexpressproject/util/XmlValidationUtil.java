package com.interoperability.aliexpressproject.util;

import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class XmlValidationUtil {


    private static final String XSD_PATH = "xsd/aliproduct.xsd";


    private static final String RNG_PATH = "rng/aliproduct.rng";

    private static final String RNG_NS      = "http://relaxng.org/ns/structure/1.0";
    private static final String RNG_FACTORY = "com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory";


    public List<String> validateXml(InputStream xmlStream, boolean useXsd) {
        List<String> errors = new ArrayList<>();
        try {
            SchemaFactory factory = useXsd
                    ? SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    : SchemaFactory.newInstance(RNG_NS, RNG_FACTORY, getClass().getClassLoader());


            String schemaPath = useXsd ? XSD_PATH : RNG_PATH;
            InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaPath);
            if (schemaStream == null) {
                errors.add("Cannot load schema from classpath: " + schemaPath);
                return errors;
            }

            Schema schema    = factory.newSchema(new StreamSource(schemaStream));
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) {  }
                public void error(SAXParseException   e) { errors.add(e.getMessage()); }
                public void fatalError(SAXParseException e) { errors.add(e.getMessage()); }
            });
            validator.validate(new StreamSource(xmlStream));
        } catch (Exception ex) {
            errors.add("Exception during validation: " + ex.getMessage());
        }
        return errors;
    }


    public boolean validateAgainstXsd(InputStream xml, List<String> errors) {
        errors.addAll(validateXml(xml, true));
        return errors.isEmpty();
    }


    public boolean validateAgainstRng(InputStream xml, List<String> errors) {
        errors.addAll(validateXml(xml, false));
        return errors.isEmpty();
    }
}
