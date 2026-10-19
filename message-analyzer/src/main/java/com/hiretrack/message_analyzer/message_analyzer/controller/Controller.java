package com.hiretrack.message_analyzer.message_analyzer.controller;

import com.hiretrack.message_analyzer.message_analyzer.dto.ApiResponse;
import com.hiretrack.message_analyzer.message_analyzer.dto.InputMessage;
import com.hiretrack.message_analyzer.message_analyzer.service.MessageAnalyzeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Slf4j
public class Controller {
    private final MessageAnalyzeService messageAnalyzeService;
    @PostMapping
    public ResponseEntity<ApiResponse> analyze(@RequestBody List<InputMessage> list){
        log.info("Received request body: {}", list);
        try{
            messageAnalyzeService.analyze(list);
            ApiResponse apiResponse = new ApiResponse(HttpStatus.CREATED,"Message analyzed and processed");
            return new ResponseEntity<>(apiResponse, apiResponse.getStatus());
        }
        catch (Exception e){
            log.error("Error in analyzing messages");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }



    }

}
