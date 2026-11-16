# Plano de implementação das funcionalidades

Este documento compara os requisitos de [FEATURES.md](./FEATURES.md) com o estado atual do repositório e organiza o trabalho restante. O plano é atualizado ao concluir cada parte; os itens distinguem validações automatizadas de validações manuais.

## Resumo executivo

O fluxo implementado cobre solicitações, oportunidades internas, histórico, agendamento e chat, expiração de oportunidades e o backend de orçamentos com notificações persistidas. A interface de orçamentos está planejada para a Parte 6.

Pagamentos de produção, execução/aprovação do serviço, disputas/evidências e garantia seguem pendentes nas partes seguintes. O status da solicitação continua limitado a `CRIADA` e `ENCERRADA`; os novos estados de orçamento são independentes.

## Matriz de funcionalidades

| Requisito | Situação | Evidência no repositório |
|---|---|---|
| Cliente cria uma solicitação com serviço, área e endereço | **Implementado** — formulário envia serviço, área, endereço, horários disponíveis e fuso IANA selecionado. | `acaba-aqui-frontend/app/cliente/novaSolicitacao.tsx`; `ServiceRequestService.java`; `v5_visit_scheduling.sql` |
| Prestadores correspondentes são notificados | **Implementado como feed interno** — ao criar a solicitação, são geradas oportunidades para prestadores correspondentes ao serviço e à cidade/UF. O produto definiu que as notificações serão somente dentro do app. | `RequestOpportunityService.java`; `RequestOpportunityResource.java`; `v2_request_communications.sql`; `ProviderOpportunitiesScreen.tsx` |
| Prestador aceita e escolhe horário da visita, reservado contra outros prestadores | **Implementado** — backend e tela do prestador permitem reservar/cancelar; blocos são protegidos por bloqueio transacional. | `VisitSlotService.java`; `VisitSlotResource.java`; `ProviderOpportunitiesScreen.tsx` |
| Cliente confirma o horário; chat abre após confirmação | **Implementado** — cliente confirma/cancela na lista de solicitações; o backend cria/reutiliza o chat somente na confirmação. | `VisitSlotService.java`; `ConversationService.java`; `solicitacoes.tsx` |
| Prestador envia orçamento; cliente vê todos, aprova um e a decisão é notificada | **Backend implementado na Parte 5** — entidade, migration v6, APIs autorizadas, expiração e notificações internas; interface segue para a Parte 6. | `ServiceQuoteService.java`; `ServiceQuoteResource.java`; `v6_service_quotes_and_notifications.sql` |
| Pagamento via Pix ou cartão | **Não implementado** — nenhuma integração ou fluxo de pagamento foi encontrado. | `pom.xml`, fontes e migrations do backend; `package.json` e fontes do frontend |
| Pagamento recebido pela plataforma e liberado ao prestador somente após aprovação do cliente | **Não implementado** — não há ciclo financeiro, estados de pagamento, webhooks, repasse ou reembolso. | Fontes e migrations do backend |
| Cliente aprova a execução e isso inicia o pagamento do prestador | **Não implementado** — não existe registro de execução, aprovação nem liberação financeira. | Fontes e migrations do backend |
| Lista de serviços com status e prazos, detalhe e ações para contestar/liberar pagamento | **Parcial** — existe lista de solicitações do cliente com serviço, área, endereço, data, status simplificado e cancelamento. Não há detalhe individual, prazos, contestação ou liberação de pagamento. | `acaba-aqui-frontend/app/(cliente)/solicitacoes.tsx`; `ServiceRequestResource.java`; `ServiceRequestStatus.java` |
| Disputa, evidências e reembolso ao cliente quando aplicável | **Não implementado** — não há estados de disputa, armazenamento de evidências, painel/decisão administrativa ou reembolso. | Fontes e migrations do backend; telas administrativas existentes |
| Chamado de garantia, prazo para correção e encerramento como resolvido | **Não implementado** — não há fluxo ou configuração de garantia. | Fontes e migrations do backend; README |

### Comportamento existente relevante

- Solicitações podem ser canceladas pelo cliente enquanto não encerradas; solicitações canceladas ficam com status `ENCERRADA`.
- Cada prestador pode aceitar sua oportunidade independentemente; somente após a confirmação da visita o chat é criado/reutilizado.
- O formulário de solicitação envia blocos de visita com o fuso IANA; a Parte 3 foi validada manualmente.
- O README documenta cadastro/login, endereço, catálogo, solicitação, oportunidades e chat. Também cita garantia, avaliação, disputa e retenção financeira como versões futuras.

## Regras de produto confirmadas

Estas decisões foram fornecidas durante a análise e devem ser tratadas como requisitos:

- Processador de pagamento: **Mercado Pago**.
- Notificações: **somente dentro do app**.
- Agenda: uma solicitação pode receber visitas de vários prestadores em horários diferentes. O cliente oferece horários exatos em blocos fixos e indivisíveis de 1 hora (por exemplo, 9h–10h, usando o horário local do endereço); o prestador escolhe um bloco e o cliente confirma. O mesmo bloco da mesma solicitação fica reservado para um prestador; outros prestadores podem reservar outros blocos. A reserva aguarda confirmação e só é liberada por cancelamento do cliente/prestador ou quando o horário passa.
- Fuso da agenda: cada bloco informa e persiste um `timeZoneId` IANA, pois o endereço não tem fuso armazenado e derivar somente pela UF pode ser impreciso. Todos os blocos de uma solicitação usam o mesmo fuso. O frontend deverá enviar o fuso correto do endereço.
- Limite de reservas: cada prestador pode manter no máximo uma reserva ativa por solicitação; ao cancelar antes do horário, pode escolher outro bloco.
- Fuso da agenda: cada bloco informa e persiste um `timeZoneId` IANA, pois o endereço não tem fuso armazenado e derivar somente pela UF pode ser impreciso. Todos os blocos de uma solicitação usam o mesmo fuso. O frontend deverá enviar o fuso correto do endereço.
- Limite de reservas: cada prestador pode manter no máximo uma reserva ativa por solicitação; ao cancelar antes do horário, pode escolher outro bloco.
- A reserva do horário é liberada se o cliente cancelar a visita, se o prestador cancelar a visita ou quando o horário passar.
- Se todas as oportunidades forem recusadas, cancelar a solicitação. Se ninguém responder em **7 dias corridos**, cancelar a solicitação. Ao cancelar, encerrar as oportunidades pendentes.
- Recusa de uma oportunidade pelo prestador a remove de sua lista. Recusa de um orçamento pelo cliente deve marcar aquele orçamento como recusado.
- Ao cancelar a solicitação, cancelar todos os orçamentos associados. Ao aprovar um orçamento, recusar todos os demais e cancelar as visitas restantes.
- Os prazos de negócio são de **7 dias corridos**.
- Repasse será **manual via Pix**, executado pela plataforma após aprovação do serviço ou após os 7 dias sem ação/disputa. O prestador não precisa de conta Mercado Pago nem solicitar o repasse; o sistema deve gerar a pendência para execução e registro administrativo. A origem informada é o saldo Mercado Pago da plataforma, com SLA operacional de até **7 dias úteis** após o repasse se tornar elegível.
- O Checkout Pro usará a conta da plataforma, sem OAuth individual por prestador. Conforme a validação informada, o cartão pode ser retido; o Pix é liquidado imediatamente no saldo da plataforma e esse saldo aguarda o repasse manual.
- O orçamento do MVP contém somente descrição e valor total, sem itens detalhados, e tem validade de **7 dias corridos**.
- Se o prestador cancelar ou não comparecer antes de iniciar o serviço, haverá reembolso integral e a solicitação reabrirá para buscar outro prestador.
- Após a conclusão registrada pelo prestador, o cliente tem 7 dias corridos para aprovar ou contestar. Se não houver ação do cliente nem disputa aberta ao fim do prazo, iniciar o repasse manual.
- Cancelamento pelo cliente é permitido após o pagamento somente antes do início da execução pelo prestador; depois do início não é permitido. Reembolsar pelo meio original: cartão pelo Mercado Pago e Pix por Pix.
- Disputa pode ser aberta após o início da execução e antes da aprovação do cliente. Aberta a disputa, ambas as partes têm 7 dias corridos para anexar evidências. Um administrador decide entre reembolsar o cliente ou pagar o prestador.
- Evidências podem ser imagens e documentos, sem vídeos; limites, armazenamento e retenção seguem a política MVP registrada abaixo.
- O período de garantia é configurado no orçamento/contrato aprovado e começa após aprovação do cliente. Após acionamento, o prestador tem 7 dias corridos para corrigir o serviço. Se não corrigir no prazo, o cliente recebe reembolso integral.

### Fluxo financeiro acordado

1. Depois de aprovar um orçamento, o cliente paga via checkout Mercado Pago (Pix ou cartão).
2. O pagamento só é considerado confirmado após evento do Mercado Pago; então o valor fica retido conforme o modelo acordado com o provedor e o serviço pode avançar para execução.
3. Quando o prestador informa que concluiu o serviço, inicia-se a janela de 7 dias para o cliente aprovar ou contestar.
4. Aprovação explícita libera o repasse manual ao prestador. Uma disputa aberta suspende a liberação até decisão administrativa.
5. Se o cliente não agir até o fim dos 7 dias e não existir disputa aberta, criar pendência administrativa de repasse manual.
6. Falha de pagamento não inicia o serviço. Reembolsos, repasses e notificações do Mercado Pago devem ser registrados e processados sem duplicidade.

### Referência Mercado Pago recebida — Checkout Pro Orders

- A integração escolhida passa a ser **Checkout Pro Orders**, com redirecionamento do cliente para o ambiente hospedado do Mercado Pago; não usar Checkout Transparente no MVP.
- A referência informa suporte a Pix, boleto e cartões, entre outros meios, e a uma API para criar, consultar, cancelar e reembolsar orders.
- O fluxo geral é: criar a order no backend, encaminhar o cliente à URL de checkout retornada, receber o retorno configurado e processar notificações do Mercado Pago.
- O retorno ao app não deve ser tratado como confirmação definitiva do pagamento; o backend deve confirmar o estado pela notificação e/ou consulta autenticada da order.
- A referência menciona conta de vendedor e certificado SSL. Para o app Expo, validar o formato aceito para URLs de retorno e o comportamento de deep link/Universal Link antes de implementar o redirecionamento.
- A decisão de Checkout Pro **não altera** o repasse manual via Pix ao prestador. Também não presumir que o fluxo hospedado permita reter valores até a conclusão do serviço: isso continua sujeito à validação comercial/operacional com o Mercado Pago.

## Pendências de decisão

As definições abaixo são necessárias para evitar regras conflitantes ou trabalho descartável:

1. **Mercado Pago antes da produção:** validar em conta de produção as condições de liquidação/retenção, grant e rotação do token da plataforma e URLs/SSL/notificações exigidas. Isso não bloqueia a modelagem do MVP.

## Plano de execução

As partes abaixo são entregas verticais pequenas, cada uma com escopo limitado e critério de aceite. Executá-las na ordem indicada; cada parte deve passar seus testes antes de começar a seguinte. Migrations sempre incrementais, sem alterar scripts já aplicados.

### Parte 0 — Fechar bloqueios de produto e operação

**Depende de:** nada.

- **Concluído para modelagem do MVP:** conta da plataforma, sem OAuth por prestador; repasse manual desde o saldo Mercado Pago da plataforma em até 7 dias úteis da elegibilidade; orçamento de descrição/valor total com validade de 7 dias; cancelamento/falta do prestador antes do início reabre a solicitação e reembolsa integralmente; evidências conforme política abaixo; garantia configurada no orçamento aprovado e reembolso integral se não corrigida em 7 dias.
- **Antes da produção:** confirmar com o Mercado Pago condições em produção, credenciais/grant e rotação do token, URLs públicas com SSL e notificações; não é bloqueio para modelar os estados.

**Política MVP de evidências aceita:** até 10 arquivos por parte; JPG/PNG/PDF; máximo 10 MB por arquivo; armazenamento privado acessível somente às partes e administradores; exclusão 180 dias após encerrar a disputa; se uma parte não anexar evidências em 7 dias, o administrador decide com o material disponível, sem vitória automática.

**Teste/aceite:** decisões do MVP registradas; uma Checkout Pro Order de teste foi criada e retornou HTTP 201. Verificações de produção permanecem obrigatórias antes do go-live.

### Parte 1 — Estados básicos e histórico

**Depende de:** Parte 0.

**Status:** implementada na camada de estados/histórico. A política comum valida transições para os domínios planejados; a Parte 1 registra solicitações e oportunidades e a Parte 2 registra visitas. A migration v4 inclui snapshots dos estados legados. Como não há executor de migrations configurado, aplique as migrations pendentes manualmente antes de iniciar o backend atualizado.

- Especificar transições permitidas e atores para solicitação, visita, orçamento, pagamento, execução, disputa e garantia.
- Adicionar a primeira migration incremental para os novos estados e histórico, preservando solicitações existentes com estados `CRIADA` e `ENCERRADA`.
- Criar testes de transições válidas/inválidas e de compatibilidade da migration.

**Teste/aceite:** os registros existentes continuam legíveis; uma transição não permitida é recusada sem alterar o estado; mudanças válidas são persistidas com data e ator.

**Matriz de transições v1**

| Entidade | Transições permitidas | Ator |
|---|---|---|
| Solicitação | `CRIADA` → `ENCERRADA` | Cliente ou sistema (cancelamento/expiração) |
| Oportunidade | `PENDENTE` → `ACEITA`/`RECUSADA` | Prestador |
| Oportunidade | `PENDENTE` → `ENCERRADA` | Cliente ou sistema |
| Visita | `DISPONIVEL` → `RESERVADA` → `CONFIRMADA` | Prestador reserva; cliente confirma |
| Visita | `RESERVADA`/`CONFIRMADA` → `CANCELADA` | Cliente ou prestador |
| Visita | `CANCELADA` → `DISPONIVEL` | Sistema, ao liberar bloco futuro |
| Visita | `DISPONIVEL`/`RESERVADA`/`CONFIRMADA` → `EXPIRADA` | Sistema, quando o horário passa |
| Orçamento | inicial → `ENVIADO` → `APROVADO`/`RECUSADO` | Prestador envia; cliente decide |
| Orçamento | `ENVIADO` → `EXPIRADO`/`CANCELADO` | Sistema |
| Pagamento | inicial → `AGUARDANDO_PAGAMENTO` → `PAGO`/`RECUSADO`/`EXPIRADO` | Sistema inicia; Mercado Pago confirma |
| Pagamento | `PAGO` → `REEMBOLSO_PENDENTE` → `REEMBOLSADO` | Cliente/administrador/sistema solicita; Mercado Pago confirma |
| Pagamento | `PAGO` → `REPASSE_PENDENTE` → `REPASSADO` | Sistema cria pendência; administrador registra Pix |
| Execução | inicial → `AGUARDANDO_INICIO` → `EM_ANDAMENTO` → `CONCLUIDA_PELO_PRESTADOR` | Sistema inicia; prestador atualiza |
| Execução | `CONCLUIDA_PELO_PRESTADOR` → `APROVADA`/`CONTESTADA` | Cliente; sistema aprova após 7 dias sem ação/disputa |
| Disputa | inicial → `ABERTA` → `ENCERRADA_REEMBOLSO`/`ENCERRADA_REPASSE` | Cliente abre; administrador decide |
| Garantia | inicial → `ATIVA` → `ACIONADA` → `CORRIGIDA` | Sistema ativa; cliente aciona; prestador corrige |
| Garantia | `ACIONADA` → `REEMBOLSO_PENDENTE` | Sistema/administrador após 7 dias sem correção |

### Parte 2 — Horários e reserva de visitas (backend)

**Depende de:** Parte 1.

- **Status:** implementada no backend; migration v5 aplicada pelo usuário. Não altere migrations já aplicadas.
- Persistem-se blocos de uma hora em horário local, com `timeZoneId` IANA. A criação aceita `horariosDisponiveis` opcional para manter compatibilidade com clientes existentes; blocos devem ser futuros, começar na hora cheia, não se repetir e ter horários locais inequívocos.
- Endpoints autenticados: `GET /visit-slots/requests/{requestId}`, `POST /visit-slots/{slotId}/reserve`, `POST /visit-slots/{slotId}/confirm` e `POST /visit-slots/{slotId}/cancel`.
- Somente prestadores com oportunidade aceita reservam; cada prestador tem uma reserva ativa por solicitação. Cliente confirma. Cliente ou prestador da reserva podem cancelar; antes do início o bloco volta a ficar disponível.
- Bloqueio pessimista da solicitação serializa reservas/alterações concorrentes; a reserva ativa é validada novamente dentro da transação. Blocos diferentes continuam reserváveis por outros prestadores.
- Expiração é aplicada sob demanda ao listar/reservar: disponibilidade expira no início do bloco e reserva ativa ao final da hora. Não foi adicionada tarefa agendada.

**Teste/aceite:** testes unitários cobrem criação/validação, autorização, limite por prestador, conflito no mesmo bloco, confirmação, cancelamento/liberação e expiração. A verificação de concorrência em banco real e o teste manual da API ficam pendentes até aplicar v5 em banco de teste; a implementação usa bloqueio pessimista transacional.

### Parte 3 — Agendamento e chat (frontend)

**Depende de:** Parte 2.

- **Status:** implementada na API e nas telas de cliente/prestador. A criação do chat foi movida do aceite da oportunidade para a confirmação da visita.
- Cliente escolhe e remove blocos de uma hora ao criar a solicitação. O seletor de fuso brasileiro usa o fuso do aparelho como padrão e permite correção pelo cliente.
- Prestador aceita a oportunidade, vê os horários e pode reservar ou cancelar um bloco; o cliente vê a reserva pendente e confirma ou cancela.
- Após a confirmação, ambos acessam a conversa; o endpoint de confirmação também devolve o ID da conversa para o app do cliente.

**Teste/aceite:** type-check do frontend, testes backend e validação manual ponta a ponta passaram. O cliente criou uma solicitação com dois blocos, o prestador reservou um, o cliente confirmou e abriu a conversa; cancelamentos também foram validados.

### Parte 4 — Expiração de oportunidades

**Depende de:** Parte 1.

- **Status:** implementada no backend; sem nova migration.
- Uma tarefa Quarkus executa a cada 15 minutos e encerra solicitações `CRIADA` com 7 dias corridos ou mais sem oportunidade aceita. O corte inclui exatamente o instante do limite.
- No encerramento, oportunidades pendentes e horários ativos são cancelados e o histórico registra as transições como `SISTEMA`.
- A solicitação é encerrada imediatamente quando todas as oportunidades existentes forem recusadas. Solicitações sem oportunidades expiram após 7 dias; se alguma oportunidade tiver sido aceita, a solicitação permanece ativa.
- O processamento é idempotente e serializado pelo bloqueio pessimista da solicitação para evitar corrida com aceite ou recusa.

**Teste/aceite:** testes com relógio controlado cobrem antes/no/depois do limite de 7 dias, todas recusadas, uma aceita, nenhuma resposta e reexecução sem duplicar efeitos.

### Parte 5 — Orçamentos (backend)

**Depende de:** Partes 2 e 0.

- **Status:** implementada no backend; migration v6 aplicada pelo usuário. Não altere migrations já aplicadas.
- O orçamento contém descrição e valor total em reais, com validade de 7 dias corridos. O prestador pode enviar um orçamento por solicitação após o horário de uma visita confirmada ter terminado.
- O cliente dono da solicitação lista todos os orçamentos e pode aprovar um ou recusar cada um. O prestador só consulta o próprio orçamento; a aprovação é serializada pelo bloqueio da solicitação e não permite dois aprovados.
- Aprovar um orçamento cancela os demais orçamentos enviados e os horários ainda ativos. Encerrar a solicitação cancela orçamentos enviados ou aprovados.
- Notificações internas persistidas informam envio, decisão, cancelamento e expiração; `GET /notifications` lista as do usuário e `PUT /notifications/{notificationId}/read` marca uma como lida.
- Orçamentos vencidos são expirados automaticamente a cada 15 minutos e também ao listar ou decidir, para respeitar o prazo mesmo antes da próxima execução da tarefa.

**Teste/aceite:** testes verificam acesso somente aos participantes, envio após visita, prazo e expiração, estados de recusa/aprovação, notificações e cancelamentos em cascata; não é possível aprovar dois orçamentos para a mesma solicitação. Migration v6 aplicada pelo usuário.

### Parte 6 — Orçamentos (frontend)

**Depende de:** Parte 5.

- **Status:** implementada na tela de oportunidades do prestador e na lista de solicitações do cliente; validação manual ponta a ponta pendente.
- Prestador vê e envia descrição/valor após uma visita concluída, acompanha seu próprio estado e prazo de validade.
- Cliente compara todos os orçamentos, aprova um ou recusa cada orçamento enviado; as telas atualizam depois da decisão e exibem os estados recusado, aprovado, cancelado e expirado.
- Cliente e prestador veem o feed de notificações internas e podem marcar avisos como lidos.

**Teste/aceite:** `tsc --noEmit` e `mvn -o package` passam. Falta o roteiro manual: concluir visitas de dois prestadores, enviar os dois orçamentos, recusar um e aprovar o outro; verificar estados, cancelamento e notificações em ambas as contas.

### Parte 7 — Cobrança e confirmação Mercado Pago

**Depende de:** Partes 5 e 0.

**Status:** fluxo principal implementado; aplicação da migration v7 e teste manual com webhook/checkout permanecem pendentes. O endpoint administrativo `POST /payments/orders/sandbox-test` foi mantido como prova isolada da integração e segue desabilitado por padrão.

- Migration `v7_payment_orders.sql` persiste um pagamento por orçamento aprovado; tentativas repetidas reutilizam o registro com novas chaves idempotentes.
- O cliente inicia Checkout Pro apenas para seu próprio orçamento aprovado. O backend cria a Order, define URLs de retorno, abre a checkout URL no app e disponibiliza consulta/sincronização de status. O retorno/deep link não altera o status financeiro.
- Webhooks exigem validação HMAC de `x-signature` e `x-request-id`; o backend consulta a Order diretamente antes de atualizar estado. Um job reconcilia Orders pendentes a cada 15 minutos, evitando depender exclusivamente do webhook.
- O gateway inclui consulta, cancelamento de Orders não pagas e reembolso integral; o reembolso é administrativo nesta parte. Estados são registrados no histórico de ciclo de vida e notificações internas.
- Para testar: aplicar v7; configurar `MERCADOPAGO_ACCESS_TOKEN` e `MERCADOPAGO_WEBHOOK_SECRET` no backend; cadastrar no painel do Mercado Pago uma URL pública HTTPS `/payments/webhook`; aprovar um orçamento com usuário cliente e testar o fluxo com credenciais/usuários de sandbox.
- Pendências para o responsável: aplicar migration v7; obter/configurar o segredo de assinatura; publicar ou tunelar endpoint HTTPS do webhook; confirmar no painel o tópico de notificação de Orders; testar Pix/cartão aprovado, recusado e cancelado, repetição de webhook e deep link em build instalado; configurar a URL de retorno HTTPS se houver frontend web em produção.
- Antes da produção: validar com o Mercado Pago a conta/grant e as credenciais apropriadas, testar os callbacks no domínio com SSL e definir retenção do saldo/repasse manual por Pix. Não habilitar o endpoint sandbox como fluxo de cobrança real.

- Implementar Checkout Pro Orders: criar a order no backend e encaminhar o cliente à URL de checkout do Mercado Pago.
- Configurar URLs de retorno compatíveis com o app, tratar o deep link de retorno e não considerar esse retorno prova de pagamento confirmado.
- Obter e usar credenciais somente no backend; manter `client_secret` e `access_token` fora do app/frontend. Se no futuro for habilitado OAuth de vendedores, armazenar os tokens por vendedor de forma protegida e tratar a rotação de `refresh_token` de uso único.
- Persistir o pagamento e processar notificações/webhooks de forma idempotente; só marcar pago após confirmação confiável do Mercado Pago. Implementado; falta teste manual no sandbox.
- Tratar os estados da order documentados pelo Checkout Pro Orders; não iniciar serviço enquanto não houver confirmação de pagamento.
- Implementar consulta/cancelamento/reembolso de order conforme a API escolhida e manter o reembolso pelo meio original. Implementado no backend para consulta, cancelamento de Order pendente e reembolso integral administrativo; integração manual ainda pendente.
- Não armazenar número completo nem código de segurança do cartão.

**Teste/aceite:** testes automatizados verificam assinatura válida/inválida e transições; `mvn -o package` (64 testes) e `tsc --noEmit` passam. No sandbox ainda é necessário testar redirecionamento/retorno, Pix/cartão aprovado e recusado, webhook repetido e eventos fora de ordem; retorno ao app sozinho nunca marca o pagamento como confirmado.

### Parte 8 — Cancelamento pré-execução e reembolso

**Depende de:** Parte 7.

- Permitir cancelamento pelo cliente após pagamento somente antes do registro de início da execução.
- Reembolsar pelo meio original: cartão pelo Mercado Pago e Pix por Pix.
- Rejeitar cancelamento após início do serviço e registrar o motivo/resultado financeiro.

**Teste/aceite:** testes cobrem cancelamento antes/depois do início e reexecução da operação; não há reembolso duplicado e o meio de reembolso corresponde ao pagamento.

### Parte 9 — Início, conclusão e aprovação do serviço

**Depende de:** Partes 7 e 8.

- Permitir ao prestador registrar início e conclusão; a conclusão inicia a janela de 7 dias corridos para aprovação ou contestação.
- Criar lista e detalhe de serviços com estado, prazo, histórico e ações disponíveis.
- Permitir aprovação pelo cliente dentro do prazo e impedir aprovação repetida ou por usuário não participante.

**Teste/aceite:** testes cobrem transições e permissões; a conclusão abre o prazo correto; o detalhe exibe estado/prazo; aprovação repetida não gera novo efeito financeiro.

### Parte 10 — Fila e execução de repasse manual via Pix

**Depende de:** Parte 9 e decisão operacional da Parte 0.

- Permitir ao prestador cadastrar/alterar chave Pix com acesso restrito e exibição mascarada.
- Criar pendência de repasse após aprovação explícita ou 7 dias corridos sem ação/disputa.
- Criar tela/fila administrativa para executar o Pix, registrar valor, destinatário, comprovante/identificador e marcar como pago.
- Bloquear repasse enquanto houver disputa aberta.

**Teste/aceite:** aprovação e expiração criam uma única pendência; disputa impede a criação/liberação; somente administrador conclui o repasse; registro repetido não paga duas vezes.

### Parte 11 — Disputa e evidências

**Depende de:** Partes 9 e 10, e política de evidências da Parte 0.

- Permitir abrir disputa somente após início do serviço e antes da aprovação.
- Permitir às duas partes anexar imagens/documentos por 7 dias corridos; rejeitar vídeos e arquivos fora dos limites definidos.
- Criar fila/detalhe administrativo e registrar uma das decisões: reembolsar cliente ou pagar prestador.
- Suspender repasse enquanto a disputa estiver aberta e notificar as partes no app.

**Teste/aceite:** testes cobrem janela de abertura/anexos, autorização, tipos/tamanhos, ambos os resultados e garantia de que nenhum repasse ocorre durante disputa aberta.

### Parte 12 — Garantia

**Depende de:** Partes 9 e 11, e decisões de garantia da Parte 0.

- Configurar duração da garantia por serviço/contrato; iniciar a vigência após aprovação do cliente.
- Permitir abrir chamado, notificar o prestador e aplicar prazo de correção de 7 dias corridos.
- Permitir ao cliente encerrar como resolvida e tratar a falta de correção conforme a decisão definida na Parte 0.

**Teste/aceite:** testes verificam início/fim da vigência, prazo de correção, transições autorizadas e encerramento como resolvida; estados e prazos aparecem no detalhe do serviço.

### Parte 13 — Regressão e documentação de entrega

**Depende de:** Partes 1–12.

- Executar testes backend, type-check/build frontend e testes de integração com ambiente de teste do Mercado Pago.
- Rodar roteiros completos de cliente, prestador e administrador, incluindo cancelamento, reembolso, aprovação, disputa, repasse e garantia.
- Atualizar README/API com configuração, migrations, webhook e procedimento administrativo de Pix.

**Teste/aceite:** todos os testes passam; migrations aplicam em banco existente; o roteiro completo respeita as regras de [FEATURES.md](./FEATURES.md); operações financeiras ficam auditáveis e não são duplicadas.

**Critério global de conclusão:** cada requisito de [FEATURES.md](./FEATURES.md) está implementado de ponta a ponta, com autorização, persistência, interface, prazos, testes e procedimento operacional.

## Arquivos atuais de referência

- Solicitações: `acaba-aqui-backend/src/main/java/com/acabaaqui/services/ServiceRequestService.java`, `ServiceRequestResource.java`, `ServiceRequestStatus.java`, `acaba-aqui-frontend/app/cliente/novaSolicitacao.tsx` e `app/(cliente)/solicitacoes.tsx`.
- Oportunidades e visitas: `RequestOpportunityService.java`, `RequestOpportunityResource.java`, `acaba-aqui-frontend/components/ProviderOpportunitiesScreen.tsx`.
- Agendamento: `VisitSlotService.java`, `VisitSlotResource.java`, `VisitSlot.java`, `v5_visit_scheduling.sql`.
- Conversas: `ConversationService.java`, `ConversationResource.java`, `acaba-aqui-frontend/components/ConversationsScreen.tsx` e `app/chat/[conversationId].tsx`.
- Persistência atual: migrations `v1_features_epic1.sql` a `v7_payment_orders.sql`.
- Testes próximos ao fluxo: `ServiceRequestServiceTest.java`, `RequestOpportunityServiceTest.java` e `VisitSlotServiceTest.java`.
