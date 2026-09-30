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
    public Map<String,Object> run(@RequestBody Request request) {
        List<Item> items = parser.parse(request.text());
        browser.setLogger(s -> System.out.println("[AGENT] " + s));
        new Thread(() -> browser.run(items), "zepto-browser-agent").start();
        return Map.of("items", items, "message", "Agent started. Watch the Chromium window.");
    }

    public record Request(String text) {}
}
