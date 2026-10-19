package com.hiretrack.message_analyzer.message_analyzer.service;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hiretrack.message_analyzer.message_analyzer.client.EntityManagerClient;
import com.hiretrack.message_analyzer.message_analyzer.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

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
        AiResponse aiResponse = new AiResponse();

        String prompt = """
        Classifiy this message among the following:   JOB_LISTING,
                                                                    JOB_UPDATE,
                                                                    NOTIFCATION,
                                                                    SHORTLIST,
        only return the classification and nothing else. Just a single word.

    
    """.formatted(text);

        String additionalInstruction = "give raw json as plain text, no markdown code fences. JSON COMPLETENESS RULES:\n" +
                "- Generate the entire JSON object before ending the response.\n" +
                "- Every opening { must have a matching closing }.\n" +
                "- Every opening [ must have a matching closing ].\n" +
                "- Close all nested objects and arrays before closing the root object.\n" +
                "- Do not stop generation while any field value, string, array, or object is incomplete.\n" +
                "- Include all required fields, even when their values are empty.\n" +
                "- Do not omit the final closing brace of the root JSON object.\n" +
                "- Do not append any text after the completed JSON object.\n" +
                "- Before responding, verify that the complete output is syntactically valid JSON\n" +
                "  and can be parsed as one complete object.\n" +
                "- If information is missing, use the specified default value instead of\n" +
                "  leaving a field unfinished.";
        try{
            String response  = chatClient.prompt(text).user(u->u.text(prompt)).call().content();

            System.out.println(response);
            if (response==null){
                throw new RuntimeException("AI response Null");
            }
            if (response.equals("JOB_LISTING")){
                String jobListingPrompt =JobListing.prompt()+ additionalInstruction;
                String jobListingString = chatClient.prompt(text).user(u->u.text(jobListingPrompt)).call().content();
                System.out.println(jobListingString);

                ObjectMapper objectMapper = new ObjectMapper();

                JobListing jobListing =
                        objectMapper.readValue(jobListingString, JobListing.class);
                jobListing.setSourceId(inputMessage.getSourceId());

                System.out.println(jobListing.toString());

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
            log.error("AI Response Error: "+e.toString());
            throw new RuntimeException("ERROR: AI RESPONSE");
        }
    }


}
