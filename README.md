# iDIN server and frontend

## MySQL configuration

 1. Install MySQL/MariaDB.
 2. Create a new user:
    
        create user ideal@localhost identified by 'password';
    
 3. Create a new database:
    
        create database ideal;
    
 4. Allow the ideal user access to this database:
    
        grant all privileges on *.ideal to ideal@localhost;
    
 5. Create a new table to store tokens:
    
        use ideal;
        CREATE TABLE idin_tokens (id INTEGER PRIMARY KEY AUTO_INCREMENT, hashedToken text);
    
 6. Modify src/test/resources/jetty-env.xml and modify the properties `url`,
    `username`, and `password`.
