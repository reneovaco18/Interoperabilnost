package hr.java.interoper.clientapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringReader;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainController {


    @FXML
    private TextField userField, passField;
    @FXML
    private Label loginStatus;

    @FXML
    private ToggleGroup schemaToggle;

    @FXML
    private TextArea uploadResult, soapResult, weatherArea;
    @FXML
    private TextField tempField;
    @FXML
    private TableView<Map<String, String>> table;
    @FXML
    private Tab uploadTab, crudTab;
    @FXML
    private TextField searchField, cityField;


    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String accessToken;


    @FXML
    private void initialize() {
        uploadTab.setDisable(true);
        crudTab.setDisable(true);

        String[] cols = {"id", "title", "imageUrl", "price", "rating"};
        for (String c : cols) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(c);
            col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(c)));
            col.setPrefWidth(c.equals("imageUrl") ? 260 : 120);
            table.getColumns().add(col);
        }
    }


    @FXML
    private void handleSeed() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/admin/seed"))
                    .POST(BodyPublishers.noBody())
                    .build();

            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            showPopup("HTTP " + r.statusCode() + "\n\n" + r.body());
            if (r.statusCode() == 200) loadAll();
        } catch (Exception ex) {
            showPopup("Seeder call failed: " + ex.getMessage());
        }
    }







    @FXML
    private void handleRegister() {
        try {
            String form = "username=" + enc(userField.getText())
                    + "&password=" + enc(passField.getText());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/auth/register"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            loginStatus.setText(
                    r.statusCode() == 200 ? "✅ user created – now log in"
                            : "❌ " + r.body()
            );
        } catch (Exception ex) {
            loginStatus.setText("⚠ " + ex);
        }
    }

    @FXML
    private void handleLogin() {
        try {
            String form = "username=" + enc(userField.getText())
                    + "&password=" + enc(passField.getText());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/auth/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) {
                accessToken = mapper.readTree(r.body()).get("accessToken").asText();
                loginStatus.setText("✅ logged in");
                uploadTab.setDisable(false);
                crudTab.setDisable(false);
            } else {
                loginStatus.setText("❌ HTTP " + r.statusCode());
            }
        } catch (Exception ex) {
            loginStatus.setText("⚠ " + ex);
        }
    }



    @FXML
    private void handleUpload() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML file", "*.xml"));
        File f = fc.showOpenDialog(null);
        if (f == null) return;

        boolean useXsd = ((RadioButton) schemaToggle.getSelectedToggle())
                .getText().contains("XSD");
        String url = useXsd
                ? "http://localhost:8100/api/aliproducts/upload/xsd"
                : "http://localhost:8100/api/aliproducts/upload/rng";

        try {
            HttpRequest req = MultipartBodyBuilder.builder()
                    .filePart("file", f)
                    .build(url, accessToken);

            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            uploadResult.setText("HTTP " + r.statusCode() + "\n" + r.body());
        } catch (Exception ex) {
            uploadResult.setText("⚠ " + ex);
        }
    }



    @FXML
    private void loadAll() {
        reqJson("GET", "/api/aliproducts", null).ifPresentOrElse(
                json -> {
                    List<Map<String, String>> rows = new ArrayList<>();
                    json.forEach(n -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("id", n.get("id").asText());
                        m.put("title", n.get("title").asText());
                        m.put("imageUrl", n.get("imageUrl").asText());
                        m.put("price", n.get("price").asText());
                        m.put("rating", n.path("rating").isMissingNode()
                                ? "" : n.get("rating").asText());
                        rows.add(m);
                    });
                    table.setItems(FXCollections.observableArrayList(rows));
                },
                () -> showPopup("Could not load products")
        );
    }


    @FXML
    private void showFindDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setHeaderText("Enter product ID");
        dlg.showAndWait().ifPresent(id -> {
            reqJson("GET", "/api/aliproducts/" + id.trim(), null).ifPresentOrElse(
                    json -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("id", json.get("id").asText());
                        m.put("title", json.get("title").asText());
                        m.put("imageUrl", json.get("imageUrl").asText());
                        m.put("price", json.get("price").asText());
                        m.put("rating", json.path("rating").isMissingNode() ? "" : json.get("rating").asText());
                        table.setItems(FXCollections.observableArrayList(List.of(m)));
                    },
                    () -> showPopup("ID not found")
            );
        });
    }



    @FXML
    private void showAddDialog() {
        editDialog(null);
    }

    @FXML
    private void showEditDialog() {
        Map<String, String> row = table.getSelectionModel().getSelectedItem();
        if (row != null) editDialog(row);
    }

    private void editDialog(Map<String, String> row) {
        Dialog<Map<String, String>> dlg = new Dialog<>();
        dlg.setTitle(row == null ? "Add product" : "Edit product");

        var grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        TextField titleF = new TextField(row == null ? "" : row.get("title"));
        TextField imgF = new TextField(row == null ? "" : row.get("imageUrl"));
        TextField priceF = new TextField(row == null ? "" : row.get("price"));
        TextField rateF = new TextField(row == null ? "" : row.get("rating"));
        grid.addRow(0, new Label("Title*"), titleF);
        grid.addRow(1, new Label("Image URL*"), imgF);
        grid.addRow(2, new Label("Price*"), priceF);
        grid.addRow(3, new Label("Rating"), rateF);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.setResultConverter(bt -> bt == ButtonType.OK
                ? Map.of("title", titleF.getText().trim(),
                "imageUrl", imgF.getText().trim(),
                "price", priceF.getText().trim(),
                "rating", rateF.getText().trim())
                : null);

        dlg.showAndWait().ifPresent(vals -> {

            List<String> errs = new ArrayList<>();
            if (vals.get("title").isEmpty()) errs.add("Title is required");
            if (vals.get("imageUrl").isEmpty()) errs.add("Image URL is required");
            try {
                new BigDecimal(vals.get("price"));
            } catch (Exception e) {
                errs.add("Price must be numeric");
            }
            if (!vals.get("rating").isBlank()) {
                try {
                    new BigDecimal(vals.get("rating"));
                } catch (Exception e) {
                    errs.add("Rating must be numeric");
                }
            }
            if (!errs.isEmpty()) {
                showPopup(String.join("\n", errs));
                return;
            }

            String body = """
                    {
                      "title":    "%s",
                      "imageUrl": "%s",
                      "price":    %s,
                      "rating":   %s
                    }""".formatted(
                    vals.get("title").replace("\"", "\\\""),
                    vals.get("imageUrl").replace("\"", "\\\""),
                    vals.get("price"),
                    vals.get("rating").isBlank() ? "null" : vals.get("rating")
            );

            if (row == null)
                req("POST", "/api/aliproducts", body);
            else
                req("PUT", "/api/aliproducts/" + row.get("id"), body);

            loadAll();
        });
    }

    @FXML
    private void handleDelete() {
        Map<String, String> row = table.getSelectionModel().getSelectedItem();
        if (row != null) {
            req("DELETE", "/api/aliproducts/" + row.get("id"), null);
            loadAll();
        }
    }



    @FXML
    private void handleSoapSearch() {
        String term = searchField.getText().trim();
        if (term.isEmpty()) return;

        String envelope = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                          xmlns:ws="http://interoperability.com/ws">
          <soapenv:Header/>
          <soapenv:Body>
            <ws:getByTitleRequest term="%s"/>
          </soapenv:Body>
        </soapenv:Envelope>""".formatted(term.replace("\"",""));

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/services/"))

                    .header("Content-Type","text/xml")
                    .POST(BodyPublishers.ofString(envelope))
                    .build();

            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());


            soapResult.setText("HTTP " + r.statusCode() + "\n\n" + pretty(r.body()));

        } catch (Exception ex) {
            soapResult.setText("⚠ " + ex);
        }
    }




    @FXML
    private void handleWeather() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) return;

        String call = """
                <methodCall><methodName>getTemperature</methodName>
                  <params><param><value><string>%s</string></value></param></params>
                </methodCall>""".formatted(city.replace("&", "&amp;"));

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/xmlrpc"))
                    .header("Content-Type", "text/xml")
                    .POST(BodyPublishers.ofString(call))
                    .build();

            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            weatherArea.setText("HTTP " + r.statusCode() + "\n\n" + r.body());
            tempField.setText(extractFirstTemp(r.body()));          //  ✅ now compiles
        } catch (Exception ex) {
            weatherArea.setText("⚠ " + ex);
            tempField.clear();
        }
    }





    private String extractFirstTemp(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = f.newDocumentBuilder();
            Document d = db.parse(new org.xml.sax.InputSource(new StringReader(xml)));
            Node n = d.getElementsByTagName("string").item(0);
            return n == null ? "" : n.getTextContent();
        } catch (Exception e) {
            return "";
        }
    }


    private String pretty(String rawXml) {
        if (rawXml == null || rawXml.isBlank()) {
            return rawXml;
        }
        try {

            var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            var db = dbf.newDocumentBuilder();
            var doc = db.parse(new InputSource(new StringReader(rawXml)));


            var tf = TransformerFactory.newInstance();
            var transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");


            var sw = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();


        } catch (Exception e) {

            return rawXml;
        }
    }


    private void showPopup(String msg) {
        Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, msg).showAndWait());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private Optional<JsonNode> reqJson(String verb, String path, String body) {
        return req(verb, path, body).flatMap(r -> {
            try {
                return Optional.of(mapper.readTree(r));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    private Optional<String> req(String verb, String path, String body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100" + path))
                    .header("Authorization", "Bearer " + accessToken);
            if ("GET".equals(verb) || "DELETE".equals(verb))
                b.method(verb, BodyPublishers.noBody());
            else
                b.header("Content-Type", "application/json")
                        .method(verb, BodyPublishers.ofString(body == null ? "" : body));

            HttpResponse<String> r =
                    http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return r.statusCode() < 400 ? Optional.ofNullable(r.body())
                    : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }



    static class MultipartBodyBuilder {
        private static final String BOUNDARY = "----AliExpressBoundary";
        private final List<byte[]> parts = new ArrayList<>();

        static MultipartBodyBuilder builder() {
            return new MultipartBodyBuilder();
        }

        MultipartBodyBuilder filePart(String name, File f) throws Exception {
            String hdr = "--%s\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n"
                    + "Content-Type: application/xml\r\n\r\n";
            parts.add(hdr.formatted(BOUNDARY, name, f.getName()).getBytes());
            parts.add(java.nio.file.Files.readAllBytes(f.toPath()));
            parts.add("\r\n".getBytes());
            return this;
        }

        HttpRequest build(String url, String token) {
            List<byte[]> all = new ArrayList<>(parts);
            all.add(("--%s--\r\n".formatted(BOUNDARY)).getBytes());

            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(all))
                    .build();
        }
    }
}