-- Seed data for the local profile (issue #15).
-- Runs on every startup after Hibernate creates the schema (spring.sql.init.mode=always).
-- Ids are not set explicitly so the identity columns keep working when new rows are
-- created through the API; related rows are linked by name or url instead.

-- ---------- Tags ----------
INSERT INTO tags (name) VALUES
    ('Cartas'),
    ('Fotografia'),
    ('Prensa'),
    ('Testimonio'),
    ('Afiches'),
    ('Manuscrito');

-- ---------- Pieces ----------
INSERT INTO pieces (name, description, date) VALUES
    ('Carta desde Cordoba', 'Carta manuscrita enviada a la familia', '1977-05-03'),
    ('Fotografia de una marcha', 'Fotografia en blanco y negro de una marcha en Plaza de Mayo', '1981-12-10'),
    ('Recorte de diario sobre las elecciones', 'Recorte de prensa con la cobertura de las elecciones', '1983-10-31'),
    ('Testimonio grabado en casete', 'Entrevista a un familiar registrada en casete', '1985-04-22'),
    ('Afiche de convocatoria', 'Afiche impreso que convoca a una marcha', '1982-03-30'),
    ('Libreta de notas', 'Libreta personal con anotaciones, pendiente de digitalizar', '1976-08-15');

-- ---------- Digitizations ----------
INSERT INTO digitizations (url, description, piece_id) VALUES
    ('https://archivo.example/carta-cordoba-p1.pdf', 'Escaneo de la pagina 1',
        (SELECT id FROM pieces WHERE name = 'Carta desde Cordoba')),
    ('https://archivo.example/carta-cordoba-p2.pdf', 'Escaneo de la pagina 2',
        (SELECT id FROM pieces WHERE name = 'Carta desde Cordoba')),
    ('https://archivo.example/foto-marcha-frente.jpg', 'Escaneo del frente en alta resolucion',
        (SELECT id FROM pieces WHERE name = 'Fotografia de una marcha')),
    ('https://archivo.example/foto-marcha-reverso.jpg', 'Escaneo del reverso con una dedicatoria escrita a mano',
        (SELECT id FROM pieces WHERE name = 'Fotografia de una marcha')),
    ('https://archivo.example/recorte-elecciones.pdf', 'Escaneo del recorte completo',
        (SELECT id FROM pieces WHERE name = 'Recorte de diario sobre las elecciones')),
    ('https://archivo.example/testimonio-casete.mp3', 'Audio digitalizado del casete',
        (SELECT id FROM pieces WHERE name = 'Testimonio grabado en casete')),
    ('https://archivo.example/testimonio-transcripcion.pdf', 'Transcripcion de la entrevista',
        (SELECT id FROM pieces WHERE name = 'Testimonio grabado en casete')),
    ('https://archivo.example/afiche-convocatoria.jpg', 'Fotografia del afiche',
        (SELECT id FROM pieces WHERE name = 'Afiche de convocatoria'));

-- ---------- Tags assigned to digitizations ----------
INSERT INTO digitization_tags (digitization_id, tag_id)
SELECT d.id, t.id FROM digitizations d, tags t
WHERE t.name = 'Cartas'
  AND d.url IN ('https://archivo.example/carta-cordoba-p1.pdf',
                'https://archivo.example/carta-cordoba-p2.pdf');

INSERT INTO digitization_tags (digitization_id, tag_id)
SELECT d.id, t.id FROM digitizations d, tags t
WHERE t.name = 'Manuscrito'
  AND d.url IN ('https://archivo.example/carta-cordoba-p1.pdf',
                'https://archivo.example/carta-cordoba-p2.pdf',
                'https://archivo.example/foto-marcha-reverso.jpg');

INSERT INTO digitization_tags (digitization_id, tag_id)
SELECT d.id, t.id FROM digitizations d, tags t
WHERE t.name = 'Fotografia'
  AND d.url IN ('https://archivo.example/foto-marcha-frente.jpg',
                'https://archivo.example/foto-marcha-reverso.jpg');

INSERT INTO digitization_tags (digitization_id, tag_id)
SELECT d.id, t.id FROM digitizations d, tags t
WHERE t.name = 'Prensa'
  AND d.url = 'https://archivo.example/recorte-elecciones.pdf';

INSERT INTO digitization_tags (digitization_id, tag_id)
SELECT d.id, t.id FROM digitizations d, tags t
WHERE t.name = 'Testimonio'
  AND d.url IN ('https://archivo.example/testimonio-casete.mp3',
                'https://archivo.example/testimonio-transcripcion.pdf');

INSERT INTO digitization_tags (digitization_id, tag_id)
SELECT d.id, t.id FROM digitizations d, tags t
WHERE t.name = 'Afiches'
  AND d.url = 'https://archivo.example/afiche-convocatoria.jpg';
