create database userdata; -- Create the new database
create user 'springuser'@'%' identified by 'ThePassword'; -- Creates the user
grant all on userdata.* to 'springuser'@'%'; -- Gives all the privileges to the new user on the newly created database