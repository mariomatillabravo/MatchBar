#PARA EJECUTAR POR PRIMERA VEZ
docker compose up --build

#SI YA SE HA EJECUTADO
docker compose up

docker compose down -v --rmi local
docker compose build --no-cache
docker compose up

#URL
http://localhost:8080/admin.html
http://localhost:8080/ui-swagger.html