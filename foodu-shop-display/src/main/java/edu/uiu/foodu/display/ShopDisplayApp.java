package edu.uiu.foodu.display;

import edu.uiu.foodu.shared.model.Entities.ShopCode;
import edu.uiu.foodu.shared.net.Protocol;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ShopDisplayApp extends Application {
	private final Timer timer = new Timer(true);
	private ListView<String> list;
	private ChoiceBox<ShopCode> shop;

	@Override
	public void start(Stage stage) {
		shop = new ChoiceBox<>();
		shop.getItems().addAll(ShopCode.values());
		shop.setValue(ShopCode.K);
		list = new ListView<>();
		VBox root = new VBox(8, new Label("Shop:"), shop, new Label("Tokens"), list);
		root.setPadding(new Insets(12));
		stage.setTitle("FOODU Shop Display");
		stage.setScene(new Scene(root, 360, 480));
		stage.show();

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override public void run() { poll(); }
		}, 1000, 2000);
	}

	private void poll() {
		try (Socket s = new Socket("127.0.0.1", Integer.parseInt(System.getProperty("foodu.port", "5050")));
			 BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
			 BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
			String payload = Protocol.GSON.toJson(Map.of("shop", shop.getValue().name()));
			w.write(new Protocol.Message(Protocol.Type.DISPLAY_TOKENS, payload).toJson());
			w.write("\n");
			w.flush();
			String resp = r.readLine();
			String[] tokens = Protocol.GSON.fromJson(resp, String[].class);
			Platform.runLater(() -> {
				list.getItems().setAll(tokens);
			});
		} catch (IOException ignored) { }
	}

	public static void main(String[] args) { launch(args); }
}