package edu.uiu.foodu.client;

import edu.uiu.foodu.shared.net.Protocol;
import edu.uiu.foodu.shared.model.Entities.ShopCode;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FooduClientApp extends Application {
	private TextArea output;

	@Override
	public void start(Stage stage) {
		ChoiceBox<ShopCode> shop = new ChoiceBox<>();
		shop.getItems().addAll(ShopCode.values());
		shop.setValue(ShopCode.K);
		TextField studentId = new TextField();
		studentId.setPromptText("Student ID");
		TextField menuItemId = new TextField();
		menuItemId.setPromptText("Menu Item ID (e.g., K01)");
		TextField qty = new TextField();
		qty.setPromptText("Quantity");
		Button place = new Button("Place Order");
		Button cancel = new Button("Cancel Order");
		TextField orderId = new TextField();
		orderId.setPromptText("Order ID to cancel");
		output = new TextArea();
		output.setEditable(false);

		place.setOnAction(e -> doPlace(shop.getValue(), studentId.getText(), menuItemId.getText(), qty.getText()));
		cancel.setOnAction(e -> doCancel(orderId.getText()));

		VBox root = new VBox(8, new Label("Shop:"), shop, new Label("Student ID"), studentId, new Label("Menu Item ID"), menuItemId, new Label("Qty"), qty, place, new Separator(), new Label("Cancel"), orderId, cancel, new Label("Output"), output);
		root.setPadding(new Insets(12));
		stage.setTitle("FOODU Student Client");
		stage.setScene(new Scene(root, 420, 520));
		stage.show();
	}

	private void doPlace(ShopCode shop, String studentId, String menuItemId, String qtyStr) {
		try {
			Map<String, Object> payload = new HashMap<>();
			payload.put("shop", shop.name());
			payload.put("studentId", studentId);
			payload.put("lines", new Object[]{ Map.of("menuItemId", menuItemId, "quantity", Integer.parseInt(qtyStr)) });
			payload.put("totalCents", 1000);
			payload.put("paymentMethod", "WALLET");
			send(Protocol.Type.PLACE_ORDER, Protocol.GSON.toJson(payload));
		} catch (Exception ex) {
			output.appendText("Error: " + ex.getMessage() + "\n");
		}
	}

	private void doCancel(String orderId) {
		Map<String, String> p = new HashMap<>();
		p.put("orderId", orderId);
		send(Protocol.Type.CANCEL_ORDER, Protocol.GSON.toJson(p));
	}

	private void send(Protocol.Type type, String json) {
		try (Socket s = new Socket("127.0.0.1", Integer.parseInt(System.getProperty("foodu.port", "5050")));
			 BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
			 BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
			w.write(new Protocol.Message(type, json).toJson());
			w.write("\n");
			w.flush();
			String resp = r.readLine();
			output.appendText(resp + "\n");
		} catch (IOException e) {
			output.appendText("Network error: " + e.getMessage() + "\n");
		}
	}

	public static void main(String[] args) { launch(args); }
}