package edu.uiu.foodu.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class Db {
	public static DataSource createDataSource(String url, String user, String password) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(url);
		if (user != null) config.setUsername(user);
		if (password != null) config.setPassword(password);
		config.setMaximumPoolSize(10);
		config.setPoolName("FOODU-POOL");
		return new HikariDataSource(config);
	}
}