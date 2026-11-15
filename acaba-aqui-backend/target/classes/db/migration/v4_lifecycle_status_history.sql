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
