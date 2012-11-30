#!/bin/sh

ROOT_DIR=${HOME}/bin/scalikejdbc-cli
mkdir -p ${ROOT_DIR}
CONFIG_PROPS=${ROOT_DIR}/config.properties
DBCONSOLE_COMMAND=${ROOT_DIR}/dbconsole
DBCONSOLE_CONFIG_COMMAND=${ROOT_DIR}/dbconsole_config
BUILD_SBT=${ROOT_DIR}/build.sbt

cd ${ROOT_DIR}
rm -f sbt-launch.jar*
wget http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.12.1/sbt-launch.jar

if [ ! -f ${CONFIG_PROPS} ]; then
  echo 'default.jdbc.url=jdbc:h2:mem:default
default.jdbc.username=
default.jdbc.password=
sandbox.jdbc.url=jdbc:h2:mem:sandbox
sandbox.jdbc.username=
sandbox.jdbc.password=
' > ${CONFIG_PROPS}
fi

echo '#!/bin/sh
cd `dirname $0`
${EDITOR:=vi} ~/bin/scalikejdbc-cli/config.properties
' > ${DBCONSOLE_CONFIG_COMMAND}

echo '#!/bin/sh
cd `dirname $0`

echo
echo "--- DB Console with ScalikeJDBC ---"
echo

while [ "${PROFILE}" = "" ]
do
  echo "Select a profile defined at ~/bin/scalikejdbc-cli/config.properties"
  echo
  read PROFILE
done

echo
echo "Starting sbt console..."
echo

java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M \
  -jar `dirname $0`/sbt-launch.jar \
  -Dscalikejdbc-cli.config.profile=${PROFILE} \
  console
' > ${DBCONSOLE_COMMAND}


echo 'resolvers += "oracle driver repo" at "http://dist.codehaus.org/mule/dependencies/maven2"

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"         % "[1.4,)",
  "org.slf4j"          % "slf4j-simple"         % "[1.7,)",
  "com.h2database"     % "h2"                   % "[1.3,)", 
  "org.apache.derby"   % "derby"                % "[10.8.2,)",
  "org.xerial"         % "sqlite-jdbc"          % "[3.7,)",
  "org.hsqldb"         % "hsqldb"               % "[2.2,)",
  "mysql"              % "mysql-connector-java" % "[5.1,)",
  "postgresql"         % "postgresql"           % "9.1-901.jdbc4",
  "oracle"             % "ojdbc14"              % "10.2.0.2"
)

initialCommands := """import scalikejdbc._
import scalikejdbc.StringSQLRunner._
def initialize() {
  val props = new java.util.Properties
  using(new java.io.FileInputStream("config.properties")) { is => props.load(is) }
  val profile = Option(System.getProperty("scalikejdbc-cli.config.profile")).getOrElse("default")
  Option(props.get(profile + ".jdbc.url")).map { obj =>
    val url = obj.toString
    if (url.startsWith("jdbc:postgresql")) { Class.forName("org.postgresql.Driver")
    } else if (url.startsWith("jdbc:mysql")) { Class.forName("com.mysql.jdbc.Driver")
    } else if (url.startsWith("jdbc:oracle")) { Class.forName("oracle.jdbc.driver.OracleDriver")
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
    throw new IllegalStateException("JDBC settings for \"" + profile + "\" is not found. Try \"dbconsole_config\" command.")
  }
}
initialize()
case class SQLResponse(list: List[Map[String, Any]]) {
  def result = list.head.apply("RESULT")
  def singleColumn = list.map(m => m.apply(m.keys.head))
}
implicit def ListToSQLResponse(list: List[Map[String, Any]]) = SQLResponse(list)
"""
' > ${BUILD_SBT}

SHELL_PROFILE=${HOME}/.bash_profile
if [[ "$SHELL" == *zsh* ]]; then 
  SHELL_PROFILE=${HOME}/.zprofile
fi

if [ ! `grep 'PATH=${PATH}:${HOME}/bin/scalikejdbc-cli' ${SHELL_PROFILE}` ]; then
  echo "PATH=\${PATH}:\${HOME}/bin/scalikejdbc-cli" >> ${SHELL_PROFILE}
fi

chmod +x ${DBCONSOLE_COMMAND}
chmod +x ${DBCONSOLE_CONFIG_COMMAND}

echo "
command installed to ${DBCONSOLE_COMMAND}
command installed to ${DBCONSOLE_CONFIG_COMMAND}
"
echo "Please execute 'source ${SHELL_PROFILE}'
"

