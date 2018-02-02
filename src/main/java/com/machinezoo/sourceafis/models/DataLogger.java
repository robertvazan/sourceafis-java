package com.machinezoo.sourceafis.models;

import java.util.function.*;

public class DataLogger {
	private static final ThreadLocal<DataLogger> local = new ThreadLocal<>();
	private static final DataLogger none = new DataLogger();
	public BiConsumer<String, Object> logger;
	public static DataLogger current() {
		DataLogger custom = local.get();
		return custom != null ? custom : none;
	}
	public <T> T get(Supplier<T> supplier) {
		local.set(this);
		try {
			return supplier.get();
		} finally {
			local.set(null);
		}
	}
	public boolean logging() {
		return logger != null;
	}
	public void log(String label, Object data) {
		if (logger != null)
			logger.accept(label, data);
	}
}
