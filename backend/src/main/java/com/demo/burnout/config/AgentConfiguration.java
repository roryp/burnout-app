package com.demo.burnout.config;

import com.demo.burnout.agent.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j Agent Configuration.
 * 
 * Configures AI services with Azure OpenAI integration using the official OpenAI SDK.
 */
@Configuration
public class AgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentConfiguration.class);

    @Value("${azure.openai.endpoint:}")
    private String azureEndpoint;

    @Value("${azure.openai.api-key:}")
    private String azureApiKey;

    @Value("${azure.openai.deployment:gpt-4o}")
    private String deploymentName;

    /**
     * Azure OpenAI Chat Model using the official OpenAI SDK.
     */
    @Bean
    public ChatLanguageModel azureChatModel() {
        log.info("Configuring Azure OpenAI with deployment: {}", deploymentName);
        
        return OpenAiOfficialChatModel.builder()
            .baseUrl(azureEndpoint)
            .apiKey(azureApiKey)
            .modelName(deploymentName)
            .isAzure(true)  // Required for Azure OpenAI!
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
    public FridayDeployAiService fridayDeployAiService(ChatLanguageModel chatModel) {
        log.info("Creating LangChain4j FridayDeployAiService with Azure OpenAI");
        return AiServices.builder(FridayDeployAiService.class)
            .chatLanguageModel(chatModel)
            .build();
    }
}
