package edu.uiu.foodu.shared.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Entities {
	public enum ShopCode { K, N, O }

	public record Shop(ShopCode code, String name) {
		public Shop {
			Objects.requireNonNull(code);
			Objects.requireNonNull(name);
		}
	}

	public record MenuItem(String id, ShopCode shop, String name, String description, int priceCents, boolean available) {
		public MenuItem {
			Objects.requireNonNull(id);
			Objects.requireNonNull(shop);
			Objects.requireNonNull(name);
			Objects.requireNonNull(description);
		}
	}

	public enum PaymentMethod { CASH, WALLET, ONLINE }

	public enum OrderStatus { PLACED, PREPARING, READY, CANCELLED, COMPLETED }

	public record OrderLine(String menuItemId, int quantity) {
		public OrderLine {
			Objects.requireNonNull(menuItemId);
			if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
		}
	}

	public record Order(
			String id,
			ShopCode shop,
			String studentId,
			List<OrderLine> lines,
			int totalCents,
			PaymentMethod paymentMethod,
			String token,
			Instant createdAt,
			OrderStatus status
	) {
		public Order {
			Objects.requireNonNull(id);
			Objects.requireNonNull(shop);
			Objects.requireNonNull(studentId);
			Objects.requireNonNull(lines);
			Objects.requireNonNull(paymentMethod);
			Objects.requireNonNull(token);
			Objects.requireNonNull(createdAt);
			Objects.requireNonNull(status);
		}

		public boolean canCancel() {
			return status == OrderStatus.PLACED && Duration.between(createdAt, Instant.now()).toMinutes() < 10;
		}
	}

	public enum ComplaintStatus { OPEN, RESOLVED_REFUND, RESOLVED_REPLACE, REJECTED }

	public record Complaint(String id, String orderId, String studentId, String description, Instant createdAt, ComplaintStatus status) {
		public Complaint {
			Objects.requireNonNull(id);
			Objects.requireNonNull(orderId);
			Objects.requireNonNull(studentId);
			Objects.requireNonNull(description);
			Objects.requireNonNull(createdAt);
			Objects.requireNonNull(status);
		}
	}

	public static String generateOrderId() { return UUID.randomUUID().toString(); }
}