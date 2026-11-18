CREATE TABLE `execucoes_servico` (
  `orcamento_id` bigint NOT NULL,
  `status` varchar(40) NOT NULL DEFAULT 'AGUARDANDO_INICIO',
  `iniciado_em` datetime DEFAULT NULL,
  `concluido_em` datetime DEFAULT NULL,
  `aprovar_ate` datetime DEFAULT NULL,
  `aprovado_em` datetime DEFAULT NULL,
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`orcamento_id`),
  KEY `idx_execucao_status_aprovar` (`status`,`aprovar_ate`),
  CONSTRAINT `fk_execucao_orcamento` FOREIGN KEY (`orcamento_id`)
    REFERENCES `orcamentos` (`orcamento_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_execucao_status` CHECK (`status` IN (
    'AGUARDANDO_INICIO','EM_ANDAMENTO','CONCLUIDA_PELO_PRESTADOR',
    'APROVADA','CONTESTADA','CANCELADA'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `execucoes_servico`
  (`orcamento_id`,`status`,`iniciado_em`,`concluido_em`,`aprovar_ate`,`aprovado_em`)
SELECT
  p.`orcamento_id`,
  COALESCE((
    SELECT h.`status_novo`
    FROM `historico_status` h
    WHERE h.`tipo_entidade` = 'EXECUCAO' AND h.`entidade_id` = p.`orcamento_id`
    ORDER BY h.`historico_id` DESC
    LIMIT 1
  ), 'AGUARDANDO_INICIO'),
  (SELECT MIN(h.`data_evento`) FROM `historico_status` h
    WHERE h.`tipo_entidade` = 'EXECUCAO' AND h.`entidade_id` = p.`orcamento_id`
      AND h.`status_novo` = 'EM_ANDAMENTO'),
  (SELECT MIN(h.`data_evento`) FROM `historico_status` h
    WHERE h.`tipo_entidade` = 'EXECUCAO' AND h.`entidade_id` = p.`orcamento_id`
      AND h.`status_novo` = 'CONCLUIDA_PELO_PRESTADOR'),
  (SELECT DATE_ADD(MIN(h.`data_evento`), INTERVAL 7 DAY) FROM `historico_status` h
    WHERE h.`tipo_entidade` = 'EXECUCAO' AND h.`entidade_id` = p.`orcamento_id`
      AND h.`status_novo` = 'CONCLUIDA_PELO_PRESTADOR'),
  (SELECT MIN(h.`data_evento`) FROM `historico_status` h
    WHERE h.`tipo_entidade` = 'EXECUCAO' AND h.`entidade_id` = p.`orcamento_id`
      AND h.`status_novo` = 'APROVADA')
FROM `pagamentos` p
WHERE p.`status` IN ('PAGO','REEMBOLSO_PENDENTE')
   OR EXISTS (
     SELECT 1 FROM `historico_status` h
     WHERE h.`tipo_entidade` = 'EXECUCAO' AND h.`entidade_id` = p.`orcamento_id`
   );

INSERT INTO `historico_status`
  (`tipo_entidade`,`entidade_id`,`status_anterior`,`status_novo`,`tipo_ator`,`ator_id`,`data_evento`)
SELECT
  'EXECUCAO', e.`orcamento_id`, NULL, 'AGUARDANDO_INICIO', 'SISTEMA', NULL, CURRENT_TIMESTAMP
FROM `execucoes_servico` e
WHERE e.`status` = 'AGUARDANDO_INICIO'
  AND NOT EXISTS (
    SELECT 1 FROM `historico_status` h
    WHERE h.`tipo_entidade` = 'EXECUCAO' AND h.`entidade_id` = e.`orcamento_id`
  );
