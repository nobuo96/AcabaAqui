-- Aplicar manualmente no schema correto antes de usar a data de nascimento.
-- O assistente nao executa este DDL no banco.
ALTER TABLE usuarios
    ADD COLUMN data_nascimento DATE NULL;