package com.arcus.arc1.ChatGPT;

import com.arcus.arc1.ChatGPT.dto.ChatRequest;
import com.arcus.arc1.ChatGPT.dto.ChatResponse;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/chat")
public class ChatGPTController {

    private final ChatGPTService chatGPTService;

    public ChatGPTController(ChatGPTService chatGPTService) {
        this.chatGPTService = chatGPTService;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        return chatGPTService.chat(request);
    }
}

