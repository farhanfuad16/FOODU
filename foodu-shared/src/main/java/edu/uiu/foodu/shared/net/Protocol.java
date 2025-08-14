package edu.uiu.foodu.shared.net;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public final class Protocol {
	public static final Gson GSON = new Gson();

	public enum Type {
		@SerializedName("PLACE_ORDER") PLACE_ORDER,
		@SerializedName("CANCEL_ORDER") CANCEL_ORDER,
		@SerializedName("ORDER_STATUS") ORDER_STATUS,
		@SerializedName("DISPLAY_TOKENS") DISPLAY_TOKENS,
		@SerializedName("MENU_REQUEST") MENU_REQUEST,
		@SerializedName("MENU_RESPONSE") MENU_RESPONSE,
		@SerializedName("COMPLAINT") COMPLAINT,
		@SerializedName("ADMIN_DECISION") ADMIN_DECISION
	}

	public record Message(Type type, String payload) {
		public String toJson() { return GSON.toJson(this); }
		public static Message fromJson(String json) { return GSON.fromJson(json, Message.class); }
	}
}