# Lauda

An ETL'ish tool that ransfers data between any JDBC compliant databases.

Lauda has its own simple repository in given database, that can be configured and loaded either from command line
or directly in database. Keeping extract-load definitions in database, makes it easier for database people to
manage transfers using a database client. And for the same reason lauda has a simple database model and no `orm` in source.

Lauda has three concepts for defining an ETL process;

- `connection`: Connection definition to source and target databases. These definitions are
kept in `connections` table in repository. A Connection has the following attributes.

  - `name`: Unique name of the connection. Case sensitive.
  - `url`: JDBC url for the connection. Example: `jdbc:oracle:thin:@localhost:1521:orcl`
  - `username`: user name for connection.
  - `password`: passowrd for connection.
  - `class_name`: Class name of the JDBC driver. Example: `oracle.jdbc.driver.OracleDriver`

- `mapping`: Mapping definition of the extract and load targets. These data is kept in `mappings` table
in repository. A mapping has the following attributes.
  - `name`: Unique name of the mapping. Case sensitive.
  - `source_connection`: Name of the source connection. Lookup to `connections`
  - `target_connection`: Name of the target connection. Lookup to `connections`
  - `source_table`: Name of the source table. Give database/schema prefix if table is another database/schema than the connection. For example the value can be `my_schema.table_name` or just `table_name`
  - `target_table`: Name of the target table. Give database/schema prefix if table is another database/schema than the connection. For example the value can be `my_schema.table_name` or just `table_name`
  - `source_hint`: Optional source query hint. Example `/*+ parallel(8) */`
  - `target_hint`: Optional target statement hint. Example `/*+ nologging */`
  - `filter`: Filter expression for the source. Example `column_a in ('X', 'Y', 'Z') and column_b=2`
  - `batch_size`: Batch select and insert size. Defaults to 1000
  - `drop_create`: Drop the target table before insert. Defaults to `0`, `false`

  Since some databases do not support boolean, in repository tables, boolean options like `drop_create` are managed by integers. For `false` give the value `0` for `true` give any positve integer.
  In repository data files(see command line options and data file loading) you can use `true` or `false`.

- `column mapping`: Column mapping pairs between source and target tables. This mappings are stored in `columns`
table in repository. Has the following attributes;

  - `mapping`: Name of the mapping, this pair belongs. Lookup to `mappings`.
  - `source`: Name of the source column or an expression. Example: `COL_A` or `to_char(COL_DATE, 'yyymmdd')`
  - `target`: Name of the target column that source will be inserted.


### Installation
- Make sure you have java 1.8 or greater
- Download archive file from [dist](/dist)
- Extract `lauda-[version].tar.gz` to desired location.
- Put the required `jdbc` drivers to `lib` folder


### Building from source
- Make sure you have [sbt](https://www.scala-sbt.org/)
- Clone the repository
- run `dist.sh`

### Configuration
- Use `config.yml` for configuratin

### Command line arguments
```
lauda [-hV] <command> [-c=<connectionName>] [-m=<mappingName>] [-r=<repositoryDataFile>]
Loads data between databases

  <command>                   Command to exeucte
  -c=<connectionName>         Name of the connection
  -h, --help                  Show this help message and exit.
  -m=<mappingName>            Name of the mapping
  -r=<repositoryDataFile>     Path to repository data file
  -V, --version               Print version information and exit.
```

`command` command can be one of the following and can take parameters;

  - `repository.up`: Initialize repository. Uses `config.yml`
  - `repository.down`: Drops the repsitory. Uses `config.yml`
  - `repository.import`: Import repository data with parameter `-r`
  - `repository.print.connections`: Print connections
  - `repository.print.mappings`: Print mappings
  - `repository.print.columns`: Print source and target columns of given mapping with `-m`
  - `mapping.delete`: Delete a mapping by name with parameter `-m`
  - `mapping.exists`: Check if mapping exists with parameter `-m`
  - `mapping.run`: Run the mapping given with `-m`
  - `mapping.create`: Create target table in given mapping with `-m`
  - `connection.delete`: Delete connection by name `-c`
  - `connection.test`: Test jdbc connection by name `-c`

**Parameters**:
  - `-m`: Mapping name. Example `-m=mapping_name` or if you have spaces, `-m="mapping name`"
  - `-c`: Connection name. Example `-c=connection_name` or if you have spaces, `-c="connection name`"
  - `-r`: Path to repository data file. Example `-r=./data/import.01.yml`

**Examples**

  Create repository, using the parameters in `config.yml`
  ```sh
  ./lauda.sh repository.up
  ```

  Reset/drop repository, using the parameters in `config.yml`
  ```sh
  ./lauda.sh repository.down
  ```

  Import mapping and connection definitions. [See example data file](/examples/repository-seed-01.yml).
  ```sh
  ./lauda.sh repository.import -r=examples/repository-seed.yml
  ```

  Print available connections
  ```sh
  ./lauda.sh repository.print.connections
  ```

  Print defined mappings
  ```sh
  ./lauda.sh repository.print.mappings
  ```

  Print the source and target columns of the given mapping
  ```sh
  ./lauda.sh repository.print.columns -m=mapping_name
  ```

  Delete a mapping definition(column mappings also)
  ```sh
  ./lauda.sh mapping.delete -m=mapping_name
  ```
  ```sh
  ./lauda.sh mapping.delete -m="Mapping name"
  ```

  Check if mapping with given name already exists
  ```sh
  ./lauda.sh mapping.exists -m=mapping_name
  ```

  Run mapping
  ```sh
  ./lauda.sh mapping.run -m=mapping_name
  ```

  Create target table in mapping. Does not run mapping, only created target.
  ```sh
  ./lauda.sh mapping.create -m=mapping_name
  ```

  Delete given connection definition
  ```sh
  ./lauda.sh connection.delete -c=connection_name
  ```

  ```sh
  ./lauda.sh connection.delete -c="Connection name"
  ```

  Test the connection
  ```sh
  ./lauda.sh connection.test -c=connection_name
  ```




### Loading definitions
  An ETL definiton can be given directly using a database client in repository or using
  command line option. With a database client you can execute a script like [this one](/examples/repository-seed-01.sql)
  For command line, use the `repsitory.import` command and `-r` option to give data file like
  [this one](/examples/repository-seed-01.yml). See [examples](/examples) for different options.

  Example command line usage;
  `./lauda.sh repository.import -r=/path/to/data-file.yml`
  `./lauda.sh repository.import -r=./data-file.yml`



### Oracle to Oracle transfers
For data transfers between two oracle databases you can use [elo](https://github.com/bluecolor/elo)
which uses oracle dblink's and is faster.