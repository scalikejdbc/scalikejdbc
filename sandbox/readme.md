# ScalikeJDBC Sandbox

## Try it now

```sh

git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console
```

```scala
[info] Starting scala interpreter...
[info] 
[run-main] INFO scalikejdbc.StatementExecutor$$anon$1 - SQL execution completed

  [Executed SQL]
   select users.id as id__on__users, users.name as name__on__users, users.company_id as company_id__on__users, companies.id as id__on__companies, companies.name as name__on__companies from users left join companies on users.company_id = companies.id; (16 ms)

  [Stack Trace]
    ...
    $line1.$read$$iw$$iw$$anonfun$3.apply(<console>:71)
    $line1.$read$$iw$$iw$$anonfun$3.apply(<console>:68)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:500)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:499)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB.readOnly(DB.scala:499)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:133)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:132)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB$.readOnly(DB.scala:132)
    $line1.$read$$iw$$iw$.<init>(<console>:68)
    $line1.$read$$iw$$iw$.<clinit>(<console>)
    $line1.$eval$.<init>(<console>:7)
    ...

-------------------
User(1,Some(Alice),None,None)
User(2,Some(Bob),Some(1),Some(Company(1,Some(Typesafe))))
User(3,Some(Chris),Some(1),Some(Company(1,Some(Typesafe))))
-------------------
[run-main] INFO scalikejdbc.StatementExecutor$$anon$1 - SQL execution completed

  [Executed SQL]
   select u.id as id__on__u, u.name as name__on__u, u.company_id as company_id__on__u, g.id as id__on__g, g.name as name__on__g, c.id as id__on__c, c.name as name__on__c from group_members gm inner join users u on u.id = gm.user_id inner join groups g on g.id = gm.group_id left join companies c on u.company_id = c.id; (0 ms)

  [Stack Trace]
    ...
    $line1.$read$$iw$$iw$$anonfun$5.apply(<console>:79)
    $line1.$read$$iw$$iw$$anonfun$5.apply(<console>:76)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:500)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:499)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB.readOnly(DB.scala:499)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:133)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:132)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB$.readOnly(DB.scala:132)
    $line1.$read$$iw$$iw$.<init>(<console>:76)
    $line1.$read$$iw$$iw$.<clinit>(<console>)
    $line1.$eval$.<init>(<console>:7)
    ...

-------------------
Group(1,Some(Japan Scala Users Group),List(User(2,Some(Bob),Some(1),Some(Company(1,Some(Typesafe)))), User(1,Some(Alice),None,None)))
-------------------
```

