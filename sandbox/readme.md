# ScalikeJDBC Sandbox

## Try it now

```sh

git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console
```

"users" table is already created. You can execute queries as follows:

```scala
case class User(id: Long, name: String)

val users = DB readOnly { implicit s => 
  SQL("select * from users")
    .map(rs => User(rs.long("id"), rs.string("name")))
    .list.apply()
}

DB localTx { implicit s => 
  SQL("insert into users (id, name) values ({id}, {name})")
    .bindByName('id -> 3, 'name -> "Charles")
    .update.apply() 
}
```

