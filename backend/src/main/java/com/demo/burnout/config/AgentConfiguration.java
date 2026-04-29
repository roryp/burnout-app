package com.demo.burnout.config;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.demo.burnout.agent.*;
import com.openai.credential.BearerTokenCredential;
import com.openai.credential.Credential;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

    @Value("${azure.openai.api-key:}")
    private String apiKey;

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
     * Build a credential for the OpenAI SDK that returns a fresh access token on every
     * request. The Azure {@link TokenCredential} caches and refreshes tokens internally
     * (~5 min before expiry), so we never serve a stale token even though the model bean
     * is built once at startup.
     *
     * If a static API key is configured (local dev / dummy creds), use that instead.
     */
    private Credential buildOpenAiCredential(TokenCredential azureCredential) {
        if (apiKey != null && !apiKey.isEmpty()) {
            log.info("Using provided API key for Azure OpenAI (static)");
            return BearerTokenCredential.create(apiKey);
        }
        log.info("Using managed identity bearer token supplier for Azure OpenAI (auto-refresh)");
        TokenRequestContext tokenRequest = new TokenRequestContext()
            .addScopes("https://cognitiveservices.azure.com/.default");
        return BearerTokenCredential.create(
            () -> azureCredential.getToken(tokenRequest).block().getToken()
        );
    }

    /**
     * Azure OpenAI Chat Model using OpenAI Official SDK.
     * Uses managed identity token or API key for authentication.
     */
    @Bean
    @Primary
    public ChatModel azureChatModel(TokenCredential azureCredential) {
        log.info("Configuring Azure OpenAI with deployment: {} using OpenAI Official SDK", deploymentName);
        return OpenAiOfficialChatModel.builder()
            .baseUrl(azureEndpoint)
            .credential(buildOpenAiCredential(azureCredential))
            .modelName(deploymentName)
            .isAzure(true)
            .maxRetries(2)
            .timeout(java.time.Duration.ofSeconds(30))
            .temperature(0.3)
            .maxCompletionTokens(1024)
            .build();
    }

    /**
     * Planner Model for Supervisor pattern orchestration.
     * Uses the same deployment but separate instance for planning decisions.
     */
    @Bean("plannerModel")
    public ChatModel plannerModel(TokenCredential azureCredential) {
        log.info("Configuring Azure OpenAI plannerModel for Supervisor pattern");
        return OpenAiOfficialChatModel.builder()
            .baseUrl(azureEndpoint)
            .credential(buildOpenAiCredential(azureCredential))
            .modelName(deploymentName)
            .isAzure(true)
            .maxRetries(2)
            .timeout(java.time.Duration.ofSeconds(30))
            .temperature(0.3)
            .maxCompletionTokens(512)
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
     * Explainer AI Service - explains action plans in human-friendly terms.
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
