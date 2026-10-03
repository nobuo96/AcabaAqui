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
