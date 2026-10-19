package com.hiretrack.message_analyzer.message_analyzer.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    public String classificationPrompt(){
        return  """
                Classify the given message into exactly one of the following categories: `JOB_LISTING`, `JOB_UPDATE`, `SHORTLIST`, or `NOTIFICATION`.
                             Category definitions:
                             1. **JOB_LISTING**: A new job or internship opportunity has been announced. This includes new openings, job descriptions, eligibility criteria, company details, compensation, application deadlines, and application instructions for a newly announced opportunity.
                             2. **JOB_UPDATE**: A new update regarding a job or internship opportunity that has already been announced. This includes changes to interview schedules, application deadlines, eligibility criteria, selection rounds, test dates, venue details, hiring status, or other updates to an existing opportunity.
                             3. **SHORTLIST**: A message announcing or listing candidates who have been shortlisted, selected, or qualified for a particular stage of a recruitment process. This includes lists of shortlisted candidates, interview selections, candidates eligible for the next round, and results of recruitment rounds.
                             4. **NOTIFICATION**: General information that does not primarily belong to any of the three categories above. This includes general placement announcements, college notices, event announcements, administrative instructions, reminders, policy changes, and other informational messages unrelated to a specific new job listing, an update to an existing job, or a shortlist announcement.
                             Classification rules:
                             - Classify based on the primary purpose of the message, not merely the presence of keywords.
                             - A newly announced job or internship opportunity is `JOB_LISTING`.
                             - A change or update concerning an existing opportunity is `JOB_UPDATE`.
                             - An announcement identifying shortlisted or qualified candidates is `SHORTLIST`.
                             - If the message does not clearly fit any of these three categories, classify it as `NOTIFICATION`.
                             - If a message contains multiple types of information, choose the category representing its primary purpose.
                             - Do not infer missing information or invent context.
                             Output requirements:
                             Return exactly one of these four labels: `JOB_LISTING`, `JOB_UPDATE`, `SHORTLIST`, or `NOTIFICATION`.
                             Do not include explanations, punctuation, quotation marks, or any additional text
    """;
    }
    public String additionalInstruction(){
        return   "give raw json as plain text, no markdown code fences. JSON COMPLETENESS RULES:\n" +
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
    }

    public String jobListingPrompt(){
        return   """
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
    public String jobUpdatePrompt() {
        return """
        You are a job update information extraction system for HireTrack.

        Extract job update information from the supplied message and
        return exactly ONE valid JSON object matching the JobUpdate
        schema defined below.

        STRICT OUTPUT RULES:
        1. Return only raw JSON. Do not include Markdown, code fences,
           labels, explanations, or text outside the JSON object.
        2. Include every field defined below. Do not add extra fields.
        3. sourceId must always be 0.
        4. Never invent information not present or reasonably supported
           by the message.
        5. Use empty strings for missing string fields.
        6. Trim leading and trailing whitespace from every string.
        7. Use the exact JSON types specified for every field.
        8. Do not return the literal strings "null", "undefined",
           "N/A", or "unknown" to represent missing information.
        9. A job update must concern an existing job listing or an
           already announced recruitment process.
        10. Do not classify a new job posting as a job update merely
            because it contains job information.

        FIELD-BY-FIELD RULES:

        1. sourceId
        - JSON type: integer.
        - Always return 0.

        2. companyName
        - JSON type: string.
        - Extract the recruiting company or employer's name.
        - Remove surrounding whitespace and unnecessary legal suffixes
          only when doing so preserves the company's identity.
        - Do not use the college name or placement cell as the company
          unless it is the actual employer.
        - If the company cannot be identified, return "".

        3. jobRole
        - JSON type: string.
        - Extract the position or role associated with the update.
        - Preserve the role title and its internship or full-time status.
        - Do not use the company name, update description, or round name
          as the job role.
        - If the role cannot be identified, return "".

        EXTRACTION GUIDELINES:
        - A job update includes new information about an existing
          recruitment process, such as changes to interview schedules,
          assessment dates, application deadlines, selection rounds,
          eligibility, compensation, joining dates, or other recruitment
          details.
        - Extract only the company and job role from the message.
        - Do not extract the update itself into additional fields.
        - If the message contains an update but does not identify the
          company or role, use "" for the missing field.
        - Do not infer the company or role from unrelated context.
        - If the message is a general notification, shortlist announcement,
          or new job listing without an update to an existing process,
          do not fabricate job update information.

        GENERAL VALIDATION BEFORE RESPONDING:
        - sourceId is 0.
        - Every field is present.
        - Every field has the expected JSON type.
        - Missing strings are represented by "".
        - No string contains leading or trailing whitespace.
        - The output parses as one complete JSON object.

        REQUIRED JSON SHAPE:
        {
          "sourceId": 0,
          "companyName": "",
          "jobRole": ""
        }

        Now extract the fields from the job update message provided
        after these instructions.
        """;
    }

    public String shortlistPrompt() {
        return """
        You are a recruitment shortlist information extraction system
        for HireTrack.

        Extract shortlist information from the supplied message and
        return exactly ONE valid JSON object matching the Shortlist
        schema defined below.

        STRICT OUTPUT RULES:
        1. Return only raw JSON. Do not include Markdown, code fences,
           labels, explanations, or text outside the JSON object.
        2. Include every field defined below. Do not add extra fields.
        3. sourceId must always be 0.
        4. Never invent information that is not present or reasonably
           supported by the message.
        5. Use empty strings for missing string fields.
        6. Use 0 for missing numeric fields.
        7. Use [] for an empty studentDetails list.
        8. Trim leading and trailing whitespace from every string.
        9. Do not return the literal strings "null", "undefined",
           "N/A", or "unknown" to represent missing information.
        10. Never include null elements inside arrays.
        11. Follow the exact JSON types specified for every field.
        12. Do not confuse a shortlist announcement with a job listing,
            job update, or general notification.

        FIELD-BY-FIELD RULES:

        1. sourceId
        - JSON type: integer.
        - Always return 0.

        2. companyName
        - JSON type: string.
        - Extract the recruiting company or employer's name.
        - Remove surrounding whitespace and unnecessary legal suffixes
          only when doing so preserves the company's identity.
        - Do not use the college name or placement cell as the company
          unless it is the actual employer.
        - If the company cannot be identified, return "".

        3. jobRole
        - JSON type: string.
        - Extract the position associated with the shortlist.
        - Preserve the role title and internship or full-time status.
        - If the role cannot be identified, return "".

        4. shortlistRound
        - JSON type: integer.
        - Extract the explicitly stated shortlist round number.
        - For example, "shortlisted for Round 2" means 2 only if the
          message clearly identifies it as the shortlist round.
        - Do not confuse the shortlist round with a candidate's rank,
          interview slot, or another unrelated number.
        - Do not infer a round number from the recruitment sequence.
        - If no round number is explicitly identifiable, return 0.

        5. studentDetails
        - JSON type: array of Student objects.
        - Extract students explicitly identified as shortlisted in
          the supplied message.
        - Include only the fields firstName, lastName, and email
          in each student object.
        - Do not add extra fields to a student object.
        - Do not invent names or email addresses.
        - Do not infer a student's identity from an ambiguous name.
        - Normalize surrounding whitespace in names and email addresses.
        - Preserve the spelling of names and email addresses as provided.
        - If only a first name is available, use "" for lastName.
        - If only a last name is available, use "" for firstName.
        - If a student's email is unavailable, use "" for email.
        - Do not include null elements or null field values.
        - Do not include placement coordinators, recruiters, or other
          people who are not shortlisted students.
        - If no student details are present, return [].
        - Include only students supported by the source message.
        - Do not create placeholder student objects when no students
          are identified.

        EXTRACTION GUIDELINES:
        - A shortlist message announces students selected, shortlisted,
          or qualified for a recruitment stage.
        - Extract the company, job role, round number, and student details.
        - A message containing only a general statement that results
          are out, without identifying any shortlisted students, should
          have an empty studentDetails array.
        - If the message explicitly identifies shortlisted students
          but does not specify their company or role, use "" for the
          missing fields.
        - If the message is a new job posting, an ordinary recruitment
          update, or unrelated information, do not invent shortlist data.

        GENERAL VALIDATION BEFORE RESPONDING:
        - sourceId is 0.
        - Every field is present.
        - Numeric fields are integers.
        - studentDetails is an array of valid Student objects.
        - Every student object contains firstName, lastName, and email.
        - Missing strings are represented by "".
        - Missing shortlistRound is represented by 0.
        - Missing student lists are represented by [].
        - No array contains null elements.
        - The output parses as one complete JSON object.

        REQUIRED JSON SHAPE:
        {
          "sourceId": 0,
          "companyName": "",
          "jobRole": "",
          "shortlistRound": 0,
          "studentDetails": []
        }

        Each student object must follow this shape:
        {
          "firstName": "",
          "lastName": "",
          "email": ""
        }

        Now extract the fields from the shortlist message provided
        after these instructions.
        """;
    }

    public String notificationPrompt() {
        return """
        You are a general recruitment notification information extraction
        system for HireTrack.

        Extract notification information from the supplied message and
        return exactly ONE valid JSON object matching the JobNotification
        schema defined below.

        STRICT OUTPUT RULES:
        1. Return only raw JSON. Do not include Markdown, code fences,
           labels, explanations, or text outside the JSON object.
        2. Include every field defined below. Do not add extra fields.
        3. sourceId must always be 0.
        4. Never invent information not present or reasonably supported
           by the message.
        5. Use empty strings for missing string fields.
        6. Trim leading and trailing whitespace from every string.
        7. Do not return the literal strings "null", "undefined",
           "N/A", or "unknown" to represent missing information.
        8. Follow the exact JSON types specified for every field.
        9. Do not confuse a general notification with a job listing,
           job update, or shortlist announcement.

        FIELD-BY-FIELD RULES:

        1. companyName
        - JSON type: string.
        - Extract the recruiting company or employer's name only when
          explicitly identified in the message.
        - Do not treat the college, placement cell, or messaging
          platform as the company unless it is the actual employer.
        - If the company cannot be identified, return "".

        2. jobRole
        - JSON type: string.
        - Extract the associated job role only when explicitly stated.
        - Preserve the role title and internship or full-time status.
        - Do not infer a role from the company name or general context.
        - If no role is identifiable, return "".

        3. sourceId
        - JSON type: integer.
        - Always return 0.
        - Never return null, a string, or another number.

        4. title
        - JSON type: string.
        - Create a short, descriptive title for the notification.
        - Base the title on the actual message content.
        - Prefer a concise title that captures the main announcement.
        - Do not invent a company, event, date, or action.
        - If the message has no identifiable subject, return "".

        5. body
        - JSON type: string.
        - Summarize the important information in the original message.
        - Preserve the original factual meaning and relevant details.
        - Keep the summary concise and understandable.
        - Include important dates, instructions, or actions only when
          explicitly supported by the message.
        - Do not add assumptions, advice, or fabricated details.
        - Do not copy unrelated or repetitive content.
        - If the message contains no meaningful notification content,
          return "".

        EXTRACTION GUIDELINES:
        - A notification is general information that does not primarily
          announce a new job listing, update an existing recruitment
          process, or publish a shortlist.
        - Examples include general placement announcements, placement
          cell instructions, reminders, campus recruitment notices,
          event announcements, and other relevant informational messages.
        - If a message contains multiple topics, summarize the main
          notification without fabricating details.
        - Do not classify a new job posting as a general notification.
        - Do not classify an existing recruitment process update as a
          general notification when the update is its primary purpose.
        - Do not classify a shortlist or selection-results announcement
          as a general notification when shortlisted students are the
          primary subject.
        - Extract companyName and jobRole only if the message explicitly
          associates them with the notification.
        - For unrelated or meaningless messages, return empty strings
          for all string fields.

        GENERAL VALIDATION BEFORE RESPONDING:
        - sourceId is 0.
        - Every field is present.
        - sourceId is an integer.
        - All other fields are strings.
        - Missing strings are represented by "".
        - No string contains leading or trailing whitespace.
        - The output parses as one complete JSON object.

        REQUIRED JSON SHAPE:
        {
          "companyName": "",
          "jobRole": "",
          "sourceId": 0,
          "title": "",
          "body": ""
        }

        Now extract the fields from the notification message provided
        after these instructions.
        """;
    }
}

