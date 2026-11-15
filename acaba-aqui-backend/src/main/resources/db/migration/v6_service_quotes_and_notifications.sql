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
