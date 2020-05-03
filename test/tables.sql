drop table lauda.source_table_1;

create table lauda.source_table_1 (
  col_1 number,
  col_2 varchar2(100),
  col_3 date
);

insert into lauda.source_table_1 values (1, 'aaa', sysdate);
commit;



drop table lauda.target_table_1;

create table lauda.target_table_1 (
  col_1 int,
  col_2 text,
  col_3 date
);