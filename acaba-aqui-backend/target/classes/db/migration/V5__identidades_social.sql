-- Aplicar manualmente no schema correto antes de habilitar o login social.
CREATE TABLE identidades_social (
    identidade_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    provedor VARCHAR(20) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    CONSTRAINT uq_identidade_social_provedor_subject UNIQUE (provedor, subject),
    INDEX idx_identidade_social_usuario (usuario_id),
    CONSTRAINT fk_identidade_social_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
