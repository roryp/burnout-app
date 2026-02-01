@description('Location for resources')
param location string

@description('Name of the Container Apps environment')
param environmentName string

@description('Name of the Log Analytics workspace')
param logAnalyticsName string

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: logAnalyticsName
  location: location
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: environmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
  }
}

@description('Container Apps environment ID')
output environmentId string = containerAppsEnvironment.id

@description('Container Apps environment name')
output name string = containerAppsEnvironment.name

@description('Default domain for the environment')
output defaultDomain string = containerAppsEnvironment.properties.defaultDomain
