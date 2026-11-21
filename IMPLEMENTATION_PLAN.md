# Plano de implementação das funcionalidades

Este documento compara os requisitos de [FEATURES.md](./FEATURES.md) com o estado atual do repositório e organiza o trabalho restante. O plano é atualizado ao concluir cada parte; os itens distinguem validações automatizadas de validações manuais.

## Resumo executivo

O fluxo implementado cobre solicitações, oportunidades internas, histórico, agendamento e chat, expiração, orçamentos, Checkout Pro, cancelamento pré-execução, execução, repasse manual via Pix, disputas/evidências e garantia. As Partes 1–12 estão implementadas no código; as migrations v10–v12 e validações operacionais/manuais correspondentes ainda precisam ser concluídas. A Parte 13 (regressão e documentação de entrega) permanece pendente.

O plano de UX abaixo é uma avaliação do frontend do cliente e não altera as regras nem os contratos do backend.

## Matriz de funcionalidades

| Requisito | Situação | Evidência no repositório |
|---|---|---|
| Cliente cria uma solicitação com serviço, área e endereço | **Implementado** — formulário envia serviço, área, endereço, horários disponíveis e fuso IANA selecionado. | `acaba-aqui-frontend/app/cliente/novaSolicitacao.tsx`; `ServiceRequestService.java`; `v5_visit_scheduling.sql` |
| Prestadores correspondentes são notificados | **Implementado como feed interno** — ao criar a solicitação, são geradas oportunidades para prestadores correspondentes ao serviço e à cidade/UF. O produto definiu que as notificações serão somente dentro do app. | `RequestOpportunityService.java`; `RequestOpportunityResource.java`; `v2_request_communications.sql`; `ProviderOpportunitiesScreen.tsx` |
| Prestador aceita e escolhe horário da visita, reservado contra outros prestadores | **Implementado** — backend e tela do prestador permitem reservar/cancelar; blocos são protegidos por bloqueio transacional. | `VisitSlotService.java`; `VisitSlotResource.java`; `ProviderOpportunitiesScreen.tsx` |
| Cliente confirma o horário; chat abre após confirmação | **Implementado** — cliente confirma/cancela na lista de solicitações; o backend cria/reutiliza o chat somente na confirmação. | `VisitSlotService.java`; `ConversationService.java`; `solicitacoes.tsx` |
| Prestador envia orçamento; cliente vê todos, aprova um e a decisão é notificada | **Implementado** — backend, tela do cliente/prestador, expiração e notificações internas. | `ServiceQuoteService.java`; `ServiceQuoteResource.java`; `v6_service_quotes_and_notifications.sql`; `solicitacoes.tsx`; `ProviderOpportunitiesScreen.tsx` |
| Pagamento via Pix ou cartão | **Checkout Pro implementado e pagamento de sandbox confirmado manualmente** — cartão/Pix são processados no ambiente Mercado Pago; faltam casos manuais de recusa, reembolso e retorno mobile. | `ServicePaymentService.java`; `MercadoPagoOrdersHttpGateway.java`; `v7_payment_orders.sql`; `payment-return.tsx` |
| Pagamento recebido pela plataforma e liberado ao prestador somente após aprovação do cliente | **Parcial** — cobrança e confirmação estão integradas; as Partes 9–10 criam e registram repasse manual, ainda aguardando migration v10 e teste operacional. | `ServicePaymentService.java`; `ServicePayoutService.java`; `v7_payment_orders.sql`; `v10_manual_pix_payouts.sql` |
| Cliente aprova a execução e isso inicia o pagamento do prestador | **Parcial** — execução/aprovação testadas; a pendência de Pix manual está implementada e aguarda validação operacional. | `ServiceExecutionService.java`; `ServicePayoutService.java`; `ServiceExecutionsScreen.tsx` |
| Lista de serviços com status e prazos, detalhe e ações para contestar/liberar pagamento | **Implementado** — cliente abre disputa antes da aprovação; ambas as partes anexam evidências; decisão administrativa libera o reembolso ou repasse. | `ServiceExecutionsScreen.tsx`; `ServiceDisputeService.java`; `AdminDisputesScreen.tsx` |
| Disputa, evidências e reembolso ao cliente quando aplicável | **Implementado; migration v11 e teste manual pendentes** — armazenamento privado, prazos, fila administrativa, decisões e notificações. | `ServiceDisputeService.java`; `v11_service_disputes.sql`; `AdminDisputesScreen.tsx` |
| Chamado de garantia, prazo para correção e encerramento como resolvido | **Implementado no código; migration v12 e validação manual pendentes** — duração definida no orçamento, chamado pelo cliente, prazo de correção e encerramento como resolvido. | `ServiceWarrantyService.java`; `v12_service_warranties.sql`; `ServiceExecutionsScreen.tsx`; `solicitacoes.tsx` |

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

**Status:** fluxo principal implementado; o usuário aplicou a v7 e confirmou pagamento de sandbox com atualização correta do estado na plataforma. O usuário relatou que o checkout não retornou automaticamente ao app; essa validação ainda está pendente. O endpoint administrativo `POST /payments/orders/sandbox-test` foi mantido como prova isolada da integração e segue desabilitado por padrão.

- Migration `v7_payment_orders.sql` persiste um pagamento por orçamento aprovado; tentativas repetidas reutilizam o registro com novas chaves idempotentes.
- O cliente inicia Checkout Pro apenas para seu próprio orçamento aprovado. O backend cria a Order, define URLs de retorno, abre a checkout URL no app e disponibiliza consulta/sincronização de status. O retorno/deep link não altera o status financeiro.
- Webhooks exigem validação HMAC de `x-signature` e `x-request-id`; o backend consulta a Order diretamente antes de atualizar estado. Um job reconcilia Orders pendentes a cada 15 minutos, evitando depender exclusivamente do webhook.
- O gateway inclui consulta, cancelamento de Orders não pagas e reembolso integral; o reembolso é administrativo nesta parte. Estados são registrados no histórico de ciclo de vida e notificações internas.
- O fluxo de checkout e a confirmação do pagamento foram testados pelo usuário com sucesso. O Access Token, segredo do webhook e a URL pública já foram configurados para o teste reportado.
- Pendências para o responsável: diagnosticar por que o deep link de retorno não reabriu o app no ambiente utilizado; validar retorno em build instalado e configurar URL HTTPS se houver frontend web; testar pagamento recusado, repetição de webhook e reembolso no sandbox.
- Antes da produção: validar com o Mercado Pago a conta/grant e as credenciais apropriadas, testar os callbacks no domínio com SSL e definir retenção do saldo/repasse manual por Pix. Não habilitar o endpoint sandbox como fluxo de cobrança real.

- Implementar Checkout Pro Orders: criar a order no backend e encaminhar o cliente à URL de checkout do Mercado Pago.
- Configurar URLs de retorno compatíveis com o app, tratar o deep link de retorno e não considerar esse retorno prova de pagamento confirmado.
- Obter e usar credenciais somente no backend; manter `client_secret` e `access_token` fora do app/frontend. Se no futuro for habilitado OAuth de vendedores, armazenar os tokens por vendedor de forma protegida e tratar a rotação de `refresh_token` de uso único.
- Persistir o pagamento e processar notificações/webhooks de forma idempotente; só marcar pago após confirmação confiável do Mercado Pago. Implementado; falta teste manual no sandbox.
- Tratar os estados da order documentados pelo Checkout Pro Orders; não iniciar serviço enquanto não houver confirmação de pagamento.
- Implementar consulta/cancelamento/reembolso de order conforme a API escolhida e manter o reembolso pelo meio original. Implementado no backend para consulta, cancelamento de Order pendente e reembolso integral administrativo; integração manual ainda pendente.
- Não armazenar número completo nem código de segurança do cartão.

**Teste/aceite:** testes automatizados verificam assinatura válida/inválida e transições; `mvn -o package` (65 testes antes da Parte 8) e `tsc --noEmit` passam. O usuário confirmou o pagamento de sandbox e a atualização do estado; redirecionamento/retorno automático, recusa, webhook repetido e eventos fora de ordem ainda precisam de validação manual. Retorno ao app sozinho nunca marca o pagamento como confirmado.

### Parte 8 — Cancelamento pré-execução e reembolso

**Depende de:** Parte 7.

- **Status:** implementado no backend e na tela do cliente; migration v8 aplicada e teste manual confirmado pelo usuário (reembolso e cancelamento concluídos sem erro).
- Migration `v8_service_cancellation.sql` armazena um pedido de cancelamento por pagamento, com motivo, estado, resultado e data do reembolso; v7 não é alterada por já estar aplicada.
- Cliente pode solicitar cancelamento após pagamento somente antes do início da execução. A execução é verificada pelo histórico `EXECUCAO`; Parte 9 deve usar o `quoteId` como `entityId` e bloquear início quando o pagamento estiver em `REEMBOLSO_PENDENTE` ou `REEMBOLSADO`.
- Reembolso integral é solicitado à Order pelo Mercado Pago, que devolve os fundos ao meio original da transação. Chave de idempotência estável por pagamento e registro único impedem duplicação em reenvios; falhas e estados ainda não confirmados ficam pendentes/tentáveis novamente.
- Após confirmação do reembolso, o estado `EXECUCAO` registra cancelamento, e cliente recebe o estado financeiro pelo fluxo de pagamento.
- Pendências para o responsável: junto com a Parte 9, testar o bloqueio do cancelamento após início da execução e serialização entre iniciar/cancelar. Testar também falha temporária/repetição e confirmar no painel/conta se o reembolso de Pix é permitido para a Order utilizada.

**Teste/aceite:** testes cobrem cancelamento antes/depois do início e reexecução da operação; não há reembolso duplicado e o meio de reembolso corresponde ao pagamento.

### Parte 9 — Início, conclusão e aprovação do serviço

**Depende de:** Partes 7 e 8.

- **Status:** implementada e testada/aceita pelo usuário; migration v9 aplicada.
- Migration `v9_service_execution.sql` cria a execução por orçamento e faz backfill do estado/histórico a partir de pagamentos e registros `EXECUCAO` existentes.
- A entidade/serviço de execução deve usar o ID do orçamento (`quoteId`) no histórico `EXECUCAO` para serializar com cancelamento e rejeitar início quando já houver cancelamento/reembolso pendente ou concluído.
- Prestador registra início/conclusão; a conclusão inicia a janela de 7 dias corridos para aprovação pelo cliente. A abertura de contestação continua na Parte 11.
- Aplicativo oferece lista/detalhe de serviços para cliente e prestador, com estado, prazo, histórico e ações.
- Aprovação do cliente é autorizada pelo vínculo com a solicitação, limitada ao prazo e idempotente; não executa repasse nesta parte.
- Início, cancelamento e alterações de pagamento serializam com a mesma ordem de locks (solicitação → pagamento), evitando que o cancelamento e o início passem simultaneamente.

**Teste/aceite:** testes automatizados e type-check passam; o usuário confirmou o fluxo manual da Parte 9.

### Parte 10 — Fila e execução de repasse manual via Pix

**Depende de:** Parte 9 e decisão operacional da Parte 0.

- **Status:** implementação pronta; migration v10, configuração da chave de criptografia e validação manual pelo usuário pendentes.
- Prestador cadastra/atualiza uma chave Pix (CPF, CNPJ, e-mail, telefone ou aleatória); a API restringe o acesso, mascara a exibição ao prestador e armazena o valor com AES-GCM.
- O segredo `APP_PIX_KEY_ENCRYPTION_SECRET` deve ser uma chave Base64 independente de 32 bytes, consistente entre instâncias e mantida com backup seguro. O detalhe administrativo de repasse pendente revela a chave para que o administrador execute o Pix.
- Uma única pendência por orçamento é criada após aprovação explícita ou pela tarefa de 15 minutos após 7 dias sem ação/disputa. O registro é criado mesmo sem chave Pix; o administrador não pode concluir até o prestador cadastrá-la.
- A fila administrativa registra valor transferido, destinatário/chave usada, identificador único de transação, referência opcional do comprovante, administrador e data. O valor informado é limitado ao valor pago, sem cálculo automático de taxa/comissão.
- O Pix é executado manualmente a partir da conta da plataforma; a API apenas registra o resultado. Reenvio de registro não duplica o estado; transição financeira usa `REPASSE_PENDENTE` → `REPASSADO`.
- A criação e conclusão do repasse consultam disputa aberta usando `quoteId` como `entityId` do histórico `DISPUTA`. A Parte 11 deve preservar essa convenção e executar os testes de suspensão/resolução do repasse.

**Teste/aceite:** testes automatizados e type-check passam; o usuário deve aplicar v10, configurar/guardar o segredo e validar chave mascarada, criação após aprovação/prazo, limite/registro do Pix, autorização administrativa, repetição e bloqueio por disputa. A UI não executa a transferência Pix.

### Parte 11 — Disputa e evidências

**Depende de:** Partes 9 e 10, e política de evidências da Parte 0.

- **Status:** implementação pronta; migration v11 e validação manual pelo usuário pendentes.
- Disputa única por orçamento, aberta pelo cliente após o início e antes da aprovação; a execução muda para `CONTESTADA` e o histórico `DISPUTA` conserva `quoteId` como `entityId`.
- Cliente e prestador podem anexar até 10 arquivos cada durante 7 dias corridos após abertura; são permitidos JPG/PNG/PDF até 10 MB por arquivo. O backend valida assinatura do arquivo, tipo e tamanho.
- Evidências ficam em BLOB privado no banco, só podem ser baixadas pelas partes/admin e são excluídas diariamente após 180 dias do encerramento. Ausência de evidência não produz decisão automática.
- Fila/detalhe administrativo registra justificativa e decisão. Reembolso usa a integração Mercado Pago e mantém a disputa aberta até a confirmação integral; uma pendência de repasse existente é cancelada após a confirmação. Decisão por repasse aprova a execução e cria/libera a pendência manual.
- A abertura e o encerramento notificam as duas partes no app; repasses usam o estado mais recente da disputa e permanecem bloqueados enquanto ela estiver aberta.

**Teste/aceite:** testes de política e transições passam; o usuário deve aplicar v11 e validar abertura autorizada, anexos privados por ambas as partes, limites, fila, decisões, notificações e bloqueio/liberação do repasse.

### Parte 12 — Garantia

**Depende de:** Partes 9 e 11, e decisões de garantia da Parte 0.

- **Implementado:** prestador informa duração obrigatória em dias corridos no orçamento; cliente vê o prazo antes de aprovar e a vigência começa na aprovação.
- **Implementado:** cliente pode abrir um chamado durante a vigência; prestador é notificado e tem 7 dias corridos para registrar a correção; cliente pode encerrar como resolvido.
- **Implementado:** sem correção no prazo, tarefa solicita reembolso integral ao Mercado Pago, inclusive se o repasse Pix já ocorreu; a plataforma assume eventual falta de saldo e retenta operações pendentes com a mesma chave de idempotência.
- **Migration:** aplicar manualmente `v12_service_warranties.sql` após a v11.

**Teste/aceite:** testes de política e transições verificam início/fim da vigência, prazo de correção e encerramento como resolvida; type-check/build e testes backend; aplicar v12 e validar manualmente os fluxos de cliente, prestador e reembolso.

### Parte 13 — Regressão e documentação de entrega

**Depende de:** Partes 1–12.

- Executar testes backend, type-check/build frontend e testes de integração com ambiente de teste do Mercado Pago.
- Rodar roteiros completos de cliente, prestador e administrador, incluindo cancelamento, reembolso, aprovação, disputa, repasse e garantia.
- Atualizar README/API com configuração, migrations, webhook e procedimento administrativo de Pix.

**Teste/aceite:** todos os testes passam; migrations aplicam em banco existente; o roteiro completo respeita as regras de [FEATURES.md](./FEATURES.md); operações financeiras ficam auditáveis e não são duplicadas.

## Plano de melhoria da experiência do cliente — somente frontend

**Escopo e método:** revisão heurística das telas e componentes do cliente comparados aos fluxos de [FEATURES.md](./FEATURES.md). Não foram feitas alterações de código nem testes com usuários ou validação visual em aparelhos; os itens abaixo são hipóteses baseadas na implementação e precisam ser validados durante a execução. O backend é imutável: reutilizar endpoints e dados existentes, sem criar campos, notificações ou regras que dependam de mudanças nele. Não simular informações que a API não fornece.

### Achados priorizados

| Prioridade | Problema observado e impacto | Correção proposta no frontend | Critério de aceite |
|---|---|---|---|
| **P0 — alta** | A home ainda se identifica como “Tela Inicial Cliente” e oferece apenas dois botões; não resume solicitações em andamento nem explica o que fazer em seguida. A navegação inferior tem cinco rótulos grandes e não usa ícones, podendo ficar apertada em telas estreitas. | Transformar a home em painel do cliente com saudação, CTA principal e resumo/atalhos para solicitações ativas, serviços e conversas, usando apenas dados já disponíveis. Ajustar a barra inferior para rótulos curtos e legíveis, ícones consistentes e indicação de aba selecionada. | Em teste, o cliente identifica sem ajuda como criar uma solicitação e como retomar um serviço existente; a navegação permanece legível em aparelhos estreitos. |
| **P0 — alta** | Criar solicitação exige preencher um formulário extenso, informar datas e horas em texto e escolher manualmente um fuso técnico. O primeiro serviço pode vir pré-selecionado, favorecendo envio acidental da categoria errada; não há revisão compacta das escolhas antes do envio. | Organizar o formulário em etapas curtas (serviço, endereço/área, disponibilidade e revisão), exigir escolha explícita do serviço, usar seletores de data/hora adequados ao aparelho e mostrar o fuso em linguagem clara, preservando a possibilidade de ajuste. Exibir resumo final editável antes de enviar e erros junto ao campo correspondente. | O usuário consegue revisar serviço, área, endereço, horários e fuso antes do envio; datas inválidas/passadas e campos obrigatórios são explicados antes da chamada à API; voltar entre etapas não apaga dados. |
| **P0 — alta** | A tela de solicitações concentra notificações, estados, visitas, orçamentos, pagamento e cancelamento em cartões longos. Isso torna difícil distinguir o estado atual, a próxima ação e os pedidos que já terminaram. | Mostrar primeiro um resumo curto por solicitação (serviço, endereço, estado em linguagem simples, próxima ação e prazo relevante); mover detalhes e ações para uma área expansível ou tela de detalhe. Separar ativas e encerradas e manter filtros simples. Não duplicar informação de estado em blocos diferentes. | Para cada solicitação, é possível identificar estado e próxima ação sem ler todo o histórico; solicitações encerradas não competem visualmente com as que exigem ação. |
| **P0 — alta** | Aprovação de orçamento e início do checkout são passos financeiros importantes; o retorno do Mercado Pago pode ficar pendente enquanto o servidor confirma o pagamento. Sem contexto e confirmação claros, o cliente pode aprovar a opção errada ou interpretar o retorno do checkout como pagamento confirmado. | Antes de aprovar, apresentar prestador, descrição, valor total e duração da garantia, com confirmação explícita. Separar aprovação e pagamento; no checkout externo explicar que a confirmação depende do servidor. Na tela de retorno, mostrar estados pendente/aprovado/recusado com linguagem simples, última atualização e ação evidente para consultar novamente, sem afirmar sucesso antes da confirmação. | O cliente distingue orçamento aprovado, checkout iniciado e pagamento confirmado; consegue ver valor e garantia antes da decisão; um retorno pendente oferece orientação sem induzir pagamento duplicado. |
| **P1 — média** | Visitas e horários aparecem dentro do fluxo geral e podem ser interpretados incorretamente, sobretudo quando o fuso é apresentado como identificador técnico. Ações que confirmam, recusam ou cancelam mudam o andamento do serviço. | Exibir data/hora local com fuso legível e contexto do endereço; diferenciar visita aguardando confirmação, confirmada, recusada e cancelada. Pedir confirmação nas ações irreversíveis ou de maior impacto e explicar o resultado da ação. | O cliente reconhece o horário local correto, o estado da visita e a consequência de cada ação antes de executá-la. |
| **P1 — média** | Execução, aprovação, disputa, evidências e garantia são etapas com consequências financeiras e prazos, mas estados e ações apresentados em blocos extensos aumentam o risco de perder um prazo ou não entender quem deve agir. | Apresentar uma linha do tempo curta e focada na próxima etapa, com responsável, prazo/contagem regressiva quando disponível e consequências em texto simples. Separar ações de aprovar, contestar e acionar/encerrar garantia. No envio de evidências, explicar formatos e limites aceitos, mostrar progresso, permitir revisar arquivos selecionados e indicar claramente erros/sucesso. | O cliente entende quem deve agir e até quando; cada ação financeira ou de disputa tem explicação e confirmação; o resultado de anexar evidências é inequívoco. |
| **P1 — média** | A caixa de notificações ocupa espaço dentro da tela e permite marcar como lida, mas não leva diretamente ao serviço relacionado. A lista de conversas mostra a data de criação, não a atividade mais recente; a tela do chat tem título genérico, não indica carregamento inicial e não mantém explicitamente a leitura na mensagem mais recente. | Recolher notificações ou apresentá-las em uma área própria; tornar cada item navegável para a solicitação/serviço existente associado, quando os dados retornados permitirem. Melhorar cartões de conversa com contexto de serviço e atualizar a data exibida apenas se a API já fornecer esse dado. Dar título contextual ao chat, mostrar carregamento e manter a mensagem recente visível após envio/recebimento. Não inventar estado de “não lida” por conversa se a API não o expõe. | Uma notificação relacionada permite chegar ao item certo em poucos toques; o cliente entende de qual serviço é a conversa e sempre consegue localizar a mensagem mais recente. |
| **P1 — média** | Falhas, carregamentos e estados vazios não seguem um padrão único. Algumas telas expõem mensagens técnicas/status HTTP ou deixam a ação disponível durante a operação sem indicar claramente que está ocupada; listas nem sempre oferecem atualização/repetição explícita. | Padronizar feedback inline para carregamento, sucesso, vazio e erro; traduzir falhas para linguagem acionável mantendo detalhes técnicos fora da mensagem principal. Oferecer tentar novamente/atualizar onde adequado e estados visuais desabilitados para ações em andamento, sem permitir submissões duplicadas. | Falha de rede, lista vazia e carregamento não se confundem; toda falha recuperável oferece caminho de recuperação; o usuário recebe confirmação visível após salvar, enviar ou atualizar. |
| **P1 — média** | Login, cadastro e recuperação de senha têm feedback e validação inconsistentes; a escolha de perfil não explica claramente a diferença entre cliente e prestador. Parte dos campos compartilhados depende apenas de placeholder. | Harmonizar validação inline, estado ocupado e mensagens de sucesso/erro; normalizar e validar e-mail no frontend; explicar a escolha de perfil e manter o fluxo de recuperação com confirmação final clara. Associar rótulos persistentes aos campos, em vez de depender só do placeholder. | Usuário entende o que deve corrigir em cada campo e se a operação foi concluída; a escolha cliente/prestador não é ambígua. |
| **P2 — melhoria** | Componentes compartilhados variam em aparência e acessibilidade: o botão comum não tem propriedade visual/semântica para estado desabilitado, o dropdown usa o placeholder fixo “Selecione um serviço” mesmo para outros tipos de escolha e controles não aplicam rótulos/roles de acessibilidade uniformemente. | Consolidar estilos e estados dos controles; tornar placeholder contextual, rotular campos e erros para leitores de tela, definir roles/estados acessíveis, garantir foco visível, áreas de toque confortáveis e contraste suficiente. Revisar comportamento de teclado/rolagem em iOS e Android. | Fluxos principais podem ser usados com leitor de tela e fonte ampliada; campos são identificados sem depender do placeholder; controles ocupados/desabilitados são anunciados e visualmente distintos. |
| **P2 — melhoria** | Perfil e gerenciamento de endereços são tarefas de suporte importantes, mas ficam fora do fluxo principal; edição pode exigir navegar por telas separadas e mensagens de validação de dados pessoais dependem de formatos digitados manualmente. | Agrupar dados pessoais, endereços e segurança em seções claras; usar máscara/seletores adequados onde aplicável, explicar campos opcionais e preservar dados quando houver erro. Manter as informações usadas na solicitação fáceis de encontrar. | O cliente encontra e atualiza endereço/dados sem perder informações e entende quais dados são usados em novas solicitações. |

### Sequência sugerida de execução

1. **Fundação visual e estados (P1/P2):** padronizar botões, campos, erros, carregamento, sucesso, vazio, foco, acessibilidade e comportamento do teclado.
2. **Descoberta e criação (P0):** melhorar home/navegação e reduzir fricção no formulário de nova solicitação, incluindo seleção explícita e revisão antes do envio.
3. **Acompanhamento e pagamento (P0/P1):** reorganizar lista/detalhe, visitas, orçamentos, confirmações e retorno do checkout.
4. **Pós-serviço e comunicação (P1):** linha do tempo de execução/disputa/garantia, anexos, notificações e conversas, limitando-se aos dados expostos pelo backend.
5. **Conta e refinamento (P1/P2):** harmonizar autenticação, perfil, endereços e validação nos aparelhos e com tecnologias assistivas.

### Validação antes de considerar o plano concluído

- Fazer testes moderados com clientes representativos cobrindo: criar solicitação; confirmar visita; comparar/aprovar orçamento e pagar; acompanhar execução; contestar/anexar evidências; acionar/encerrar garantia; localizar e retomar conversa.
- Registrar uma linha de base de conclusão sem ajuda, erros, tempo e entendimento dos estados; priorizar eliminar falhas críticas nas tarefas antes de otimizar velocidade.
- Validar em pelo menos um aparelho Android e um iOS, em tela estreita e com ampliação de fonte/leitor de tela. Verificar contraste, foco, teclado, rolagem, mensagens e prevenção de ações duplicadas.
- Exercitar estados de rede lenta/indisponível e todos os estados de pagamento e serviço com os dados de teste disponíveis. Confirmar que nenhum texto de sucesso contradiz o estado retornado pela API.
- Antes da implementação, reconciliar a matriz e o resumo executivo deste plano com as migrations e validações operacionais concluídas, pois algumas linhas ainda podem refletir o estado anterior das Partes 10–12.

**Critério global de conclusão:** cada requisito de [FEATURES.md](./FEATURES.md) está implementado de ponta a ponta, com autorização, persistência, interface, prazos, testes e procedimento operacional.

## Arquivos atuais de referência

- Solicitações: `acaba-aqui-backend/src/main/java/com/acabaaqui/services/ServiceRequestService.java`, `ServiceRequestResource.java`, `ServiceRequestStatus.java`, `acaba-aqui-frontend/app/cliente/novaSolicitacao.tsx` e `app/(cliente)/solicitacoes.tsx`.
- Oportunidades e visitas: `RequestOpportunityService.java`, `RequestOpportunityResource.java`, `acaba-aqui-frontend/components/ProviderOpportunitiesScreen.tsx`.
- Agendamento: `VisitSlotService.java`, `VisitSlotResource.java`, `VisitSlot.java`, `v5_visit_scheduling.sql`.
- Conversas: `ConversationService.java`, `ConversationResource.java`, `acaba-aqui-frontend/components/ConversationsScreen.tsx` e `app/chat/[conversationId].tsx`.
- Persistência atual: migrations `v1_features_epic1.sql` a `v7_payment_orders.sql`.
- Testes próximos ao fluxo: `ServiceRequestServiceTest.java`, `RequestOpportunityServiceTest.java` e `VisitSlotServiceTest.java`.
