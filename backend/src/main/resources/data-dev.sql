-- =====================================================
-- DATOS DE PRUEBA PARA PERFIL DEV (H2 en memoria)
-- Contraseñas BCrypt para "password123":
--   $2b$10$JvRv8eBbhVcxNN6nW6uiru1PRRKrKfb88UNpz8gyDDFmiJQ4yfqH6
-- =====================================================

-- Usuarios (admin, 1 user normal, 3 bares)
INSERT INTO users (id, email, password, name, role, created_at) VALUES
(1, 'admin@matchbar.com',  '$2b$10$JvRv8eBbhVcxNN6nW6uiru1PRRKrKfb88UNpz8gyDDFmiJQ4yfqH6', 'Administrador',     'ADMIN', CURRENT_TIMESTAMP),
(2, 'mario@test.com',      '$2b$10$JvRv8eBbhVcxNN6nW6uiru1PRRKrKfb88UNpz8gyDDFmiJQ4yfqH6', 'Mario',             'USER',  CURRENT_TIMESTAMP),
(3, 'rincon@test.com',     '$2b$10$JvRv8eBbhVcxNN6nW6uiru1PRRKrKfb88UNpz8gyDDFmiJQ4yfqH6', 'El Rincon',         'BAR',   CURRENT_TIMESTAMP),
(4, 'centenario@test.com', '$2b$10$JvRv8eBbhVcxNN6nW6uiru1PRRKrKfb88UNpz8gyDDFmiJQ4yfqH6', 'Bar Centenario',    'BAR',   CURRENT_TIMESTAMP),
(5, 'penalti@test.com',    '$2b$10$JvRv8eBbhVcxNN6nW6uiru1PRRKrKfb88UNpz8gyDDFmiJQ4yfqH6', 'El Penalti',        'BAR',   CURRENT_TIMESTAMP);

-- Competiciones
INSERT INTO competitions (id, name, country, logo_url) VALUES
(1, 'LaLiga',           'Espana',     null),
(2, 'Premier League',   'Inglaterra', null),
(3, 'Champions League', 'Europa',     null);

-- Equipos
INSERT INTO teams (id, name, logo_url, competition_id) VALUES
(1, 'Real Madrid',       null, 1),
(2, 'FC Barcelona',      null, 1),
(3, 'Atletico de Madrid', null, 1),
(4, 'Sevilla FC',        null, 1),
(5, 'Manchester City',   null, 2),
(6, 'Liverpool FC',      null, 2),
(7, 'Arsenal FC',        null, 2);

-- Partidos (kickoff dentro de los proximos dias desde "hoy")
INSERT INTO matches (id, competition_id, home_team_id, away_team_id, kickoff_at) VALUES
(1, 1, 1, 2, DATEADD('DAY', 1, CURRENT_TIMESTAMP)),
(2, 1, 3, 4, DATEADD('DAY', 2, CURRENT_TIMESTAMP)),
(3, 2, 5, 6, DATEADD('DAY', 3, CURRENT_TIMESTAMP)),
(4, 3, 1, 5, DATEADD('DAY', 5, CURRENT_TIMESTAMP)),
(5, 1, 2, 3, DATEADD('DAY', 7, CURRENT_TIMESTAMP));

-- Bares de Madrid centro (todos APROBADOS para que aparezcan al usuario)
INSERT INTO bars (id, user_id, name, description, address, latitude, longitude, license_doc, photo_url, status, created_at) VALUES
(1, 3, 'El Rincon del Hincha', 'Bar tradicional con pantallas en cada esquina y ambiente futbolero',
   'Calle Bravo Murillo 56, Madrid', 40.4500, -3.7038, 'licencia-rincon.pdf', null, 'APPROVED', CURRENT_TIMESTAMP),
(2, 4, 'Bar Centenario', 'Cerveza fria y futbol nacional e internacional. Tapas caseras',
   'Calle de Alcala 102, Madrid', 40.4220, -3.6786, 'licencia-centenario.pdf', null, 'APPROVED', CURRENT_TIMESTAMP),
(3, 5, 'El Penalti', 'Pantalla gigante 4K. Reservado para grupos los dias de Champions',
   'Plaza Mayor 8, Madrid', 40.4155, -3.7074, 'licencia-penalti.pdf', null, 'PENDING', CURRENT_TIMESTAMP);

-- Broadcasts: que bares retransmiten que partidos
INSERT INTO broadcasts (id, bar_id, match_id) VALUES
(1, 1, 1),  -- Rincon emite Madrid-Barca
(2, 2, 1),  -- Centenario emite Madrid-Barca
(3, 1, 4),  -- Rincon emite la Champions
(4, 2, 3),  -- Centenario emite la Premier
(5, 1, 5);  -- Rincon emite Barca-Atleti

-- Algunas valoraciones iniciales
INSERT INTO reviews (id, user_id, bar_id, rating_atmosphere, rating_food, rating_price, comment, created_at) VALUES
(1, 2, 1, 5, 4, 3, 'Ambiente brutal en los partidos del Madrid. Se ven todas las pantallas bien.', CURRENT_TIMESTAMP),
(2, 2, 2, 4, 5, 4, 'Las tapas estan muy buenas y el precio es razonable.', CURRENT_TIMESTAMP);

-- Reset de los IDENTITY (H2)
ALTER TABLE users        ALTER COLUMN id RESTART WITH 100;
ALTER TABLE competitions ALTER COLUMN id RESTART WITH 100;
ALTER TABLE teams        ALTER COLUMN id RESTART WITH 100;
ALTER TABLE matches      ALTER COLUMN id RESTART WITH 100;
ALTER TABLE bars         ALTER COLUMN id RESTART WITH 100;
ALTER TABLE broadcasts   ALTER COLUMN id RESTART WITH 100;
ALTER TABLE reviews      ALTER COLUMN id RESTART WITH 100;
