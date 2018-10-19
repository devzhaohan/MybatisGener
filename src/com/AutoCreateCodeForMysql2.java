package com;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.MetaDataEntity;

public class AutoCreateCodeForMysql2 {
	private Connection conn = null;

	private String saveToPath = "";

	private String mapperPath = "";

	private String flexBeanPath = "";

	private String flexBeanPackagename = "";

	private String mapperResourcePath;

	private String flexUiPath = "";

	private String tablePrefix = "tb_";

	private boolean usePackage = true;

	// private String[] tableNames = null;
	//
	// private String[] entityNames = null;

	private String[] pkIds = null;

	private String packagename = "packagename";

	private Map<String, String> sequencessMap = new HashMap<String, String>();

	private AutoCreateCodeForMysql2 gener = null;

	private static final String parentEntity = "import com.loveart.base.entity.BaseEntity;";

	public static void main(String[] args) {

		if (args.length < 1) {
			System.out.println("缺少参数centerName");
			return;
		}

		String centerName = args[0];

		AutoCreateCodeForMysql2 gen = new AutoCreateCodeForMysql2();

		Properties prop = new Properties();

		String path = "build.properties";
		InputStream fis;
		try {
			fis = new FileInputStream("build.properties");
			prop.load(fis);
		} catch (IOException e) {
			e.printStackTrace();
		}

		gen.setConn(prop.getProperty(centerName + ".dbDriverClassName"), prop.getProperty(centerName + ".dbUrl"), prop.getProperty(centerName + ".dbUsername"),
				prop.getProperty(centerName + ".dbPassword"));
		gen.setSaveToPath("src/" + prop.getProperty("entity.package").replaceAll("\\.", "/"));
		gen.setFlexBeanPath("src/" + prop.getProperty("entity.flexPath").replaceAll("\\.", "/"));
		gen.setMapperPath("src/" + prop.getProperty("entity.mapperPath").replaceAll("\\.", "/"));
		gen.setFlexUiPath("src/" + prop.getProperty("entity.flexUiPath").replaceAll("\\.", "/"));
		gen.setFlexBeanPackagename(prop.getProperty("entity.flexPath"));
		gen.setMapperResourcePath(prop.getProperty("entity.mapperPath"));
		// gen.setTableNames(tables);
		// gen.setEntityNames(entitys);
		// gen.setPkIds(pkIds);
		gen.setPackagename(prop.getProperty("entity.package"));
		
		gen.setTablePrefix(prop.getProperty("entity.tablePrefix"));

		System.out.println("============" + gen.packagename + "==============");
		try {
			try {
				gen.startProcess();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		gen.closeConn();
	}

	public String getMapperResourcePath() {
		return mapperResourcePath;
	}

	public void setMapperResourcePath(String mapperResourcePath) {
		this.mapperResourcePath = mapperResourcePath;
	}
	

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public AutoCreateCodeForMysql2() {
	}

	public AutoCreateCodeForMysql2(Connection conn, String saveToPath, String[] tableNames, String packagename, String[] entityNames, String[] pkIds) {
		gener.conn = conn;
		gener.saveToPath = saveToPath;
		// gener.tableNames = tableNames;
		gener.packagename = packagename;
		// gener.entityNames = entityNames;
		gener.pkIds = pkIds;
	}

	/**
	 * jdbc connection
	 * 
	 * @param conn
	 */
	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public void setConn(String classname, String uri, String user, String pass) {
		try {
			Class.forName(classname);
			conn = DriverManager.getConnection(uri, user, pass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void closeConn() {
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setSaveToPath(String saveToPath) {
		this.saveToPath = saveToPath;
	}

	// public void setTableNames(String[] tableNames) {
	// this.tableNames = tableNames;
	// }

	public void setPackagename(String packagename) {
		this.packagename = packagename;
	}

	/**
	 * Start process1.
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 * @throws IOException
	 */
	public void startProcess() throws SQLException, IOException {
		Statement stmt = conn.createStatement();

		String sql = "show tables;";

		ResultSet rs = null;

		rs = stmt.executeQuery(sql);

		String tableName = "";

		List<String> tableNames = new ArrayList<String>();

		while (rs.next()) {
			// 排除不同数据库的列名不同影响，因此此处使用index
			tableName = rs.getString(1);
			tableNames.add(tableName);
		}

		for (int i = 0; i < tableNames.size(); i++) {

			String element = tableNames.get(i);

			
			// TODO  略过tb_cde_dlvorg_freq
			if ("tb_cde_dlvorg_freq".equalsIgnoreCase(element)) {
				continue;
			}

			sql = "show create table " + element + ";";

			rs = stmt.executeQuery(sql);

			String create_sql = null;
			if (rs.next()) {
				create_sql = rs.getString("Create Table");
			}

			// 根据建表语句获取分库分表字段
			String dbpartition_name = getDbpartitionName(create_sql);
			String tbpartition_name = getTbpartitionName(create_sql);

			sql = "show full columns from  " + element + ";";

			rs = stmt.executeQuery(sql);

			MetaDataEntity metadata = null;
			List<MetaDataEntity> createColums = new ArrayList<MetaDataEntity>();
			String dataType = "";

			// 主键
			String pkName = "";
			String autoIncrement = "auto_increment";
			String extra = "";

			while (rs.next()) {
				metadata = new MetaDataEntity();
				metadata.setColumnName(rs.getString("Field"));
				dataType = rs.getString("Type");
				System.out.println(rs.getString("Field") + "===============" + "dataType==" + dataType);
				metadata.setAllDataType(dataType);
				metadata.setComments(rs.getString("Comment"));
				String defaultValue = rs.getString("Default");
				if (defaultValue == null || defaultValue.trim().equalsIgnoreCase("")) {
					defaultValue = null;
				}
				metadata.setDefaultValue(defaultValue);

				extra = rs.getString("Extra");
				if (extra.equalsIgnoreCase(autoIncrement)) {
					pkName = rs.getString("Field");
					System.out.println(element + "====" + "主键：" + pkName);
				}

				if (dataType.indexOf("(") != -1) {
					metadata.setDataType(dataType.substring(0, dataType.indexOf("(")));
					System.out.println(metadata.getDataType());
					if (dataType.substring(dataType.indexOf("(")).indexOf(",") != -1) {
						metadata.setDataLength(Integer.valueOf(dataType.substring(dataType.indexOf("(")).split(",")[0].substring(1)));
						metadata.setDataPrecision(Integer.valueOf(dataType.substring(dataType.indexOf("(")).split(",")[0].substring(1)));
						metadata.setDataScale(Integer.valueOf(dataType.substring(dataType.indexOf("(")).split(",")[1].substring(0, 1)));
					} else {
						metadata.setDataLength(Integer.valueOf(dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")"))));
						metadata.setDataPrecision(Integer.valueOf(dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")"))));
						metadata.setDataScale(null);
					}
				} else {
					metadata.setDataType(dataType);
					metadata.setDataPrecision(null);
					metadata.setDataScale(null);
				}

				// 判断分库
				if (rs.getString("Field").equalsIgnoreCase(dbpartition_name)) {
					metadata.setDbPartition(true);
				}
				// 判断分表
				if (rs.getString("Field").equalsIgnoreCase(tbpartition_name)) {
					metadata.setTbPartition(true);
				}

				createColums.add(metadata);
			}

			String entityName = getEntityName(element);
			if (entityName != null) {
				System.out.println("表=" + element + " >>entity=" + entityName);
			} else if (null != this.tablePrefix && !"".equalsIgnoreCase(this.tablePrefix)) {
				entityName = getCamelCaseString(element.toLowerCase().replaceAll(this.tablePrefix, "").toLowerCase(), true);
			} else {
				entityName = getCamelCaseString(element.toLowerCase(), true);
			}
			System.out.println("表=" + element + " >>entity=" + entityName);

			// base 的xml
			produceXml(createColums, element, entityName, pkName);
			// ext 的xml
			produceExtXml(createColums, element, entityName, pkName);
			produceJavaFile(createColums, element, this.packagename, entityName, pkName);
			// createFlexBeanFile(createColums, element, this.packagename,
			// getCamelCaseString(tableName.toLowerCase(), true),"SID");
			createTypeAliasXml(entityName, i);
			createMapperResourceXml(entityName, i);
			createRemotingConfigXml(entityName, i);

		}
	}

	/**
	 * 根据建表语句获取分表字段
	 * 
	 * eg: tbpartition by hash(`id`)
	 * 
	 * @param create_sql
	 *            建表语句
	 * @return 小写的字段名称
	 */
	private String getTbpartitionName(String create_sql) {
		String str = "tbpartition by hash";
		if (create_sql == null || create_sql.equalsIgnoreCase("")) {
			return null;
		}
		create_sql = create_sql.toLowerCase();
		if (create_sql.indexOf(str) != -1) {
			String sql = create_sql.substring(create_sql.indexOf(str));
			if (sql.indexOf("(") != -1 && sql.indexOf(")") != -1) {
				sql = sql.substring(sql.indexOf("(") + 1, sql.indexOf(")"));
				sql = sql.replace("`", "");
				return sql;
			}
		}
		return null;
	}

	/**
	 * 根据建表语句获取分库字段
	 * 
	 * eg: dbpartition by hash(`company_id`)
	 * 
	 * @param create_sql
	 *            建表语句
	 * @return 小写的字段名称
	 */
	private String getDbpartitionName(String create_sql) {
		String str = "dbpartition by hash";
		if (create_sql == null || create_sql.equalsIgnoreCase("")) {
			return null;
		}
		create_sql = create_sql.toLowerCase();
		if (create_sql.indexOf(str) != -1) {
			String sql = create_sql.substring(create_sql.indexOf(str));
			if (sql.indexOf("(") != -1 && sql.indexOf(")") != -1) {
				sql = sql.substring(sql.indexOf("(") + 1, sql.indexOf(")"));
				sql = sql.replace("`", "");
				return sql;
			}
		}
		return null;
	}

	private void createRemotingConfigXml(String entityName, int i) {
		File file = new File(this.mapperPath + "\remotingConfig.xml");
		if (i == 0) {
			if (file.exists()) {
				file.delete();
			} else {
				file = new File(mapperPath + "\remotingConfig.xml");
				file.mkdir();
			}
		} else {
			file = new File(mapperPath + "\remotingConfig.xml");
			if (!file.exists()) {
				file.mkdir();
			}
		}
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("\t");
		sb.append("<destination id=\"");
		sb.append(entityName.substring(0, 1).toLowerCase() + entityName.substring(1, entityName.length()) + "Service");
		sb.append("\"");
		sb.append(">");
		sb.append(newline);
		sb.append("\t\t");
		sb.append("<properties>");
		sb.append(newline);
		sb.append("\t\t\t");
		sb.append("<factory>spring</factory>");
		sb.append(newline);
		sb.append("\t\t\t");
		sb.append("<source>");
		sb.append(entityName.substring(0, 1).toLowerCase() + entityName.substring(1, entityName.length()) + "Service");
		sb.append("</source>");
		sb.append(newline);
		sb.append("\t\t");
		sb.append("</properties>");
		sb.append(newline);
		sb.append("\t");
		sb.append("</destination>");
		sb.append(newline);
		writefileToEnd(saveToPath + "remotingConfig.xml", sb.toString());

	}

	/**
	 * create typeAliasXml
	 * 
	 * @param entityName
	 * @param i
	 * @throws IOException
	 */
	private void createTypeAliasXml(String entityName, int i) throws IOException {
		File file = new File(this.mapperPath + "\typeAlias.xml");
		if (i == 0) {
			if (file.exists()) {
				file.delete();
			} else {
				file = new File(mapperPath + "\typeAlias.xml");
				file.mkdir();
			}
		} else {
			file = new File(mapperPath + "\typeAlias.xml");
			if (!file.exists()) {
				file.mkdir();
			}
		}
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<typeAlias alias=\"");
		sb.append(entityName);
		sb.append("\"");
		sb.append(" type=\"");
		sb.append(packagename);
		sb.append(".");
		sb.append(entityName + "\"");
		sb.append("/>");
		sb.append(newline);
		writefileToEnd(saveToPath + "typeAlias.xml", sb.toString());
	}

	private void createMapperResourceXml(String entityName, int i) {
		File file = new File(this.mapperPath + "\typeAliasMapper.xml");
		if (i == 0) {
			if (file.exists()) {
				file.delete();
			} else {
				file = new File(mapperPath + "\typeAliasMapper.xml");
				file.mkdir();
			}
		} else {
			file = new File(mapperPath + "\typeAliasMapper.xml");
			if (!file.exists()) {
				file.mkdir();
			}
		}
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<mapper resource=\"");
		sb.append(this.mapperResourcePath.replaceAll("\\.", "/"));
		sb.append("/");
		sb.append(entityName);
		sb.append("Ext.xml");
		sb.append("\"");
		sb.append("/>");
		sb.append(newline);
		writefileToEnd(saveToPath + "typeAliasMapper.xml", sb.toString());
	}

	/**
	 * create mybatis mapper xml
	 * 
	 * @param metaData
	 * @param tableName
	 * @param entityName
	 */
	private void produceXml(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) {

		System.out.println("生成表" + tableName + "的Mapper");
		System.out.println(entityName);
		System.out.println(pkId);
		try {
			File file = new File(mapperPath + "/base");
			makeDir(file);
			String fileName = "";
			if (!(mapperPath.endsWith(File.separator) || mapperPath.endsWith("/") || mapperPath.endsWith("\\"))) {
				if (null == entityName || entityName.equalsIgnoreCase("")) {
					fileName = mapperPath + "/base" + File.separator + getCamelCaseString(tableName.toLowerCase(), true) + ".xml";
				} else {
					fileName = mapperPath + "/base" + File.separator + entityName + "Mapper" + ".xml";

				}

			} else {
				if (null == entityName || entityName.equalsIgnoreCase("")) {
					fileName = mapperPath + File.separator + getCamelCaseString(tableName.toLowerCase(), true) + ".xml";
				} else {
					fileName = mapperPath + File.separator + entityName + "Mapper" + ".xml";

				}
			}
			System.out.println("fileName :" + fileName);
			file = new File(fileName);
			StringBuilder sb = new StringBuilder();
			String newline = System.getProperty("line.separator");
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?> ");
			sb.append(newline);
			sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >");
			sb.append(newline);
			sb.append("<mapper namespace=\"");
			if (null == entityName || entityName.equalsIgnoreCase("")) {
				sb.append(getCamelCaseString(tableName, false));
			} else {
				if (usePackage) {
					sb.append(this.packagename + "." + entityName);
				} else {
					sb.append(entityName);
				}
			}
			sb.append("\">");
			sb.append(newline);
			sb.append("\t");
			sb.append(newline);
			sb.append(createResultMap(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createBaseWhereClause(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createInsertSql(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createInsertBatchSql(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createUpdateSql(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createUpdateBatchSql(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createDeleteSql(metaData, tableName, pkId));
			sb.append(newline);
			sb.append(createDeleteByParamsSql(metaData, tableName, pkId));
			sb.append(newline);
			sb.append(createSelectSql(metaData, tableName, entityName));
			sb.append(newline);
			sb.append(createFindCountSql(tableName));
			sb.append(newline);
			sb.append("</mapper>");

			writefile(file, sb.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * create mybatis mapper xml
	 * 
	 * @param metaData
	 * @param tableName
	 * @param entityName
	 */
	private void produceExtXml(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) {

		System.out.println("生成表" + tableName + "的Mapper");
		System.out.println(entityName);
		System.out.println(pkId);
		try {
			File file = new File(mapperPath + "/ext");
			makeDir(file);
			String fileName = mapperPath + "/ext" + File.separator + entityName + "Ext.xml";
			System.out.println("fileName :" + fileName);
			file = new File(fileName);
			StringBuilder sb = new StringBuilder();
			String newline = System.getProperty("line.separator");
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?> ");
			sb.append(newline);
			sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >");
			sb.append(newline);
			sb.append("<mapper namespace=\"");
			if (null == entityName || entityName.equalsIgnoreCase("")) {
				sb.append(getCamelCaseString(tableName, false));
			} else {
				if (usePackage) {
					sb.append("sqlmapper." + entityName);
				} else {
					sb.append(entityName);
				}
			}
			sb.append("\">");
			sb.append(newline);
			sb.append("\t");
			sb.append(newline);
			sb.append(createResultMap(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createBaseWhereClause(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createInsertSql(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createUpdateSql(metaData, tableName, entityName, pkId));
			sb.append(newline);
			sb.append(createDeleteSql(metaData, tableName, pkId));
			sb.append(newline);
			sb.append(createDeleteByParamsSql(metaData, tableName, pkId));
			sb.append(newline);
			sb.append(createSelectSql(metaData, tableName, entityName));
			sb.append(newline);
			// sb.append(createFindCountSql(tableName));
			sb.append(newline);
			sb.append("</mapper>");

			writefile(file, sb.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * create java enrity
	 * 
	 * @param metadata
	 * @param tableName
	 * @param packagename
	 */
	private void produceJavaFile(List<MetaDataEntity> metaData, String tableName, String packagename, String entityName, String pkId) {
		try {
			File file = new File(saveToPath);
			makeDir(file);
			if (!(saveToPath.endsWith(File.separator) || saveToPath.endsWith("/") || saveToPath.endsWith("\\"))) {
				// file = new File(saveToPath + File.separator
				// + getCamelCaseString(tableName, true) + ".java");
				file = new File(saveToPath + File.separator + entityName + ".java");
			} else {
				// file = new File(saveToPath + File.separator
				// + getCamelCaseString(tableName, true) + ".java");
				file = new File(saveToPath + File.separator + entityName + ".java");
			}
			StringBuilder sb = new StringBuilder();
			String newline = System.getProperty("line.separator");
			int metaDataCount = metaData.size();
			sb.append("package	");
			sb.append(packagename);
			sb.append(";");
			sb.append(newline);
			sb.append(newline);
			sb.append("import java.util.Date;");
			sb.append(newline);
			sb.append(parentEntity);

			if (pkId != null && !pkId.equalsIgnoreCase("")) {
				sb.append(newline);
				sb.append("import org.apache.commons.lang3.builder.EqualsBuilder;");
				sb.append(newline);
				sb.append("import org.apache.commons.lang3.builder.HashCodeBuilder;");
			}

			sb.append(newline);
			sb.append(newline);
			sb.append("public class ");
			sb.append(entityName);
			sb.append("  extends BaseEntity ");
			sb.append("");
			sb.append(" {");
			sb.append(newline);

			sb.append(newline);

			sb.append("	private static final long serialVersionUID = -1L;");
			sb.append(newline);

			sb.append(newline);

			for (MetaDataEntity metaDataEntity : metaData) {
				if (!"sid".equalsIgnoreCase(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false))) {
					sb.append("\t");
					sb.append("/**");
					sb.append(newline);
					sb.append("\t");
					sb.append(" * ");
					sb.append(metaDataEntity.getComments());
					sb.append(newline);
					sb.append("\t");
					sb.append(" */");
					sb.append(newline);
					sb.append("\t");
					sb.append("private ");
					sb.append(convertDataType(metaDataEntity.getDataType(), metaDataEntity.getDataPrecision(), metaDataEntity.getDataScale()));
					sb.append(" ");
					sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false));
					// 设置默认值
					if (metaDataEntity.getDefaultValue() != null) {
						sb.append(" = ");
						sb.append(getDefaultValue(metaDataEntity.getDataType(), metaDataEntity.getDataPrecision(), metaDataEntity.getDataScale(),
								metaDataEntity.getDefaultValue()));
					}
					sb.append(";");
					sb.append(newline);
				}
			}
			// set get
			for (MetaDataEntity metaDataEntity : metaData) {
				if (!"sid".equalsIgnoreCase(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false))) {
					sb.append(newline);
					sb.append("\t");
					sb.append("public  ");
					sb.append(convertDataType(metaDataEntity.getDataType(), metaDataEntity.getDataPrecision(), metaDataEntity.getDataScale()));
					sb.append(" ");
					sb.append("get");
					sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), true));
					sb.append("() {");
					sb.append(newline);
					sb.append("\t");
					sb.append("\t");
					sb.append("return ");
					sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false));
					sb.append(";");
					sb.append(newline);
					sb.append("\t");
					sb.append("}");
					sb.append(newline);
					sb.append(newline);
					sb.append("\t");
					sb.append("public  void  ");
					sb.append("set");
					sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), true));
					sb.append("(");
					sb.append(convertDataType(metaDataEntity.getDataType(), metaDataEntity.getDataPrecision(), metaDataEntity.getDataScale()));
					sb.append("  ");
					sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false));
					sb.append(") {");
					sb.append(newline);
					sb.append("\t");
					sb.append("\t");
					sb.append("this.");
					sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false));
					sb.append(" = ");
					sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false));
					sb.append(";");
					sb.append(newline);
					sb.append("\t");
					sb.append("}");
					sb.append(newline);
				}
			}
			// 重写equle和hashCode方法

			if (pkId != null && !pkId.equalsIgnoreCase("")) {
				sb.append("\t");
				sb.append("/**");
				sb.append(newline);
				sb.append("\t");
				sb.append("* 重写equals方法");
				sb.append(newline);
				sb.append("\t");
				sb.append("*/");
				sb.append(newline);
				sb.append("\t");
				sb.append("public boolean equalsIgnoreCase(Object arg0) {");
				sb.append(newline);
				sb.append("\t\t");
				sb.append("if (!(arg0 instanceof " + entityName + ")) {");
				sb.append(newline);
				sb.append("\t\t\t");
				sb.append(" return false; ");
				sb.append(newline);
				sb.append("\t\t");
				sb.append("}");
				sb.append(newline);
				sb.append("\t\t");
				sb.append(entityName + " o = (" + entityName + ") arg0 ;");
				sb.append(newline);
				sb.append("\t\t");
				sb.append("return new EqualsBuilder().append(this.get");
				sb.append(getCamelCaseString(pkId.toLowerCase(), true));
				sb.append("(),o.get");
				sb.append(getCamelCaseString(pkId.toLowerCase(), true));
				sb.append("()).isEquals();");
				sb.append(newline);
				sb.append("\t");
				sb.append("}");
				sb.append(newline);
				sb.append("\t");
				sb.append("/**");
				sb.append(newline);
				sb.append("\t");
				sb.append("* 重写hashCode方法");
				sb.append(newline);
				sb.append("\t");
				sb.append("*/");
				sb.append(newline);
				sb.append("\t");
				sb.append("public int hashCode() {");
				sb.append(newline);
				sb.append("\t\t");
				sb.append("return new HashCodeBuilder(61, 15).append(this.get");
				sb.append(getCamelCaseString(pkId.toLowerCase(), true));
				sb.append("()).toHashCode();");
				sb.append(newline);
				sb.append("\t");
				sb.append("}");
				sb.append(newline);
			}

			
			
			// 自定义getRealClass方法
			sb.append("\t");
			sb.append("/**");
			sb.append(newline);
			sb.append("\t");
			sb.append("* 自定义getRealClass方法");
			sb.append(newline);
			sb.append("\t");
			sb.append("*/");
			sb.append(newline);
			sb.append("\t");
			sb.append("public Class<?> getRealClass() {");
			sb.append(newline);
			sb.append("\t\t");
			sb.append("return this.getClass();");
			sb.append(newline);
			sb.append("\t");
			sb.append("}");
			sb.append(newline);
			
			sb.append("}");
			sb.append(newline);
			writefile(file, sb.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * create flex entity bean
	 * 
	 * @param metadata
	 * @param tableName
	 * @param packagename
	 * @param entityName
	 */
	private void createFlexBeanFile(List<MetaDataEntity> metaData, String tableName, String packagename, String entityName, String pkId) {
		try {
			File file = new File(this.flexBeanPath);
			makeDir(file);
			if (!(flexBeanPath.endsWith(File.separator) || flexBeanPath.endsWith("/") || flexBeanPath.endsWith("\\"))) {
				file = new File(flexBeanPath + File.separator + entityName + ".as");
			} else {
				file = new File(flexBeanPath + File.separator + entityName + ".as");
			}
			StringBuilder sb = new StringBuilder();
			String newline = System.getProperty("line.separator");
			int metaDataCount = metaData.size();
			sb.append("package	");
			sb.append(this.getFlexBeanPackagename());
			sb.append(newline);
			sb.append("{");
			sb.append(newline);
			sb.append("\t\t");
			sb.append("[RemoteClass(alias=\"" + packagename + "." + entityName + "\")]");
			sb.append("\t\t");
			sb.append(newline);
			sb.append("\t\t");
			sb.append("public class ");
			sb.append(entityName);
			sb.append(" {");
			sb.append(newline);
			if (!"SID".equalsIgnoreCase(pkId)) {
				sb.append("\t\t\t");
				sb.append("public  var");
				sb.append(" sid");
				sb.append(":");
				sb.append("Number");
				sb.append(";");
				sb.append(newline);
			}
			for (MetaDataEntity metaDataEntity : metaData) {
				sb.append("\t\t\t");
				sb.append("public  var ");
				sb.append(getCamelCaseString(metaDataEntity.getColumnName().toLowerCase(), false));
				sb.append(":");
				sb.append(this.convertFlexDateType(metaDataEntity.getDataType()));
				sb.append(";");
				sb.append(newline);
			}
			sb.append("\t\t");
			sb.append("}");
			sb.append(newline);
			sb.append("}");
			writefile(file, sb.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method description
	 * 
	 * 
	 * @param metadata
	 * @param tableName
	 * 
	 * @return
	 * 
	 * @throws SQLException
	 */
	private String createInsertSql(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) throws SQLException {
		int metaDataCount = metaData.size();
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("\n");
		sb.append("<insert id=\"");
		sb.append("insert\" parameterType=\"");
		if (usePackage) {
			sb.append(this.packagename + "." + entityName);
		} else {
			sb.append(entityName);
		}
		sb.append("\">");
		sb.append(newline);
		if (pkId != null && !"".equalsIgnoreCase(pkId)) {
			sb.append(" <selectKey keyProperty=\"sid\" order=\"AFTER\" resultType=\"java.lang.Long\">");
			sb.append(newline);
			sb.append(" SELECT @@IDENTITY");
			sb.append(newline);
			sb.append(" </selectKey>");
			sb.append(newline);
		}
		sb.append("\t\t");
		sb.append("INSERT INTO  ");
		sb.append(tableName);
		sb.append(" (");
		sb.append(newline);
		sb.append("\t");
		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t\t");
			sb.append(metaData.get(i).getColumnName());
			if (i != metaDataCount - 1) {
				sb.append(",");
			}
			if (i % 5 == 0) {
				sb.append(newline);
				sb.append("\t");
			}
		}
		sb.append(")");
		sb.append(newline);
		sb.append(" values( ");
		sb.append(newline);
		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t");
			sb.append("#{");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(",jdbcType=");
			sb.append(convertNullableType(metaData.get(i).getDataType()));
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			if (i + 1 != metaDataCount) {
				sb.append(",");
			}
			if (i + 1 % 5 == 0) {
				sb.append(newline);
			}
		}
		sb.append(" ) ");
		sb.append(newline);
		sb.append("</insert>");
		sb.append(newline);

		return sb.toString();

	}
	
	private String createInsertBatchSql(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) throws SQLException {
		int metaDataCount = metaData.size();
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("\n");
		sb.append("<insert id=\"");
		sb.append("insertBatch\" parameterType=\"");
		
		sb.append("java.util.List");
		
		sb.append("\">");
		sb.append(newline);
		if (pkId != null && !"".equalsIgnoreCase(pkId)) {
			sb.append("\t<selectKey keyProperty=\"sid\" order=\"AFTER\" resultType=\"java.lang.Long\">");
			sb.append(newline);
			sb.append("\t\t SELECT @@IDENTITY");
			sb.append(newline);
			sb.append("\t </selectKey>");
			sb.append(newline);
		}
		sb.append("\t\t");
		sb.append("INSERT INTO  ");
		sb.append(tableName);
		sb.append(" (");
		sb.append(newline);
		sb.append("\t");
		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t\t");
			sb.append(metaData.get(i).getColumnName());
			if (i != metaDataCount - 1) {
				sb.append(",");
			}
			if (i % 5 == 0) {
				sb.append(newline);
				sb.append("\t");
			}
		}
		sb.append(")");
		sb.append(newline);
		sb.append(" values ");
		sb.append(newline);
		sb.append("<foreach collection=\"list\" item=\"item\" index=\"index\" separator=\",\" >");
		sb.append(newline);  
		sb.append(" ( ");
		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t");
			sb.append("#{item.");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(",jdbcType=");
			sb.append(convertNullableType(metaData.get(i).getDataType()));
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			if (i + 1 != metaDataCount) {
				sb.append(",");
			}
			if (i + 1 % 5 == 0) {
				sb.append(newline);
			}
		}
		sb.append(" ) ");
		sb.append(newline);
		sb.append("</foreach>");
		sb.append(newline);
		sb.append("</insert>");
		sb.append(newline);

		return sb.toString();

	}

	/**
	 * Creates the update sql.
	 * 
	 * @param metadata
	 *            the metadata
	 * @param tableName
	 *            the table name
	 * 
	 * @return the string
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	private String createUpdateSql(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) throws SQLException {
		if (pkId == null || pkId.equalsIgnoreCase("")) {
			return "";
		}

		MetaDataEntity tbpartition = null;
		MetaDataEntity dbpartition = null;

		int metaDataCount = metaData.size();
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<update id=\"");
		sb.append("update\" parameterType=\"");
		if (usePackage) {
			sb.append(this.packagename + "." + entityName);
		} else {
			sb.append(entityName);
		}
		sb.append("\">");
		sb.append(newline);
		sb.append("UPDATE");
		sb.append(newline);
		sb.append("\t");
		sb.append(tableName);
		sb.append(newline);
		sb.append("\t");
		sb.append("<set>");
		sb.append(newline);
		for (int i = 0; i < metaDataCount; i++) {

			// 主键不能修改
			if (metaData.get(i).getColumnName().equalsIgnoreCase(pkId)) {
				continue;
			}

			// 分库键不能修改
			if (metaData.get(i).isDbPartition()) {
				dbpartition = metaData.get(i);
				continue;
			}

			// 分表键不能修改
			if (metaData.get(i).isTbPartition()) {
				tbpartition = metaData.get(i);
				continue;
			}

			sb.append("\t\t");
			sb.append("<if test=\"");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(" != null");
			sb.append("\">");
			sb.append(newline);
			sb.append("\t\t\t");
			sb.append(metaData.get(i).getColumnName());
			sb.append(" = ");
			sb.append("#{");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(",");
			sb.append("jdbcType=");
			sb.append(convertNullableType(metaData.get(i).getDataType()));
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			if (i + 1 != metaDataCount) {
				sb.append(",");
			}
			sb.append(newline);
			sb.append("\t\t");
			sb.append("</if>");
			sb.append(newline);
		}
		sb.append("\t");
		sb.append("</set>");
		sb.append(newline);
		sb.append("\t");
		sb.append(" WHERE ");
		sb.append(newline);
		sb.append("\t\t ");
		sb.append(pkId);
		sb.append(" = #{");
		sb.append(getCamelCaseString(pkId.toLowerCase(), false));
		sb.append(",jdbcType=BIGINT}");
		sb.append(newline);

		// 分库键
		if (dbpartition != null) {
			sb.append("\t\t AND ");
			sb.append(dbpartition.getColumnName());
			sb.append(" = ");
			sb.append("#{");
			sb.append(getCamelCaseString(dbpartition.getColumnName().toLowerCase(), false));
			sb.append(",");
			sb.append("jdbcType=");
			sb.append(convertNullableType(dbpartition.getDataType()));
			if (dbpartition.getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (dbpartition.getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			sb.append(newline);

			// 分表键
			if (tbpartition != null && !tbpartition.getColumnName().equalsIgnoreCase(dbpartition.getColumnName())) {
				// 分表键不为空并且和分库键不同
				sb.append("\t\t AND ");
				sb.append(tbpartition.getColumnName());
				sb.append(" = ");
				sb.append("#{");
				sb.append(getCamelCaseString(tbpartition.getColumnName().toLowerCase(), false));
				sb.append(",");
				sb.append("jdbcType=");
				sb.append(convertNullableType(tbpartition.getDataType()));
				if (tbpartition.getDataType().equalsIgnoreCase("CLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
				}
				if (tbpartition.getDataType().equalsIgnoreCase("BLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
				}
				sb.append("}");
				sb.append(newline);
			}
		} else {
			// 分表键
			if (tbpartition != null) {
				// 分表键不为空
				sb.append("\t\t AND ");
				sb.append(tbpartition.getColumnName());
				sb.append(" = ");
				sb.append("#{");
				sb.append(getCamelCaseString(tbpartition.getColumnName().toLowerCase(), false));
				sb.append(",");
				sb.append("jdbcType=");
				sb.append(convertNullableType(tbpartition.getDataType()));
				if (tbpartition.getDataType().equalsIgnoreCase("CLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
				}
				if (tbpartition.getDataType().equalsIgnoreCase("BLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
				}
				sb.append("}");
				sb.append(newline);
			}
		}

		sb.append("</update>");
		sb.append(newline);

		return sb.toString();
	}

	private String createUpdateBatchSql(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) throws SQLException {
		if (pkId == null || pkId.equalsIgnoreCase("")) {
			return "";
		}

		MetaDataEntity tbpartition = null;
		MetaDataEntity dbpartition = null;

		int metaDataCount = metaData.size();
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<update id=\"");
		sb.append("updateBatch\" parameterType=\"");

		sb.append("java.util.List");

		sb.append("\">");
		sb.append(newline);

		sb.append("<foreach collection=\"list\" item=\"item\" index=\"index\" separator=\",\" >");
		sb.append(newline);

		sb.append("UPDATE");
		sb.append(newline);
		sb.append("\t");
		sb.append(tableName);
		sb.append(newline);
		sb.append("\t");
		sb.append("<set>");
		sb.append(newline);
		for (int i = 0; i < metaDataCount; i++) {

			// 主键不能修改
			if (metaData.get(i).getColumnName().equalsIgnoreCase(pkId)) {
				continue;
			}

			// 分库键不能修改
			if (metaData.get(i).isDbPartition()) {
				dbpartition = metaData.get(i);
				continue;
			}

			// 分表键不能修改
			if (metaData.get(i).isTbPartition()) {
				tbpartition = metaData.get(i);
				continue;
			}

			sb.append("\t\t");
			sb.append("<if test=\"item.");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(" != null");
			sb.append("\">");
			sb.append(newline);
			sb.append("\t\t\t");
			sb.append(metaData.get(i).getColumnName());
			sb.append(" = ");
			sb.append("#{item.");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(",");
			sb.append("jdbcType=");
			sb.append(convertNullableType(metaData.get(i).getDataType()));
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			if (i + 1 != metaDataCount) {
				sb.append(",");
			}
			sb.append(newline);
			sb.append("\t\t");
			sb.append("</if>");
			sb.append(newline);
		}
		sb.append("\t");
		sb.append("</set>");
		sb.append(newline);
		sb.append("\t");
		sb.append(" WHERE ");
		sb.append(newline);
		sb.append("\t\t ");
		sb.append(pkId);
		sb.append(" = #{item.");
		sb.append(getCamelCaseString(pkId.toLowerCase(), false));
		sb.append(",jdbcType=BIGINT}");
		sb.append(newline);

		// 分库键
		if (dbpartition != null) {
			sb.append("\t\t AND ");
			sb.append(dbpartition.getColumnName());
			sb.append(" = ");
			sb.append("#{item.");
			sb.append(getCamelCaseString(dbpartition.getColumnName().toLowerCase(), false));
			sb.append(",");
			sb.append("jdbcType=");
			sb.append(convertNullableType(dbpartition.getDataType()));
			if (dbpartition.getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (dbpartition.getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			sb.append(newline);

			// 分表键
			if (tbpartition != null && !tbpartition.getColumnName().equalsIgnoreCase(dbpartition.getColumnName())) {
				// 分表键不为空并且和分库键不同
				sb.append("\t\t AND ");
				sb.append(tbpartition.getColumnName());
				sb.append(" = ");
				sb.append("#{item.");
				sb.append(getCamelCaseString(tbpartition.getColumnName().toLowerCase(), false));
				sb.append(",");
				sb.append("jdbcType=");
				sb.append(convertNullableType(tbpartition.getDataType()));
				if (tbpartition.getDataType().equalsIgnoreCase("CLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
				}
				if (tbpartition.getDataType().equalsIgnoreCase("BLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
				}
				sb.append("}");
				sb.append(newline);
			}
		} else {
			// 分表键
			if (tbpartition != null) {
				// 分表键不为空
				sb.append("\t\t AND ");
				sb.append(tbpartition.getColumnName());
				sb.append(" = ");
				sb.append("#{item.");
				sb.append(getCamelCaseString(tbpartition.getColumnName().toLowerCase(), false));
				sb.append(",");
				sb.append("jdbcType=");
				sb.append(convertNullableType(tbpartition.getDataType()));
				if (tbpartition.getDataType().equalsIgnoreCase("CLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
				}
				if (tbpartition.getDataType().equalsIgnoreCase("BLOB")) {
					sb.append(", javaType=byte[],");
					sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
				}
				sb.append("}");
				sb.append(newline);
			}
		}

		sb.append("</foreach>");
		sb.append(newline);

		sb.append("</update>");
		sb.append(newline);

		return sb.toString();

	}

	private String createUpdateByParamSql(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) throws SQLException {
		int metaDataCount = metaData.size();
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<update id=\"");
		sb.append("update\" parameterType=\"map");
		sb.append("\">");
		sb.append(newline);
		sb.append("UPDATE");
		sb.append(newline);
		sb.append("\t");
		sb.append(tableName);
		sb.append(newline);
		sb.append("\t");
		sb.append("<set>");
		sb.append(newline);
		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t\t");
			sb.append("<if test=\"");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(" != null");
			sb.append("\">");
			sb.append(newline);
			sb.append("\t\t\t");
			sb.append(metaData.get(i).getColumnName());
			sb.append(" = ");
			sb.append("#{");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(",");
			sb.append("jdbcType=");
			sb.append(convertNullableType(metaData.get(i).getDataType()));
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			if (i + 1 != metaDataCount) {
				sb.append(",");
			}
			sb.append(newline);
			sb.append("\t\t");
			sb.append("</if>");
			sb.append(newline);
		}
		sb.append("\t");
		sb.append("</set>");
		sb.append(newline);
		sb.append("\t");
		sb.append(" WHERE ");
		sb.append(newline);
		sb.append("\t\t ");
		if ("".equalsIgnoreCase(pkId) || null == pkId) {
			sb.append("SID");
			sb.append(" = #{sid,jdbcType=DECIMAL}");
		} else {
			sb.append(pkId);
			sb.append(" = #{");
			sb.append(getCamelCaseString(pkId.toLowerCase(), false));
			sb.append(",jdbcType=DECIMAL}");
		}
		sb.append(newline);
		sb.append("</update>");
		sb.append(newline);

		return sb.toString();
	}

	/**
	 * Creates the delete sql.
	 * 
	 * @param metadata
	 *            the metadata
	 * @param tableName
	 *            the table name
	 * 
	 * @return the string
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	private String createDeleteSql(List<MetaDataEntity> metaData, String tableName, String pkId) throws SQLException {
		if (pkId == null || pkId.equalsIgnoreCase("")) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<delete id=\"");
		sb.append("delete\" ");
		sb.append(" parameterType=\"java.lang.Long\"");
		sb.append(">");
		sb.append(newline);
		sb.append("DELETE FROM");
		sb.append(newline);
		sb.append("\t");
		sb.append(tableName);
		sb.append(newline);
		sb.append("WHERE  ");
		if ("".equalsIgnoreCase(pkId) || null == pkId) {
			sb.append("SID");
			sb.append(" = ");
			sb.append(newline);
			sb.append(" #{sid,jdbcType=DECIMAL}");
		} else {
			sb.append(pkId);
			sb.append(" = #{");
			sb.append(getCamelCaseString(pkId.toLowerCase(), false));
			sb.append(",jdbcType=DECIMAL}");
		}

		sb.append(newline);
		sb.append("</delete>");
		sb.append(newline);

		return sb.toString();
	}

	private String createDeleteByParamsSql(List<MetaDataEntity> metaData, String tableName, String pkId) throws SQLException {
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		int metaDataCount = metaData.size();
		sb.append("<delete id=\"");
		sb.append("deleteByParams\" ");
		sb.append(" parameterType=\"map\"");
		sb.append(">");
		sb.append(newline);
		sb.append("DELETE FROM");
		sb.append(newline);
		sb.append("\t");
		sb.append(tableName);
		sb.append(newline);
		sb.append("<where>");
		sb.append(newline);

		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t\t");
			sb.append("<if test=\"");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(" != null");
			sb.append("\">");
			sb.append(newline);
			sb.append("\t\t\t");
			if (i != 0) {
				sb.append(" AND ");
			}
			sb.append(metaData.get(i).getColumnName());
			sb.append(" = ");
			sb.append("#{");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(",");
			sb.append("jdbcType=");
			sb.append(convertNullableType(metaData.get(i).getDataType()));
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			sb.append(newline);
			sb.append("\t\t");
			sb.append("</if>");
			sb.append(newline);
		}
		sb.append("</where>");
		sb.append(newline);
		sb.append("</delete>");
		sb.append(newline);

		return sb.toString();
	}

	/**
	 * Creates the select sql.
	 * 
	 * @param metadata
	 * @param tableName
	 * @return the string
	 * @throws SQLException
	 *             the SQL exception
	 */
	private String createSelectSql(List<MetaDataEntity> metaData, String tableName, String entityName) throws SQLException {
		int metaDataCount = metaData.size();
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<select id=\"");
		sb.append("find\" parameterType=\"");
		sb.append("map");
		sb.append("\" resultMap=\"");
		if (null == entityName || entityName.equalsIgnoreCase("")) {
			sb.append(getCamelCaseString(tableName.toLowerCase(), true));
		} else {
			sb.append(entityName);
		}
		sb.append("_Result\"");
		sb.append(">");
		sb.append(newline);
		sb.append("\t");
		sb.append("SELECT ");
		sb.append(newline);
		sb.append("\t");
		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t");
			sb.append(metaData.get(i).getColumnName());
			if (i != metaDataCount - 1) {
				sb.append(",");
			}
			if (i + 1 % 5 == 0) {
				sb.append(newline);
				sb.append("\t");
			}
		}
		sb.append(newline);
		sb.append("\t");
		sb.append("FROM ");
		sb.append("\t");
		sb.append(tableName);
		sb.append(newline);
		sb.append("<if test=\"_parameter != null\">");
		sb.append(newline);
		sb.append("\t");
		sb.append("<include refid=\"baseWhereClause\" />");
		sb.append(newline);
		sb.append("</if>");
		sb.append(newline);
		sb.append("</select>");
		sb.append(newline);
		return sb.toString();
	}

	/**
	 * 
	 * @throws SQLException
	 */
	private String createFindCountSql(String tableName) throws SQLException {
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<select id=\"findCount\" parameterType=\"map\" resultType=\"int\">");
		sb.append(newline);
		sb.append("\t");
		sb.append("select count(*) from ");
		sb.append(tableName);
		sb.append(newline);
		sb.append("\t\t");
		sb.append("<if test=\"_parameter != null\">");
		sb.append(newline);
		sb.append("\t\t\t");
		sb.append("<include refid=\"baseWhereClause\" />");
		sb.append(newline);
		sb.append("\t\t");
		sb.append("</if>");
		sb.append(newline);
		sb.append("</select>");
		return sb.toString();
	}

	/**
	 * create BaseWhereClause sql
	 * 
	 * @param metadata
	 * @param tableName
	 * @param entityName
	 * @return
	 * @throws SQLException
	 */
	private String createBaseWhereClause(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) throws SQLException {
		int metaDataCount = metaData.size();
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<sql id=\"baseWhereClause\">");
		sb.append(newline);
		sb.append("<where>");
		sb.append(newline);
		sb.append("\t\t");
		sb.append("<if test=\"sids != null and sids.size()>0 \"> ");
		sb.append(newline);
		sb.append("\t\t\t");
		sb.append("AND SID IN");
		sb.append(newline);
		sb.append("\t\t\t");
		sb.append("<foreach collection=\"sids\" item=\"item\" index=\"index\" ");
		sb.append("open=\"");
		sb.append("(");
		sb.append("\"");
		sb.append(" separator=\"");
		sb.append(",");
		sb.append("\"");
		sb.append(" close=\"");
		sb.append(")");
		sb.append("\">");
		sb.append(newline);
		sb.append("\t\t\t\t");
		sb.append("#{item}");
		sb.append(newline);
		sb.append("\t\t\t");
		sb.append("</foreach>");
		sb.append(newline);
		sb.append("\t\t");
		sb.append("</if>");
		sb.append(newline);
		if (!"SID".equalsIgnoreCase(pkId) && StringUtils.isNotEmpty(pkId)) {
			sb.append("\t\t");
			sb.append("<if test=\"sid != null\" >");
			sb.append(newline);
			sb.append("\t\t\t");
			sb.append(" AND ");
			sb.append(pkId);
			sb.append(" = ");
			sb.append("#{");
			sb.append("sid");
			sb.append(",");
			sb.append(" jdbcType=BIGINT");
			sb.append("}");
			sb.append(newline);
			sb.append("\t\t");
			sb.append("</if>");
			sb.append(newline);
		}
		for (int i = 0; i < metaDataCount; i++) {
			sb.append("\t\t");
			sb.append("<if test=\"");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(" != null");
			sb.append("\">");
			sb.append(newline);
			sb.append("\t\t\t");
			sb.append(" AND ");
			sb.append(metaData.get(i).getColumnName());
			sb.append(" = ");
			sb.append("#{");
			sb.append(getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false));
			sb.append(",");
			sb.append("jdbcType=");
			sb.append(convertNullableType(metaData.get(i).getDataType()));
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.ClobTypeHandler");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(", javaType=byte[],");
				sb.append(" typeHandler=org.apache.ibatis.type.BlobTypeHandler");
			}
			sb.append("}");
			sb.append(newline);
			sb.append("\t\t");
			sb.append("</if>");
			sb.append(newline);

			if (metaData.get(i).isDbPartition() || metaData.get(i).isTbPartition()) {
				// 添加按照分库分表键批量查询
				sb.append("\t\t");
				sb.append("<if test=\"" + getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false) + "s != null\"> ");
				sb.append(newline);
				sb.append("\t\t\t");
				sb.append("AND " + metaData.get(i).getColumnName() + " IN");
				sb.append(newline);
				sb.append("\t\t\t");
				sb.append("<foreach collection=\"" + getCamelCaseString(metaData.get(i).getColumnName().toLowerCase(), false) + "s\" item=\"element\" ");
				sb.append("open=\"");
				sb.append("(");
				sb.append("\"");
				sb.append(" separator=\"");
				sb.append(",");
				sb.append("\"");
				sb.append(" close=\"");
				sb.append(")");
				sb.append("\">");
				sb.append(newline);
				sb.append("\t\t\t\t");
				sb.append("#{element}");
				sb.append(newline);
				sb.append("\t\t\t");
				sb.append("</foreach>");
				sb.append(newline);
				sb.append("\t\t");
				sb.append("</if>");
				sb.append(newline);
			}
		}
		sb.append("\t");
		sb.append("</where>");
		sb.append(newline);
		sb.append("\t");
		sb.append("<if test=\"orderBy != null\">");
		sb.append(newline);
		sb.append("\t\t");
		sb.append(" ORDER BY ${orderBy} ");
		sb.append(newline);
		sb.append("\t");
		sb.append("</if>");
		sb.append(newline);
		

		if (pkId != null && !pkId.equalsIgnoreCase("")) {
			sb.append("\t");
			sb.append("<if test=\"orderBy == null\">");
			sb.append(newline);
			sb.append("\t\t");
			sb.append(" ORDER BY ");
			sb.append(pkId);
			sb.append(" ASC ");
			sb.append(newline);
			sb.append("\t");
			sb.append("</if>");
			sb.append(newline);
		}
		
		sb.append("\t");
		sb.append("<if test=\"limitt != null\">");
		sb.append(newline);
		sb.append("\t\t");
		sb.append(" limit ${limitt} ");
		sb.append(newline);
		sb.append("\t");
		sb.append("</if>");
		sb.append(newline);

		sb.append("</sql>");
		return sb.toString();
	}

	/**
	 * Method description.
	 * 
	 * @param metaData
	 *            the meta data
	 * @param tableName
	 *            the table name
	 * 
	 * @return the string
	 * 
	 * @throws SQLException
	 *             the SQL exception
	 */
	private String createResultMap(List<MetaDataEntity> metaData, String tableName, String entityName, String pkId) throws SQLException {
		int metaDataCount = metaData.size();

		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		sb.append("<resultMap id=\"");
		if (null == entityName || entityName.equalsIgnoreCase("")) {
			sb.append(getCamelCaseString(tableName.toLowerCase(), false));
		} else {
			sb.append(entityName);
		}
		sb.append("_Result\"  type=\"");
		if (usePackage) {
			sb.append(this.packagename + "." + entityName);
		} else {
			sb.append(entityName);
		}
		sb.append("\">");
		sb.append(newline);
		if (!"SID".equalsIgnoreCase(pkId) && StringUtils.isNotEmpty(pkId)) {
			sb.append("\t");
			sb.append("<result property=\"sid\"");
			sb.append(" column=\"");
			sb.append(pkId);
			sb.append("\"");
			sb.append(" jdbcType=\"BIGINT\"");
			sb.append("/>");
			sb.append(newline);
		}
		for (int i = 0; i < metaDataCount; i++) {
			String column = metaData.get(i).getColumnName();
			sb.append("\t");
			sb.append("<result property=\"");
			sb.append(getCamelCaseString(column.toLowerCase(), false));
			sb.append("\" column=\"");
			sb.append(column);
			sb.append("\"");
			sb.append(" jdbcType=\"");
			System.out.println(metaData.get(i).getColumnName() + "的类型是：" + metaData.get(i).getDataType());
			// sb.append(convertNullableType(metaData.get(i).getDataType()));
			sb.append(convertDataTypeMySql(metaData.get(i).getDataType()));
			sb.append("\"");
			if (metaData.get(i).getDataType().equalsIgnoreCase("CLOB")) {
				sb.append(" javaType=\"byte[]\"");
				sb.append(" typeHandler=\"org.apache.ibatis.type.ClobTypeHandler\"");
			}
			if (metaData.get(i).getDataType().equalsIgnoreCase("BLOB")) {
				sb.append(" javaType=\"byte[]\"");
				sb.append(" typeHandler=\"org.apache.ibatis.type.BlobTypeHandler\"");
			}
			sb.append("/>");
			sb.append(newline);
		}
		sb.append("</resultMap>");
		sb.append(newline);

		return sb.toString();
	}

	private String convertDataTypeMySql(String dataType) {
		if ("int".equalsIgnoreCase(dataType)) {
			return "INTEGER";
		} else if ("DATETIME".equalsIgnoreCase(dataType)) {
			return "TIMESTAMP";
		} else if ("TEXT".equalsIgnoreCase(dataType)) {
			return "LONGVARCHAR";
		} else {
			return dataType.toUpperCase();
		}
	}

	/**
	 * Method description
	 * 
	 * @param dataType
	 * @return
	 */
	private Object getDefaultValue(String dataType, Integer length, Integer dataScale, String value) {
		String defaultValue = "";
		// dataType = dataType.toUpperCase();
		if (dataType.equalsIgnoreCase("NUMBER")  || dataType.equalsIgnoreCase("DECIMAL")
				|| dataType.equalsIgnoreCase("smallint") || dataType.equalsIgnoreCase("int")) {
			if (null != length && length > 5) {
				defaultValue = value;
				if (dataType.equalsIgnoreCase("smallint") || dataType.equalsIgnoreCase("int")) {
					defaultValue = value;
				}
			} else {
				defaultValue = value;
			}
			if (null != dataScale && 0 != dataScale) {
				defaultValue = value;
			}
		}else if (dataType.equalsIgnoreCase("BIGINT")) {
			defaultValue = value+"L";
		}
		else if (dataType.equalsIgnoreCase("bit")) {
			defaultValue = value;
		} else if (dataType.equalsIgnoreCase("BLOB")) {
			defaultValue = "";
		} else if (dataType.equalsIgnoreCase("CLOB")) {
			defaultValue = "";
		} else if (dataType.equalsIgnoreCase("VARCHAR2") || dataType.equalsIgnoreCase("VARCHAR") || dataType.equalsIgnoreCase("longtext")
				|| dataType.equalsIgnoreCase("text")) {
			defaultValue = "\"" + value + "\"";
		} else if (dataType.equalsIgnoreCase("CHAR")) {
			defaultValue = "\"" + value + "\"";
		} else if (dataType.toUpperCase().indexOf("TIMESTAMP") != -1) {
			if ("CURRENT_TIMESTAMP".equalsIgnoreCase(value)) {
				defaultValue = "new Date()";
			} else {
				defaultValue = "";
			}
		} else if (dataType.equalsIgnoreCase("DATE") || dataType.equalsIgnoreCase("datetime")) {
			if ("CURRENT_TIMESTAMP".equalsIgnoreCase(value)) {
				defaultValue = "new Date()";
			} else {
				defaultValue = "";
			}
		} else {
			defaultValue = "";
		}

		return defaultValue;
	}

	/**
	 * Method description
	 * 
	 * @param dataType
	 * @return
	 */
	private String convertDataType(String dataType, Integer length, Integer dataScale) {
		String rtnType = "";
		// dataType = dataType.toUpperCase();
		if (dataType.equalsIgnoreCase("NUMBER") || dataType.equalsIgnoreCase("BIGINT") || dataType.equalsIgnoreCase("DECIMAL")
				|| dataType.equalsIgnoreCase("smallint") || dataType.equalsIgnoreCase("int")) {
			if (null != length && length > 5) {
				rtnType = "Long";
				if (dataType.equalsIgnoreCase("smallint") || dataType.equalsIgnoreCase("int")) {
					rtnType = "Integer";
				}
			} else {
				rtnType = "Integer";
			}
			if (null != dataScale && 0 != dataScale) {
				rtnType = "Double";
			}
		} else if (dataType.equalsIgnoreCase("bit")) {
			rtnType = "Boolean";
		} else if (dataType.equalsIgnoreCase("BLOB")) {
			rtnType = "byte[]";
		} else if (dataType.equalsIgnoreCase("CLOB")) {
			rtnType = "byte[]";
		} else if (dataType.equalsIgnoreCase("VARCHAR2") || dataType.equalsIgnoreCase("VARCHAR") || dataType.equalsIgnoreCase("longtext")
				|| dataType.equalsIgnoreCase("text")) {
			rtnType = "String";
		} else if (dataType.equalsIgnoreCase("CHAR")) {
			rtnType = "String";
		} else if (dataType.toUpperCase().indexOf("TIMESTAMP") != -1) {
			rtnType = "Date";
		} else if (dataType.equalsIgnoreCase("DATE") || dataType.equalsIgnoreCase("datetime")) {
			rtnType = "Date";
		} else {
			rtnType = dataType;
		}

		return rtnType;
	}

	/**
	 * Convert nullable type.
	 * 
	 * @param dataType
	 *            the data type
	 * @return the string
	 */
	private String convertNullableType(String dataType) {
		if ("int".equalsIgnoreCase(dataType)) {
			return "INTEGER";
		} else if ("DATETIME".equalsIgnoreCase(dataType)) {
			return "TIMESTAMP";
		} else if ("TEXT".equalsIgnoreCase(dataType)) {
			return "LONGVARCHAR";
		}else if ("LONGTEXT".equalsIgnoreCase(dataType)) {
			return "LONGVARCHAR";
		} else {
			return dataType.toUpperCase();
		}
	}

	/**
	 * Flex Type
	 * 
	 * @param dataType
	 * @return
	 */
	private String convertFlexDateType(String dataType) {
		String rtnType = "";
		if (dataType.equalsIgnoreCase("NUMBER")) {
			rtnType = "Number";
		} else if (dataType.equalsIgnoreCase("VARCHAR2")) {
			rtnType = "String";
		} else if (dataType.equalsIgnoreCase("DATE")) {
			rtnType = "Date";
		} else if (dataType.equalsIgnoreCase("TIMESTAMP")) {
			rtnType = "Date";
		} else if (dataType.equalsIgnoreCase("CHAR")) {
			rtnType = "String";
		} else if (dataType.equalsIgnoreCase("BLOB")) {
			rtnType = "Object";
		} else if (dataType.equalsIgnoreCase("CLOB")) {
			rtnType = "Object";
		} else {
			rtnType = "Object";
		}
		return rtnType;
	}

	/**
	 * Make dir.
	 * 
	 * @param file
	 *            the file
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public static void makeDir(File file) throws Exception {
		if (file.isFile()) {
			throw new Exception("Given path have samename file");
		}
		while (!file.getParentFile().exists()) {
			makeDir(file.getParentFile());
		}
		file.mkdirs();
	}

	/**
	 * write out to file end
	 * 
	 * @param file
	 * @param conent
	 */
	private void writefileToEnd(String file, String conent) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(conent);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Writefile.
	 * 
	 * @param file
	 *            the file
	 * @param content
	 *            the content
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void writefile(File file, String content) throws IOException {
		FileWriter writer = new FileWriter(file);
		writer.write(content);
		writer.flush();
		writer.close();
	}

	/**
	 * Gets the camel case string.
	 * 
	 * @param inputString
	 *            the input string
	 * @param firstCharacterUppercase
	 *            the first character uppercase
	 * 
	 * @return the camel case string
	 */
	public static String getCamelCaseString(String inputString, boolean firstCharacterUppercase) {
		StringBuffer sb = new StringBuffer();

		boolean nextUpperCase = false;
		for (int i = 0; i < inputString.length(); i++) {
			char c = inputString.charAt(i);

			switch (c) {
			case '_':
			case '-':
			case '@':
			case '$':
			case '#':
			case ' ':
				if (sb.length() > 0) {
					nextUpperCase = true;
				}
				break;

			default:
				if (nextUpperCase) {
					sb.append(Character.toUpperCase(c));
					nextUpperCase = false;
				} else {
					sb.append(Character.toLowerCase(c));
				}
				break;
			}
		}

		if (firstCharacterUppercase) {
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		}

		return sb.toString();
	}

	// public void setEntityNames(String[] entityNames) {
	// this.entityNames = entityNames;
	// }
	public String getMapperPath() {
		return mapperPath;
	}

	public void setMapperPath(String mapperPath) {
		this.mapperPath = mapperPath;
	}

	public String getFlexBeanPath() {
		return flexBeanPath;
	}

	public void setFlexBeanPath(String flexBeanPath) {
		this.flexBeanPath = flexBeanPath;
	}

	public String getFlexUiPath() {
		return flexUiPath;
	}

	public void setFlexUiPath(String flexUiPath) {
		this.flexUiPath = flexUiPath;
	}

	public String getFlexBeanPackagename() {
		return flexBeanPackagename;
	}

	public void setFlexBeanPackagename(String flexBeanPackagename) {
		this.flexBeanPackagename = flexBeanPackagename;
	}

	public String[] getPkIds() {
		return pkIds;
	}

	public void setPkIds(String[] pkIds) {
		this.pkIds = pkIds;
	}

	private static String getEntityName(String tableName) {
		for (Entry<String, String> en : tablesMap.entrySet()) {
			if (en.getKey().equalsIgnoreCase(tableName)) {
				return en.getValue();
			}
		}
		return null;
	}

	private static Map<String, String> tablesMap = new HashMap<String, String>();

	static {

	}
}
