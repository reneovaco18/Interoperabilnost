package com.interoperability.aliexpressproject.endpoint;

import com.interoperability.aliexpressproject.service.ProductXmlAggregator;
import com.interoperability.aliexpressproject.util.XmlValidationUtil;
import org.springframework.ws.server.endpoint.annotation.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


@Endpoint
public class ProductsSoapEndpoint {

    private static final String WS_NS   = "http://interoperability.com/ws";
    private static final String PROD_NS = "http://interoperability.com/aliproduct";

    private final ProductXmlAggregator aggregator;
    private final XmlValidationUtil    validator;
    private final XPath                xpath;

    public ProductsSoapEndpoint(ProductXmlAggregator aggregator,
                                XmlValidationUtil validator) {
        this.aggregator = aggregator;
        this.validator  = validator;

        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new SimpleNs("a", PROD_NS));
    }



    @PayloadRoot(namespace = WS_NS, localPart = "getByTitleRequest")
    @ResponsePayload
    public Element handle(@RequestPayload Element req) throws Exception {


        String term = req.getAttribute("term");
        term = term == null ? "" : term.trim().toLowerCase();


        String xml = aggregator.buildXml();

        List<String> validationErrs = new ArrayList<>();
        try (var in = new ByteArrayInputStream(xml.getBytes())) {
            validator.validateAgainstXsd(in, validationErrs);
        }


        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db   = dbf.newDocumentBuilder();
        Document fullDoc     = db.parse(new InputSource(new StringReader(xml)));


        NodeList hits;
        if (term.isEmpty()) {
            hits = (NodeList) xpath.evaluate(
                    "//a:aliproduct", fullDoc, XPathConstants.NODESET);
        } else {

            String xsafe = term.replace("'", "''");
            String exprStr =
                    "//a:aliproduct[" +
                            "contains(" +
                            "translate(a:title," +
                            "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')," +
                            "'" + xsafe + "')" +
                            "]";
            hits = (NodeList) xpath.compile(exprStr)
                    .evaluate(fullDoc, XPathConstants.NODESET);
        }


        Document respDoc = db.newDocument();
        Element  resp    = respDoc.createElementNS(WS_NS, "getByTitleResponse");
        resp.setAttribute("xmlns:a", PROD_NS);
        respDoc.appendChild(resp);


        for (int i = 0; i < hits.getLength(); i++) {
            resp.appendChild(respDoc.importNode(hits.item(i), true));
        }


        for (String err : validationErrs) {
            Element ve = respDoc.createElementNS(WS_NS, "validationError");
            ve.setTextContent(err);
            resp.appendChild(ve);
        }

        return resp;
    }



    private static record SimpleNs(String prefix, String uri) implements NamespaceContext {
        @Override public String getNamespaceURI(String p)           {
            return prefix.equals(p) ? uri : XMLConstants.NULL_NS_URI;
        }
        @Override public String getPrefix(String namespaceURI)      {
            return uri.equals(namespaceURI) ? prefix : null;
        }
        @Override public java.util.Iterator<String> getPrefixes(String namespaceURI) {
            return java.util.Collections.singletonList(prefix).iterator();
        }
    }
}
