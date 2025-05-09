package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.service.AliproductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;        // ← add this
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AliproductController.class)
@AutoConfigureMockMvc(addFilters = false)  // ← disable security filters
class AliproductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AliproductService service;

    private MockMultipartFile validFile;
    private MockMultipartFile invalidFile;
    private MockMultipartFile customIdFile;

    @BeforeEach
    void setUp() {
        String validXml =
                "<?xml version=\"1.0\"?>\n" +
                        "<aliproduct xmlns=\"http://interoperability.com/aliproduct\">\n" +
                        "  <title>Test</title>\n" +
                        "  <imageUrl>http://foo</imageUrl>\n" +
                        "  <price>10.0</price>\n" +
                        "</aliproduct>";

        String badXml =
                "<?xml version=\"1.0\"?>\n" +
                        "<aliproduct xmlns=\"http://interoperability.com/aliproduct\">\n" +
                        "  <imageUrl>http://foo</imageUrl>\n" +
                        "  <price>10.0</price>\n" +
                        "</aliproduct>";

        String customIdXml =
                "<?xml version=\"1.0\"?>\n" +
                        "<aliproduct xmlns=\"http://interoperability.com/aliproduct\">\n" +
                        "  <id>custom-123</id>\n" +
                        "  <title>Test</title>\n" +
                        "  <imageUrl>http://foo</imageUrl>\n" +
                        "  <price>10.0</price>\n" +
                        "</aliproduct>";

        validFile    = new MockMultipartFile("file", "valid.xml",
                MediaType.APPLICATION_XML_VALUE, validXml.getBytes());
        invalidFile  = new MockMultipartFile("file", "invalid.xml",
                MediaType.APPLICATION_XML_VALUE, badXml.getBytes());
        customIdFile = new MockMultipartFile("file", "custom.xml",
                MediaType.APPLICATION_XML_VALUE, customIdXml.getBytes());
    }

    @Test
    void uploadXsd_valid_returns200() throws Exception {
        when(service.validateAndSaveXsd(validFile)).thenReturn(List.of());

        mockMvc.perform(multipart("/api/aliproducts/upload/xsd").file(validFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File validated and saved."));
    }

    @Test
    void uploadXsd_invalid_returns400AndErrors() throws Exception {
        when(service.validateAndSaveXsd(invalidFile))
                .thenReturn(List.of("Missing title element"));

        mockMvc.perform(multipart("/api/aliproducts/upload/xsd").file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Missing title element"));
    }

    @Test
    void uploadXsd_customId_persistsCustomId() throws Exception {
        when(service.validateAndSaveXsd(customIdFile)).thenReturn(List.of());

        mockMvc.perform(multipart("/api/aliproducts/upload/xsd").file(customIdFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File validated and saved."));
    }

    @Test
    void uploadXsd_emptyFile_returns400() throws Exception {
        var empty = new MockMultipartFile("file", "empty.xml",
                MediaType.APPLICATION_XML_VALUE, new byte[0]);
        when(service.validateAndSaveXsd(empty))
                .thenReturn(List.of("Unexpected end of file"));

        mockMvc.perform(multipart("/api/aliproducts/upload/xsd").file(empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Unexpected end of file"));
    }

    @Test
    void uploadRng_validAndInvalid_behaveSame() throws Exception {
        when(service.validateAndSaveRng(validFile)).thenReturn(List.of());
        mockMvc.perform(multipart("/api/aliproducts/upload/rng").file(validFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File validated and saved."));

        when(service.validateAndSaveRng(invalidFile))
                .thenReturn(List.of("RNG: missing title"));
        mockMvc.perform(multipart("/api/aliproducts/upload/rng").file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("RNG: missing title"));
    }
}
