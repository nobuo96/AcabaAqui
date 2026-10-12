CREATE TABLE enderecos (
    endereco_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    cep CHAR(8) NOT NULL,
    logradouro VARCHAR(150) NULL,
    numero VARCHAR(20) NOT NULL,
    complemento VARCHAR(100) NULL,
    bairro VARCHAR(100) NULL,
    cidade VARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL,
    apelido VARCHAR(50) NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_enderecos_usuario_ativo (usuario_id, ativo),
    CONSTRAINT fk_enderecos_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(usuario_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
