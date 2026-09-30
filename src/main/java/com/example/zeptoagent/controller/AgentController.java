package com.example.zeptoagent.controller;

import com.example.zeptoagent.model.Item;
import com.example.zeptoagent.service.CommandParser;
import com.example.zeptoagent.service.ZeptoBrowserAgent;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final CommandParser parser;
    private final ZeptoBrowserAgent browser;

    public AgentController(CommandParser parser, ZeptoBrowserAgent browser) {
        this.parser = parser;
        this.browser = browser;
    }

    @PostMapping("/run")
    public Map<String, Object> run(@RequestBody Request request) {

        List<Item> items = parser.parse(request.text());

        browser.setLogger(
            message -> System.out.println("[AGENT] " + message)
        );

        // Run Playwright directly so the Vercel container
        // keeps the agent alive while the browser automation runs.
        browser.run(items);

        return Map.of(
            "items", items,
            "message", "Agent completed. Cart is ready for your review."
        );
    }

    public record Request(String text) {}
}