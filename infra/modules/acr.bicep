@description('Location for resources')
param location string

@description('Name of the container registry (alphanumeric only)')
param acrName string

@description('Principal ID to assign AcrPull role to')
param principalId string

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: true
  }
}

// AcrPull role
var acrPullRoleId = '7f951dda-4ed3-4680-a7ca-43fe172d538d'

resource acrPullRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, principalId, acrPullRoleId)
  scope: acr
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', acrPullRoleId)
    principalId: principalId
    principalType: 'ServicePrincipal'
  }
}

@description('ACR login server')
output loginServer string = acr.properties.loginServer

@description('ACR resource ID')
output resourceId string = acr.id

@description('ACR name')
output name string = acr.name
