package com.java.mc.utils;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import com.java.mc.bean.DatasourceConfig;

public class DBUtils {
	private static final Logger logger = LoggerFactory.getLogger(DBUtils.class);

	/**
	 * get connection validation sql by sql type.
	 * @param sqlType
	 * @return
	 */
	public static String getCheckSQL(int sqlType) {
		return sqlType == Constants.SQL_TYPE_ORACLE ? "select 1 from dual" : "select 1";
	}

	// get remote database url
	public static String getDBURL(Short sqlType, String host, Integer port, String name, Short authType) {
		StringBuffer url = new StringBuffer("");
		if (sqlType == Constants.SQL_TYPE_ORACLE) {
			url.append("jdbc:oracle:thin:@").append(host).append(":")
					.append(port == null ? Constants.ORACLE_DEFAULT_PORT : port).append(":").append(name);
		} else {
			if (sqlType == Constants.SQL_TYPE_SQLSERVER) {
				url.append("jdbc:sqlserver://").append(host).append(":")
						.append(port == null ? Constants.SQLSERVER_DEFAULT_PORT : port).append(";databaseName=")
						.append(name)
						.append(authType == Constants.SQLSERVER_WINDOWS_AUTH ? ";integratedSecurity=true;" : "");
			}
		}
		return url.toString();
	}

	public static String getSQL(String sql, Short sqlType, String dbArchName, String... tableNames) {
		if (tableNames != null && tableNames.length > 0) {
			for (String tableName : tableNames) {
				sql = sql.replaceFirst(":t", getTableName(sqlType, tableName, dbArchName));
			}
		}
		return sql;
	}

	private static String getTableName(Short sqlType, String tableName, String dbArchName) {
		if (sqlType != null && sqlType == Constants.SQL_TYPE_SQLSERVER) {
			if (dbArchName == null) {
				dbArchName = Constants.SQLSERVER_DEFAULT_ARCHNAME;
			}
			return new StringBuffer(dbArchName).append(".").append(tableName).toString();
		}
		return tableName;
	}
	
	/**
	 * check the mail table whether is exist or not.
	 * 
	 */
	public static void checkMailTable(DatasourceConfig dsc) throws Exception {
		checkTable(dsc, Constants.WEBMAIL_V2);
	}
	
	/**
	 * check the short message table whether is exist or not.
	 * 
	 */
	public static void checkSMTable(DatasourceConfig dsc) throws Exception {
		checkTable(dsc, Constants.WEB_SMS);
	}
	
	/**
	 * check the database table whether is exist or not.
	 * 
	 */
	public static void checkTable(DatasourceConfig dsConfig, String tableName) throws Exception {
		String url = DBUtils.getDBURL(dsConfig.getSqlType(), dsConfig.getHost(), dsConfig.getPort(),
				dsConfig.getDbName(), dsConfig.getAuthType());
		DataSource ds = DataSourceBuilder.create().url(url).username(dsConfig.getUsername())
				.password(dsConfig.getPassword()).build();
		JdbcTemplate jt = new JdbcTemplate(ds);
		jt.execute(DBUtils.getSQL("select 1 from :t", dsConfig.getSqlType(), dsConfig.getArchName(),
				tableName));
	}
}
