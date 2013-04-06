# --- !Ups

INSERT INTO task (id, title, done, due_date, assigned_to, project, folder) VALUES (1000, 'Fix the documentation', FALSE, null, 'guillaume@sample.com', 1, 'Todo');
INSERT INTO task (id, title, done, due_date, assigned_to, project, folder) VALUES (1001, 'Prepare the beta release', FALSE, '2011-11-15 00:00:00.0', null, 1, 'Urgent');
INSERT INTO task (id, title, done, due_date, assigned_to, project, folder) VALUES (1002, 'Buy some milk', FALSE, null, null, 9, 'Todo');
INSERT INTO task (id, title, done, due_date, assigned_to, project, folder) VALUES (1003, 'Check 1.2.4-RC2', FALSE, '2011-11-18 00:00:00.0', 'guillaume@sample.com', 2, 'Todo');
INSERT INTO task (id, title, done, due_date, assigned_to, project, folder) VALUES (1004, 'Finish zentask integration', TRUE, '2011-11-15 00:00:00.0', 'maxime@sample.com', 7, 'Todo');
INSERT INTO task (id, title, done, due_date, assigned_to, project, folder) VALUES (1005, 'Release the secret project', FALSE, '2012-01-01 00:00:00.0', 'sadek@sample.com', 4, 'Todo');
ALTER SEQUENCE task_seq RESTART WITH 1006;

# --- !Downs
ALTER SEQUENCE task_seq RESTART WITH 1000;
DELETE FROM task;

