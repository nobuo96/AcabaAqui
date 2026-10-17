CREATE TABLE `usuarios` (
  `usuario_id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `perfil` enum('cliente','prestador','administrador') NOT NULL,
  `telefone` varchar(20) DEFAULT NULL,
  `data_criacao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ativo` tinyint(1) DEFAULT '1',
  `email_verificado` tinyint(1) NOT NULL DEFAULT '0',
  `data_nascimento` date DEFAULT NULL,
  PRIMARY KEY (`usuario_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cadastro_pendente_email` (
  `cadastro_id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `telefone` varchar(20) NOT NULL,
  `perfil` varchar(20) NOT NULL,
  `senha_hash` varchar(255) NOT NULL,
  `salt` varchar(64) NOT NULL,
  `algoritmo` varchar(10) NOT NULL,
  `codigo_hmac` varchar(64) NOT NULL,
  `criado_em` datetime NOT NULL,
  `expira_em` datetime NOT NULL,
  `ultimo_envio_em` datetime NOT NULL,
  `inicio_janela_envio_em` datetime NOT NULL,
  `tentativas` int NOT NULL DEFAULT '0',
  `envios_na_janela` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`cadastro_id`),
  UNIQUE KEY `uq_cadastro_pendente_email` (`email`),
  KEY `idx_cadastro_pendente_expira` (`expira_em`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `credenciais_usuario` (
  `credencial_id` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `senha_hash` varchar(255) NOT NULL,
  `salt` varchar(64) NOT NULL,
  `algoritmo` enum('bcrypt','argon2id','pbkdf2') DEFAULT 'argon2id',
  `data_criacao` datetime DEFAULT CURRENT_TIMESTAMP,
  `ultima_atualizacao` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ativo` tinyint(1) DEFAULT '1',
  `versao_token` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`credencial_id`),
  UNIQUE KEY `usuario_id` (`usuario_id`),
  CONSTRAINT `credenciais_usuario_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`usuario_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `login_social` (
  `login_id` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `provedor` enum('google','facebook') NOT NULL,
  `social_id` varchar(255) NOT NULL,
  `token` varchar(500) DEFAULT NULL,
  `data_login` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`login_id`),
  UNIQUE KEY `provedor` (`provedor`,`social_id`),
  KEY `usuario_id` (`usuario_id`),
  CONSTRAINT `login_social_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `identidades_social` (
  `identidade_id` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `provedor` varchar(20) NOT NULL,
  `subject` varchar(255) NOT NULL,
  PRIMARY KEY (`identidade_id`),
  UNIQUE KEY `uq_identidade_social_provedor_subject` (`provedor`,`subject`),
  KEY `idx_identidade_social_usuario` (`usuario_id`),
  CONSTRAINT `fk_identidade_social_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `redefinicao_senha` (
  `redefinicao_id` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `codigo_hmac` varchar(64) NOT NULL,
  `criado_em` datetime NOT NULL,
  `expira_em` datetime NOT NULL,
  `ultimo_envio_em` datetime NOT NULL,
  `inicio_janela_envio_em` datetime NOT NULL,
  `tentativas` int NOT NULL DEFAULT '0',
  `envios_na_janela` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`redefinicao_id`),
  UNIQUE KEY `uq_redefinicao_usuario` (`usuario_id`),
  KEY `idx_redefinicao_expira` (`expira_em`),
  CONSTRAINT `fk_redefinicao_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `enderecos` (
  `endereco_id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `cep` char(8) NOT NULL,
  `logradouro` varchar(150) DEFAULT NULL,
  `numero` varchar(20) NOT NULL,
  `complemento` varchar(100) DEFAULT NULL,
  `bairro` varchar(100) DEFAULT NULL,
  `cidade` varchar(100) NOT NULL,
  `uf` char(2) NOT NULL,
  `apelido` varchar(50) DEFAULT NULL,
  `principal` tinyint(1) NOT NULL DEFAULT '0',
  `ativo` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`endereco_id`),
  KEY `idx_enderecos_usuario_ativo` (`usuario_id`,`ativo`),
  CONSTRAINT `fk_enderecos_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `categorias_servico` (
  `categoria_id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(80) NOT NULL,
  `descricao` varchar(255) DEFAULT NULL,
  `ativo` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`categoria_id`),
  UNIQUE KEY `uq_categorias_servico_nome` (`nome`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `servicos` (
  `servico_id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `descricao` varchar(1000) NOT NULL,
  `ativo` tinyint(1) NOT NULL DEFAULT '1',
  `categoria_id` int NOT NULL,
  PRIMARY KEY (`servico_id`),
  KEY `idx_servicos_categoria` (`categoria_id`),
  CONSTRAINT `fk_servicos_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categorias_servico` (`categoria_id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `solicitacoes_servico` (
  `solicitacao_id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `servico_id` int NOT NULL,
  `endereco_origem_id` bigint NOT NULL,
  `area_m2` decimal(10,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'CRIADA',
  `data_criacao` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `endereco_cep` char(8) NOT NULL,
  `endereco_logradouro` varchar(150) DEFAULT NULL,
  `endereco_numero` varchar(20) NOT NULL,
  `endereco_complemento` varchar(100) DEFAULT NULL,
  `endereco_bairro` varchar(100) DEFAULT NULL,
  `endereco_cidade` varchar(100) NOT NULL,
  `endereco_uf` char(2) NOT NULL,
  `endereco_apelido` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`solicitacao_id`),
  KEY `idx_solicitacoes_usuario_data` (`usuario_id`,`data_criacao`,`solicitacao_id`),
  KEY `fk_solicitacoes_servico` (`servico_id`),
  CONSTRAINT `fk_solicitacoes_servico` FOREIGN KEY (`servico_id`) REFERENCES `servicos` (`servico_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_solicitacoes_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`usuario_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_solicitacoes_area_positiva` CHECK ((`area_m2` > 0)),
  CONSTRAINT `chk_solicitacoes_status` CHECK ((`status` in (_utf8mb4'CRIADA',_utf8mb4'ENCERRADA')))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

