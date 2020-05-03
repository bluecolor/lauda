
truncate table lauda.connections;
truncate table lauda.mappings;
truncate table lauda.columns;

insert into lauda.connections (
  name, url, username, password, class_name
) values (
  'oracle_1', 'jdbc:oracle:thin:@localhost:1521:orcl', 'lauda', 'lauda', 'oracle.jdbc.driver.OracleDriver'
);

insert into lauda.connections (
  name, url, username, password, class_name
) values (
  'postgre_1', 'jdbc:postgresql://localhost/lauda', 'lauda', 'lauda', 'org.postgresql.Driver'
);

insert into lauda.mappings (
  name, source_connection, target_connection, source_table, target_table,
  source_hint, target_hint, filter, drop_create
) values (
  'ora2pg_1', 'oracle_1', 'postgre_1', 'lauda.source_table_1', 'target_table_1',
  '/*+ parallel(8) */', null, 'col_1 = 2', 1
);

insert into lauda.columns (
  mapping, source, target
) values (
  'ora2pg_1', 'col_1', 'col_1'
);

insert into lauda.columns (
  mapping, source, target
) values (
  'ora2pg_1', 'col_2', 'col_2'
);

insert into lauda.columns (
  mapping, source, target
) values (
  'ora2pg_1', 'col_3', 'col_3'
);

commit;