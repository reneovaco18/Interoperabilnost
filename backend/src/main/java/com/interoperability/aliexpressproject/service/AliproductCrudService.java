
package com.interoperability.aliexpressproject.service;

import com.interoperability.aliexpressproject.model.Aliproduct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.UncheckedIOException;
@Service
public class AliproductCrudService {
    private static final Path DIR = Paths.get("data", "aliproducts");
    private final JAXBContext jaxb;

    public AliproductCrudService() throws JAXBException, IOException {
        this.jaxb = JAXBContext.newInstance(Aliproduct.class);
        Files.createDirectories(DIR);
        }

        public List<Aliproduct> findAll() throws JAXBException, IOException {
            Unmarshaller u = jaxb.createUnmarshaller();
            try (var files = Files.list(DIR)) {
                return files
                        .filter(p -> p.toString().endsWith(".xml"))
                        .map(p -> {
                            try {
                                return (Aliproduct) u.unmarshal(p.toFile());
                            } catch (Exception e) {
                                throw new UncheckedIOException(new IOException(e));
                            }
                        })
                        .collect(Collectors.toList());
            }
        }

    public Optional<Aliproduct> findById(String id) throws JAXBException {
        Path f = DIR.resolve(id + ".xml");
        if (Files.exists(f)) {
            Unmarshaller u = jaxb.createUnmarshaller();
            return Optional.of((Aliproduct) u.unmarshal(f.toFile()));
        }
        return Optional.empty();
    }

    public Aliproduct save(Aliproduct p) throws JAXBException, IOException {
        if (p.getId() == null || p.getId().isBlank()) {
            p.setId(UUID.randomUUID().toString());
        }
        Marshaller m = jaxb.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Path f = DIR.resolve(p.getId() + ".xml");
        m.marshal(p, f.toFile());
        return p;
    }

    public Optional<Aliproduct> update(String id, Aliproduct p) throws JAXBException, IOException {
        Path f = DIR.resolve(id + ".xml");
        if (!Files.exists(f)) return Optional.empty();
        p.setId(id);
        Marshaller m = jaxb.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(p, f.toFile());
        return Optional.of(p);
    }

    public boolean delete(String id) throws IOException {
        return Files.deleteIfExists(DIR.resolve(id + ".xml"));
    }
}
