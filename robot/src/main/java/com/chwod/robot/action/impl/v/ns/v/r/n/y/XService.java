package com.chwod.robot.action.impl.v.ns.v.r.n.y;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.chwod.robot.action.ActionService;
import com.chwod.robot.action.impl.d.v.ns.ULService;
import com.chwod.robot.bean.EventContext;
import com.chwod.robot.bean.Sentence;
import com.chwod.robot.domain.Knowledge;
import com.chwod.robot.service.KnowledgeService;
import com.chwod.robot.utils.Constants;
import com.chwod.robot.utils.Constants.LEARNING;

/**
 * @implSpec	知道北京是什么地方吗？
 * @author liuxun
 *
 */
@Component("SS:V-NS-V-R-N-Y-X")
public class XService implements ActionService {
	
	private static final Logger logger = LoggerFactory.getLogger(ULService.class);

	@Autowired
	private KnowledgeService knowledgeService;
	
	@Override
	public Sentence process(Sentence sentence, EventContext eventContext) {

		logger.debug("Process class : [{}], sentence : [{}], deep : [{}]", this.getClass().getName(),
				sentence.getRequestWord(), sentence.getProcessDeep());

		String subject = sentence.getPartList().get(1).getWord();
		List<String> subjectList = new ArrayList<>();
		subjectList.add(subject);
		eventContext.setSubject(subjectList);

		String modificationContent = sentence.getPartList().get(4).getWord();
		List<Knowledge> knowledgeList = this.knowledgeService.getAllValidContentSolutions(subject, modificationContent);

		// unknown
		if (knowledgeList == null || knowledgeList.size() <= 0) {
			// search for possible solutions
			knowledgeList = this.knowledgeService.getAllPossibleContentSolutions(subject);
			if(knowledgeList != null && knowledgeList.size() >= 0) {
				//one solution or only one highest possible solution.
				if((knowledgeList.size() == 1) || (knowledgeList.get(1).getCount() * 2 <= knowledgeList.get(0).getCount())) {
					StringBuffer result = new StringBuffer("知道,");
					result.append(subject).append("是").append(knowledgeList.get(0).getContent());
					result.append("，不过我不知道它是不是你说的").append(modificationContent).append("？");
					// context setup
					eventContext.setType(EventContext.SENTENCE_TYPE_QUESTION_YESNO);
					eventContext.setPredicate(EventContext.SENTECNE_PREDICATE_TYPE_IS);
					List<String> objectList = new ArrayList<>();
					objectList.add(modificationContent);
					eventContext.setObject(objectList);
					eventContext.setFocus(EventContext.SENTENCE_PART_OF_SPEECH_SUBJECT);
					sentence.setResponseWord(result.toString());
					return sentence;
				}
				
				// more than one solutions.
				StringBuffer result = new StringBuffer(StringUtils.EMPTY);
				subjectList = new ArrayList<>();
				result.append("最有可能的是").append(knowledgeList.get(0).getContent()).append("，但是还有可能是");
				for (int i = 1; i < knowledgeList.size(); i++) {
					Knowledge knowledge = knowledgeList.get(i);
					if (knowledge.getCount() * 2 >= knowledgeList.get(0).getCount()) {
						result.append(knowledge.getContent()).append("、");
						subjectList.add(knowledge.getContent());
					}
				}
				result.deleteCharAt(result.length() - 1);
				result.replace(result.lastIndexOf("、"), result.lastIndexOf("、") + 1, "，或者是");
				result.append("，不过我不确定它们是不是你说的").append(modificationContent).append("，你能告诉我吗？");
				
				// setup context.
				eventContext.setType(EventContext.SENTENCE_TYPE_QUESTION_YESNO);
				eventContext.setSubject(subjectList);
				eventContext.setPredicate(EventContext.SENTECNE_PREDICATE_TYPE_IS);
				List<String> objectList = new ArrayList<>();
				objectList.add(modificationContent);
				eventContext.setObject(objectList);
				eventContext.setFocus(EventContext.SENTENCE_PART_OF_SPEECH_SUBJECT);
				
				sentence.setResponseWord(result.toString());
				return sentence;
			}
			
			// context setup
			eventContext.setType(EventContext.SENTENCE_TYPE_DECLARATIVE_NEGATIVE);
			sentence.setResponseWord("不知道");
			return sentence;
		}

		// one solution or one highest possible solution
		if ((knowledgeList.size() == 1) || (knowledgeList.get(1).getCount() * 2 <= knowledgeList.get(0).getCount())) {

			String object = knowledgeList.get(0).getContent();

			// context setup
			eventContext.setType(EventContext.SENTENCE_TYPE_DECLARATIVE);
			eventContext.setPredicate(EventContext.SENTECNE_PREDICATE_TYPE_IS);
			List<String> objectList = new ArrayList<>();
			objectList.add(object);
			eventContext.setObject(objectList);
			eventContext.setFocus(EventContext.SENTENCE_PART_OF_SPEECH_SUBJECT);

			sentence.setResponseWord(new StringBuffer("知道,").append(subject).append("是")
					.append(object).toString());
			return sentence;
		}

		// more than one solution
		eventContext.setType(EventContext.SENTENCE_TYPE_DECLARATIVE);
		eventContext.setPredicate(EventContext.SENTECNE_PREDICATE_TYPE_IS);
		eventContext.setFocus(EventContext.SENTENCE_PART_OF_SPEECH_SUBJECT);

		List<String> objectList = new ArrayList<>();
		objectList.add(subject);
		StringBuffer responseWords = new StringBuffer("知道,").append(subject).append("是");
		for (int i = 0; i < knowledgeList.size(); i++) {
			Knowledge knowledge = knowledgeList.get(i);
			if(knowledge.getCount() * 2 >= knowledgeList.get(0).getCount()) {
				String content = knowledge.getContent();
				responseWords.append(content);
				objectList.add(content);
				if (i < knowledgeList.size() - 1) {
					responseWords.append(", 也是");
				}
			}
		}
		responseWords.append("。");

		eventContext.setObject(objectList);
		sentence.setResponseWord(responseWords.toString());
		return sentence;
	}

	@Override
	public void learning(EventContext eventContext, LEARNING flag) {

		logger.debug("Learning class : [{}], sentence : [{}]", this.getClass().getName(),
				eventContext.getCurrentEvent());
		
		if(Constants.LEARNING.OK == flag) {
			if(eventContext.getType() == EventContext.SENTENCE_TYPE_QUESTION_YESNO) {
				if (eventContext.getSubject() != null && eventContext.getSubject().size() > 0
						&& eventContext.getObject() != null && eventContext.getObject().size() > 0) {
					for (String subject : eventContext.getSubject()) {
						String uuid = UUID.randomUUID().toString();
						for (String object : eventContext.getObject()) {
							Knowledge knowledge = new Knowledge(eventContext.getSessionId());
							knowledge.setEventId(uuid);
							knowledge.setName(object);
							knowledge.setProperty(EventContext.SENTECNE_PREDICATE_TYPE_IS);
							knowledge.setContent(subject);
							this.knowledgeService.save(knowledge);

							knowledge = new Knowledge(eventContext.getSessionId());
							knowledge.setEventId(uuid);
							knowledge.setName(subject);
							knowledge.setProperty(EventContext.SENTECNE_PREDICATE_TYPE_IS);
							knowledge.setContent(object);
							this.knowledgeService.save(knowledge);
						}
					}
					eventContext.setType(null);
				}
			}
		}
	}

}
