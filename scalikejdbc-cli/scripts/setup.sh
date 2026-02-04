#!/bin/sh

HOME_DIR=`echo $HOME | sed -e s/\\\\/$//`
ROOT_DIR=${HOME_DIR}/bin/scalikejdbc-cli
mkdir -p ${ROOT_DIR}
CONFIG_PROPS=${ROOT_DIR}/config.properties
DBCONSOLE_COMMAND=${ROOT_DIR}/dbconsole
BUILD_SBT=${ROOT_DIR}/build.sbt
INIT_DIR=${ROOT_DIR}/init
INIT_SCRIPT=${INIT_DIR}/init.scala
cd ${ROOT_DIR}
rm -f sbt-launch.jar*
wget https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/1.10.0/sbt-launch-1.10.0.jar

mkdir -p ./db
cd ./db
rm -f sandbox.h2.db
wget https://raw.github.com/scalikejdbc/scalikejdbc/master/scalikejdbc-cli/db/sandbox.h2.db
cd -

if [ ! -f ${CONFIG_PROPS} ]; then
  echo 'sandbox.jdbc.url=jdbc:h2:file:./db/sandbox
sandbox.jdbc.username=
sandbox.jdbc.password=
#mysql.jdbc.url=jdbc:mysql://localhost:3306/dbname
#postgres.jdbc.url=jdbc:postgresql://localhost:5432/dbname
#oracle.jdbc.url=jdbc:oracle:thin:@localhost:1521:dbname
' > ${CONFIG_PROPS}
fi

echo '#!/bin/bash

function edit_config() {
  cd `dirname $0`
  ${EDITOR:=vi} ~/bin/scalikejdbc-cli/config.properties
}

function show_help() {
  echo
  echo "dbconsole is an extended sbt console to connect database easily."
  echo
  echo "Usage:"
  echo "  dbconsole [OPTION]... [PROFILE]"
  echo
  echo "General options:"
  echo "  -e, --edit    edit configuration, then exit"
  echo "  -c, --clean   clean sbt environment, then exit"
  echo "  -h, --help    show this help, then exit"
  echo
}

function run_sbt() {
  java -Xms256M -Xmx1024M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxMetaspaceSize=386M \
    -Dfile.encoding=UTF-8 \
    -jar `dirname $0`/sbt-launch.jar \
    -Dscalikejdbc-cli.config.profile=${PROFILE} \
    $1
}

function ltrim () {
  perl -pe '"'"'s/^\s*//'"'"'
}

function ignore_comment() {
  perl -pe '"'"'s/^(.*?)#.*$/$1/'"'"'
}

function remove_blank_line() {
  grep -v ^$
}

function take_until_first_dot() {
  cut -f 1 -d "."
}

function collect_profile() {
  cat config.properties \
    | ltrim \
    | ignore_comment \
    | remove_blank_line \
    | take_until_first_dot \
    | sort -u
}


cd `dirname $0`

until [ -z "$1" ]; do
  case $1 in
    "-e"|"--edit")
      edit_config
      exit 0
      ;;
    "-c"|"--clean")
      run_sbt "clean"
      exit 0
      ;;
    "-h"|"--help")
      show_help
      exit 0
      ;;
    *)
      if [ "`echo $1 | cut -c 1`" == "-" ]; then
        echo
        echo "ERROR: Unknown option ($1)"
        show_help
        exit 1
      fi
      PROFILE=$1
      shift
      ;;
  esac
done

if [ "$PROFILE" == "" ]; then
  echo "Select a profile."
  select INPUT in `collect_profile` EXIT
  do
    if [ "$INPUT" == "EXIT" ]; then
      exit;
    fi
    PROFILE=$INPUT
    if [ "$INPUT" == "" ]; then
      continue
    fi
    break
  done || exit
fi

echo
echo "Starting sbt console for ${PROFILE}..."
echo

run_sbt "console"

' > ${DBCONSOLE_COMMAND}


echo 'scalaVersion := "2.12.21"

libraryDependencies ++= Seq(
  "org.scalikejdbc"    %% "scalikejdbc"         % "3.4.0",
  "org.slf4j"          % "slf4j-simple"         % "1.7.29",
  "com.h2database"     % "h2"                   % "1.4.200",
  "org.apache.derby"   % "derby"                % "10.14.2.0",
  "org.xerial"         % "sqlite-jdbc"          % "3.47.1.0",
  "org.hsqldb"         % "hsqldb"               % "2.5.2",
  "com.mysql"          % "mysql-connector-j"    % "9.6.0",
  "org.postgresql"     % "postgresql"           % "42.2.21"
)

initialCommands := {
  def using[A, R <: { def close() }](r : R)(f : R => A) : A = try { f(r) } finally { r.close() }
  def readFileAsString(file: File): String = using (_root_.scala.io.Source.fromFile(file)) { _.mkString }
  readFileAsString(new File("init/init.scala")) +
  ";" +
  new File("init").listFiles.filter(f => f.isFile && f.getName != "init.scala").map(f => readFileAsString(f)).mkString("\\n")
}
' > ${BUILD_SBT}

mkdir ${INIT_DIR} 2> /dev/null

echo 'import scalikejdbc._
import scalikejdbc.StringSQLRunner._
def initialize() = {
  val props = new java.util.Properties
  using(new java.io.FileInputStream("config.properties")) { is => props.load(is) }
  val profile = Option(System.getProperty("scalikejdbc-cli.config.profile")).getOrElse("default")
  Option(props.get(profile + ".jdbc.url")).map { obj =>
    val url = obj.toString
    if (url.startsWith("jdbc:postgresql")) { Class.forName("org.postgresql.Driver")
    } else if (url.startsWith("jdbc:mysql")) { Class.forName("com.mysql.cj.jdbc.Driver")
    } else if (url.startsWith("jdbc:h2")) { Class.forName("org.h2.Driver")
    } else if (url.startsWith("jdbc:hsqldb")) { Class.forName("org.hsqldb.jdbc.JDBCDriver")
    } else if (url.startsWith("jdbc:derby")) { Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
    } else if (url.startsWith("jdbc:sqlite")) { Class.forName("org.sqlite.JDBC")
    } else { throw new IllegalStateException("Driver is not found for " + url)
    }
    val user = Option(props.get(profile + ".jdbc.username")).map(_.toString).orNull[String]
    val password = Option(props.get(profile + ".jdbc.password")).map(_.toString).orNull[String]
    ConnectionPool.singleton(url, user, password)
  }.getOrElse {
    throw new IllegalStateException("JDBC settings for \"" + profile + "\" is not found. Try \"dbconsole --edit\".")
  }
}
initialize()
def describe(table: String) = println(DB.describe(table))
def tables = println(DB.showTables())
implicit val session: DBSession = AutoSession
' > ${INIT_SCRIPT}

SHELL_PROFILE=${HOME_DIR}/.bash_profile
if [[ "$SHELL" == *zsh* ]]; then
  SHELL_PROFILE=${HOME_DIR}/.zprofile
fi

if [ ! `grep 'PATH=${PATH}:${HOME}/bin/scalikejdbc-cli' ${SHELL_PROFILE}` ]; then
  echo "PATH=\${PATH}:\${HOME}/bin/scalikejdbc-cli" >> ${SHELL_PROFILE}
fi

chmod +x ${DBCONSOLE_COMMAND}

echo "
command installed to ${DBCONSOLE_COMMAND}

Please execute 'source ${SHELL_PROFILE}'
"

