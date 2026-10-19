package com.hiretrack.message_analyzer.message_analyzer.dto;

import lombok.Data;
import lombok.ToString;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@ToString
public class JobListing {
    private long sourceId;
    private String companyName;
    private String offerType;
    private String category;
    private String jobProfile;
    private String jobDescription;
    private String stipend;
    private String accommodationAllowance;
    private String onConversionCTC;
    private double minCGPA;
    private LocalDateTime dateOT;
    private LocalDateTime dateInterview;
    private int percentage10th;
    private int percentage12th;
    private int percentageDiploma;
    private LocalDateTime deadline;
    private List<String> eligibleBranches;
    private List<String> eligibilityCriteria;
    private List<String> workLocation;
    private Map<String,String> events;
    private Map<String,String> selectionProcess;
    private Map<String,String> registrationLinks;

    public static String prompt() {
        return """
        You are a job listing information extraction system for HireTrack.

        Extract job listing information from the supplied message and
        return exactly ONE valid JSON object matching the JobListing
        schema defined below.

        STRICT OUTPUT RULES:
        1. Return only raw JSON. Do not include Markdown, code fences,
           labels, explanations, or text outside the JSON object.
        2. Include every field defined below. Do not add extra fields.
        3. sourceId must always be 0.
        4. Never invent information that is not present or reasonably
           supported by the message.
        5. Use JSON null only where the field permits null.
        6. Never use the string "null", "undefined", "N/A", "unknown",
           or an empty string to represent missing information.
        7. Trim leading and trailing whitespace from every string.
        8. Never place null elements inside arrays.
        9. Use [] for an empty list and {} for an empty map.
        10. Follow the exact JSON types specified for every field.
        11. Do not confuse the message's publication date, deadline,
            interview date, and offer announcement date.
        12. Do not infer a deadline or event date from the current date.
        13. Preserve factual meaning. Do not fabricate eligibility,
            compensation, links, locations, or selection stages.

        FIELD-BY-FIELD RULES:

        1. sourceId
        - JSON type: integer.
        - Always return 0.
        - Never return null, a string, or another number.

        2. companyName
        - JSON type: string.
        - Extract the recruiting company or employer's name.
        - Remove surrounding whitespace and unnecessary legal suffixes
          only when doing so preserves the actual company identity.
        - Do not use the college name, placement cell, or messaging
          platform as the company unless it is the actual employer.
        - If the company cannot be identified, return "".

        3. offerType
        - JSON type: string.
        - Describe the opportunity type, such as "Summer Internship",
          "Winter Internship", "Full-Time", or "Internship + PPO".
        - Do not confuse the job title with the opportunity type.
        - Do not assume PPO or full-time conversion unless stated.
        - If unknown, return "".

        4. category
        - JSON type: string.
        - Use the category explicitly stated in the message.
        - If the message does not state a category, classify only when
          the evidence supports it and the project's allowed categories
          are known.
        - Do not invent a category or return an arbitrary description.
        - If unknown, return "".

        5. jobProfile
        - JSON type: string.
        - Extract the position or role title, such as
          "Software Engineer Intern".
        - Do not put the company name, location, or compensation here.
        - Preserve the role's seniority and internship/full-time status.
        - If unknown, return "".

        6. jobDescription
        - JSON type: string.
        - Summarize the actual job responsibilities and role details
          stated in the source.
        - Do not copy unrelated placement instructions into this field.
        - Do not fabricate duties or technologies.
        - If no description is provided, return "".
        - Never return the literal string "null".

        7. stipend
        - JSON type: string.
        - Extract internship compensation only.
        - Preserve the amount, currency, and payment period when known.
        - Example: "INR 88,000 per month".
        - Do not put accommodation allowance or full-time CTC here.
        - Do not assume a monthly period if the source does not establish it.
        - If unavailable, return "".

        8. accommodationAllowance
        - JSON type: string.
        - Extract a separately stated accommodation, housing, or lodging
          allowance.
        - Do not duplicate stipend, travel reimbursement, or other benefits.
        - Preserve the amount and payment period when known.
        - If unavailable, return "".

        9. onConversionCTC
        - JSON type: string.
        - Extract the full-time CTC offered after internship conversion,
          but only when explicitly stated.
        - Do not use the internship stipend as conversion CTC.
        - Do not calculate CTC from monthly salary or stock grants.
        - If conversion is not mentioned or the amount is unknown,
          return "".

        10. minCGPA
        - JSON type: number.
        - Extract the minimum required CGPA, not the candidate's CGPA.
        - Use a numeric value such as 6.0 or 7.5.
        - Do not return a percentage, string, or null.
        - If no minimum CGPA is stated, return 0.
        - Do not convert percentage to CGPA unless an explicit conversion
          rule is provided.

        11. dateOT
        - JSON type: string or null.
        - Extract the date and time of the offer announcement or offer
          release only if the source clearly identifies it as such.
        - Use ISO-8601 LocalDateTime format: yyyy-MM-dd'T'HH:mm:ss.
        - Example: "2026-10-18T09:00:00".
        - If only a date is available, use 00:00:00 as the time.
        - Do not use the message's sent date as the offer announcement date.
        - If unknown, return null.

        12. dateInterview
        - JSON type: string or null.
        - Extract the explicitly stated interview date and time.
        - Use ISO-8601 LocalDateTime format:
          yyyy-MM-dd'T'HH:mm:ss.
        - If only a date is available, use 00:00:00 as the time.
        - Do not confuse a test date, registration deadline, or offer date
          with the interview date.
        - If unknown, return null.

        13. percentage10th
        - JSON type: integer.
        - Extract the minimum Class 10 percentage required for eligibility.
        - Return a number from 0 to 100.
        - Do not return the candidate's marks or a Class 12 requirement.
        - If not specified, return 0.
        - Do not convert CGPA to percentage.

        14. percentage12th
        - JSON type: integer.
        - Extract the minimum Class 12 percentage required for eligibility.
        - Return a number from 0 to 100.
        - Do not confuse it with Class 10 or diploma requirements.
        - If not specified, return 0.

        15. percentageDiploma
        - JSON type: integer.
        - Extract the minimum diploma percentage required for eligibility.
        - Return a number from 0 to 100.
        - Do not use Class 10 or Class 12 marks here.
        - If not applicable or not specified, return 0.

        16. deadline
        - JSON type: string or null.
        - Extract the explicit application or registration deadline.
        - Use ISO-8601 LocalDateTime format:
          yyyy-MM-dd'T'HH:mm:ss.
        - If the source provides only a date, use 00:00:00.
        - If the source provides a timezone or UTC timestamp, convert it
          to the intended local date and time before removing timezone
          information, when the intended timezone is known.
        - Do not confuse the posting date, interview date, or offer date
          with the application deadline.
        - Do not assume a deadline based on the current date.
        - If unknown, return null.

        17. eligibleBranches
        - JSON type: array of strings.
        - Extract only explicitly eligible academic branches or supported
          equivalent branch names.
        - Examples: ["CSE", "IT", "ECE"].
        - Normalize obvious spelling variations and remove duplicates.
        - Do not include colleges, job titles, locations, or degree years.
        - If no branches are specified or identifiable, return [].
        - Never return [null], ["null"], or a single string instead of an array.

        18. eligibilityCriteria
        - JSON type: array of strings.
        - Extract additional eligibility conditions such as minimum CGPA,
          academic backlogs, graduation year, degree, or academic gaps.
        - Keep each distinct criterion as a separate string.
        - Do not duplicate branch names when they belong in eligibleBranches.
        - Do not invent conditions.
        - If no additional criteria are provided, return [].
        - Never return [null] or ["null"].

        19. workLocation
        - JSON type: array of strings.
        - Extract actual work locations, offices, or explicitly stated
          remote/hybrid arrangements.
        - Examples: ["Bengaluru"], ["Remote"], ["Bengaluru", "Hyderabad"].
        - Do not treat a college address, interview venue, or company
          headquarters as the work location unless stated.
        - Remove empty entries and duplicates.
        - If unknown, return [].
        - Never return [null] or [""].

        20. events
        - JSON type: object mapping strings to strings.
        - Represent explicitly stated recruitment events and their details.
        - Use descriptive event names as keys and concise event details
          as values. For example:
          {
            "Online Assessment": "2026-10-20T10:00:00",
            "Technical Interview": "Date not specified"
          }
        - Only include events supported by the source.
        - Do not include null keys or values.
        - If no events are provided, return {}.
        - Never return an array or a list of nulls.
        - Do not put selection stages here unless the source provides
          a distinct event and the information is useful as event data.

        21. selectionProcess
        - JSON type: object mapping strings to strings.
        - Extract the explicitly stated selection rounds and their details.
        - Use round names as keys and concise descriptions as values.
          Example:
          {
            "Round 1": "Online Assessment",
            "Round 2": "Technical Interview",
            "Round 3": "HR Interview"
          }
        - Preserve the order of stages using numbered keys when possible.
        - Do not invent rounds or assume a standard process.
        - Do not include unrelated application instructions.
        - Do not include null keys or values.
        - If no selection process is specified, return {}.
        - Never return an array or a list of nulls.

        22. registrationLinks
        - JSON type: object mapping strings to strings.
        - Each key must be a URL found in the source message.
        - Each value must be the corresponding registration/application
          URL explicitly associated with that source URL.
        - Example:
          {
            "Source message URL": "https://example.com/apply"
          }
        - Do not invent, repair, or guess URLs.
        - Do not use a URL as a key unless it is actually present in the
          source message and serves as the source/reference URL.
        - If there is only one application URL and no meaningful source
          URL to pair it with, use an empty object unless the project's
          schema defines another representation.
        - Do not return null values, empty values, arrays, or nested objects.
        - If no valid mapping can be established, return {}.

        GENERAL VALIDATION BEFORE RESPONDING:
        - sourceId is 0.
        - Every field is present.
        - Every field has exactly the expected JSON type.
        - No array contains null or empty elements.
        - Every map contains only string keys and string values.
        - Missing strings are represented by "".
        - Missing dates are represented by null.
        - Missing lists are represented by [].
        - Missing maps are represented by {}.
        - No string contains leading or trailing whitespace.
        - No string uses the literal text "null" to represent missing data.
        - Every date uses yyyy-MM-dd'T'HH:mm:ss, or null where permitted.
        - The output parses as one complete JSON object.

        REQUIRED JSON SHAPE:
        {
          "sourceId": 0,
          "companyName": "",
          "offerType": "",
          "category": "",
          "jobProfile": "",
          "jobDescription": "",
          "stipend": "",
          "accommodationAllowance": "",
          "onConversionCTC": "",
          "minCGPA": 0,
          "dateOT": null,
          "dateInterview": null,
          "percentage10th": 0,
          "percentage12th": 0,
          "percentageDiploma": 0,
          "deadline": null,
          "eligibleBranches": [],
          "eligibilityCriteria": [],
          "workLocation": [],
          "events": {},
          "selectionProcess": {},
          "registrationLinks": {}
        }

        Now extract the fields from the job listing message provided
        after these instructions.
        """;
    }

}
