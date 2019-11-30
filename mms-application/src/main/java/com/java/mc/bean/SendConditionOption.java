package com.java.mc.bean;

import java.io.Serializable;

public class SendConditionOption implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2832602154477663846L;
	private String name;
	private String code;
	private Integer value;
	private String parentCode;
	private String description;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getParentCode() {
		return parentCode;
	}
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}
	public Integer getValue() {
		return value;
	}
	public void setValue(Integer value) {
		this.value = value;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
