package edu.uiu.foodu.admin;

import edu.uiu.foodu.shared.net.Protocol;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AdminApp extends Application {
	private TextArea output;

	@Override
	public void start(Stage stage) {
		TextField orderId = new TextField();
		orderId.setPromptText("Order ID");
		TextArea description = new TextArea();
		description.setPromptText("Complaint description");
		Button send = new Button("File Complaint");
		output = new TextArea();
		output.setEditable(false);

		send.setOnAction(e -> fileComplaint(orderId.getText(), description.getText()));

		VBox root = new VBox(8, new Label("Order ID"), orderId, new Label("Description"), description, send, new Label("Output"), output);
		root.setPadding(new Insets(12));
		stage.setTitle("FOODU Admin Console");
		stage.setScene(new Scene(root, 420, 480));
		stage.show();
	}

	private void fileComplaint(String orderId, String description) {
		try {
			Map<String, Object> c = Map.of(
				"id", UUID.randomUUID().toString(),
				"orderId", orderId,
				"studentId", "admin",
				"description", description,
				"createdAt", Instant.now().toString(),
				"status", "OPEN"
			);
			send(Protocol.Type.COMPLAINT, Protocol.GSON.toJson(c));
		} catch (Exception ex) {
			output.appendText("Error: " + ex.getMessage() + "\n");
		}
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