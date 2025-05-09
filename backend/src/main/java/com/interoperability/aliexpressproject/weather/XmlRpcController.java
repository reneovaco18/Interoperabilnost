// src/main/java/com/interoperability/aliexpressproject/weather/XmlRpcController.java
package com.interoperability.aliexpressproject.weather;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Map;

@RestController
public class XmlRpcController {

    private final WeatherService weatherService;
    private final SAXBuilder saxBuilder = new SAXBuilder();
    private final XMLOutputter xmlOutputter;

    public XmlRpcController(WeatherService weatherService) {
        this.weatherService = weatherService;
        // ensure declaration + raw format
        Format fmt = Format.getRawFormat()
                .setOmitDeclaration(false)
                .setEncoding("UTF-8");
        this.xmlOutputter = new XMLOutputter(fmt);
    }

    @PostMapping(value = "/xmlrpc", consumes = "text/xml", produces = "text/xml")
    public void handleXmlRpc(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // 1) read incoming XML-RPC body
        String xmlIn;
        try (InputStream is = req.getInputStream()) {
            xmlIn = new String(is.readAllBytes(), resp.getCharacterEncoding());
        }

        // 2) parse methodCall → extract <string> term
        Document reqDoc = saxBuilder.build(new StringReader(xmlIn));
        Element stringEl = reqDoc.getRootElement()
                .getChild("params")
                .getChild("param")
                .getChild("value")
                .getChild("string");
        String term = stringEl.getText().trim();

        // 3) query service
        Map<String, String> results = weatherService.getTemperature(term);

        // 4) build <methodResponse>
        Document resDoc = new Document();
        Element methodResponse = new Element("methodResponse");
        resDoc.setRootElement(methodResponse);

        Element params = new Element("params");
        methodResponse.addContent(params);

        Element param = new Element("param");
        params.addContent(param);

        Element value = new Element("value");
        param.addContent(value);

        Element struct = new Element("struct");
        value.addContent(struct);

        // each city → temperature
        for (Map.Entry<String, String> e : results.entrySet()) {
            Element member = new Element("member");
            struct.addContent(member);

            member.addContent(new Element("name").setText(e.getKey()));

            Element memberValue = new Element("value");
            memberValue.addContent(new Element("string").setText(e.getValue()));
            member.addContent(memberValue);
        }

        // 5) write back
        resp.setStatus(200);
        resp.setContentType("text/xml;charset=UTF-8");
        try (OutputStream os = resp.getOutputStream()) {
            xmlOutputter.output(resDoc, os);
        }
    }
}
