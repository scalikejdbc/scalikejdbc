# --- !Ups

INSERT INTO project (id, name, folder) VALUES (1, 'Play 2.0', 'Play framework');
INSERT INTO project (id, name, folder) VALUES (2, 'Play 1.2.4', 'Play framework');
INSERT INTO project (id, name, folder) VALUES (3, 'Website', 'Play framework');
INSERT INTO project (id, name, folder) VALUES (4, 'Secret project', 'Zenexity');
INSERT INTO project (id, name, folder) VALUES (5, 'Playmate', 'Zenexity');
INSERT INTO project (id, name, folder) VALUES (6, 'Things to do', 'Personal');
INSERT INTO project (id, name, folder) VALUES (7, 'Play samples', 'Zenexity');
INSERT INTO project (id, name, folder) VALUES (8, 'Private', 'Personal');
INSERT INTO project (id, name, folder) VALUES (9, 'Private', 'Personal');
INSERT INTO project (id, name, folder) VALUES (10, 'Private', 'Personal');
INSERT INTO project (id, name, folder) VALUES (11, 'Private', 'Personal');
ALTER SEQUENCE project_seq RESTART WITH 12;

# --- !Downs
ALTER SEQUENCE project_seq RESTART WITH 1;
DELETE FROM project;