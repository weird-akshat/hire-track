package com.hiretrack.message_analyzer.message_analyzer.service;

import com.hiretrack.message_analyzer.message_analyzer.dto.*;
import com.hiretrack.message_analyzer.message_analyzer.enums.ResponseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.management.Notification;

@Slf4j
@Service
public class LlmService {
    private final ChatClient chatClient;
    private  final ObjectMapper objectMapper;
    private final PromptService promptService;
    public LlmService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, PromptService promptService){
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.promptService = promptService;
    }

    @Retryable(
            includes =  RuntimeException.class,
            maxRetries = 4,
            delay = 1000
    )
    public String classify(InputMessage inputMessage){
        String text= inputMessage.getText();
        String response  = chatClient.prompt(text).user(u->u.text(promptService.classificationPrompt())).call().content();

        if (response == null || (!response.equals("JOB_LISTING") && !response.equals("NOTIFICATION") && !response.equals("JOB_UPDATE") && !response.equals("SHORTLIST"))){
            throw new RuntimeException("LLM: Incorrect Classification");
        }
        return response;
    }
    @Retryable(
            includes =  tools.jackson.core.JacksonException.class,
            maxRetries = 4,
            delay = 1000
    )
    public JobNotification getNotification(InputMessage inputMessage){
        String text= inputMessage.getText();

        String notificationPrompt = promptService.notificationPrompt() + promptService.additionalInstruction();
        String notifcationResposneString = chatClient.prompt(text).user(u->u.text(notificationPrompt)).call().content();
        JobNotification notification = objectMapper.readValue(notifcationResposneString,JobNotification.class);
        notification.setSourceId(inputMessage.getSourceId());

        log.info(notification.toString());
        return notification;
    }
    @Retryable(
            includes =  tools.jackson.core.JacksonException.class,
            maxRetries = 4,
            delay = 1000
    )
    public AiResponse getStructuredMessage(InputMessage inputMessage, String classification){
        log.info(classification);
        String text= inputMessage.getText();
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResponseType(ResponseType.valueOf(classification));

        try{
            switch (classification){
                case "JOB_LISTING":
                    String jobListingPrompt = promptService.jobListingPrompt()+ promptService.additionalInstruction();
                    String jobListingResponseString = chatClient.prompt(text).user(u->u.text(jobListingPrompt)).call().content();
                    JobListing jobListing =
                            objectMapper.readValue(jobListingResponseString, JobListing.class);
                    jobListing.setSourceId(inputMessage.getSourceId());
                    aiResponse.setJobListing(jobListing);
                    log.info(jobListing.toString());

                    break;
                case ("JOB_UPDATE"):
                    String jobUpdatePrompt = promptService.jobUpdatePrompt()+ promptService.additionalInstruction();
                    String jobUpdateResponseString = chatClient.prompt(text).user(u->u.text(jobUpdatePrompt)).call().content();
                    JobUpdate jobUpdate = objectMapper.readValue(jobUpdateResponseString,JobUpdate.class);
                    jobUpdate.setSourceId(inputMessage.getSourceId());
                    log.info(jobUpdate.toString());
                    aiResponse.setJobUpdate(jobUpdate);

                    break;
                case("SHORTLIST"):
                    String shortlistPrompt = promptService.shortlistPrompt()+promptService.additionalInstruction();
                    String shortlistResponseString = chatClient.prompt(text).user(u->u.text(shortlistPrompt)).call().content();
                    Shortlist shortlist = objectMapper.readValue(shortlistResponseString,Shortlist.class);
                    shortlist.setSourceId(inputMessage.getSourceId());
                    log.info(shortlist.toString());
                    aiResponse.setShortlist(shortlist);
                    break;
                default:
                    log.info("Object: Job Notification");
                    break;

            }

        }
        catch(Exception e){
            log.error(e.toString());
            throw e;
        }


        if (aiResponse.getJobListing()!=null){
            aiResponse.getJobListing().setSourceId(inputMessage.getSourceId());
        }
        if (aiResponse.getShortlist()!=null){
            aiResponse.getShortlist().setSourceId(inputMessage.getSourceId());
        }
        if (aiResponse.getJobNotification()!=null){
            aiResponse.getJobNotification().setSourceId(inputMessage.getSourceId());
        }
        log.info("Response returned from LLM: {}", aiResponse.toString());
        return aiResponse;


    }


}
