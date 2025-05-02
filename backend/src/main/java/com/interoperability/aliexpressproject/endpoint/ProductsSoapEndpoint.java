package com.interoperability.aliexpressproject.endpoint;

import com.interoperability.aliexpressproject.service.ProductXmlAggregator;
import com.interoperability.aliexpressproject.util.XmlValidationUtil;
import org.springframework.ws.server.endpoint.annotation.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.util.List;

@Endpoint
public class ProductsSoapEndpoint {

    private static final String WS_NS = "http://interoperability.com/ws";
    private static final String PROD_NS = "http://interoperability.com/aliproduct";

    private final ProductXmlAggregator aggregator;
    private final XmlValidationUtil    validator;
    private final XPath                xpath;        // shared engine

    public ProductsSoapEndpoint(ProductXmlAggregator aggregator,
                                XmlValidationUtil validator) {
        this.aggregator = aggregator;
        this.validator  = validator;

        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(SimpleNs.of("a", PROD_NS));
    }

    /* ---------- SOAP entry‑point ---------- */

    @PayloadRoot(namespace = WS_NS, localPart = "getByTitleRequest")
    @ResponsePayload
    public Element handle(@RequestPayload Element req) throws Exception {

        String term = req.getAttribute("term").toLowerCase();
        String xml  = aggregator.buildXml();

        /* 1) optional XSD re‑validation */
        List<String> validationErrs = new java.util.ArrayList<>();
        try (java.io.ByteArrayInputStream in =
                     new java.io.ByteArrayInputStream(xml.getBytes())) {
            validator.validateAgainstXsd(in, validationErrs);
        }

        /* 2) DOM‑parse with namespaces */
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        /* 3) build XPath expression at runtime */
        String exprStr = "//a:aliproduct"
                + "[contains("
                + "        translate(a:title,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),"
                + "        '" + term + "')]";
        XPathExpression expr = xpath.compile(exprStr);
        NodeList matches = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        /* 4) build SOAP response */
        Document respDoc = f.newDocumentBuilder().newDocument();
        Element resp = respDoc.createElementNS(WS_NS, "getByTitleResponse");
        respDoc.appendChild(resp);

        for (int i = 0; i < matches.getLength(); i++) {
            resp.appendChild(respDoc.importNode(matches.item(i), true));
        }
        for (String err : validationErrs) {
            Element ve = respDoc.createElementNS(WS_NS, "validationError");
            ve.setTextContent(err);
            resp.appendChild(ve);
        }
        return resp;
    }

    /* ---------- tiny namespace helper ---------- */
    private record SimpleNs(String p, String uri) implements javax.xml.namespace.NamespaceContext {
        public String getNamespaceURI(String prefix){
            return p.equals(prefix) ? uri : XMLConstants.NULL_NS_URI;
        }
        public String getPrefix(String namespaceURI){
            return uri.equals(namespaceURI) ? p : null;
        }
        public java.util.Iterator<String> getPrefixes(String ns){
            return java.util.Collections.singletonList(p).iterator();
        }
        static SimpleNs of(String p, String uri){ return new SimpleNs(p, uri); }
    }
}
