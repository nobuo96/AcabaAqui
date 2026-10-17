# AcabaAqui
Plataforma para contratação de serviços de acabamento residencial

## Autenticação

O backend assina tokens JWT com `APP_JWT_SECRET`. Configure essa variável com um segredo aleatório em Base64 com pelo menos 32 bytes antes de iniciar o Quarkus. Exemplo no PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
```

O banco deve conter as tabelas `usuarios` e `credenciais_usuario` conforme o schema já criado. O cadastro grava os dados pessoais em `usuarios` e grava `senha_hash`, `salt` e `algoritmo` (`pbkdf2`) em `credenciais_usuario`. O hash usa PBKDF2-HMAC-SHA256 com 600.000 iterações. Contas antigas sem linha em `credenciais_usuario` precisam cadastrar ou redefinir a senha.

## Login social em desenvolvimento

O login por Google e Facebook é habilitado somente no perfil Quarkus `dev`; fora dele, `app.social-auth.enabled` permanece `false`. A tabela de vínculo entre a conta AcabaAqui e a identidade no provedor precisa ser criada manualmente aplicando `acaba-aqui-backend/src/main/resources/db/migration/V5__identidades_social.sql`. Não é necessário alterar as tabelas de senha existentes.

Crie apps de teste no Google Cloud e Meta for Developers. Mantenha o consentimento do Google em modo Testing e o Facebook em Development mode, adicionando os usuários de teste permitidos. Configure credenciais OAuth para as plataformas que pretende testar. O Android usa `com.acabaaqui.app.dev` como package; o iOS usa `com.acabaaqui.app.dev` como bundle identifier. O Facebook usa o esquema de retorno `fb<FACEBOOK_APP_ID>://authorize`; `app.config.ts` o registra no build nativo a partir do ID público.

Adicione os IDs públicos ao `acaba-aqui-frontend/.env.local` (não versione esse arquivo):

```env
EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID=...
EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=...
EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=...
EXPO_PUBLIC_FACEBOOK_APP_ID=...
```

No backend, configure os IDs de cliente Google permitidos e os dados do app Facebook. O segredo Facebook é somente do backend e nunca deve ser prefixado com `EXPO_PUBLIC_`:

```powershell
$env:SOCIAL_AUTH_ENABLED = "true"
$env:GOOGLE_CLIENT_IDS = "ID_ANDROID,ID_IOS,ID_WEB"
$env:FACEBOOK_APP_ID = "ID_DO_APP_FACEBOOK"
$env:FACEBOOK_APP_SECRET = "SEGREDO_DO_APP_FACEBOOK"
```

Inicie o backend na mesma sessão PowerShell em que definiu as variáveis. Em seguida, gere e execute um development build Android com `npx expo run:android` e inicie o Metro com `npx expo start --dev-client`. Expo Go não suporta os esquemas de retorno OAuth nativos usados por este fluxo. No iOS, gere um development build com EAS ou em um Mac. Na web, use `npm run web` e cadastre no Google/Meta o redirect URI exibido abaixo dos botões sociais na tela de login em desenvolvimento. Para o Android Google, cadastre também o package e o SHA-1 da chave usada na development build; para o Facebook, o app ID é parte do esquema nativo de retorno. As regras atuais do Meta podem exigir uma URL de publicação na Play Store para ativar Android no app Facebook; se o painel impedir essa configuração, valide primeiro o fluxo web e consulte os requisitos atuais do Meta.

No primeiro acesso social, o app pede telefone e perfil Cliente/Prestador; nos acessos seguintes usa o perfil já salvo. Um e-mail que já exista em outra conta não é vinculado automaticamente; entre pelo método já cadastrado nessa conta (a vinculação de provedores não faz parte desta implementação). O Facebook pode não fornecer e-mail para determinadas contas; nesses casos, use outro método de cadastro/login. A API valida a credencial junto ao provedor, não armazena tokens sociais e não deve ser habilitada fora do ambiente de desenvolvimento até haver configuração e revisão próprias para produção.

## Acesso pelo celular (Expo Go)

Em um dispositivo físico, `localhost` aponta para o próprio celular. Coloque o IPv4 do computador na mesma rede Wi-Fi em `acaba-aqui-frontend/.env.local`:

```env
EXPO_PUBLIC_API_URL=http://192.168.1.42:8080
```

Troque o IP de exemplo pelo endereço IPv4 do Wi-Fi exibido por `ipconfig`. O `.env.local` está ignorado pelo Git. O Quarkus aceita conexões LAN no perfil `dev`; após mudar o arquivo, reinicie o backend e o Expo com `npx expo start --clear --lan`. Permita o Java/Quarkus na porta `8080` no Firewall do Windows para redes privadas e confirme que celular e PC estão no mesmo Wi-Fi. Android Emulator continua usando `10.0.2.2`; web continua usando `localhost` se `EXPO_PUBLIC_API_URL` não estiver definido.

O endpoint `POST /auth/login` recebe `{ "email": "...", "senha": "..." }`. Cadastro e login retornam um token e o perfil. O app salva o token como `app-secure-token` no SecureStore e o envia como `Authorization: Bearer <token>` nas chamadas feitas com `apiFetch`.

## Verificação de E-mail

O cadastro agora tem duas etapas: `POST /auth/email-verification/request` recebe `nome`, `email`, `telefone`, `perfil` e `senha`; `POST /auth/email-verification/confirm` recebe `email` e `code`. Só depois de confirmar o código são criados os registros em `usuarios` e `credenciais_usuario`, e a resposta inclui a sessão JWT. Para solicitar outro código, use `POST /auth/email-verification/resend` com o e-mail.

Antes de iniciar a versão atualizada do backend, aplique manualmente `acaba-aqui-backend/src/main/resources/db/migration/V3__password_reset.sql` no banco MySQL correto, usando um usuário com permissões de `ALTER` e `CREATE`. O assistente não conecta ao banco nem executa esse SQL. A V3 adiciona `email_verificado` e `versao_token` e cria `redefinicao_senha`; preserve as migrations anteriores. O Hibernate não cria tabelas automaticamente neste projeto.

Configure `APP_EMAIL_VERIFICATION_SECRET` como Base64 de pelo menos 32 bytes; ele é a chave HMAC usada para armazenar os códigos sem guardar o código em texto puro. Exemplo para gerar no PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_EMAIL_VERIFICATION_SECRET = [Convert]::ToBase64String($bytes)
```

No perfil de desenvolvimento, o Mailer aponta para o Mailpit local em `localhost:1025`, sem autenticação ou TLS. Inicie o Mailpit com Docker:

```powershell
docker run --rm --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

Consulte as mensagens recebidas em `http://localhost:8025`. Para simular o envio sem SMTP, configure `SMTP_MOCK=true`. Para entregar e-mails reais em produção, configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` e `SMTP_START_TLS`; use um remetente autorizado pelo provedor e nunca versione as credenciais.

Os códigos têm 6 dígitos, expiram em 10 minutos, aceitam até 5 tentativas e têm cooldown de 60 segundos e limite de 3 envios por hora por e-mail. Pedidos de código têm resposta genérica para não revelar se um e-mail já está cadastrado.

## Recuperação de Senha

`POST /auth/password-reset/request` recebe `{ "email": "..." }`; a resposta é genérica mesmo quando não existe conta. `POST /auth/password-reset/resend` recebe o mesmo corpo e respeita os limites de envio. `POST /auth/password-reset/confirm` recebe `{ "email": "...", "code": "123456", "newPassword": "..." }`. Após confirmar, a senha é atualizada, o e-mail é marcado como verificado e JWTs anteriores são invalidados; o usuário volta ao login com a nova senha.

## Informações Pessoais

As telas de cliente e prestador carregam `GET /user/{id}` usando o ID e JWT da sessão. Nome, telefone e data de nascimento podem ser alterados com `PUT /user/{id}`; e-mail e perfil são somente leitura. A data é opcional e enviada como `YYYY-MM-DD`.

Antes de usar a data de nascimento, aplique manualmente `acaba-aqui-backend/src/main/resources/db/migration/V4__user_birth_date.sql` no banco MySQL com um usuário que tenha permissão de `ALTER`. O SQL apenas adiciona `data_nascimento DATE NULL`; o assistente não executa DDL nem conecta ao banco.

## Gestão de endereços

Cliente e prestador gerenciam endereços pela opção **Gerenciar endereços** na tela de Informações Pessoais. A tela permite cadastrar vários locais, editar, desativar e definir opcionalmente um endereço principal. A desativação é lógica e a lista mostra apenas endereços ativos. Endereços do prestador representam locais de atendimento/base; área de cobertura não é configurada nesta etapa.

Antes de usar os endereços, aplique manualmente `acaba-aqui-backend/src/main/resources/db/migration/V7__enderecos.sql` no banco MySQL correto. A migration cria a tabela `enderecos` relacionada a `usuarios`; o Hibernate não executa o DDL automaticamente.

As rotas autenticadas são `GET` e `POST /user/{userId}/addresses`, `PUT` e `DELETE /user/{userId}/addresses/{addressId}` e `PUT /user/{userId}/addresses/{addressId}/principal`. A API confere o usuário autenticado antes de permitir o acesso. `GET /addresses/cep/{cep}` consulta o ViaCEP no backend e retorna CEP, logradouro, complemento, bairro, cidade e UF. A consulta não exige credencial; se o CEP não for encontrado ou o serviço estiver indisponível, o app permite preencher o endereço manualmente.

## Gestão administrativa do catálogo

O grupo administrativo permite gerenciar categorias e serviços. Categorias têm nome, descrição opcional e situação; serviços têm nome, descrição, categoria e situação. A exclusão é lógica: categorias e serviços inativos permanecem consultáveis e podem ser reativados. A desativação de uma categoria preserva os serviços associados, mas impede que ela seja selecionada em novos serviços ou em alterações de categoria.

Antes de usar as telas de gestão, aplique manualmente `acaba-aqui-backend/src/main/resources/db/migration/V6__catalogo_servicos.sql` no banco MySQL correto. O Hibernate está configurado com `quarkus.hibernate-orm.database.generation=none` e não cria essas tabelas. Cadastre uma categoria ativa antes de cadastrar serviços.

As rotas `/categories` e `/services` exigem um JWT de usuário com perfil `administrador`. Cada recurso oferece `GET` para listagem, `POST` para cadastro, `PUT /{id}` para alteração e `DELETE /{id}` para desativação. As listagens aceitam o filtro opcional `?ativo=true` ou `?ativo=false`; sem filtro, retornam registros ativos e inativos.

## Solicitações de serviço do cliente

Clientes podem criar solicitações de serviços ativos informando área aproximada em m² e um endereço ativo próprio. A tela **Minhas solicitações** apresenta todas as solicitações, da mais recente para a mais antiga, com filtro para abertas. Solicitações abertas podem ser canceladas e passam ao status `ENCERRADA`. A solicitação guarda uma cópia dos dados do endereço usados na criação; alterações ou desativação posterior do endereço não alteram o histórico. Para usar o recurso, aplique manualmente `acaba-aqui-backend/src/main/resources/db/migration/V8__solicitacoes_servico.sql` no banco MySQL.

As rotas autenticadas `GET` e `POST /user/{userId}/service-requests` listam e criam solicitações do próprio cliente. A criação recebe `servicoId`, `enderecoId` e `areaM2` (maior que zero, com até duas casas decimais). `POST /user/{userId}/service-requests/{requestId}/cancel` encerra uma solicitação que ainda não esteja encerrada; solicitações de outros clientes não são acessíveis. `GET /services` também permite a clientes consultar somente serviços ativos; operações administrativas do catálogo continuam restritas a administradores.


# AcabaAqui

Aplicativo para conectar clientes a profissionais especializados em acabamentos residenciais, como gesso, drywall, pisos, porcelanato, pintura e outros serviços.

## Preparando o ambiente
### Requisitos

Antes de rodar o projeto, certifique-se de ter instalado:

Java 21
Maven
MySQL
Node.js
Docker (opcional, para testes de e-mail)
Configuração do Banco de Dados

### Crie um banco de dados MySQL:

SQL
CREATE DATABASE acabaaqui_db;

Configure um usuário da aplicação:

SQL
CREATE USER 'acabaaqui_app'@'localhost'
IDENTIFIED BY 'SuaSenhaForte';
 
GRANT ALL PRIVILEGES
ON acabaaqui_db.*
TO 'acabaaqui_app'@'localhost';
 
FLUSH PRIVILEGES;
Configuração do Backend
Variáveis obrigatórias

### Autenticação

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

Plain Text
http://localhost:8025

Todos os e-mails enviados pela aplicação aparecerão nessa interface.

Caso não queira configurar SMTP durante os testes:

```PowerShell
$env:SMTP_MOCK="true"
```

### Aplicando as migrations

O Hibernate não cria tabelas automaticamente.

Antes de iniciar a aplicação, execute os scripts localizados em:

Plain Text
src/main/resources/db/migration

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
env não tem suporte total. O realce de sintaxe é baseado em PowerShell.
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