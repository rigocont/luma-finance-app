-- Se ejecuta una sola vez, cuando el volumen de MySQL esta vacio.
-- El esquema de LUMA NO se crea aqui: de eso se encarga Flyway.
ALTER DATABASE luma CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
