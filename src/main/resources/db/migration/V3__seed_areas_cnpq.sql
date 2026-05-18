-- Seed mínimo de áreas temáticas CNPq (Tabela de Áreas do Conhecimento).
-- Incluye las 9 grandes áreas y un sub-árbol detallado para Ciência da Computação
-- (relevante para AcademConnect/IFSul TDS). El seed completo (~9000 nodos) se
-- importará en una migración posterior cuando se incorpore el dataset oficial.

-- Grandes áreas
INSERT INTO area_tematica (codigo_externo, nombre, parent_id, thesaurus_origen, activo, created_at, updated_at, created_by, updated_by) VALUES
  ('1.00.00.00-3', 'Ciências Exatas e da Terra',     NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('2.00.00.00-6', 'Ciências Biológicas',            NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('3.00.00.00-9', 'Engenharias',                    NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('4.00.00.00-1', 'Ciências da Saúde',              NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('5.00.00.00-4', 'Ciências Agrárias',              NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('6.00.00.00-7', 'Ciências Sociais Aplicadas',     NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('7.00.00.00-0', 'Ciências Humanas',               NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('8.00.00.00-2', 'Linguística, Letras e Artes',    NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('9.00.00.00-5', 'Outros',                         NULL, 'CNPQ', TRUE, now(), now(), 'system', 'system');

-- Área: Ciência da Computação (bajo Ciências Exatas e da Terra)
INSERT INTO area_tematica (codigo_externo, nombre, parent_id, thesaurus_origen, activo, created_at, updated_at, created_by, updated_by) VALUES
  ('1.03.00.00-7', 'Ciência da Computação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.00.00.00-3' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system');

-- Sub-áreas de Ciência da Computação
INSERT INTO area_tematica (codigo_externo, nombre, parent_id, thesaurus_origen, activo, created_at, updated_at, created_by, updated_by) VALUES
  ('1.03.01.00-3', 'Teoria da Computação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.00.00-7' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.02.00-0', 'Matemática da Computação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.00.00-7' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.03.00-6', 'Metodologia e Técnicas da Computação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.00.00-7' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.04.00-2', 'Sistemas de Computação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.00.00-7' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system');

-- Especialidades bajo Metodologia e Técnicas da Computação (nivel 3)
INSERT INTO area_tematica (codigo_externo, nombre, parent_id, thesaurus_origen, activo, created_at, updated_at, created_by, updated_by) VALUES
  ('1.03.03.01-0', 'Linguagens de Programação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.03.00-6' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.03.02-9', 'Engenharia de Software',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.03.00-6' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.03.03-7', 'Banco de Dados',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.03.00-6' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.03.04-5', 'Sistemas de Informação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.03.00-6' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.03.05-3', 'Processamento Gráfico (Graphics)',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.03.00-6' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system');

-- Especialidades bajo Sistemas de Computação (nivel 3)
INSERT INTO area_tematica (codigo_externo, nombre, parent_id, thesaurus_origen, activo, created_at, updated_at, created_by, updated_by) VALUES
  ('1.03.04.01-0', 'Hardware',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.04.00-2' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.04.02-9', 'Arquitetura de Sistemas de Computação',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.04.00-2' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.04.03-7', 'Software Básico',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.04.00-2' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system'),
  ('1.03.04.04-5', 'Teleinformática',
    (SELECT id FROM area_tematica WHERE codigo_externo = '1.03.04.00-2' AND thesaurus_origen = 'CNPQ'),
    'CNPQ', TRUE, now(), now(), 'system', 'system');
