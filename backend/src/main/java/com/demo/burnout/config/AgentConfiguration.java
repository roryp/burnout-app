package com.demo.burnout.config;

import com.demo.burnout.agent.*;
import com.demo.burnout.agent.tools.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j Agent Configuration.
 * 
 * Configures AI services with Azure OpenAI integration.
 * Falls back to stub implementations when Azure credentials are not configured.
 */
@Configuration
public class AgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentConfiguration.class);

    @Value("${azure.openai.endpoint:}")
    private String azureEndpoint;

    @Value("${azure.openai.api-key:}")
    private String azureApiKey;

    @Value("${azure.openai.deployment-name:gpt-4o}")
    private String deploymentName;

    @Value("${azure.openai.enabled:false}")
    private boolean azureEnabled;

    /**
     * Azure OpenAI Chat Model - the LLM backbone for all agents.
     */
    @Bean
    @ConditionalOnProperty(name = "azure.openai.enabled", havingValue = "true")
    public ChatLanguageModel azureChatModel() {
        log.info("Configuring Azure OpenAI with deployment: {}", deploymentName);
        
        return AzureOpenAiChatModel.builder()
            .endpoint(azureEndpoint)
            .apiKey(azureApiKey)
            .deploymentName(deploymentName)
            .temperature(0.3) // Lower temperature for consistent, focused responses
            .maxTokens(500)   // Keep responses concise
            .timeout(Duration.ofSeconds(30))
            .logRequestsAndResponses(false)
            .build();
    }

    /**
     * Chat memory for maintaining conversation context within a session.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();
    }

    /**
     * Explainer AI Service - explains GOAP plans in human-friendly terms.
     */
    @Bean
    @ConditionalOnProperty(name = "azure.openai.enabled", havingValue = "true")
    public ExplainerAiService explainerAiService(ChatLanguageModel chatModel) {
        log.info("Creating LangChain4j ExplainerAiService with Azure OpenAI");
        return AiServices.builder(ExplainerAiService.class)
            .chatLanguageModel(chatModel)
            .build();
    }

    /**
     * Protective AI Service - generates emotionally supportive responses.
     */
    @Bean
    @ConditionalOnProperty(name = "azure.openai.enabled", havingValue = "true")
    public ProtectiveAiService protectiveAiService(ChatLanguageModel chatModel) {
        log.info("Creating LangChain4j ProtectiveAiService with Azure OpenAI");
        return AiServices.builder(ProtectiveAiService.class)
            .chatLanguageModel(chatModel)
            .build();
    }

    /**
     * Friday Deploy AI Service - assesses deploy readiness.
     */
    @Bean
    @ConditionalOnProperty(name = "azure.openai.enabled", havingValue = "true")
    public FridayDeployAiService fridayDeployAiService(ChatLanguageModel chatModel) {
        log.info("Creating LangChain4j FridayDeployAiService with Azure OpenAI");
        return AiServices.builder(FridayDeployAiService.class)
            .chatLanguageModel(chatModel)
            .build();
    }
}
