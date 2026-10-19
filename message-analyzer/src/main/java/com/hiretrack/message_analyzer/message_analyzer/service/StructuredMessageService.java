package com.hiretrack.message_analyzer.message_analyzer.service;

import com.hiretrack.message_analyzer.message_analyzer.client.EntityManagerClient;
import com.hiretrack.message_analyzer.message_analyzer.dto.AiResponse;
import com.hiretrack.message_analyzer.message_analyzer.dto.InputMessage;
import com.hiretrack.message_analyzer.message_analyzer.dto.JobListing;
import com.hiretrack.message_analyzer.message_analyzer.enums.ResponseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import static java.awt.SystemColor.text;

@Slf4j
@Service
public class StructuredMessageService {
    private final ChatClient chatClient;
    private  final ObjectMapper objectMapper;
    private final PromptService promptService;
    public StructuredMessageService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, PromptService promptService){
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
    public AiResponse getStructuredMessage(InputMessage inputMessage, String classification){

        String text= inputMessage.getText();
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResponseType(ResponseType.valueOf(classification));
        switch (classification){
            case "JOB_LISTING":
                String jobListingPrompt = promptService.jobListingPrompt()+ promptService.additionalInstruction();
                String jobListingString = chatClient.prompt(text).user(u->u.text(jobListingPrompt)).call().content();
                System.out.println(jobListingString);
                JobListing jobListing;
                try{
                    jobListing =
                            objectMapper.readValue(jobListingString, JobListing.class);
                    jobListing.setSourceId(inputMessage.getSourceId());
                    aiResponse.setJobListing(jobListing);
                    System.out.println(jobListing.toString());

                }
                catch (Exception e){
                    System.out.println(e);
                    throw e;
                }
                break;
            case ("JOB_UPDATE"):
                break;
            case("SHORTLIST"):
                break;

        }

        //notification case
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
