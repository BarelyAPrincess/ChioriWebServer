#Datastore

Most features of the server can utilize either file or database based configuration. On first run, the server will create a SQLite database within the server root as a placeholder. You can easily switch to a MySQL or H2 database via configuration. Automated database deployment is in the works, but until then you can use the distributed 'frameworkdb.sql' file to initialize a new table.
