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
import java.util.List;

@Component
public class XmlValidationUtil {

    private static final String XSD_PATH = "xsd/aliproduct.xsd";
    private static final String RNG_PATH = "rng/aliproduct.rng";

    /* Relax‑NG constants */
    private static final String RNG_NS =
            "http://relaxng.org/ns/structure/1.0";
    /**  <-  THIS is the provider class actually shipped in jing‑20091111.jar  */
    private static final String RNG_FACTORY =
            "com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory";  // ← correct

    /* ------------ public API ------------ */

    public boolean validateAgainstXsd(InputStream xml, List<String> errs) {
        return validate(xml, errs,
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI),
                XSD_PATH,
                "XSD");
    }

    public boolean validateAgainstRng(InputStream xml, List<String> errs) {
        return validate(xml, errs,
                SchemaFactory.newInstance(RNG_NS, RNG_FACTORY,
                        getClass().getClassLoader()),
                RNG_PATH,
                "RNG");
    }

    /* ------------ internals ------------ */

    private boolean validate(InputStream xml, List<String> errs,
                             SchemaFactory factory,
                             String schemaPath,
                             String tag) {

        try {
            Schema schema = factory.newSchema(
                    new StreamSource(
                            getClass().getClassLoader()
                                    .getResourceAsStream(schemaPath)));
            Validator v = schema.newValidator();
            v.setErrorHandler(new CollectingHandler(errs));
            v.validate(new StreamSource(xml));
            return errs.isEmpty();
        } catch (Exception ex) {
            errs.add(tag + " validation failed: " + ex.getMessage());
            return false;
        }
    }

    /* Collect *all* validation problems in one list */
    private record CollectingHandler(List<String> errs) implements ErrorHandler {
        @Override public void warning   (SAXParseException e){ errs.add("Warn:  " + e.getMessage()); }
        @Override public void error     (SAXParseException e){ errs.add("Error: " + e.getMessage()); }
        @Override public void fatalError(SAXParseException e){ errs.add("Fatal: " + e.getMessage()); }
    }
}
