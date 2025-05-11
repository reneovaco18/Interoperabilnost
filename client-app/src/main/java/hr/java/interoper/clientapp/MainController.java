// src/main/java/hr/java/interoper/clientapp/MainController.java
package hr.java.interoper.clientapp;

import com.fasterxml.jackson.databind.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.http.HttpResponse;                 // already used, but add if missing
import java.net.http.HttpResponse.BodyHandlers;     // <‑‑ for ofString()

import javafx.scene.layout.GridPane;                // <‑‑ for the “edit” dialog

public class MainController {

    /* ── FXML wired ─────────────────────────────────────────────── */
    @FXML private TextField userField, passField;
    @FXML private Label      loginStatus;
    // MainController.java
    @FXML private ToggleGroup schemaToggle;   //  ❌ NO “= new ToggleGroup()”

    @FXML private TextArea   uploadResult, soapResult, weatherArea;
    @FXML private TableView<Map<String,String>> table;   // quick Map‑backed table
    @FXML private Tab        uploadTab, crudTab;
    @FXML private TextField  searchField, cityField;

    /* ── HTTP / JSON helpers ────────────────────────────────────── */
    private final HttpClient  http   = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String accessToken;

    /* ── init ───────────────────────────────────────────────────── */
    @FXML
    private void initialize() {
        uploadTab.setDisable(true);
        crudTab.setDisable(true);

        String[] cols = {"id", "title", "imageUrl", "price", "rating"};

        for (String c : cols) {
            TableColumn<Map<String,String>,String> col = new TableColumn<>(c);
            col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(c)));
            table.getColumns().add(col);
        }
    }
    @FXML
    private void handleSeed() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/admin/seed"))
                    .POST(BodyPublishers.noBody())            // no body needed
                    .build();

            HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
            showPopup("Server says:\n\n"+resp.body());
            loadAll();          // immediately refresh the table
        } catch (Exception ex) {
            showPopup("Seeder call failed: "+ex.getMessage());
        }
    }

    /* ── REGISTER ──────────────────────────────────────────────── */
    @FXML
    private void handleRegister() {
        try {
            String form = "username="+enc(userField.getText())
                    + "&password="+enc(passField.getText());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/auth/register"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            loginStatus.setText(
                    resp.statusCode()==200
                            ? "✅ user created – now log in"
                            : "❌ "+resp.body()
            );

        } catch (Exception ex) { loginStatus.setText("⚠ "+ex); }
    }

    /* ── LOGIN ─────────────────────────────────────────────────── */
    @FXML
    private void handleLogin() {
        try {
            String form = "username="+enc(userField.getText())
                    + "&password="+enc(passField.getText());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100/auth/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode()==200) {
                accessToken = mapper.readTree(resp.body()).get("accessToken").asText();
                loginStatus.setText("✅ logged in");
                uploadTab.setDisable(false);
                crudTab.setDisable(false);
            } else {
                loginStatus.setText("❌ HTTP "+resp.statusCode());
            }

        } catch (Exception ex) { loginStatus.setText("⚠ "+ex); }
    }

    /* ── 1️⃣  Upload XML ─────────────────────────────────────────── */
    @FXML
    private void handleUpload() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML file","*.xml"));
        File file = fc.showOpenDialog(null);
        if (file==null) return;

        boolean useXsd = ((RadioButton)schemaToggle.getSelectedToggle()).getText().contains("XSD");
        String url = useXsd
                ? "http://localhost:8100/api/aliproducts/upload/xsd"
                : "http://localhost:8100/api/aliproducts/upload/rng";

        try {
            HttpRequest req = MultipartBodyBuilder
                    .builder()
                    .filePart("file", file)
                    .build(url, accessToken);

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            uploadResult.setText(
                    "HTTP "+resp.statusCode()+"\n"+resp.body()
            );
        } catch (Exception ex) { uploadResult.setText("⚠ "+ex); }
    }

    /* ── 2️⃣  CRUD ──────────────────────────────────────────────── */
    @FXML
    private void loadAll() {
        reqJson("GET","/api/aliproducts",null).ifPresentOrElse(
                json -> {
                    List<Map<String,String>> rows = new ArrayList<>();
                    json.forEach(n -> {
                        Map<String,String> m = new LinkedHashMap<>();
                        m.put("id",   n.get("id").asText());
                        m.put("title",n.get("title").asText());
                        m.put("price",n.get("price").asText());
                        rows.add(m);
                    });
                    table.setItems(FXCollections.observableArrayList(rows));
                },
                () -> showPopup("Could not load products")
        );
    }

    @FXML
    private void showAddDialog() { editDialog(null); }
    @FXML
    private void showEditDialog() {
        Map<String,String> row = table.getSelectionModel().getSelectedItem();
        if (row!=null) editDialog(row); }

    private void editDialog(Map<String,String> row) {
        Dialog<Map<String,String>> dlg = new Dialog<>();
        dlg.setTitle(row==null ? "Add product" : "Edit product");

        var grid = new GridPane(); grid.setHgap(5); grid.setVgap(5);
        TextField titleF = new TextField(row==null? "" : row.get("title"));
        TextField imgF   = new TextField(row==null? "" : row.get("imageUrl"));
        TextField priceF = new TextField(row==null? "" : row.get("price"));
        TextField rateF  = new TextField(row==null? "" : row.get("rating"));
        grid.addRow(0, new Label("Title"),    titleF);
        grid.addRow(1, new Label("Image URL"),imgF);
        grid.addRow(2, new Label("Price"),    priceF);
        grid.addRow(3, new Label("Rating"),   rateF);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.setResultConverter(bt -> bt==ButtonType.OK
                ? Map.of("title",titleF.getText(),
                "imageUrl",imgF.getText(),
                "price",priceF.getText(),
                "rating",rateF.getText())
                : null);

        dlg.showAndWait().ifPresent(vals -> {
            String body = """
            {
              "title":      "%s",
              "imageUrl":   "%s",
              "price":      %s,
              "rating":     %s
            }""".formatted(
                    vals.get("title").replace("\"","\\\""),
                    vals.get("imageUrl").replace("\"","\\\""),
                    vals.get("price"), vals.get("rating")
            );
            if (row==null)
                req("POST","/api/aliproducts",body);
            else
                req("PUT","/api/aliproducts/"+row.get("id"),body);
            loadAll();
        });
    }


    @FXML
    private void handleDelete() {
        Map<String,String> row = table.getSelectionModel().getSelectedItem();
        if (row!=null) {
            req("DELETE","/api/aliproducts/"+row.get("id"),null);
            loadAll();
        }
    }

    /* ── 3️⃣  SOAP search ───────────────────────────────────────── */
    @FXML
    private void handleSoapSearch() {
        String term = searchField.getText().trim();
        if (term.isEmpty()) return;
        String xml =
                "<getByTitleRequest xmlns=\"http://interoperability.com/ws\" term=\""
                        + term.replace("\"","") + "\"/>";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8100/services"))
                .header("Content-Type","text/xml")
                .POST(BodyPublishers.ofString(xml))
                .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            soapResult.setText(resp.body());
        } catch (Exception ex) { soapResult.setText("⚠ "+ex); }
    }

    /* ── 4️⃣  Weather (XML‑RPC) ─────────────────────────────────── */
    @FXML
    private void handleWeather() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) return;

        String xml =
                "<methodCall><methodName>getTemperature</methodName>"
                        + "<params><param><value><string>"
                        + city.replace("&","&amp;")
                        + "</string></value></param></params></methodCall>";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8100/xmlrpc"))
                .header("Content-Type","text/xml")
                .POST(BodyPublishers.ofString(xml))
                .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            weatherArea.setText(resp.body());
        } catch (Exception ex) { weatherArea.setText("⚠ "+ex); }
    }

    /* ── helpers ────────────────────────────────────────────────── */
    private void showPopup(String msg){ Platform.runLater(() ->
            new Alert(Alert.AlertType.ERROR,msg).showAndWait()); }

    private static String enc(String s){ return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private Optional<JsonNode> reqJson(String verb,String path,String body){
        return req(verb,path,body).flatMap(r -> {
            try { return Optional.of(mapper.readTree(r)); }
            catch(Exception e){ return Optional.empty(); }
        });
    }

    private Optional<String> req(String verb,String path,String body){
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8100"+path))
                    .header("Authorization","Bearer "+accessToken);
            if ("GET".equals(verb)||"DELETE".equals(verb)) b.method(verb,BodyPublishers.noBody());
            else b.header("Content-Type","application/json")
                    .method(verb,BodyPublishers.ofString(body==null?"":body));
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return r.statusCode()<400?Optional.ofNullable(r.body()):Optional.empty();
        } catch(Exception e){ return Optional.empty(); }
    }

}

class MultipartBodyBuilder {
    private static final String BOUNDARY = "----AliExpressBoundary";

    private final List<byte[]> parts = new ArrayList<>();

    static MultipartBodyBuilder builder() { return new MultipartBodyBuilder(); }

    MultipartBodyBuilder filePart(String name, File f) throws Exception {
        String hdr = "--%s\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n"
                + "Content-Type: application/xml\r\n\r\n";
        parts.add(hdr.formatted(BOUNDARY,name,f.getName()).getBytes());
        parts.add(java.nio.file.Files.readAllBytes(f.toPath()));
        parts.add("\r\n".getBytes());
        return this;
    }

    HttpRequest build(String url,String token) {
        byte[] closing = ("--%s--\r\n").formatted(BOUNDARY).getBytes();
        List<byte[]> all = new ArrayList<>(parts); all.add(closing);

        var body = HttpRequest.BodyPublishers.ofByteArrays(all);
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization","Bearer "+token)
                .header("Content-Type","multipart/form-data; boundary="+BOUNDARY)
                .POST(body)
                .build();
    }
}

