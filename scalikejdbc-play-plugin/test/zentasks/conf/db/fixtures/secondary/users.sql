# --- !Ups
INSERT INTO users (email, name, password) VALUES ('guillaume@sample.com', 'Guillaume Bort', 'secret');
INSERT INTO users (email, name, password) VALUES ('maxime@sample.com', 'Maxime Dantec', 'secret');
INSERT INTO users (email, name, password) VALUES ('sadek@sample.com', 'Sadek Drobi', 'secret');
INSERT INTO users (email, name, password) VALUES ('erwan@sample.com', 'Erwan Loisant', 'secret');


# --- !Downs
DELETE FROM users;
