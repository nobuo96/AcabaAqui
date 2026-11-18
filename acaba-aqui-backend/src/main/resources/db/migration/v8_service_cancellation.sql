CREATE TABLE `cancelamentos_servico` (
  `cancelamento_id` bigint NOT NULL AUTO_INCREMENT,
  `pagamento_id` bigint NOT NULL,
  `motivo` varchar(1000) NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'REEMBOLSO_PENDENTE',
  `resultado` varchar(500) DEFAULT NULL,
  `data_solicitacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `data_reembolso` datetime DEFAULT NULL,
  PRIMARY KEY (`cancelamento_id`),
  UNIQUE KEY `uq_cancelamento_pagamento` (`pagamento_id`),
  CONSTRAINT `fk_cancelamento_pagamento` FOREIGN KEY (`pagamento_id`)
    REFERENCES `pagamentos` (`pagamento_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_cancelamento_status` CHECK (`status` IN (
    'REEMBOLSO_PENDENTE','FALHA_REEMBOLSO','REEMBOLSADO'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
