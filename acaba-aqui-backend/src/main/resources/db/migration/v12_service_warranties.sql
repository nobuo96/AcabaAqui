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
