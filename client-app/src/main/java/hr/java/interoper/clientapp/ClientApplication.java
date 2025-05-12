
package hr.java.interoper.clientapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxml = new FXMLLoader(
                ClientApplication.class.getResource("MainView.fxml")
        );
        Scene scene = new Scene(fxml.load(), 800, 550);
        stage.setTitle("AliExpress Project Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(); }
}
