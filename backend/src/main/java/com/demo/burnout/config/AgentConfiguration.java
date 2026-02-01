package com.demo.burnout.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.demo.burnout.agent.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j Agent Configuration.
 * 
 * Configures AI services with Azure OpenAI integration using managed identity
 * for secure, keyless authentication.
 */
@Configuration
public class AgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentConfiguration.class);

    @Value("${azure.openai.endpoint:}")
    private String azureEndpoint;

    @Value("${azure.openai.deployment:gpt-4o}")
    private String deploymentName;

    @Value("${azure.identity.client-id:}")
    private String managedIdentityClientId;

    /**
     * Azure credential using DefaultAzureCredential.
     * Supports managed identity in Azure, and falls back to other methods locally.
     */
    @Bean
    public TokenCredential azureCredential() {
        DefaultAzureCredentialBuilder builder = new DefaultAzureCredentialBuilder();
        
        // If a specific managed identity client ID is provided, use it
        if (managedIdentityClientId != null && !managedIdentityClientId.isEmpty()) {
            log.info("Using user-assigned managed identity: {}", managedIdentityClientId);
            builder.managedIdentityClientId(managedIdentityClientId);
        } else {
            log.info("Using DefaultAzureCredential (auto-detect)");
        }
        
        return builder.build();
    }

    /**
     * Azure OpenAI Chat Model using managed identity.
     */
    @Bean
    public ChatModel azureChatModel(TokenCredential azureCredential) {
        log.info("Configuring Azure OpenAI with deployment: {} using managed identity", deploymentName);
        
        return AzureOpenAiChatModel.builder()
            .endpoint(azureEndpoint)
            .tokenCredential(azureCredential)
            .deploymentName(deploymentName)
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
    public ExplainerAiService explainerAiService(ChatModel chatModel) {
        log.info("Creating LangChain4j ExplainerAiService with Azure OpenAI");
        return AiServices.builder(ExplainerAiService.class)
            .chatModel(chatModel)
            .build();
    }

    /**
     * Protective AI Service - generates emotionally supportive responses.
     */
    @Bean
    public ProtectiveAiService protectiveAiService(ChatModel chatModel) {
        log.info("Creating LangChain4j ProtectiveAiService with Azure OpenAI");
        return AiServices.builder(ProtectiveAiService.class)
            .chatModel(chatModel)
            .build();
    }

    /**
     * Friday Deploy AI Service - assesses deploy readiness.
     */
    @Bean
    public FridayDeployAiService fridayDeployAiService(ChatModel chatModel) {
        log.info("Creating LangChain4j FridayDeployAiService with Azure OpenAI");
        return AiServices.builder(FridayDeployAiService.class)
            .chatModel(chatModel)
            .build();
    }
}
