CREATE TABLE `prestador_servicos` (
  `prestador_servico_id` bigint NOT NULL AUTO_INCREMENT,
  `prestador_id` int NOT NULL,
  `servico_id` int NOT NULL,
  PRIMARY KEY (`prestador_servico_id`),
  UNIQUE KEY `uq_prestador_servico` (`prestador_id`,`servico_id`),
  KEY `idx_prestador_servicos_servico` (`servico_id`),
  CONSTRAINT `fk_prestador_servicos_prestador` FOREIGN KEY (`prestador_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_prestador_servicos_servico` FOREIGN KEY (`servico_id`) REFERENCES `servicos` (`servico_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `prestador_regioes` (
  `regiao_id` bigint NOT NULL AUTO_INCREMENT,
  `prestador_id` int NOT NULL,
  `cidade` varchar(100) NOT NULL,
  `uf` char(2) NOT NULL,
  PRIMARY KEY (`regiao_id`),
  UNIQUE KEY `uq_prestador_regiao` (`prestador_id`,`cidade`,`uf`),
  KEY `idx_prestador_regioes_cidade_uf` (`cidade`,`uf`),
  CONSTRAINT `fk_prestador_regioes_prestador` FOREIGN KEY (`prestador_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `prestador_servicos` (`prestador_id`, `servico_id`)
SELECT DISTINCT `prestador_id`, `servico_id`
FROM `prestador_areas_servico`;

INSERT INTO `prestador_regioes` (`prestador_id`, `cidade`, `uf`)
SELECT DISTINCT `prestador_id`, `cidade`, `uf`
FROM `prestador_areas_servico`;

DROP TABLE `prestador_areas_servico`;
