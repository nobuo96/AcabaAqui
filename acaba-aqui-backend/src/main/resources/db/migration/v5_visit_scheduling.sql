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
