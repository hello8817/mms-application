package com.java.mc.web;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.quartz.CronExpression;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.java.mc.bean.BatchJob;
import com.java.mc.bean.DatasourceConfig;
import com.java.mc.bean.GlobalConfig;
import com.java.mc.bean.JobSendLog;
import com.java.mc.bean.KV;
import com.java.mc.bean.MailServerConfig;
import com.java.mc.bean.MailServerTemplate;
import com.java.mc.bean.RemoteConfigInfo;
import com.java.mc.bean.Schedule;
import com.java.mc.bean.ScheduleLog;
import com.java.mc.bean.SendCondition;
import com.java.mc.bean.SendConditionOperation;
import com.java.mc.bean.SendConditionOption;
import com.java.mc.bean.ShortMessageConfiguration;
import com.java.mc.db.DBConnection;
import com.java.mc.db.DBOperation;
import com.java.mc.schedule.ScheduleConfiguration;
import com.java.mc.service.BatchService;
import com.java.mc.utils.Constants;
import com.java.mc.utils.DBUtils;
import com.java.mc.utils.WebUtils;

import net.sf.json.JSONArray;
import net.sf.json.JsonConfig;

@Controller
@Configuration
@RequestMapping("/")
public class WebController {
	private static final Logger logger = LoggerFactory.getLogger(WebController.class);

	@Autowired
	private DBOperation dbOperation;

	@Autowired
	private DBConnection dbConnection;

	@Autowired
	private ScheduleConfiguration scheduleConfiguration;

	@Autowired
	private BatchService batchService;

	@Autowired
	private Scheduler scheduler;

	@Value("${cn.com.vgtech.mc.global.version}")
	private String version;

	@RequestMapping(path = "/", method = RequestMethod.GET)
	public String index(String _t) {
		if (_t != null) {
			return "redirect:/";
		}
		if (this.dbOperation.isPasswordExpired()) {
			return "changepassword";
		}
		return "index";
	}

	@RequestMapping(path = "/version", method = RequestMethod.POST)
	@ResponseBody
	public String version() {
		return this.version;
	}

	@RequestMapping(path = "login", method = RequestMethod.GET)
	public String login(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "login";
	}

	@RequestMapping(path = "/cron/", method = RequestMethod.GET)
	public String cron(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "cron";
	}

	@RequestMapping(path = "/dsm/", method = RequestMethod.GET)
	public String dsm(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "dsm";
	}

	@RequestMapping(path = "/msm/", method = RequestMethod.GET)
	public String msm(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "msm";
	}

	@RequestMapping(path = "/smm/", method = RequestMethod.GET)
	public String smm(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "smm";
	}

	@RequestMapping(path = "/schedule/", method = RequestMethod.GET)
	public String schedule(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "schedule";
	}

	@RequestMapping(path = "/state/", method = RequestMethod.GET)
	public String state(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "state";
	}

	@RequestMapping(path = "/conf/", method = RequestMethod.GET)
	public String conf(HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		return "conf";
	}

	@RequestMapping(path = "/state/statusinfo", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> statusinfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		result.put("taskCount", this.dbOperation.getAllNormalScheduleCount());
		result.put("executeCount", this.dbOperation.getNormalScheduleLogExecuteCount());
		result.put("successCount", this.dbOperation.getNormalScheduleLogSuccessCount());
		result.put("failedCount", this.dbOperation.getNormalScheduleLogFailedCount());
		return result;
	}

	@RequestMapping(path = "/state/mailLogList", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> mailJobLogList(int id, Long from, Long to, Boolean flag) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);

		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求!");
			return result;
		}

		List<BatchJob> batchJobList = new ArrayList<>();

		// mail sender action
		if (Constants.ACTION_DO_BATCH_JOB == schedule.getActionType()) {
			result.put("totalCount", this.dbOperation.getBatchJobCompleteCount(Constants.ACTION_MAIL_SCAN,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("successCount", this.dbOperation.getBatchJobSuccessCount(Constants.ACTION_MAIL_SCAN,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("failedCount", this.dbOperation.getBatchJobFailedCount(Constants.ACTION_MAIL_SCAN,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));

			batchJobList = flag == null || flag == false
					? this.dbOperation.getBatchJobFailedList(Constants.ACTION_MAIL_SCAN,
							from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to))
					: this.dbOperation.getBatchJobList(Constants.ACTION_MAIL_SCAN,
							from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to));
		}

		// mail scan action
		if (Constants.ACTION_MAIL_SCAN == schedule.getActionType()) {
			result.put("totalCount", this.dbOperation.getBatchJobCompleteCountByScheduleId(id,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("successCount", this.dbOperation.getBatchJobSuccessCountByScheduleId(id,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("failedCount", this.dbOperation.getBatchJobFailedCountByScheduleId(id,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			batchJobList = flag == null || flag == false
					? this.dbOperation.getBatchJobFailedListByScheduleId(id, from == null ? null : new Timestamp(from),
							to == null ? null : new Timestamp(to))
					: this.dbOperation.getBatchJobListByScheduleId(id, from == null ? null : new Timestamp(from),
							to == null ? null : new Timestamp(to));
		}

		List<JobSendLog> jobSendLogList = new ArrayList<JobSendLog>();
		if (batchJobList != null) {
			for (BatchJob job : batchJobList) {
				JobSendLog jobSendLog = new JobSendLog();
				jobSendLog.setId(job.getJobId());
				jobSendLog.setSeq(job.getSeq());
				jobSendLog.setFrom(job.getFromAddress());
				jobSendLog.setTo(job.getToAddressList());
				jobSendLog.setStatus(job.getCode());
				jobSendLog.setSubject(job.getSubject());
				jobSendLog.setMessage(job.getMessage());
				jobSendLog.setAttachment(job.getAttachment());
				jobSendLog.setFormatSendTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(job.getSendTime()));
				jobSendLog.setFormatCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(job.getCreateTime()));
				jobSendLogList.add(jobSendLog);
			}
		}
		result.put("mailLogList", jobSendLogList);
		return result;
	}

	@RequestMapping(path = "/state/smLogList", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> smJobLogList(int id, Long from, Long to, Boolean flag) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);

		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求!");
			return result;
		}

		List<BatchJob> batchJobList = new ArrayList<>();

		// mail sender action
		if (Constants.ACTION_DO_BATCH_JOB == schedule.getActionType()) {
			result.put("totalCount", this.dbOperation.getBatchJobCompleteCount(Constants.ACTION_SM_SCAN,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("successCount", this.dbOperation.getBatchJobSuccessCount(Constants.ACTION_SM_SCAN,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("failedCount", this.dbOperation.getBatchJobFailedCount(Constants.ACTION_SM_SCAN,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));

			batchJobList = flag == null || flag == false
					? this.dbOperation.getBatchJobFailedList(Constants.ACTION_SM_SCAN,
							from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to))
					: this.dbOperation.getBatchJobList(Constants.ACTION_SM_SCAN,
							from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to));
		}

		// short message scan action
		if (Constants.ACTION_SM_SCAN == schedule.getActionType()) {
			result.put("totalCount", this.dbOperation.getBatchJobCompleteCountByScheduleId(id,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("successCount", this.dbOperation.getBatchJobSuccessCountByScheduleId(id,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			result.put("failedCount", this.dbOperation.getBatchJobFailedCountByScheduleId(id,
					from == null ? null : new Timestamp(from), to == null ? null : new Timestamp(to)));
			batchJobList = flag == null || flag == false
					? this.dbOperation.getBatchJobFailedListByScheduleId(id, from == null ? null : new Timestamp(from),
							to == null ? null : new Timestamp(to))
					: this.dbOperation.getBatchJobListByScheduleId(id, from == null ? null : new Timestamp(from),
							to == null ? null : new Timestamp(to));
		}

		List<JobSendLog> jobSendLogList = new ArrayList<JobSendLog>();
		if (batchJobList != null) {
			for (BatchJob job : batchJobList) {
				JobSendLog jobSendLog = new JobSendLog();
				jobSendLog.setId(job.getJobId());
				jobSendLog.setSeq(job.getSeq());
				jobSendLog.setFrom(job.getFromAddress());
				jobSendLog.setTo(job.getToAddressList());
				jobSendLog.setStatus(job.getCode());
				jobSendLog.setSubject(job.getSubject());
				jobSendLog.setMessage(job.getMessage());
				jobSendLog.setReturnCode(job.getGatewayId());
				jobSendLog.setContent(job.getContent());
				jobSendLog.setFormatSendTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(job.getSendTime()));
				jobSendLog.setFormatCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(job.getCreateTime()));
				jobSendLogList.add(jobSendLog);
			}
		}
		result.put("smLogList", jobSendLogList);
		return result;
	}

	@RequestMapping(path = "/cron/cronValidation", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getLastCronTime(String cron) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		if (cron == null) {
			result.put("success", false);
			result.put("message", "cron表达式不能为空！");
			return result;
		}
		CronTriggerImpl trigger = new CronTriggerImpl();
		try {
			trigger.setCronExpression(cron);
		} catch (ParseException e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		List<Date> dataList = TriggerUtils.computeFireTimes(trigger, null, 10);
		List<String> resultDate = new LinkedList<String>();
		if (dataList != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd : HH:mm:ss");
			for (Date date : dataList) {
				resultDate.add(sdf.format(date));
			}
		}
		result.put("date", resultDate);
		return result;
	}

	@RequestMapping(path = "/state/recordlist", method = RequestMethod.GET)
	@ResponseBody
	public List<ScheduleLog> scheduleLogList(int id, Boolean flag, Long from, Long to) {
		Timestamp fromTime = new Timestamp(from == null ? 0 : from);
		Timestamp toTime = new Timestamp(to == null ? System.currentTimeMillis() : to);
		if (flag == null || flag == false) {
			return this.dbOperation.getScheduleLogFailedListBySchedueId(id, fromTime, toTime);
		}
		return this.dbOperation.getScheduleLogListBySchedueId(id, fromTime, toTime);
	}

	@RequestMapping(path = "/schedule/stop", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> stopSchedule(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);

		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}
		JobKey jobKey = new JobKey(schedule.getJobName(), schedule.getGroupName());
		try {
			if (this.scheduler.checkExists(jobKey)) {
				TriggerKey triggerKey = new TriggerKey(schedule.getJobName(), schedule.getGroupName());
				this.scheduler.unscheduleJob(triggerKey);
				this.dbOperation.setScheduleStatus(id, Constants.E);
			} else {
				result.put("success", false);
				result.put("message", "非法请求！");
				return result;
			}
		} catch (SchedulerException e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		return result;
	}

	@RequestMapping(path = "/schedule/resume", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> resumeSchedule(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);

		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}
		JobKey jobKey = new JobKey(schedule.getJobName(), schedule.getGroupName());
		try {
			if (this.scheduler.checkExists(jobKey)) {
				TriggerKey triggerKey = new TriggerKey(schedule.getJobName(), schedule.getGroupName());
				if (this.scheduler.getTriggerState(triggerKey) == TriggerState.PAUSED) {
					this.scheduler.resumeJob(jobKey);
				} else {
					result.put("success", false);
					result.put("message", "非法请求！");
					return result;
				}
			} else {
				result.put("success", false);
				result.put("message", "非法请求！");
				return result;
			}
		} catch (SchedulerException e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		return result;
	}

	@RequestMapping(path = "/schedule/pause", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> pauseSchedule(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);

		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}
		JobKey jobKey = new JobKey(schedule.getJobName(), schedule.getGroupName());
		try {
			if (this.scheduler.checkExists(jobKey)) {
				TriggerKey triggerKey = new TriggerKey(schedule.getJobName(), schedule.getGroupName());
				if (this.scheduler.getTriggerState(triggerKey) == TriggerState.NORMAL) {
					this.scheduler.pauseJob(jobKey);
				} else {
					result.put("success", false);
					result.put("message", "非法请求！");
					return result;
				}
			} else {
				result.put("success", false);
				result.put("message", "非法请求！");
				return result;
			}
		} catch (SchedulerException e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		return result;
	}

	@RequestMapping(path = "/schedule/runonce", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> runonceSchedule(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}

		JobKey jobKey = new JobKey(schedule.getJobName(), schedule.getGroupName());
		try {
			if (!this.scheduler.checkExists(jobKey) || schedule.getStatus().equalsIgnoreCase(Constants.E)) {
				this.scheduleConfiguration.addorupdateJob(schedule);
			}
			this.scheduler.triggerJob(jobKey);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		return result;
	}

	@RequestMapping(path = "/schedule/remove", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> removeSchedule(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}

		JobKey jobKey = new JobKey(schedule.getJobName(), schedule.getGroupName());
		try {
			if (this.scheduler.checkExists(jobKey)) {
				TriggerKey triggerKey = new TriggerKey(schedule.getJobName(), schedule.getGroupName());
				if (this.scheduler.checkExists(triggerKey)) {
					this.scheduler.unscheduleJob(triggerKey);
				}
				this.scheduler.deleteJob(jobKey);
			}
			this.dbOperation.setScheduleStatus(id, Constants.N);
		} catch (SchedulerException e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		return result;
	}

	@RequestMapping(path = "/schedule/reset", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> resetSchedule(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}

		try {
			this.scheduleConfiguration.scheduleSync(schedule);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		return result;
	}

	@RequestMapping(path = "/schedule/run", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> runSchedule(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		Schedule schedule = this.dbOperation.getScheduleById(id);
		if (schedule == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}

		// check cron expression
		if (schedule.getCronExpression() != null) {
			try {
				new CronExpression(schedule.getCronExpression());
			} catch (ParseException e) {
				result.put("success", false);
				result.put("message", "计划执行规则错误");
				return result;
			}
		} else {
			result.put("success", false);
			result.put("message", "缺少计划规则表达式");
			return result;
		}

		JobKey jobKey = new JobKey(schedule.getJobName(), schedule.getGroupName());
		TriggerKey triggerKey = new TriggerKey(schedule.getJobName(), schedule.getGroupName());
		try {
			if (this.scheduler.checkExists(jobKey) && this.scheduler.checkExists(triggerKey)) {
				if (this.scheduler.getTriggerState(triggerKey) == TriggerState.NORMAL) {
					result.put("success", false);
					result.put("message", "任务已经在运行！");
					return result;
				} else {
					if (schedule.getStatus().equalsIgnoreCase(Constants.E)
							|| schedule.getStatus().equalsIgnoreCase(Constants.I)) {
						this.scheduleConfiguration.scheduleSync(schedule);
					} else {
						this.scheduler.resumeTrigger(triggerKey);
					}
				}
			} else {
				this.scheduleConfiguration.scheduleSync(schedule);
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		return result;
	}

	@RequestMapping(path = "/schedule/list", method = RequestMethod.GET)
	@ResponseBody
	public List<Schedule> getScheduleList(Boolean s, Boolean n, Boolean u, Boolean o) {
		List<Schedule> scheduleList = this.dbOperation.getAllScheduleList();
		if (scheduleList != null) {
			for (Schedule schedule : scheduleList) {
				// set schedule status
				TriggerKey key = new TriggerKey(schedule.getJobName(), Constants.MMS);
				try {
					if (this.scheduler.checkExists(key)) {
						schedule.setScheduleStatus(this.scheduler.getTriggerState(key).toString());
					} else {
						schedule.setScheduleStatus(Constants.SCHEDULE_STATUS_UNSCHEDULE);
					}
				} catch (SchedulerException e) {
				}
			}
		}

		// system schedule filter
		if (s == null || s == false) {
			Iterator<Schedule> it = scheduleList.iterator();
			while (it.hasNext()) {
				Schedule schedule = it.next();
				if (Constants.SYSTEM_SCHEDULE.equalsIgnoreCase(schedule.getScheduleType())) {
					it.remove();
				}
			}
		}

		// normal schedule filter
		if (n != null && n == false) {
			Iterator<Schedule> it = scheduleList.iterator();
			while (it.hasNext()) {
				Schedule schedule = it.next();
				if (TriggerState.NORMAL.toString().equalsIgnoreCase(schedule.getScheduleStatus())) {
					it.remove();
				}
			}
		}

		// un-schedule filter
		if (u != null && u == false) {
			Iterator<Schedule> it = scheduleList.iterator();
			while (it.hasNext()) {
				Schedule schedule = it.next();
				if (Constants.SCHEDULE_STATUS_UNSCHEDULE.equalsIgnoreCase(schedule.getScheduleStatus())) {
					it.remove();
				}
			}
		}

		// other-schedule filter
		if (o != null && o == false) {
			Iterator<Schedule> it = scheduleList.iterator();
			while (it.hasNext()) {
				Schedule schedule = it.next();
				if ((!Constants.SCHEDULE_STATUS_UNSCHEDULE.equalsIgnoreCase(schedule.getScheduleStatus()))
						&& (!TriggerState.NORMAL.toString().equalsIgnoreCase(schedule.getScheduleStatus()))) {
					it.remove();
				}
			}
		}

		// add send rules data
		for (Schedule schedule : scheduleList) {
			if (Constants.ACTION_MAIL_SCAN == schedule.getActionType()
					|| Constants.ACTION_SM_SCAN == schedule.getActionType()) {
				List<SendCondition> optionList = this.dbOperation.getSendConditionListByScheduleId(schedule.getId());
				if (optionList != null) {
					List<SendConditionOperation> operationList = this.dbOperation
							.getSendConditionOperationListByScheduleId(schedule.getId());
					if (operationList != null && operationList.size() > 0 && optionList.size() > 0) {
						for (SendCondition sc : optionList) {
							List<SendConditionOperation> opeList = new ArrayList<>();
							for (SendConditionOperation sco : operationList) {
								if (sco.getOptionId() == sc.getId()) {
									opeList.add(sco);
								}
							}
							sc.setOperationList(opeList);
						}
					}
				}
				schedule.setSendCondition(optionList);
			}
		}

		return scheduleList;
	}

	@RequestMapping(path = "/schedule/normalScheduleActionList", method = RequestMethod.GET)
	public @ResponseBody List<KV> getNormalScheduleTypeList() {
		List<GlobalConfig> globalConfigList = this.dbOperation.getNormalScheduleActionTypeList();
		List<KV> kvList = new ArrayList<KV>();
		if (globalConfigList != null) {
			for (GlobalConfig gc : globalConfigList) {
				KV kv = new KV();
				kv.setKey(gc.getTitle());
				kv.setVal(gc.getVal());
				kvList.add(kv);
			}
		}
		return kvList;
	}

	@RequestMapping(path = "/dsList", method = RequestMethod.GET)
	@ResponseBody
	public List<KV> dsList() {
		List<DatasourceConfig> dscList = this.dbOperation.getDSConfigurationList();
		List<KV> kvList = new ArrayList<KV>();
		if (dscList != null) {
			for (DatasourceConfig dsc : dscList) {
				KV kv = new KV();
				kv.setKey(dsc.getDisplayName());
				kv.setVal(dsc.getId());
				kvList.add(kv);
			}
		}
		return kvList;
	}

	@RequestMapping(path = "/dscList", method = RequestMethod.GET)
	@ResponseBody
	public List<DatasourceConfig> datasourceConfigList() {
		return this.dbOperation.getDSConfigurationList();
	}

	@RequestMapping(path = "/msList", method = RequestMethod.GET)
	@ResponseBody
	public List<KV> msList() {
		List<MailServerConfig> mscList = this.dbOperation.getMailServerConfigrationList();
		List<KV> kvList = new ArrayList<KV>();
		if (mscList != null) {
			for (MailServerConfig msc : mscList) {
				KV kv = new KV();
				kv.setKey(msc.getDisplayName());
				kv.setVal(msc.getId());
				kvList.add(kv);
			}
		}
		return kvList;
	}

	@RequestMapping(path = "/smList", method = RequestMethod.GET)
	@ResponseBody
	public List<KV> smList() {
		List<ShortMessageConfiguration> smcList = this.dbOperation.getShortMessageConfigrationList();
		List<KV> kvList = new ArrayList<KV>();
		if (smcList != null) {
			for (ShortMessageConfiguration smc : smcList) {
				KV kv = new KV();
				kv.setKey(smc.getDisplayName());
				kv.setVal(smc.getId());
				kvList.add(kv);
			}
		}
		return kvList;
	}

	@RequestMapping(path = "/mscList", method = RequestMethod.GET)
	@ResponseBody
	public List<MailServerConfig> getMailServerConfigList() {
		return this.dbOperation.getMailServerConfigrationList();
	}

	@RequestMapping(path = "/smcList", method = RequestMethod.GET)
	@ResponseBody
	public List<ShortMessageConfiguration> getShortMessageConfigurationList() {
		return this.dbOperation.getShortMessageConfigrationList();
	}

	@RequestMapping(path = "/smm/smtList", method = RequestMethod.GET)
	@ResponseBody
	public List<KV> getShortMessageTunnelList() {
		return this.dbOperation.getShortMessageTunnelList();
	}

	@RequestMapping(path = "schedule/condition/list", method = RequestMethod.GET)
	@ResponseBody
	public List<SendConditionOption> getSendConditionList() {
		return this.dbOperation.getSendConditionOptionList();
	}

	@RequestMapping(path = "schedule/condition/operation/list", method = RequestMethod.GET)
	@ResponseBody
	public List<SendConditionOption> getSendConditionOperationlist() {
		return this.dbOperation.getSendConditionOperationList();
	}

	@RequestMapping(path = "schedule/neworupdate", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> neworupdateSchedule(Schedule schedule) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		if (schedule.getId() != null) {
			Schedule scheduleObject = this.dbOperation.getScheduleById(schedule.getId());
			if (scheduleObject == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

			if (scheduleObject.getActionType() != schedule.getActionType()) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}
			schedule.setJobName(scheduleObject.getJobName());
			schedule.setGroupName(scheduleObject.getGroupName());

			// set default cron expression if necessary.
			if (Constants.SCHEDULE_START_DENIED == schedule.getStartflag()) {
				if (StringUtils.isEmpty(schedule.getCronExpression())) {
					schedule.setCronExpression(scheduleObject.getCronExpression());
				}
			}
		}
		if ((schedule.getActionType() == null) || ((schedule.getStartflag() != Constants.SCHEDULE_START_DENIED
				&& schedule.getCronExpression() == null)) || (schedule.getDisplayName() == null)) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// check action type
		if (!this.dbOperation.checkActionTypeExist(schedule.getActionType())) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// check cron expression
		if (schedule.getCronExpression() != null) {
			try {
				new CronExpression(schedule.getCronExpression());
			} catch (ParseException e) {
				result.put("success", false);
				result.put("message", e.getLocalizedMessage());
				return result;
			}
		}

		// check delay regulations.
		if (schedule.getStartflag() == Constants.SCHEDULE_START_DELAY) {
			if (schedule.getDelay() == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

			if (schedule.getDelay() == 0) {
				if (schedule.getDelayhour() == null || schedule.getDelayMinute() == null) {
					result.put("success", false);
					result.put("message", "非法请求");
					return result;
				}
				schedule.setDelayTime(
						Long.valueOf((schedule.getDelayhour() * 3600 + schedule.getDelayMinute() * 60) * 1000));
			} else {
				if (schedule.getDelay() == 1) {
					if (schedule.getDelayDay() == null) {
						result.put("success", false);
						result.put("message", "非法请求");
						return result;
					}
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					try {
						schedule.setDelayDate(new Timestamp(df.parse(schedule.getDelayDay()).getTime()));
					} catch (ParseException e) {
						result.put("success", false);
						result.put("message", e.getLocalizedMessage());
						return result;
					}
				} else {
					result.put("success", false);
					result.put("message", "非法请求");
					return result;
				}
			}
		}

		if (schedule.getSendConditions() != null && schedule.getSendConditions().length() > 0) {
			JSONArray jsonArray = JSONArray.fromObject(schedule.getSendConditions());
			JsonConfig jsonConfig = new JsonConfig();
			jsonConfig.setRootClass(SendCondition.class);
			Map<String, Class> classMap = new HashMap<>();
			classMap.put("operationList", SendConditionOperation.class);
			jsonConfig.setClassMap(classMap);
			@SuppressWarnings("unchecked")
			List<SendCondition> sendConditionList = (List<SendCondition>) JSONArray.toCollection(jsonArray, jsonConfig);
			schedule.setSendConditionList(sendConditionList);
		}

		// action mail scan or short message scan schedule
		if (Constants.ACTION_MAIL_SCAN == schedule.getActionType()
				|| Constants.ACTION_SM_SCAN == schedule.getActionType()) {

			if (schedule.getDsid() == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

			// check datasource configuration
			DatasourceConfig dsc = this.dbOperation.getDSConfigurationById(schedule.getDsid());
			if (dsc == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}
			
			// duplicate check
			if(Constants.ACTION_MAIL_SCAN == schedule.getActionType() || Constants.ACTION_SM_SCAN == schedule.getActionType()){
				List<Schedule> duplicateScheduleList = this.dbOperation
						.getScheduleListByActionTypeAndDsId(schedule.getActionType().shortValue(), schedule.getDsid());
				if (duplicateScheduleList != null && duplicateScheduleList.size() > 0) {
					for (Schedule s : duplicateScheduleList) {
						if(s.getId().equals(schedule.getId())){
							continue;
						}
						List<Integer> keys = new ArrayList<>();
						keys.add(s.getId());
						result.put("success", false);
						result.put("message", "重复计划任务－不能对同一个数据源使用多个邮件或者短信扫描任务，请参考第"
								+ StringUtils.collectionToCommaDelimitedString(keys) + "号计划任务");
						return result;
					}
				}
			}

			if ((Constants.ACTION_MAIL_SCAN == schedule.getActionType() && schedule.getMsid() == null)
					|| (Constants.ACTION_SM_SCAN == schedule.getActionType() && schedule.getSmid() == null)) {
				if (schedule.getSendConditionList() == null || schedule.getSendConditionList().size() <= 0) {
					result.put("success", false);
					result.put("message", "非法请求");
					return result;
				}

				// no custom handler and no default hander
				for (SendCondition sc : schedule.getSendConditionList()) {
					if (sc.getOperationList() == null || sc.getOperationList().size() <= 0) {
						result.put("success", false);
						result.put("message", "非法请求");
						return result;
					}
				}
			}

			try {
				if (Constants.ACTION_MAIL_SCAN == schedule.getActionType()) {

					// check mail server configuration
					if (schedule.getMsid() != null) {
						MailServerConfig msc = this.dbOperation.getMailServerConfigrationById(schedule.getMsid());
						if (msc == null) {
							result.put("success", false);
							result.put("message", "非法请求");
							return result;
						}

						// check mail table.
						DBUtils.checkMailTable(dsc);
					}
				}
				if (Constants.ACTION_SM_SCAN == schedule.getActionType()) {

					// check short message tunnel configuration
					ShortMessageConfiguration smc = this.dbOperation
							.getShortMessageConfigrationById(schedule.getSmid());
					if (smc == null) {
						result.put("success", false);
						result.put("message", "非法请求");
						return result;
					}

					// check short message table
					DBUtils.checkSMTable(dsc);
				}
			} catch (Exception e) {
				result.put("success", false);
				result.put("message", e.getLocalizedMessage());
				return result;
			}

			// schedule.send rules
			if (schedule.getSendConditionList() != null) {
				for (SendCondition sc : schedule.getSendConditionList()) {
					if (sc.getHandlerId() == null) {
						result.put("success", false);
						result.put("message", "非法请求");
						return result;
					}

					if (Constants.ACTION_MAIL_SCAN == schedule.getActionType()) {
						MailServerConfig msc = this.dbOperation.getMailServerConfigrationById(sc.getHandlerId());
						if (msc == null) {
							result.put("success", false);
							result.put("message", "数据库配置信息未找到");
							return result;
						}
					}

					if (Constants.ACTION_SM_SCAN == schedule.getActionType()) {
						ShortMessageConfiguration smc = this.dbOperation
								.getShortMessageConfigrationById(sc.getHandlerId());
						if (smc == null) {
							result.put("success", false);
							result.put("message", "短信通道配置信息未找到");
							return result;
						}
					}

					if (sc.getOperationList() != null) {
						// check option and operation whether it is supported or
						// not.
						for (SendConditionOperation sco : sc.getOperationList()) {
							if (sco.getOption() != null) {
								SendConditionOption config = null;
								if (sco.getOperation() != null) {
									config = this.dbOperation.getSendConditionOperation(sco.getOption(),
											sco.getOperation());
								} else {
									config = this.dbOperation.getSendConditionOption(sco.getOption());
								}
								if (config == null) {
									result.put("success", false);
									result.put("message", "非法请求");
									return result;
								}
								if (Constants.TYPE_STRING.equalsIgnoreCase(config.getCode())) {
									if (StringUtils.isEmpty(sco.getValue())) {
										result.put("success", false);
										result.put("message", "非法请求");
										return result;
									}
								}
								if (Constants.TYPE_DIGITAL.equalsIgnoreCase(config.getCode())) {
									if (sco.getVal() == null) {
										result.put("success", false);
										result.put("message", "非法请求");
										return result;
									}
								}
								if (Constants.TYPE_TIME.equalsIgnoreCase(config.getCode())) {
									if (StringUtils.isEmpty(sco.getBegin())) {
										result.put("success", false);
										result.put("message", "非法请求");
										return result;
									}
								}
								if (Constants.TYPE_TIME_ZONE.equalsIgnoreCase(config.getCode())) {
									if (StringUtils.isEmpty(sco.getBegin()) || StringUtils.isEmpty(sco.getEnd())) {
										result.put("success", false);
										result.put("message", "非法请求");
										return result;
									}
								}
							}
						}
					}
				}
			}
		}

		// MMS plan
		if (Constants.ACTION_MMS_PLAN == schedule.getActionType()) {

			// check url
			if (schedule.getUrl() == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

			try {
				URL url = new URL(schedule.getUrl());
				String protocol = url.getProtocol();
				String host = url.getHost();
				int port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
				URL tryurl = new URL(protocol, host, port, "");
				WebUtils.access(tryurl.toURI());
			} catch (Exception e) {
				result.put("success", false);
				result.put("message", e.getMessage());
				return result;
			}

		}

		// window command executor
		if (Constants.ACTION_WINDOW_COMMAND == schedule.getActionType()) {

			// check command
			if (schedule.getCommand() == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}
		}

		// run db sql action
		if (Constants.ACTION_DB_RUNSQL == schedule.getActionType()) {

			// check sql sentence and dsid
			if (schedule.getSqlSentence() == null || schedule.getDsid() == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

			// check ds configuration
			DatasourceConfig dsc = this.dbOperation.getDSConfigurationById(schedule.getDsid());
			if (dsc == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}
		}

		// set job name
		if (schedule.getId() == null) {
			StringBuffer jobName = new StringBuffer(Constants.JOB).append("_").append(schedule.getActionType())
					.append("_").append(System.currentTimeMillis());
			schedule.setJobName(jobName.toString());
		}

		// set status
		if (schedule.getStartflag() == Constants.SCHEDULE_START_DENIED) {
			schedule.setStatus(Constants.E);
		} else {
			schedule.setStatus(Constants.I);
		}

		// save the schedule
		if (schedule.getId() != null) {
			this.dbOperation.updateSchedule(schedule);
		} else {
			schedule.setId(this.dbOperation.saveSchedule(schedule));
		}

		// save or update send rules.
		if (Constants.ACTION_SM_SCAN == schedule.getActionType()
				|| Constants.ACTION_MAIL_SCAN == schedule.getActionType()) {
			if (schedule.getId() != null) {
				this.dbOperation.clearSendOperationByScheduleId(schedule.getId());
				this.dbOperation.clearSendOptionByScheduleId(schedule.getId());
			}

			if (schedule.getSendConditionList() != null) {
				for (int i = 0; i < schedule.getSendConditionList().size(); i++) {
					SendCondition sc = schedule.getSendConditionList().get(i);
					int sendId = this.dbOperation.saveSendOption(schedule.getId(), sc, i);
					this.dbOperation.saveSendOperation(sc.getOperationList(), sendId);
				}
			}
		}

		try {
			this.scheduleConfiguration.newOrUpdateSchedule();
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}

		// remember the last data input.
		if (Constants.ACTION_MMS_PLAN == schedule.getActionType()) {
			this.dbOperation.setLastInputData(schedule.getVprotocol(), Constants.VPROTOCOL);
			this.dbOperation.setLastInputData(schedule.getVhost(), Constants.VHOST);
			this.dbOperation.setLastInputData(String.valueOf(schedule.getVport()), Constants.VPORT);
			this.dbOperation.setLastInputData(schedule.getVappname(), Constants.VAPPNAME);
			this.dbOperation.setLastInputData(schedule.getVprgname(), Constants.VPRGNAME);
		}

		return result;
	}

	@RequestMapping(path = "/schedule/lastinput", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getLastInputData() {
		Map<String, Object> result = new HashMap<String, Object>();
		List<GlobalConfig> gcList = this.dbOperation.getLastInputDataList();
		if (gcList != null && gcList.size() > 0) {
			for (GlobalConfig gc : gcList) {
				result.put(gc.getTitle().toLowerCase(), gc.getContent());
			}
		}
		return result;
	}

	@RequestMapping(path = "/msm/mstList", method = RequestMethod.GET)
	public @ResponseBody List<KV> mslist() {
		List<MailServerTemplate> mstList = this.dbOperation.getBatchServerTemplateList();
		List<KV> kvList = new ArrayList<KV>();
		if (mstList != null) {
			for (MailServerTemplate mst : mstList) {
				KV kv = new KV();
				kv.setKey(mst.getTemplateName());
				kv.setVal(mst.getTemplateId());
				kvList.add(kv);
			}
		}
		return kvList;
	}

	@RequestMapping(path = "/suffixlist", method = RequestMethod.GET)
	public @ResponseBody List<Map<String, String>> getSuffixList() {
		List<String> suffixList = this.dbOperation.getfileSuffixFilterList();
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		for (String suffix : suffixList) {
			Map<String, String> suffixMap = new HashMap<String, String>();
			suffixMap.put("suffix", suffix);
			resultList.add(suffixMap);
		}
		return resultList;
	}

	@RequestMapping(path = "/schedule/setAttachmentProcessConfiguration", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> setAttachmentProcessConfiguration(String suffixList) {
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("success", true);

		this.dbOperation.setAttachmentProcessConfiguration(suffixList);

		return info;
	}

	@RequestMapping(path = "/state/scheduleList", method = RequestMethod.GET)
	@ResponseBody
	public List<Schedule> getScheduleStatusList(Integer scheduleId, Long from, Long to, Boolean showNormalflag,
			Boolean showSystemflag) {
		List<Schedule> scheduleList = showSystemflag != null && showSystemflag == true
				? this.dbOperation.getAllScheduleList() : this.dbOperation.getAllNormalScheduleList();
		Timestamp fromTime = from == null ? null : new Timestamp(from);
		Timestamp toTime = to == null ? null : new Timestamp(to);
		if (scheduleList != null) {
			Map<Integer, Integer> executeCountMap = this.dbOperation.getScheduleLogExecuteCountStatistics(fromTime,
					toTime);
			Map<Integer, Integer> successCountMap = this.dbOperation.getScheduleLogSuccessStatistics(fromTime, toTime);
			Map<Integer, Integer> failedCountMap = this.dbOperation.getScheduleLogFailedStatistics(fromTime, toTime);
			for (Schedule schedule : scheduleList) {
				// set schedule status
				TriggerKey key = new TriggerKey(schedule.getJobName(), Constants.MMS);
				try {
					if (this.scheduler.checkExists(key)) {
						schedule.setScheduleStatus(this.scheduler.getTriggerState(key).toString());
					} else {
						schedule.setScheduleStatus(Constants.SCHEDULE_STATUS_UNSCHEDULE);
					}
				} catch (SchedulerException e) {
					schedule.setScheduleStatus(Constants.SCHEDULE_STATUS_UNSCHEDULE);
				}

				GlobalConfig globalConfig = this.dbOperation.getScheduleActionTypeByVal(schedule.getActionType());
				// set action name
				schedule.setActionDisplayName(globalConfig.getTitle());
				// set statistics data
				schedule.setExecuteCount(
						executeCountMap.containsKey(schedule.getId()) ? executeCountMap.get(schedule.getId()) : 0);
				schedule.setSuccessCount(
						successCountMap.containsKey(schedule.getId()) ? successCountMap.get(schedule.getId()) : 0);
				schedule.setFailedCount(
						failedCountMap.containsKey(schedule.getId()) ? failedCountMap.get(schedule.getId()) : 0);
			}
		}

		// filter the normal schedule
		if (showNormalflag != null && showNormalflag) {
			Iterator<Schedule> it = scheduleList.iterator();
			while (it.hasNext()) {
				Schedule schedule = it.next();
				if (!schedule.getScheduleStatus().equalsIgnoreCase(TriggerState.NORMAL.toString())) {
					it.remove();
				}
			}
		}

		return scheduleList;
	}

	@RequestMapping(path = "/dsm/dssetup", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> dsSetup(DatasourceConfig dsConfig) {

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);

		if (dsConfig.getId() != null) {
			DatasourceConfig dsc = this.dbOperation.getDSConfigurationById(dsConfig.getId());
			if (dsc == null) {
				result.put("success", false);
				result.put("message", "非法请求！");
				return result;
			}
			dsConfig.setStatus(dsc.getStatus());
		}

		// check whether the dabase configuration is valid or invalid.
		try {
			this.check(dsConfig);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getLocalizedMessage());
			return result;
		}

		// save or update database configuration.
		if (dsConfig.getId() != null) {
			this.dbOperation.updateDataSourceConfiguration(dsConfig);
			this.dbConnection.resetConnectionByid(dsConfig.getId());
		} else {
			this.dbOperation.saveDatabaseConfiguration(dsConfig);
		}

		return result;
	}

	@RequestMapping(path = "/msm/regular", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> MailRegular(int id, int cycle, int count) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		int updatecount = this.dbOperation.setMailLimit(cycle, count, id);
		if (updatecount < 1) {
			result.put("success", false);
			result.put("message", "未找到对应配置信息!");
			return result;
		}
		return result;
	}

	@RequestMapping(path = "/smm/regular", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> SMRegular(int id, int cycle, int count) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		int updatecount = this.dbOperation.setSMLimit(cycle, count, id);
		if (updatecount < 1) {
			result.put("success", false);
			result.put("message", "未找到对应配置信息!");
			return result;
		}
		return result;
	}

	@RequestMapping(path = "/dsm/remove", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> removeDSSetup(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		DatasourceConfig dsc = this.dbOperation.getDSConfigurationById(id);
		if (dsc == null) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// remove schedule which use this mail setup
		List<Schedule> scheduleList = this.dbOperation.getScheduleListByDSID(id);
		if (scheduleList != null && scheduleList.size() > 0) {
			result.put("success", false);
			result.put("message", "无法删除－有计划任务正在使用，如需删除请确保所有的计划任务都没有使用此配置");
			return result;
		}
		this.dbOperation.removeDSConfigurationById(id);

		return result;
	}

	@RequestMapping(path = "/msm/remove", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> removeMailSetup(int id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		MailServerConfig msc = this.dbOperation.getMailServerConfigrationById(id);
		if (msc == null) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// remove schedule which use this mail setup.
		List<Schedule> scheduleList = this.dbOperation.getScheduleListByHandlerIDForMS(id);
		if (scheduleList != null && scheduleList.size() > 0) {
			result.put("success", false);
			result.put("message", "无法删除－有计划任务正在使用，如需删除请确保所有的计划任务都没有使用此配置");
			return result;
		}
		this.dbOperation.removeMailServerConfigurationById(id);

		return result;
	}

	@RequestMapping(path = "/smm/remove", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> removeSMSetup(Integer id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		ShortMessageConfiguration smc = this.dbOperation.getShortMessageConfigrationById(id);
		if (smc == null) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// remove schedule which use this short message configuration
		List<Schedule> scheduleList = this.dbOperation.getScheduleListByHandlerIDForSM(id);
		if (scheduleList != null && scheduleList.size() > 0) {
			result.put("success", false);
			result.put("message", "无法删除－有计划任务正在使用，如需删除请确保所有的计划任务都没有使用此配置");
			return result;
		}
		this.dbOperation.removeShortMessageConfigurationById(id);

		return result;
	}

	@RequestMapping(path = "/msm/mailsetup", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> mailSetup(MailServerConfig mailServerConfig) {

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		if (mailServerConfig == null || StringUtils.isEmpty(mailServerConfig.getDisplayName())) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// update mail server configuration
		if (mailServerConfig.getId() != null) {
			MailServerConfig msc = this.dbOperation.getMailServerConfigrationById(mailServerConfig.getId());
			if (msc == null) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

			try {
				this.customConfig(mailServerConfig);
			} catch (Exception e) {
				result.put("success", false);
				result.put("message", e.getMessage());
				return result;
			}

			mailServerConfig.setStatus(msc.getStatus());
			this.dbOperation.updateMSConfiguration(mailServerConfig);
		} else {
			// add new mail server configuration
			if (mailServerConfig.getSetuptype() <= 1 || mailServerConfig.getSetuptype() > 3) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

			// import from remote database
			if (mailServerConfig.getSetuptype() == 1) {
				try {
					this.importConfig(mailServerConfig);
				} catch (Exception e) {
					result.put("success", false);
					result.put("message", e.getLocalizedMessage());
					return result;
				}
			}

			// use the available mail server configuration info.
			if (mailServerConfig.getSetuptype() == 2) {
				try {
					this.templateConfig(mailServerConfig);
				} catch (Exception e) {
					result.put("success", false);
					result.put("message", e.getLocalizedMessage());
					return result;
				}
			}

			// use custom mail server setup information
			if (mailServerConfig.getSetuptype() == 3) {
				try {
					this.customConfig(mailServerConfig);
				} catch (Exception e) {
					result.put("success", false);
					result.put("message", e.getMessage());
					return result;
				}
			}

			this.dbOperation.saveMSConfiguration(mailServerConfig);
		}

		return result;
	}

	@RequestMapping(path = "/smm/smtsetup", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> shortMessageTunnelSetup(ShortMessageConfiguration smc) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		if (smc == null) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// required check
		if (smc.getApplicationId() == null || smc.getApplicationPassword() == null || StringUtils.isEmpty(smc.getServiceAddress()) 
				|| smc.getServiceType() == null || StringUtils.isEmpty(smc.getDisplayName())
				|| smc.getTestPhoneNumber() == null) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// check test phone number format
		if (!Pattern.matches("1\\d{10}", smc.getTestPhoneNumber())) {
			result.put("success", false);
			result.put("message", "手机号不被受理");
			return result;
		}

		// check short message tunnel is support or not.
		if (!this.dbOperation.checkShortMessageTunnelIsSupported(smc.getSmTunnel())) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// check extend code
		if (smc.getExtendCode() != null && (!(Pattern.matches("\\d{0,4}", smc.getExtendCode())))) {
			result.put("success", false);
			result.put("message", "非法请求");
			return result;
		}

		// check whether the configuration is valid or not.
		try {
			this.check(smc);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
			return result;
		}

		// add a new short message tunnel
		if (smc.getId() == null) {
			this.dbOperation.saveSMConfiguration(smc);
		} else {
			// update short message tunnel configuration
			if (this.dbOperation.updateSMConfiguration(smc) == false) {
				result.put("success", false);
				result.put("message", "非法请求");
				return result;
			}

		}

		return result;
	}

	@RequestMapping(path = "changepassword", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> changepassword(String oldpassword, String newpassword, HttpServletRequest request) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		if (oldpassword == null || newpassword == null) {
			result.put("success", false);
			result.put("message", "非法请求！");
			return result;
		}

		if (oldpassword.equals(newpassword)) {
			result.put("success", false);
			result.put("message", "新密码不能于原密码相同！");
			return result;
		}

		int count = this.dbOperation.changePassword(newpassword, oldpassword, request.getRemoteUser());

		if (count == 0) {
			result.put("success", false);
			result.put("message", "原密码不对");
			return result;
		}

		return result;
	}

	private void importConfig(MailServerConfig mailServerConfig) throws Exception {

		if (mailServerConfig.getTestflag() != null && mailServerConfig.getTestflag() == true
				&& mailServerConfig.getTestmail() == null) {
			throw new Exception("非法请求!");
		}

		Map<String, String> configMap = null;
		try {
			configMap = this.getBatchServerConfigurationsFromRemoteDatasource(mailServerConfig.getDsid());
		} catch (Exception e) {
			throw new Exception("数据库中的必要信息不全或缺失，请选择其它方式");
		}

		mailServerConfig.setSmtpHost(configMap.get("host"));
		mailServerConfig.setSmtpPort(Integer.valueOf(configMap.get("port")));
		mailServerConfig.setDefaultSenderAddress(configMap.get("mailaddress"));
		mailServerConfig.setDefaultSenderTitle(configMap.get("title"));
		mailServerConfig.setDefaultSenderUserName(configMap.get("username"));
		mailServerConfig.setDefaultSenderPassword(configMap.get("password"));

		if (mailServerConfig.getTestflag() != null && mailServerConfig.getTestflag() == true) {
			try {
				this.check(mailServerConfig);
			} catch (Exception e) {
				throw e;
			}
		}
	}

	private void templateConfig(MailServerConfig mailServerConfig) throws Exception {

		if (mailServerConfig.getMsid() == null) {
			throw new Exception("非法请求!");
		}

		MailServerTemplate msTemplate = this.dbOperation.getTemplateMailConfigurations(mailServerConfig.getMsid());

		mailServerConfig.setSmtpHost(msTemplate.getSmtphost());
		mailServerConfig.setSmtpPort(msTemplate.getSmtpport());

		try {
			this.check(mailServerConfig);
		} catch (Exception e) {
			throw e;
		}
	}

	private void customConfig(MailServerConfig mailServerConfig) throws Exception {

		if (Constants.SERVER_TYPE_LOTUS == mailServerConfig.getServerType()) {
			if (Constants.LOTUS_CONNECTION_IOR == mailServerConfig.getConnType() && mailServerConfig.getIor() == null) {
				throw new Exception("非法请求!");
			}
		}

		try {
			this.check(mailServerConfig);
		} catch (Exception e) {
			throw e;
		}
	}

	// import mail server configurations from remote database.
	private Map<String, String> getBatchServerConfigurationsFromRemoteDatasource(int dsid) throws Exception {
		JdbcTemplate remoteJdbcTemplate = this.dbConnection.getRemoteJdbcTemplate(dsid);
		DatasourceConfig dsc = this.dbOperation.getDSConfigurationById(dsid);
		String sql = DBUtils.getSQL("SELECT * FROM :t WHERE SEQ >= 70 AND SEQ <= 81", dsc.getSqlType(),
				dsc.getArchName(), Constants.INF_COMPANY);
		List<RemoteConfigInfo> remoteConfigInfoList = remoteJdbcTemplate.query(sql, new RowMapper<RemoteConfigInfo>() {

			@Override
			public RemoteConfigInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
				RemoteConfigInfo rcinfo = new RemoteConfigInfo();
				rcinfo.setSeq(rs.getLong(Constants.SEQ));
				rcinfo.setCode(rs.getString(Constants.CODE));
				rcinfo.setDescription(rs.getString(Constants.DESCRIPTION));
				return rcinfo;
			}
		});

		if (remoteConfigInfoList == null || remoteConfigInfoList.size() < 1) {
			throw new Exception();
		}

		Map<String, String> result = new HashMap<String, String>();
		for (RemoteConfigInfo rcinfo : remoteConfigInfoList) {
			if (rcinfo.getSeq() == 70) {
				String mailaddress = rcinfo.getDescription().trim();
				if (mailaddress.length() < 1) {
					throw new Exception();
				}
				result.put("mailaddress", mailaddress);
				continue;
			}
			if (rcinfo.getSeq() == 71) {
				continue;
			}
			if (rcinfo.getSeq() == 72) {
				String[] value = rcinfo.getDescription().trim().split("\\|\\|");
				if (value == null || value.length <= 1) {
					throw new Exception();
				}
				result.put("host", value[0].trim());
				result.put("port", value[1].trim());
				continue;
			}
			if (rcinfo.getSeq() == 73) {
				String username = rcinfo.getDescription().trim();
				if (username.length() < 1) {
					throw new Exception();
				}
				result.put("username", username);
				continue;
			}
			if (rcinfo.getSeq() == 74) {
				String password = rcinfo.getDescription().trim();
				if (password.length() < 1) {
					throw new Exception();
				}
				result.put("password", password);
				break;
			}
		}

		if (result.size() > 0) {
			if (result.get("host") == null || result.get("port") == null || result.get("mailaddress") == null
					|| result.get("username") == null || result.get("password") == null) {
				throw new Exception();
			}
		}

		result.put("title", result.get("mailaddress").split("@")[0]);

		return result;
	}

	// check whether the mail configuration is valid or not.
	private void check(MailServerConfig mailServerConfig) throws Exception {
		MailServerConfig msc = new MailServerConfig();
		msc.setSmtpHost(mailServerConfig.getSmtpHost());
		msc.setSmtpPort(mailServerConfig.getSmtpPort());
		msc.setServerType(mailServerConfig.getServerType());
		msc.setMailFile(mailServerConfig.getMailFile());
		msc.setConnType(mailServerConfig.getConnType());
		msc.setIor(mailServerConfig.getIor());
		msc.setAuth(mailServerConfig.isAuth());
		msc.setDomainName(mailServerConfig.getDomainName());
		msc.setSsl(mailServerConfig.isSsl());
		msc.setTls(mailServerConfig.isTls());
		BatchJob batchJob = new BatchJob();
		batchJob.setMsConfig(msc);
		batchJob.setToAddressList(mailServerConfig.getTestmail());
		batchJob.setSenderAddress(mailServerConfig.getDefaultSenderAddress());
		batchJob.setSenderTitle(mailServerConfig.getDefaultSenderTitle());
		batchJob.setSubject("此邮件为测试邮件，收到此邮件表明配置正确，如错收请忽略。");
		batchJob.setContent("此邮件为测试邮件，收到此邮件表明配置正确，如错收请忽略。");
		batchJob.setSenderUserName(mailServerConfig.getDefaultSenderUserName());
		batchJob.setSenderPassword(mailServerConfig.getDefaultSenderPassword());
		batchJob.setActionType(Constants.ACTION_MAIL_SCAN);
		this.batchService.run(batchJob);
	}

	/**
	 * check short message tunnel setup valid or invalid.
	 * 
	 * @param smc
	 * @throws Exception
	 */
	private void check(ShortMessageConfiguration smc) throws Exception {
		BatchJob batchJob = new BatchJob();
		batchJob.setSmConfig(smc);
		batchJob.setToAddressList(smc.getTestPhoneNumber());
		batchJob.setActionType(Constants.ACTION_SM_SCAN);
		batchJob.setContent("测试短信，收到此短信代表配置正确，如错收请忽略。");
		this.batchService.run(batchJob);
	}

	/**
	 * check the database connection whether is valid or invalid.
	 * 
	 */
	private void check(DatasourceConfig dsConfig) throws Exception {
		String url = DBUtils.getDBURL(dsConfig.getSqlType(), dsConfig.getHost(), dsConfig.getPort(),
				dsConfig.getDbName(), dsConfig.getAuthType());
		DataSource ds = DataSourceBuilder.create().url(url).username(dsConfig.getUsername())
				.password(dsConfig.getPassword()).build();
		JdbcTemplate jt = new JdbcTemplate(ds);
		jt.execute(DBUtils.getCheckSQL(dsConfig.getSqlType()));
	}

	@RequestMapping(path = "/reset", method = RequestMethod.GET)
	@ResponseBody
	public String reset(HttpServletRequest request) {
		if (request.getRemoteAddr().equalsIgnoreCase(request.getLocalAddr())) {
			this.dbOperation.resetPassword();
			return "密码重置成功，请使用默认用户名密码登陆，登陆后必须修改密码";
		}
		return "不被允许的访问";
	}

}
