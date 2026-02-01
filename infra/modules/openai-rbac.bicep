@description('Principal ID to assign the role to')
param principalId string

@description('Resource ID of the Azure OpenAI resource')
param openAiResourceId string

// Cognitive Services OpenAI User role
var cognitiveServicesOpenAiUserRoleId = '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd'

resource openAiResource 'Microsoft.CognitiveServices/accounts@2024-10-01' existing = {
  name: last(split(openAiResourceId, '/'))
}

resource openAiRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(openAiResource.id, principalId, cognitiveServicesOpenAiUserRoleId)
  scope: openAiResource
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', cognitiveServicesOpenAiUserRoleId)
    principalId: principalId
    principalType: 'ServicePrincipal'
  }
}
