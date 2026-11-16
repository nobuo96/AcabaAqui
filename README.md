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

As alterações do banco são aplicadas por migrations SQL incrementais em `acaba-aqui-backend/src/main/resources/db/migration`; o Hibernate não cria/atualiza o schema automaticamente. Aplique manualmente `v4_lifecycle_status_history.sql` após `v1`–`v3`, depois `v5_visit_scheduling.sql`, `v6_service_quotes_and_notifications.sql` e `v7_payment_orders.sql`. Se o banco já tem a v6 aplicada, execute somente a v7 antes de iniciar o backend.

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

Para testar Checkout Pro Orders, configure um Access Token de teste na sessão do PowerShell em que o backend será iniciado:

```PowerShell
$env:MERCADOPAGO_ACCESS_TOKEN="SEU_ACCESS_TOKEN_DE_TESTE"
$env:MERCADOPAGO_ORDERS_SANDBOX_TEST_ENABLED="true"
```

O endpoint de criação de Order de teste fica desabilitado por padrão. Ative a variável acima somente em desenvolvimento local e somente junto a credenciais de teste; reinicie o backend após configurá-la. Não habilite essa opção em produção. O prefixo do Access Token não é usado para identificar o ambiente.

Com o backend em execução, rode o script abaixo em outro PowerShell. Ele solicita as credenciais sem exibir a senha, faz login em `/auth/login`, verifica o perfil, imprime o JWT no terminal, testa a conexão e cria uma Order:

```PowerShell
$baseUrl = "http://localhost:8080"
$email = Read-Host "E-mail do administrador"
$securePassword = Read-Host "Senha do administrador" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $loginBody = @{ email = $email; senha = $password } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod `
        -Uri "$baseUrl/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    $password = $null
    $loginBody = $null
}

if ($login.perfil -ine "administrador") {
    throw "A conta autenticada não tem perfil de administrador."
}
if ([string]::IsNullOrWhiteSpace($login.token)) {
    throw "A resposta de login não contém o JWT."
}

$adminJwt = $login.token
Write-Host "JWT de administrador (não compartilhe):"
Write-Host $adminJwt

$authHeaders = @{ Authorization = "Bearer $adminJwt" }
Write-Host "`nVerificação da integração:"
Invoke-RestMethod `
    -Uri "$baseUrl/payments/integration-check" `
    -Method Get `
    -Headers $authHeaders

$orderHeaders = @{
    Authorization = "Bearer $adminJwt"
    "X-Idempotency-Key" = [guid]::NewGuid().ToString()
}
$orderBody = '{"description":"Servico de teste","amount":1.00}'
Write-Host "`nCriando Checkout Pro Order de sandbox:"
$order = Invoke-RestMethod `
    -Uri "$baseUrl/payments/orders/sandbox-test" `
    -Method Post `
    -Headers $orderHeaders `
    -ContentType "application/json" `
    -Body $orderBody
$order
```

Abra `checkoutUrl` para verificar o redirecionamento. Use somente credenciais de teste. O JWT é impresso localmente para facilitar inspeção, mas não o compartilhe nem o grave em arquivo. Cada nova Order deve usar uma chave de idempotência própria; reutilize a mesma chave apenas ao repetir a mesma tentativa.

O endpoint de sandbox acima continua separado do fluxo real de cobrança. Para o fluxo integrado, aplique `v7_payment_orders.sql` e configure também o segredo de webhook somente no ambiente do backend:

```PowerShell
$env:MERCADOPAGO_WEBHOOK_SECRET="SEGREDO_DE_ASSINATURA_DO_WEBHOOK"
# Opcional: este é o deep link configurado em app.json.
$env:MERCADOPAGO_PAYMENT_RETURN_URL="acabaaquifrontend://payment-return"
```

No painel de integrações do Mercado Pago, configure as notificações de Orders para uma URL pública HTTPS que aponte para `/payments/webhook`. Para testar localmente, use um túnel HTTPS e encaminhe a URL até a porta do backend; `localhost` não é acessível pelos servidores do Mercado Pago. O segredo deve ser o segredo de assinatura fornecido para essa integração. O endpoint do webhook é público, mas valida `x-signature` antes de processar qualquer notificação.

Após aprovar um orçamento, o cliente usa **Pagar orçamento** na tela de solicitações. O backend cria e persiste uma Order idempotente, vinculada ao orçamento, e o app abre `checkoutUrl`. O retorno usa o deep link configurado, mas não confirma pagamento: a tela consulta o estado local, atualizado após consulta autenticada da Order ao Mercado Pago. Webhooks repetidos consultam o estado atual do Mercado Pago e não repetem transições; pagamentos pendentes também são reconciliados a cada 15 minutos. Pagamentos recusados, expirados ou cancelados podem ser tentados novamente.

`MERCADOPAGO_PAYMENT_RETURN_URL` deve ser ajustada se o esquema/rota do app mudar. Para builds web, configure uma URL HTTPS de retorno compatível com o domínio publicado. O Access Token continua somente no backend. O endpoint administrativo de reembolso integral `POST /payments/{paymentId}/refund` e o cancelamento de Order pendente `POST /payments/{paymentId}/cancel` dependem das permissões da credencial no Mercado Pago; a automação de cancelamento do serviço após pagamento é uma etapa posterior.

Rotas do fluxo integrado:

- `POST /payments/quotes/{quoteId}/checkout`: inicia ou retoma o checkout do orçamento aprovado do cliente autenticado.
- `GET /payments/quotes/{quoteId}` e `GET /payments/{paymentId}`: consultam o status visível ao cliente.
- `POST /payments/{paymentId}/sync`: consulta a Order diretamente no Mercado Pago antes de atualizar o status local.
- `POST /payments/{paymentId}/cancel`: cancela uma Order ainda não paga; após o pagamento, a API rejeita o cancelamento.
- `POST /payments/{paymentId}/refund`: reembolso integral administrativo, restrito a administradores.
- `POST /payments/webhook`: callback público, protegido pela assinatura do Mercado Pago.

### Configuração de e-mail

A aplicação utiliza validação de e-mail durante o cadastro.

Gere também uma chave para assinatura dos códigos:

```PowerShell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_EMAIL_VERIFICATION_SECRET = [Convert]::ToBase64String($bytes)
```

Defina as duas variáveis na mesma sessão do PowerShell usada para iniciar o backend. Variáveis `$env:` deixam de existir quando essa janela é fechada.

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
v1_features_epic1.sql
v2_request_communications.sql
v3_provider_services_and_regions.sql
v4_lifecycle_status_history.sql
v5_visit_scheduling.sql
v6_service_quotes_and_notifications.sql
v7_payment_orders.sql
```

Execute apenas as migrations ainda não aplicadas, sempre em ordem, antes de iniciar a aplicação.

## Rodando o Backend

Dentro da pasta do projeto:

```PowerShell
mvn quarkus:dev
```

No modo de desenvolvimento, o terminal do backend registra o método e o status de cada requisição HTTP.

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

#### Horários de visita

Na criação, `horariosDisponiveis` é opcional para manter clientes antigos compatíveis. Cada bloco deve começar em uma hora cheia e informar o fuso IANA do endereço. Todos os blocos da solicitação usam o mesmo fuso:

```JSON
{
  "servicoId": 1,
  "enderecoId": 5,
  "areaM2": 75,
  "horariosDisponiveis": [
    {
      "inicioLocal": "2030-05-10T09:00:00",
      "timeZoneId": "America/Sao_Paulo"
    },
    {
      "inicioLocal": "2030-05-10T11:00:00",
      "timeZoneId": "America/Sao_Paulo"
    }
  ]
}
```

O backend valida que os horários são futuros, não repetidos e inequívocos no fuso informado. O app permite escolher o fuso brasileiro correspondente ao endereço.

```HTTP
GET /visit-slots/requests/{requestId}
POST /visit-slots/{slotId}/reserve
POST /visit-slots/{slotId}/confirm
POST /visit-slots/{slotId}/cancel
```

O prestador só pode reservar após aceitar a oportunidade e pode manter uma reserva ativa por solicitação. O cliente confirma e qualquer participante da visita pode cancelar; uma reserva cancelada antes do início libera o bloco. O mesmo bloco não pode ser reservado por dois prestadores: as alterações da solicitação são serializadas com bloqueio de linha no banco. Horários disponíveis expiram no início do bloco; reservas expiram ao final da hora. A expiração é aplicada sob demanda ao listar ou operar os horários, sem tarefa agendada.

### Oportunidades, notificações e conversas

Prestadores escolhem os serviços que oferecem em uma única seleção e cadastram
as regiões atendidas separadamente. Cada serviço selecionado vale para todas as
regiões cadastradas:

```HTTP
GET /services
GET /provider/services
PUT /provider/services
GET /provider/coverage
POST /provider/coverage
DELETE /provider/coverage/{areaId}
```

O `PUT /provider/services` recebe `{ "servicoIds": [1, 2, 3] }`. A cobertura
recebe somente cidade e UF, por exemplo `{ "cidade": "Campinas", "uf": "SP" }`.

Uma nova solicitação gera uma oportunidade para cada prestador ativo que atende
ao mesmo serviço e cidade/UF. O prestador aceita a oportunidade e reserva um
horário. O cliente confirma; somente então a conversa é criada e aparece para
ambos. As oportunidades são notificações dentro do app:

```HTTP
GET /provider/opportunities
PUT /provider/opportunities/read
POST /provider/opportunities/{opportunityId}/accept
POST /provider/opportunities/{opportunityId}/decline
```

Após o horário de uma visita confirmada, o prestador pode enviar um orçamento com descrição e valor total pela tela de oportunidades. O cliente compara os orçamentos na lista de solicitações e pode aprovar um ou recusar cada um. Cada orçamento enviado vence em 7 dias corridos; a aprovação cancela os demais orçamentos e os horários restantes. O feed de notificações aparece nas telas de oportunidades e solicitações.

```HTTP
POST /service-quotes/requests/{requestId}
GET /service-quotes/requests/{requestId}
POST /service-quotes/{quoteId}/approve
POST /service-quotes/{quoteId}/reject
GET /notifications
PUT /notifications/{notificationId}/read
```

O `POST /service-quotes/requests/{requestId}` recebe `{ "descricao": "Serviço e materiais incluídos", "valorTotal": 1250.00 }`.

O feed interno registra o envio, a decisão do cliente, o cancelamento e a expiração do orçamento. Prestadores veem somente seus orçamentos; o cliente dono da solicitação vê todos.

Se todas as oportunidades forem recusadas, a solicitação é encerrada imediatamente. Sem aceite, solicitações com mais de 7 dias corridos também são encerradas por uma tarefa automática executada a cada 15 minutos; oportunidades pendentes e horários disponíveis são cancelados junto com a solicitação. Uma solicitação com ao menos uma oportunidade aceita permanece ativa para o agendamento.

As conversas existentes e as criadas após confirmação são acessadas por:

```HTTP
GET /conversations
GET /conversations/{conversationId}/messages
POST /conversations/{conversationId}/messages
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