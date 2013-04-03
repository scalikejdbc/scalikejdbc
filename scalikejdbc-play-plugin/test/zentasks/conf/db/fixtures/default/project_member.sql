# --- !Ups

INSERT INTO project_member (project_id, user_email) VALUES (1, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (1, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (1, 'sadek@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (1, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (2, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (2, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (3, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (3, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'sadek@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (5, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (6, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (7, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (7, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (8, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (9, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (10, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (11, 'sadek@sample.com');

# --- !Downs

DELETE FROM project_member;
