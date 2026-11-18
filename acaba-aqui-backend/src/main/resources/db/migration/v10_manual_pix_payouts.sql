ALTER TABLE `pagamentos` DROP CHECK `chk_pagamento_status`;
ALTER TABLE `pagamentos`
  ADD CONSTRAINT `chk_pagamento_status` CHECK (`status` IN (
    'AGUARDANDO_PAGAMENTO','PAGO','RECUSADO','EXPIRADO','CANCELADO',
    'REEMBOLSO_PENDENTE','REEMBOLSADO','REEMBOLSADO_PARCIAL','CONTESTADO',
    'REPASSE_PENDENTE','REPASSADO'
  ));

CREATE TABLE `chaves_pix_prestador` (
  `prestador_id` int NOT NULL,
  `tipo_chave` varchar(20) NOT NULL,
  `chave_criptografada` varchar(512) NOT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`prestador_id`),
  CONSTRAINT `fk_chave_pix_prestador` FOREIGN KEY (`prestador_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_chave_pix_tipo` CHECK (`tipo_chave` IN (
    'CPF','CNPJ','EMAIL','TELEFONE','ALEATORIA'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `repasses` (
  `repasse_id` bigint NOT NULL AUTO_INCREMENT,
  `orcamento_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDENTE',
  `motivo_elegibilidade` varchar(30) NOT NULL,
  `elegivel_em` datetime NOT NULL,
  `valor_pago` decimal(12,2) DEFAULT NULL,
  `destinatario_nome` varchar(100) DEFAULT NULL,
  `destinatario_tipo_chave` varchar(20) DEFAULT NULL,
  `destinatario_chave_criptografada` varchar(512) DEFAULT NULL,
  `referencia_transferencia` varchar(500) DEFAULT NULL,
  `referencia_comprovante` varchar(1000) DEFAULT NULL,
  `pago_em` datetime DEFAULT NULL,
  `administrador_id` int DEFAULT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`repasse_id`),
  UNIQUE KEY `uq_repasse_orcamento` (`orcamento_id`),
  UNIQUE KEY `uq_repasse_referencia_transferencia` (`referencia_transferencia`),
  KEY `idx_repasse_status_elegivel` (`status`,`elegivel_em`),
  CONSTRAINT `fk_repasse_execucao` FOREIGN KEY (`orcamento_id`)
    REFERENCES `execucoes_servico` (`orcamento_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_repasse_administrador` FOREIGN KEY (`administrador_id`)
    REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_repasse_status` CHECK (`status` IN ('PENDENTE','PAGO')),
  CONSTRAINT `chk_repasse_motivo` CHECK (`motivo_elegibilidade` IN (
    'APROVACAO_CLIENTE','PRAZO_EXPIRADO'
  )),
  CONSTRAINT `chk_repasse_valor` CHECK (`valor_pago` IS NULL OR `valor_pago` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
