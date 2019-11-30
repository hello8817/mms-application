package com.java.mc.bean;

import java.io.File;
import java.sql.Timestamp;

public class BatchJob {

	private Long jobId;
	private String seq;
	private Short actionType;
	private String senderAddress;
	private String senderTitle;
	private String senderUserName;
	private String senderPassword;
	private Integer msid;
	private Integer dsid;
	private Integer smid;
	private Integer scheduleId;
	private MailServerConfig msConfig;
	private ShortMessageConfiguration smConfig;
	private String name;
	private String fromName;
	private String fromEmail;
	private String fromId;
	private String fromAddress;
	private String gatewayId;
	private String to;
	private String toAddressList;
	private String[] toList;
	private String ccAddressList;
	private String[] ccList;
	private String bccAddressList;
	private String[] bccList;
	private String subject;
	private String content;
	private String attachment;
	private File attachmentFile;
	private Short status;
	private String message;
	private Short code;
	private Timestamp createTime;
	private Timestamp sendTime;
	
	public Long getJobId() {
		return jobId;
	}
	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}
	public String getSeq() {
		return seq;
	}
	public void setSeq(String seq) {
		this.seq = seq;
	}
	public String getSenderAddress() {
		return senderAddress;
	}
	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}
	public String getSenderTitle() {
		return senderTitle;
	}
	public void setSenderTitle(String senderTitle) {
		this.senderTitle = senderTitle;
	}
	public String getSenderUserName() {
		return senderUserName;
	}
	public void setSenderUserName(String senderUserName) {
		this.senderUserName = senderUserName;
	}
	public String getSenderPassword() {
		return senderPassword;
	}
	public void setSenderPassword(String senderPassword) {
		this.senderPassword = senderPassword;
	}
	public Integer getMsid() {
		return msid;
	}
	public void setMsid(Integer msid) {
		this.msid = msid;
	}
	public Integer getDsid() {
		return dsid;
	}
	public void setDsid(Integer dsid) {
		this.dsid = dsid;
	}
	public String getToAddressList() {
		return toAddressList;
	}
	public void setToAddressList(String toAddressList) {
		this.toAddressList = toAddressList;
		if(this.toAddressList != null && this.toAddressList.length() > 0){
			this.setToList(this.toAddressList.split(","));
		}
	}
	public String getCcAddressList() {
		return ccAddressList;
	}
	public void setCcAddressList(String ccAddressList) {
		this.ccAddressList = ccAddressList;
		if(this.ccAddressList != null && this.ccAddressList.length() > 0){
			this.setCcList(this.ccAddressList.split(","));
		}
	}
	public String getBccAddressList() {
		return bccAddressList;
	}
	public void setBccAddressList(String bccAddressList) {
		this.bccAddressList = bccAddressList;
		if(this.bccAddressList != null && this.bccAddressList.length() > 0){
			this.setBccList(this.bccAddressList.split(","));
		}
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getAttachment() {
		return attachment;
	}
	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}
	public File getAttachmentFile() {
		return attachmentFile;
	}
	public void setAttachmentFile(File attachmentFile) {
		this.attachmentFile = attachmentFile;
	}
	public Short getStatus() {
		return status;
	}
	public void setStatus(Short status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Short getCode() {
		return code;
	}
	public void setCode(Short code) {
		this.code = code;
	}
	public Timestamp getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}
	public Timestamp getSendTime() {
		return sendTime;
	}
	public void setSendTime(Timestamp sendTime) {
		this.sendTime = sendTime;
	}
	public String[] getToList() {
		return toList;
	}
	private void setToList(String[] toList) {
		this.toList = toList;
	}
	public String[] getCcList() {
		return ccList;
	}
	private void setCcList(String[] ccList) {
		this.ccList = ccList;
	}
	public String[] getBccList() {
		return bccList;
	}
	private void setBccList(String[] bccList) {
		this.bccList = bccList;
	}
	public Integer getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(Integer scheduleId) {
		this.scheduleId = scheduleId;
	}
	public Integer getSmid() {
		return smid;
	}
	public void setSmid(Integer smid) {
		this.smid = smid;
	}
	public MailServerConfig getMsConfig() {
		return msConfig;
	}
	public void setMsConfig(MailServerConfig msConfig) {
		this.msConfig = msConfig;
	}
	public ShortMessageConfiguration getSmConfig() {
		return smConfig;
	}
	public void setSmConfig(ShortMessageConfiguration smConfig) {
		this.smConfig = smConfig;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Short getActionType() {
		return actionType;
	}
	public void setActionType(Short actionType) {
		this.actionType = actionType;
	}
	public String getGatewayId() {
		return gatewayId;
	}
	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}
	public String getFromName() {
		return fromName;
	}
	public void setFromName(String fromName) {
		this.fromName = fromName;
	}
	public String getFromEmail() {
		return fromEmail;
	}
	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}
	public String getFromId() {
		return fromId;
	}
	public void setFromId(String fromId) {
		this.fromId = fromId;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getFromAddress() {
		return fromAddress;
	}
	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}
}
