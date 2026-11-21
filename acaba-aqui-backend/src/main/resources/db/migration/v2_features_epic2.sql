
CREATE TABLE `solicitacao_oportunidades` (
  `oportunidade_id` bigint NOT NULL AUTO_INCREMENT,
  `solicitacao_id` bigint NOT NULL,
  `prestador_id` int NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDENTE',
  `lida` tinyint(1) NOT NULL DEFAULT '0',
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oportunidade_id`),
  UNIQUE KEY `uq_oportunidade_solicitacao_prestador` (`solicitacao_id`,`prestador_id`),
  KEY `idx_oportunidade_prestador_status` (`prestador_id`,`status`,`data_criacao`),
  CONSTRAINT `fk_oportunidade_solicitacao` FOREIGN KEY (`solicitacao_id`) REFERENCES `solicitacoes_servico` (`solicitacao_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_oportunidade_prestador` FOREIGN KEY (`prestador_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_oportunidade_status` CHECK (`status` IN ('PENDENTE','ACEITA','RECUSADA','ENCERRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `conversas` (
  `conversa_id` bigint NOT NULL AUTO_INCREMENT,
  `solicitacao_id` bigint NOT NULL,
  `cliente_id` int NOT NULL,
  `prestador_id` int NOT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`conversa_id`),
  UNIQUE KEY `uq_conversa_solicitacao_prestador` (`solicitacao_id`,`prestador_id`),
  KEY `idx_conversa_cliente` (`cliente_id`,`data_criacao`),
  KEY `idx_conversa_prestador` (`prestador_id`,`data_criacao`),
  CONSTRAINT `fk_conversa_solicitacao` FOREIGN KEY (`solicitacao_id`) REFERENCES `solicitacoes_servico` (`solicitacao_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversa_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversa_prestador` FOREIGN KEY (`prestador_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `mensagens_conversa` (
  `mensagem_id` bigint NOT NULL AUTO_INCREMENT,
  `conversa_id` bigint NOT NULL,
  `remetente_id` int NOT NULL,
  `conteudo` varchar(4000) NOT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`mensagem_id`),
  KEY `idx_mensagem_conversa_data` (`conversa_id`,`data_criacao`,`mensagem_id`),
  CONSTRAINT `fk_mensagem_conversa` FOREIGN KEY (`conversa_id`) REFERENCES `conversas` (`conversa_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mensagem_remetente` FOREIGN KEY (`remetente_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `prestador_servicos` (
  `prestador_servico_id` bigint NOT NULL AUTO_INCREMENT,
  `prestador_id` int NOT NULL,
  `servico_id` int NOT NULL,
  PRIMARY KEY (`prestador_servico_id`),
  UNIQUE KEY `uq_prestador_servico` (`prestador_id`,`servico_id`),
  KEY `idx_prestador_servicos_servico` (`servico_id`),
  CONSTRAINT `fk_prestador_servicos_prestador` FOREIGN KEY (`prestador_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_prestador_servicos_servico` FOREIGN KEY (`servico_id`) REFERENCES `servicos` (`servico_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `prestador_regioes` (
  `regiao_id` bigint NOT NULL AUTO_INCREMENT,
  `prestador_id` int NOT NULL,
  `cidade` varchar(100) NOT NULL,
  `uf` char(2) NOT NULL,
  PRIMARY KEY (`regiao_id`),
  UNIQUE KEY `uq_prestador_regiao` (`prestador_id`,`cidade`,`uf`),
  KEY `idx_prestador_regioes_cidade_uf` (`cidade`,`uf`),
  CONSTRAINT `fk_prestador_regioes_prestador` FOREIGN KEY (`prestador_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `historico_status` (
  `historico_id` bigint NOT NULL AUTO_INCREMENT,
  `tipo_entidade` varchar(30) NOT NULL,
  `entidade_id` bigint NOT NULL,
  `status_anterior` varchar(40) DEFAULT NULL,
  `status_novo` varchar(40) NOT NULL,
  `tipo_ator` varchar(20) NOT NULL,
  `ator_id` int DEFAULT NULL,
  `data_evento` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`historico_id`),
  KEY `idx_historico_status_entidade` (`tipo_entidade`,`entidade_id`,`data_evento`,`historico_id`),
  KEY `idx_historico_status_ator` (`ator_id`,`data_evento`),
  CONSTRAINT `fk_historico_status_ator` FOREIGN KEY (`ator_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_historico_status_entidade` CHECK (`tipo_entidade` IN (
    'SOLICITACAO','OPORTUNIDADE','VISITA','ORCAMENTO','PAGAMENTO','EXECUCAO','DISPUTA','GARANTIA'
  )),
  CONSTRAINT `chk_historico_status_ator` CHECK (`tipo_ator` IN (
    'CLIENTE','PRESTADOR','ADMINISTRADOR','SISTEMA','MERCADO_PAGO','MIGRACAO'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `historico_status`
  (`tipo_entidade`,`entidade_id`,`status_anterior`,`status_novo`,`tipo_ator`,`ator_id`,`data_evento`)
SELECT 'SOLICITACAO', `solicitacao_id`, NULL, `status`, 'MIGRACAO', NULL, CURRENT_TIMESTAMP
FROM `solicitacoes_servico`;

INSERT INTO `historico_status`
  (`tipo_entidade`,`entidade_id`,`status_anterior`,`status_novo`,`tipo_ator`,`ator_id`,`data_evento`)
SELECT 'OPORTUNIDADE', `oportunidade_id`, NULL, `status`, 'MIGRACAO', NULL, CURRENT_TIMESTAMP
FROM `solicitacao_oportunidades`;

CREATE TABLE `solicitacao_visita_horarios` (
  `horario_id` bigint NOT NULL AUTO_INCREMENT,
  `solicitacao_id` bigint NOT NULL,
  `inicio_local` datetime NOT NULL,
  `time_zone_id` varchar(64) NOT NULL,
  `status` varchar(20) NOT NULL,
  `prestador_id` int DEFAULT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`horario_id`),
  UNIQUE KEY `uq_visita_horario_solicitacao_inicio` (`solicitacao_id`,`inicio_local`),
  UNIQUE KEY `uq_visita_horario_prestador_ativo` (`solicitacao_id`,`prestador_id`),
  KEY `idx_visita_horario_solicitacao_status_inicio` (`solicitacao_id`,`status`,`inicio_local`),
  KEY `idx_visita_horario_prestador_status` (`prestador_id`,`status`),
  CONSTRAINT `fk_visita_horario_solicitacao` FOREIGN KEY (`solicitacao_id`)
    REFERENCES `solicitacoes_servico` (`solicitacao_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_visita_horario_prestador` FOREIGN KEY (`prestador_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_visita_horario_status` CHECK (`status` IN (
    'DISPONIVEL','RESERVADA','CONFIRMADA','CANCELADA','EXPIRADA'
  )),
  CONSTRAINT `chk_visita_horario_prestador` CHECK (
    (`status` IN ('RESERVADA','CONFIRMADA') AND `prestador_id` IS NOT NULL)
    OR (`status` IN ('DISPONIVEL','CANCELADA','EXPIRADA') AND `prestador_id` IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;






ALTER TABLE `solicitacao_visita_horarios`
  ADD COLUMN `prestador_confirmado_id` int DEFAULT NULL AFTER `prestador_id`,
  ADD KEY `idx_visita_prestador_confirmado` (`prestador_confirmado_id`),
  ADD CONSTRAINT `fk_visita_prestador_confirmado` FOREIGN KEY (`prestador_confirmado_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT;

CREATE TABLE `orcamentos` (
  `orcamento_id` bigint NOT NULL AUTO_INCREMENT,
  `solicitacao_id` bigint NOT NULL,
  `prestador_id` int NOT NULL,
  `descricao` varchar(2000) NOT NULL,
  `valor_total` decimal(12,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENVIADO',
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expira_em` datetime NOT NULL,
  PRIMARY KEY (`orcamento_id`),
  UNIQUE KEY `uq_orcamento_solicitacao_prestador` (`solicitacao_id`,`prestador_id`),
  KEY `idx_orcamento_solicitacao_status_expira` (`solicitacao_id`,`status`,`expira_em`),
  KEY `idx_orcamento_prestador_data` (`prestador_id`,`data_criacao`),
  CONSTRAINT `fk_orcamento_solicitacao` FOREIGN KEY (`solicitacao_id`)
    REFERENCES `solicitacoes_servico` (`solicitacao_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orcamento_prestador` FOREIGN KEY (`prestador_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_orcamento_valor_positivo` CHECK (`valor_total` > 0),
  CONSTRAINT `chk_orcamento_status` CHECK (
    `status` IN ('ENVIADO','APROVADO','RECUSADO','EXPIRADO','CANCELADO')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notificacoes_internas` (
  `notificacao_id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `solicitacao_id` bigint DEFAULT NULL,
  `orcamento_id` bigint DEFAULT NULL,
  `tipo` varchar(50) NOT NULL,
  `mensagem` varchar(255) NOT NULL,
  `lida` tinyint(1) NOT NULL DEFAULT '0',
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notificacao_id`),
  KEY `idx_notificacao_usuario_lida_data` (`usuario_id`,`lida`,`data_criacao`),
  CONSTRAINT `fk_notificacao_usuario` FOREIGN KEY (`usuario_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notificacao_solicitacao` FOREIGN KEY (`solicitacao_id`)
    REFERENCES `solicitacoes_servico` (`solicitacao_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notificacao_orcamento` FOREIGN KEY (`orcamento_id`)
    REFERENCES `orcamentos` (`orcamento_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `pagamentos` (
  `pagamento_id` bigint NOT NULL AUTO_INCREMENT,
  `orcamento_id` bigint NOT NULL,
  `mercado_pago_order_id` varchar(64) DEFAULT NULL,
  `idempotency_key` char(36) NOT NULL,
  `external_reference` varchar(64) NOT NULL,
  `tentativa` int NOT NULL DEFAULT '1',
  `checkout_url` varchar(2048) DEFAULT NULL,
  `valor_total` decimal(12,2) NOT NULL,
  `valor_pago` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` varchar(30) NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
  `status_order` varchar(40) DEFAULT NULL,
  `status_detail` varchar(80) DEFAULT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`pagamento_id`),
  UNIQUE KEY `uq_pagamento_orcamento` (`orcamento_id`),
  UNIQUE KEY `uq_pagamento_order` (`mercado_pago_order_id`),
  UNIQUE KEY `uq_pagamento_idempotency` (`idempotency_key`),
  UNIQUE KEY `uq_pagamento_external_reference` (`external_reference`),
  KEY `idx_pagamento_status_atualizacao` (`status`,`ultima_atualizacao`),
  CONSTRAINT `fk_pagamento_orcamento` FOREIGN KEY (`orcamento_id`)
    REFERENCES `orcamentos` (`orcamento_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_pagamento_valores` CHECK (`valor_total` > 0 AND `valor_pago` >= 0 AND `tentativa` > 0),
  CONSTRAINT `chk_pagamento_status` CHECK (`status` IN (
    'AGUARDANDO_PAGAMENTO','PAGO','RECUSADO','EXPIRADO','CANCELADO',
    'REEMBOLSO_PENDENTE','REEMBOLSADO','REEMBOLSADO_PARCIAL','CONTESTADO'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `pagamento_tentativas` (
  `tentativa_id` bigint NOT NULL AUTO_INCREMENT,
  `pagamento_id` bigint NOT NULL,
  `numero_tentativa` int NOT NULL,
  `mercado_pago_order_id` varchar(64) NOT NULL,
  `idempotency_key` char(36) NOT NULL,
  `external_reference` varchar(64) NOT NULL,
  `checkout_url` varchar(2048) NOT NULL,
  `status_order` varchar(40) DEFAULT NULL,
  `status_detail` varchar(80) DEFAULT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tentativa_id`),
  UNIQUE KEY `uq_pagamento_tentativa_numero` (`pagamento_id`,`numero_tentativa`),
  UNIQUE KEY `uq_pagamento_tentativa_order` (`mercado_pago_order_id`),
  UNIQUE KEY `uq_pagamento_tentativa_idempotency` (`idempotency_key`),
  UNIQUE KEY `uq_pagamento_tentativa_reference` (`external_reference`),
  CONSTRAINT `fk_pagamento_tentativa_pagamento` FOREIGN KEY (`pagamento_id`)
    REFERENCES `pagamentos` (`pagamento_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_pagamento_tentativa_numero` CHECK (`numero_tentativa` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `repasses` DROP CHECK `chk_repasse_status`;
ALTER TABLE `repasses`
  ADD CONSTRAINT `chk_repasse_status` CHECK (`status` IN ('PENDENTE','PAGO','CANCELADO'));

CREATE TABLE `disputas_servico` (
  `disputa_id` bigint NOT NULL AUTO_INCREMENT,
  `orcamento_id` bigint NOT NULL,
  `cliente_id` int NOT NULL,
  `motivo` varchar(1000) NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'ABERTA',
  `aberta_em` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `evidencias_ate` datetime NOT NULL,
  `decisao` varchar(30) DEFAULT NULL,
  `justificativa_decisao` varchar(1000) DEFAULT NULL,
  `administrador_id` int DEFAULT NULL,
  `encerrada_em` datetime DEFAULT NULL,
  PRIMARY KEY (`disputa_id`),
  UNIQUE KEY `uq_disputa_orcamento` (`orcamento_id`),
  KEY `idx_disputa_status_aberta` (`status`,`aberta_em`),
  KEY `idx_disputa_encerrada_em` (`encerrada_em`),
  CONSTRAINT `fk_disputa_orcamento` FOREIGN KEY (`orcamento_id`)
    REFERENCES `orcamentos` (`orcamento_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_disputa_cliente` FOREIGN KEY (`cliente_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_disputa_administrador` FOREIGN KEY (`administrador_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_disputa_status` CHECK (`status` IN (
    'ABERTA','ENCERRADA_REEMBOLSO','ENCERRADA_REPASSE'
  )),
  CONSTRAINT `chk_disputa_decisao` CHECK (
    (`status` = 'ABERTA' AND `decisao` IS NULL AND `administrador_id` IS NULL AND `encerrada_em` IS NULL)
    OR (`status` = 'ENCERRADA_REEMBOLSO' AND `decisao` = 'REEMBOLSAR_CLIENTE'
      AND `administrador_id` IS NOT NULL AND `encerrada_em` IS NOT NULL)
    OR (`status` = 'ENCERRADA_REPASSE' AND `decisao` = 'PAGAR_PRESTADOR'
      AND `administrador_id` IS NOT NULL AND `encerrada_em` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `evidencias_disputa` (
  `evidencia_id` bigint NOT NULL AUTO_INCREMENT,
  `disputa_id` bigint NOT NULL,
  `enviado_por` int NOT NULL,
  `nome_arquivo` varchar(255) NOT NULL,
  `tipo_conteudo` varchar(100) NOT NULL,
  `tamanho_bytes` bigint NOT NULL,
  `conteudo` mediumblob NOT NULL,
  `criada_em` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`evidencia_id`),
  KEY `idx_evidencia_disputa_autor` (`disputa_id`,`enviado_por`,`criada_em`),
  CONSTRAINT `fk_evidencia_disputa` FOREIGN KEY (`disputa_id`)
    REFERENCES `disputas_servico` (`disputa_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_evidencia_autor` FOREIGN KEY (`enviado_por`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_evidencia_tamanho` CHECK (`tamanho_bytes` > 0 AND `tamanho_bytes` <= 10485760)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `orcamentos`
  ADD COLUMN `garantia_dias` int NOT NULL DEFAULT 0,
  ADD CONSTRAINT `chk_orcamento_garantia_dias` CHECK (`garantia_dias` >= 0);

CREATE TABLE `garantias_servico` (
  `garantia_id` bigint NOT NULL AUTO_INCREMENT,
  `orcamento_id` bigint NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'ATIVA',
  `iniciada_em` datetime NOT NULL,
  `termina_em` datetime NOT NULL,
  `motivo_chamado` varchar(1000) DEFAULT NULL,
  `acionada_em` datetime DEFAULT NULL,
  `correcao_ate` datetime DEFAULT NULL,
  `corrigida_em` datetime DEFAULT NULL,
  `resolvida_em` datetime DEFAULT NULL,
  `reembolsada_em` datetime DEFAULT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`garantia_id`),
  KEY `idx_garantia_orcamento` (`orcamento_id`,`garantia_id`),
  KEY `idx_garantia_status_correcao` (`status`,`correcao_ate`),
  CONSTRAINT `fk_garantia_orcamento` FOREIGN KEY (`orcamento_id`)
    REFERENCES `orcamentos` (`orcamento_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_garantia_status` CHECK (`status` IN (
    'ATIVA','ACIONADA','CORRIGIDA','RESOLVIDA',
    'REEMBOLSO_PENDENTE','REEMBOLSADA','FALHA_REEMBOLSO'
  )),
  CONSTRAINT `chk_garantia_periodo` CHECK (`termina_em` >= `iniciada_em`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
