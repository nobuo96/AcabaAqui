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
