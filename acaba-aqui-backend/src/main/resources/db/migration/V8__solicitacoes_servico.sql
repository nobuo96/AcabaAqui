CREATE TABLE solicitacoes_servico (
    solicitacao_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    servico_id INT NOT NULL,
    endereco_origem_id BIGINT NOT NULL,
    area_m2 DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CRIADA',
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endereco_cep CHAR(8) NOT NULL,
    endereco_logradouro VARCHAR(150) NULL,
    endereco_numero VARCHAR(20) NOT NULL,
    endereco_complemento VARCHAR(100) NULL,
    endereco_bairro VARCHAR(100) NULL,
    endereco_cidade VARCHAR(100) NOT NULL,
    endereco_uf CHAR(2) NOT NULL,
    endereco_apelido VARCHAR(50) NULL,
    INDEX idx_solicitacoes_usuario_data (usuario_id, data_criacao, solicitacao_id),
    CONSTRAINT fk_solicitacoes_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitacoes_servico FOREIGN KEY (servico_id)
        REFERENCES servicos(servico_id) ON DELETE RESTRICT,
    CONSTRAINT chk_solicitacoes_area_positiva CHECK (area_m2 > 0),
    CONSTRAINT chk_solicitacoes_status CHECK (status IN ('CRIADA', 'ENCERRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
