@description('Location for all resources')
param location string = 'swedencentral'

@description('Environment name used as prefix for resource names')
param environmentName string = 'burnout'

@description('Azure OpenAI model deployment name')
param openAiDeployment string = 'gpt-5-mini'

@description('Azure OpenAI model name')
param openAiModelName string = 'gpt-5-mini'

@description('Azure OpenAI model version')
param openAiModelVersion string = '2025-08-07'

@description('Azure OpenAI capacity in thousands of tokens per minute')
param openAiCapacityK int = 50

// Resource naming
var identityName = '${environmentName}-identity'
var openAiName = '${environmentName}-openai'
var acrName = replace('${environmentName}acr', '-', '')
var containerAppsEnvName = '${environmentName}-cae'
var logAnalyticsName = '${environmentName}-logs'
var containerAppName = '${environmentName}-backend'

// User-Assigned Managed Identity
module identity 'modules/identity.bicep' = {
  name: 'identity-deployment'
  params: {
    location: location
    identityName: identityName
  }
}

// Azure OpenAI Service
module openAi 'modules/openai.bicep' = {
  name: 'openai-deployment'
  params: {
    location: location
    openAiName: openAiName
    deploymentName: openAiDeployment
    modelName: openAiModelName
    modelVersion: openAiModelVersion
    capacityK: openAiCapacityK
  }
}

// RBAC: Assign Cognitive Services OpenAI User to managed identity
module openAiRbac 'modules/openai-rbac.bicep' = {
  name: 'openai-rbac-deployment'
  params: {
    principalId: identity.outputs.principalId
    openAiResourceId: openAi.outputs.resourceId
  }
}

// Azure Container Registry
module acr 'modules/acr.bicep' = {
  name: 'acr-deployment'
  params: {
    location: location
    acrName: acrName
    principalId: identity.outputs.principalId
  }
}

// Container Apps Environment
module containerAppsEnv 'modules/container-apps-env.bicep' = {
  name: 'container-apps-env-deployment'
  params: {
    location: location
    environmentName: containerAppsEnvName
    logAnalyticsName: logAnalyticsName
  }
}

// Container App
module containerApp 'modules/container-app.bicep' = {
  name: 'container-app-deployment'
  params: {
    location: location
    containerAppName: containerAppName
    environmentId: containerAppsEnv.outputs.environmentId
    acrLoginServer: acr.outputs.loginServer
    imageName: 'mcr.microsoft.com/k8se/quickstart:latest'
    identityResourceId: identity.outputs.resourceId
    identityClientId: identity.outputs.clientId
    openAiEndpoint: openAi.outputs.endpoint
    openAiDeployment: openAiDeployment
  }
  dependsOn: [
    openAiRbac  // Ensure RBAC is set before container app tries to use OpenAI
  ]
}

// Outputs for azd
@description('Container App URL - used by azd for SERVICE_BACKEND_URI')
output SERVICE_BACKEND_URI string = containerApp.outputs.url

@description('ACR endpoint - required by azd for container deployments')
output AZURE_CONTAINER_REGISTRY_ENDPOINT string = acr.outputs.loginServer

@description('ACR name - required by azd')
output AZURE_CONTAINER_REGISTRY_NAME string = acr.outputs.name

@description('Container Apps Environment name')
output AZURE_CONTAINER_APPS_ENVIRONMENT_NAME string = containerAppsEnv.outputs.name

@description('Container App name for backend service')
output SERVICE_BACKEND_NAME string = containerAppName

@description('User-assigned identity resource ID for ACR pull')
output AZURE_CONTAINER_REGISTRY_MANAGED_IDENTITY_ID string = identity.outputs.resourceId

// Additional outputs
@description('Azure OpenAI endpoint')
output AZURE_OPENAI_ENDPOINT string = openAi.outputs.endpoint

@description('Managed identity client ID')
output identityClientId string = identity.outputs.clientId
