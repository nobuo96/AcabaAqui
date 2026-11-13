CREATE TABLE `prestador_areas_servico` (
  `area_id` bigint NOT NULL AUTO_INCREMENT,
  `prestador_id` int NOT NULL,
  `servico_id` int NOT NULL,
  `cidade` varchar(100) NOT NULL,
  `uf` char(2) NOT NULL,
  PRIMARY KEY (`area_id`),
  UNIQUE KEY `uq_prestador_servico_regiao` (`prestador_id`,`servico_id`,`cidade`,`uf`),
  KEY `idx_area_servico_regiao` (`servico_id`,`cidade`,`uf`),
  CONSTRAINT `fk_area_prestador` FOREIGN KEY (`prestador_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_area_servico` FOREIGN KEY (`servico_id`) REFERENCES `servicos` (`servico_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
