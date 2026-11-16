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
