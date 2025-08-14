package edu.uiu.foodu.server;

import com.google.gson.reflect.TypeToken;
import edu.uiu.foodu.shared.model.Entities;
import edu.uiu.foodu.shared.model.Entities.*;
import edu.uiu.foodu.shared.net.Protocol;
import edu.uiu.foodu.shared.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FooduServer {
	private static final Logger log = LoggerFactory.getLogger(FooduServer.class);
	private final int port;
	private final ExecutorService pool = Executors.newFixedThreadPool(32);

	private final Map<String, Order> ordersById = new ConcurrentHashMap<>();
	private final List<Order> readyQueue = Collections.synchronizedList(new ArrayList<>());
	private final Map<ShopCode, List<Order>> byShop = new ConcurrentHashMap<>();

	public FooduServer(int port) {
		this.port = port;
		for (ShopCode sc : ShopCode.values()) byShop.put(sc, Collections.synchronizedList(new ArrayList<>()));
	}

	public void start() throws IOException {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			log.info("FOODU server listening on {}", port);
			while (true) {
				Socket client = serverSocket.accept();
				pool.submit(() -> handleClient(client));
			}
		}
	}

	private void handleClient(Socket socket) {
		try (socket;
			 InputStream in = socket.getInputStream();
			 OutputStream out = socket.getOutputStream();
			 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				Protocol.Message msg = Protocol.Message.fromJson(line);
				switch (msg.type()) {
					case PLACE_ORDER -> writer.write(placeOrder(msg.payload()) + "\n");
					case CANCEL_ORDER -> writer.write(cancelOrder(msg.payload()) + "\n");
					case ORDER_STATUS -> writer.write(orderStatus(msg.payload()) + "\n");
					case DISPLAY_TOKENS -> writer.write(displayTokens(msg.payload()) + "\n");
					case MENU_REQUEST -> writer.write(menuResponse() + "\n");
					case COMPLAINT -> writer.write(complaint(msg.payload()) + "\n");
					default -> writer.write(new Protocol.Message(Protocol.Type.ADMIN_DECISION, "{}" ).toJson() + "\n");
				}
				writer.flush();
			}
		} catch (Exception e) {
			log.error("Client handler error", e);
		}
	}

	private String menuResponse() {
		List<MenuItem> items = List.of(
			new MenuItem("K01", ShopCode.K, "Kacchi", "Authentic biryani", 25000, true),
			new MenuItem("N01", ShopCode.N, "Noodles", "Spicy noodles", 15000, true),
			new MenuItem("O01", ShopCode.O, "Coffee", "Hot coffee", 8000, true)
		);
		return new Protocol.Message(Protocol.Type.MENU_RESPONSE, Protocol.GSON.toJson(items)).toJson();
	}

	private String placeOrder(String payload) {
		Type type = TypeToken.getParameterized(List.class, OrderLine.class).getType();
		Map<String, Object> map = Protocol.GSON.fromJson(payload, Map.class);
		ShopCode shop = ShopCode.valueOf((String) map.get("shop"));
		String studentId = (String) map.get("studentId");
		List<Map<String, Object>> rawLines = (List<Map<String, Object>>) map.get("lines");
		List<OrderLine> lines = new ArrayList<>();
		for (Map<String, Object> raw : rawLines) {
			String menuItemId = (String) raw.get("menuItemId");
			int qty = ((Number) raw.get("quantity")).intValue();
			lines.add(new OrderLine(menuItemId, qty));
		}
		int totalCents = ((Number) map.getOrDefault("totalCents", 0)).intValue();
		PaymentMethod pm = PaymentMethod.valueOf((String) map.getOrDefault("paymentMethod", "CASH"));
		String token = TokenUtil.nextToken(shop);
		String id = Entities.generateOrderId();
		Order order = new Order(id, shop, studentId, lines, totalCents, pm, token, Instant.now(), OrderStatus.PLACED);
		ordersById.put(id, order);
		byShop.get(shop).add(order);
		return Protocol.GSON.toJson(new HashMap<String, Object>() {{
			put("ok", true);
			put("orderId", id);
			put("token", token);
		}});
	}

	private String cancelOrder(String payload) {
		Map<String, String> map = Protocol.GSON.fromJson(payload, Map.class);
		Order order = ordersById.get(map.get("orderId"));
		boolean ok = false;
		if (order != null && order.canCancel()) {
			Order cancelled = new Order(order.id(), order.shop(), order.studentId(), order.lines(), order.totalCents(), order.paymentMethod(), order.token(), order.createdAt(), OrderStatus.CANCELLED);
			ordersById.put(order.id(), cancelled);
			ok = true;
		}
		return Protocol.GSON.toJson(Map.of("ok", ok));
	}

	private String orderStatus(String payload) {
		Map<String, String> map = Protocol.GSON.fromJson(payload, Map.class);
		Order order = ordersById.get(map.get("orderId"));
		return Protocol.GSON.toJson(order);
	}

	private String displayTokens(String payload) {
		Map<String, String> map = Protocol.GSON.fromJson(payload, Map.class);
		ShopCode shop = ShopCode.valueOf(map.get("shop"));
		List<Order> list = byShop.get(shop);
		List<String> tokens = new ArrayList<>();
		if (list != null) {
			for (Order o : list) if (o.status() == OrderStatus.PLACED || o.status() == OrderStatus.PREPARING || o.status() == OrderStatus.READY) tokens.add(o.token());
		}
		return Protocol.GSON.toJson(tokens);
	}

	private String complaint(String payload) {
		Complaint c = Protocol.GSON.fromJson(payload, Complaint.class);
		return Protocol.GSON.toJson(Map.of("ok", true, "complaintId", c.id()));
	}

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(System.getProperty("foodu.port", "5050"));
		new FooduServer(port).start();
	}
}