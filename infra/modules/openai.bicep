@description('Location for resources')
param location string

@description('Name of the Azure OpenAI resource')
param openAiName string

@description('Model deployment name')
param deploymentName string = 'gpt-4o'

@description('Model name to deploy')
param modelName string = 'gpt-4o'

@description('Model version')
param modelVersion string = '2024-08-06'

@description('Capacity in thousands of tokens per minute')
param capacityK int = 50

resource openAi 'Microsoft.CognitiveServices/accounts@2024-10-01' = {
  name: openAiName
  location: location
  kind: 'OpenAI'
  sku: {
    name: 'S0'
  }
  properties: {
    customSubDomainName: openAiName
    publicNetworkAccess: 'Enabled'
  }
}

resource deployment 'Microsoft.CognitiveServices/accounts/deployments@2024-10-01' = {
  parent: openAi
  name: deploymentName
  sku: {
    name: 'Standard'
    capacity: capacityK
  }
  properties: {
    model: {
      format: 'OpenAI'
      name: modelName
      version: modelVersion
    }
  }
}

@description('Azure OpenAI endpoint URL')
output endpoint string = openAi.properties.endpoint

@description('Azure OpenAI resource ID')
output resourceId string = openAi.id

@description('Azure OpenAI resource name')
output name string = openAi.name
