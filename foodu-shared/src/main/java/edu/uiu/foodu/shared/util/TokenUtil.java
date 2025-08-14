package edu.uiu.foodu.shared.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import edu.uiu.foodu.shared.model.Entities.ShopCode;

public final class TokenUtil {
	private static final ConcurrentHashMap<ShopCode, AtomicInteger> counters = new ConcurrentHashMap<>();
	public static String nextToken(ShopCode code) {
		AtomicInteger counter = counters.computeIfAbsent(code, k -> new AtomicInteger(0));
		int value = counter.incrementAndGet();
		return String.format("%s%03d", code.name(), value);
	}
}