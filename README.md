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

O endpoint `POST /auth/login` recebe `{ "email": "...", "senha": "..." }`. Cadastro e login retornam um token e o perfil. O app salva o token como `app-secure-token` no SecureStore e o envia como `Authorization: Bearer <token>` nas chamadas feitas com `apiFetch`.

## Verificação de E-mail

O cadastro agora tem duas etapas: `POST /auth/email-verification/request` recebe `nome`, `email`, `telefone`, `perfil` e `senha`; `POST /auth/email-verification/confirm` recebe `email` e `code`. Só depois de confirmar o código são criados os registros em `usuarios` e `credenciais_usuario`, e a resposta inclui a sessão JWT. Para solicitar outro código, use `POST /auth/email-verification/resend` com o e-mail.

Antes de iniciar o backend, aplique manualmente `acaba-aqui-backend/src/main/resources/db/migration/V2__cadastro_pendente_email.sql` no mesmo banco MySQL. O Hibernate não cria tabelas automaticamente neste projeto.

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
