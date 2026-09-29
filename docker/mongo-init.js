// Lo ejecuta la imagen oficial de MongoDB solo en el primer arranque (volumen
// vacío). Crea el usuario con el que se conecta la API, con permisos
// únicamente sobre la base de datos de la aplicación: no es administrador del
// servidor, así que una fuga de sus credenciales no compromete nada más.
const appDb = db.getSiblingDB(process.env.MONGO_INITDB_DATABASE || 'matchbar');

appDb.createUser({
  user: process.env.MONGO_APP_USER,
  pwd: process.env.MONGO_APP_PASSWORD,
  roles: [{ role: 'readWrite', db: appDb.getName() }],
});
