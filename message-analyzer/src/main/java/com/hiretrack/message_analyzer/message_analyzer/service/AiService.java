package com.hiretrack.message_analyzer.message_analyzer.service;
import com.hiretrack.message_analyzer.message_analyzer.client.EntityManagerClient;
import com.hiretrack.message_analyzer.message_analyzer.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static com.hiretrack.message_analyzer.message_analyzer.enums.ResponseType.*;

@Slf4j
@Service
public class AiService {
    private final ChatClient chatClient;
    private final EntityManagerClient entityManagerClient;
    public AiService(ChatClient.Builder chatClientBuilder, EntityManagerClient entityManagerClient){
        this.chatClient = chatClientBuilder.build();
        this.entityManagerClient= entityManagerClient;
    }
    public void analyze(List<InputMessage> list){
        try{
            List<AiResponse> aiResponses= new ArrayList<>();
            for (InputMessage inputMessage : list){
                AiResponse aiResponse = getStructuredMessage(inputMessage);
                aiResponse.setOriginalText(inputMessage.toString());
                aiResponses.add(aiResponse);
            }
            aiResponses.sort(Comparator.comparing(AiResponse::getResponseType));
            saveToEntity(aiResponses);
        }
        catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
    public void saveToEntity(List<AiResponse> aiResponses){
        for (AiResponse aiResponse: aiResponses){
            if (aiResponse.getResponseType()==JOB_LISTING){
                log.info("JOB LISTING: : {}", aiResponse.getJobListing().toString());
                log.info("Sending to message extractor.");
                ApiResponse response = entityManagerClient.createJobListing(aiResponse.getJobListing());
                log.info("Request completed");
                log.info("Entity Manager response: {}", response.toString());
            }
            if (aiResponse.getResponseType() == JOB_UPDATE){
                log.info("Sending request to entity manager for updating the job.");
                log.info("JOB UPDATE: {}", aiResponse.getJobUpdate().toString());
                JobUpdateListing jobUpdateListing= entityManagerClient.link(aiResponse.getJobUpdate());
                String updatePrompt = "Given this job Listing object, update the fields according to the new update that has been given. DO NOT WRAP THE JSON IN MARKDOWN CODE FENCES (e.g., ```json or ```).";
                try{
                    JobUpdateListing newJobUpdateListing= chatClient.prompt(jobUpdateListing.toString()+aiResponse.getOriginalText()).user(u->u.text(updatePrompt)).call().entity(JobUpdateListing.class);
                    if (newJobUpdateListing==null){
                        log.error("JobUpdateListing not created by the llm.");
                        throw new RuntimeException("JobUpdateListing not created by the llm.");
                    }
                    log.info("JOB UPDATE LISTING CREATED: {}",newJobUpdateListing.toString());
                    log.info("Sending the new job update listing to entity manager");
                    ApiResponse apiResponse = entityManagerClient.updateJob(newJobUpdateListing);
                    log.info(apiResponse.toString());
                }
                catch (Exception e){
                    log.error(e.getMessage());
                }
            }
            if (aiResponse.getResponseType()==SHORTLIST){
                log.info("Shortlist: {}", aiResponse.getShortlist().toString());
                try{
                    log.info("Send request to create shortlist to entity manager.");
                    ApiResponse apiResponse = entityManagerClient.createShortlist(aiResponse.getShortlist());
                    log.info("Shortlist created successfully\nResponse from entity manager {}",apiResponse.toString());
                }
                catch (Exception e){
                    log.error("Error in creating shortlist.");
                    throw new RuntimeException();
                }
            }
            try{
                log.info("Sending request to entity manager to create notification.");
                ApiResponse apiResponse= entityManagerClient.createNotification(aiResponse.getJobNotification());
                log.info("Created notification successfully. \nEntity manager response: {}", apiResponse.toString());
            }
            catch (Exception e){
                log.info("Issue in creating notification");
                throw new RuntimeException();
            }
        }
    }
    public AiResponse getStructuredMessage(InputMessage inputMessage){
        String text= inputMessage.getText();
        String prompt = "Extract relevant fields; normalize company names (remove suffixes like 'OT','Inc.'); abbreviate branches (no spaces, exclude 'BTech'); parse all dates into LocalDateTime as ISO-8601 strings in yyyy-MM-dd'T'HH:mm:ss format without timezone, even from natural language; never return dates as objects containing '$date'; always create a notification object with concise message (detailed only if objectType='notification'); select proper enum; leave unknowns null except for numeric primitive fields, which must use 0 for int and 0.0 for double; always return valid JSON matching the Java DTO types; for all Map<String,String> fields, return a JSON object with string keys and string values, using {} if unknown; for all List<String> fields, return a JSON array of strings, using [] if unknown; never return a string or array for a Map field; use empty strings for unknown String fields unless explicitly allowed to be null; classify job listing changes (OT/interview postponement, new details) as 'jobupdate'; always ensure JSON is properly closed and minimal; if a jobUpdate exists, still include a notification; responseType should be NOTIFICATION only if no other object apart from notification is being created. If a message begins with caption: xxx.pdf format, classify it as job update and make it a job update object. If a message has a registration link given, classify it as joblisting and make it a jobListing object. If it's a job description, classify it as job update and create a jobUpdate object, not a jobListing. Keep the job role an empty string if no job role is found. Set originalText as an empty string. Ensure every field matches its Java DTO type, including nested objects. DO NOT WRAP THE JSON IN MARKDOWN CODE FENCES (e.g., ```json or ```).";        AiResponse aiResponse;
        try{
            aiResponse = chatClient.prompt(text).user(u->u.text(prompt)).call().entity(AiResponse.class);
            if (aiResponse==null){
                throw new RuntimeException("AI response Null");
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
        catch (Exception e){
            log.error("AI Response Error");
            throw new RuntimeException("ERROR: AI RESPONSE");
        }
    }


}
