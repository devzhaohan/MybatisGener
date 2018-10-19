package com;

public class MetaDataEntity {
	private String columnName;
	private String dataType;
	private Integer dataLength;
	private Integer dataPrecision;
	private Integer dataScale;
	private String nullable;
	private String allDataType;
	private String comments;

	private boolean isDbPartition = false;// 是否是分库键

	private boolean isTbPartition = false;// 是否是分表键

	private String defaultValue;// 默认值

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public Integer getDataLength() {
		return dataLength;
	}

	public void setDataLength(Integer dataLength) {
		this.dataLength = dataLength;
	}

	public Integer getDataPrecision() {
		return dataPrecision;
	}

	public void setDataPrecision(Integer dataPrecision) {
		this.dataPrecision = dataPrecision;
	}

	public Integer getDataScale() {
		return dataScale;
	}

	public void setDataScale(Integer dataScale) {
		this.dataScale = dataScale;
	}

	public String getNullable() {
		return nullable;
	}

	public void setNullable(String nullable) {
		this.nullable = nullable;
	}

	public String getAllDataType() {
		return allDataType;
	}

	public void setAllDataType(String allDataType) {
		this.allDataType = allDataType;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public boolean isDbPartition() {
		return isDbPartition;
	}

	public void setDbPartition(boolean isDbPartition) {
		this.isDbPartition = isDbPartition;
	}

	public boolean isTbPartition() {
		return isTbPartition;
	}

	public void setTbPartition(boolean isTbPartition) {
		this.isTbPartition = isTbPartition;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

}
