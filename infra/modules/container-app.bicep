@description('Location for resources')
param location string

@description('Name of the container app')
param containerAppName string

@description('Container Apps environment ID')
param environmentId string

@description('ACR login server')
param acrLoginServer string

@description('Full container image reference (e.g., myacr.azurecr.io/app:tag)')
param imageName string = 'mcr.microsoft.com/k8se/quickstart:latest'

@description('User-assigned managed identity resource ID')
param identityResourceId string

@description('User-assigned managed identity client ID')
param identityClientId string

@description('Azure OpenAI endpoint URL')
param openAiEndpoint string

@description('Azure OpenAI deployment name')
param openAiDeployment string = 'gpt-4o'

resource containerApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: containerAppName
  location: location
  tags: {
    'azd-service-name': 'backend'
  }
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${identityResourceId}': {}
    }
  }
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      ingress: {
        external: true
        targetPort: 8080
        transport: 'auto'
        allowInsecure: false
      }
      registries: [
        {
          server: acrLoginServer
          identity: identityResourceId
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'burnout-backend'
          image: imageName
          resources: {
            cpu: json('2')
            memory: '4Gi'
          }
          env: [
            {
              name: 'AZURE_OPENAI_ENDPOINT'
              value: openAiEndpoint
            }
            {
              name: 'AZURE_OPENAI_DEPLOYMENT'
              value: openAiDeployment
            }
            {
              name: 'AZURE_IDENTITY_CLIENT_ID'
              value: identityClientId
            }
            {
              name: 'SECURITY_ENABLED'
              value: 'true'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health'
                port: 8080
              }
              initialDelaySeconds: 30
              periodSeconds: 10
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health'
                port: 8080
              }
              initialDelaySeconds: 10
              periodSeconds: 5
            }
          ]
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 3
        rules: [
          {
            name: 'http-scaling'
            http: {
              metadata: {
                concurrentRequests: '10'
              }
            }
          }
        ]
      }
    }
  }
}

@description('Container App FQDN')
output fqdn string = containerApp.properties.configuration.ingress.fqdn

@description('Container App URL')
output url string = 'https://${containerApp.properties.configuration.ingress.fqdn}'

@description('Container App resource ID')
output resourceId string = containerApp.id
