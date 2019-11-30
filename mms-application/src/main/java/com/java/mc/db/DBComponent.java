package com.java.mc.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DBComponent {


	@Value("${vg.batch.url}")
	private String url;
	
	@Bean
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(DataSourceBuilder.create().url(url).build());
	};
}
