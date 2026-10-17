# AcabaAqui

Aplicativo para conectar clientes a profissionais especializados em acabamentos residenciais, como gesso, drywall, pisos, porcelanato, pintura e outros serviços.

## Preparando o ambiente
### Requisitos

Antes de rodar o projeto, certifique-se de ter instalado:

- Java 21
- Maven
- MySQL
- Node.js
- Docker (opcional, para testes de e-mail)

## Configuração do Banco de Dados

### Crie um banco de dados MySQL:

```SQL
CREATE DATABASE acabaaqui_db;
```

Configure um usuário da aplicação:

```SQL
CREATE USER 'acabaaqui_app'@'localhost'
IDENTIFIED BY 'SuaSenhaForte';
 
GRANT ALL PRIVILEGES
ON acabaaqui_db.*
TO 'acabaaqui_app'@'localhost';
 
FLUSH PRIVILEGES;
```

## Configuração do Backend
### Variáveis obrigatórias

O backend utiliza JWT para autenticação.

Gere uma chave aleatória:

```PowerShell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Defina a variável de ambiente:

```PowerShell
$env:APP_JWT_SECRET="CHAVE_GERADA"
```

### Configuração de e-mail

A aplicação utiliza validação de e-mail durante o cadastro.

Gere também uma chave para assinatura dos códigos:

```PowerShell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_EMAIL_VERIFICATION_SECRET = [Convert]::ToBase64String($bytes)
```

#### Rodando os e-mails localmente

Para desenvolvimento utilizamos o Mailpit.

Execute:

```PowerShell
docker run --rm --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

Acesse:

```Plain Text
http://localhost:8025
```

Todos os e-mails enviados pela aplicação aparecerão nessa interface.

Caso não queira configurar SMTP durante os testes:

```PowerShell
$env:SMTP_MOCK="true"
```

### Aplicando as migrations

O Hibernate não cria tabelas automaticamente.

Antes de iniciar a aplicação, execute os scripts localizados em:

```Plain Text
src/main/resources/db/migration
```

Os principais arquivos atualmente são:

```Plain Text
V1_features_epic1.sql
```

## Rodando o Backend

Dentro da pasta do projeto:

```PowerShell
mvn quarkus:dev
```

A API ficará disponível em:

```Plain Text
http://localhost:8080
```

## Rodando o Frontend

Instale as dependências:

```PowerShell
npm install
```

Crie o arquivo:

```Plain Text
.env.local
```

Exemplo:

```Plain Text
env não tem suporte total. O realce de sintaxe é baseado em Plain Text.
EXPO_PUBLIC_API_URL=http://localhost:8080
```

### Inicie o Expo:

```PowerShell
npx expo start
```

### Acessando pelo celular

Quando utilizar um dispositivo físico, substitua o localhost pelo IP da sua máquina.

Descubra o IP:

```PowerShell
ipconfig
```

Exemplo:

```PowerShell
EXPO_PUBLIC_API_URL=http://192.168.1.42:8080
```

### Reinicie o Expo:

```PowerShell
npx expo start --clear --lan
```

Celular e computador devem estar na mesma rede Wi-Fi.

## Funcionalidades

### Cadastro e Login

#### Cadastro

O cadastro acontece em duas etapas:

##### Solicitar código
```HTTP
POST /auth/email-verification/request
```

Dados enviados:

```JSON
{
"nome": "Victor Hugo",
"email": "victor@email.com",
"telefone": "61999999999",
"perfil": "cliente",
"senha": "SenhaForte123!"
}
```
##### Confirmar código
```HTTP
POST /auth/email-verification/confirm
```

```JSON
{
"email": "victor@email.com",
"code": "123456"
}
```

Após a confirmação a conta é criada e o usuário recebe seu JWT.

#### Login
```HTTP
POST /auth/login
```

```JSON
{
"email": "victor@email.com",
"senha": "SenhaForte123!"
}
```

A resposta contém:

```JSON
{
"token": "...",
"perfil": "cliente"
}
```

### Recuperação de Senha

Solicitar recuperação:

```HTTP
POST /auth/password-reset/request
```

```JSON
{
"email": "victor@email.com"
}
```

#### Confirmar nova senha:

```HTTP
POST /auth/password-reset/confirm
```

```JSON
{
"email": "victor@email.com",
"code": "123456",
"newPassword": "NovaSenha123!"
}
```

### Login Social

O login via Google e Facebook está disponível apenas para ambiente de desenvolvimento.

Configure as variáveis:

```PowerShell
$env:SOCIAL_AUTH_ENABLED="true"
$env:GOOGLE_CLIENT_IDS="ID_ANDROID,ID_IOS,ID_WEB"
$env:FACEBOOK_APP_ID="APP_ID"
$env:FACEBOOK_APP_SECRET="APP_SECRET"
```

No frontend:

```PowerShell
env não tem suporte total. O realce de sintaxe é baseado em PowerShell.
EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID=
EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=
EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=
EXPO_PUBLIC_FACEBOOK_APP_ID=
```

### Gestão de Usuário

O usuário autenticado pode:

- Consultar os próprios dados
- Alterar nome
- Alterar telefone
- Alterar data de nascimento
- Gerenciar endereços

#### Rotas principais:

```HTTP
GET /user/{id}
PUT /user/{id}
```

### Gestão de Endereços

Clientes e prestadores podem cadastrar vários endereços.

Funcionalidades:

- Criar endereço
- Editar endereço
- Desativar endereço
- Definir endereço principal

#### Rotas:

HTTP
GET /user/{userId}/addresses
POST /user/{userId}/addresses
PUT /user/{userId}/addresses/{addressId}
DELETE /user/{userId}/addresses/{addressId}
PUT /user/{userId}/addresses/{addressId}/principal

### Consulta de CEP:

```HTTP
GET /addresses/cep/{cep}
```

### Catálogo de Serviços

Administradores podem gerenciar categorias e serviços.

Exemplos:

- Forro de Gesso
- Drywall
- Piso Laminado
- Porcelanato
- Pintura

#### Rotas disponíveis:

```HTTP
GET /categories
POST /categories
PUT /categories/{id}
DELETE /categories/{id}
 
GET /services
POST /services
PUT /services/{id}
DELETE /services/{id}
```

### Solicitações de Serviço

Clientes podem criar solicitações informando:

Serviço desejado
Endereço
Área aproximada em m²

Exemplo:

```HTTP
POST /user/{userId}/service-requests
```

```JSON
{
"servicoId": 1,
"enderecoId": 5,
"areaM2": 75
}
```

#### Listar solicitações:

```HTTP
GET /user/{userId}/service-requests
```

#### Cancelar solicitação:

```HTTP
POST /user/{userId}/service-requests/{requestId}/cancel
```

## Fluxo atual da plataforma
Cliente cria uma solicitação.
Prestadores interessados recebem a oportunidade.
Prestadores realizam visita técnica.
Prestadores enviam orçamento.
Cliente escolhe o orçamento.
Cliente realiza o pagamento.
Serviço é executado.
Cliente aprova a conclusão.

As etapas de garantia, avaliação, disputa e retenção financeira serão implementadas nas próximas versões.