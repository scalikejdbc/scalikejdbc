@if (@_win32==@_win16) /*
@echo off
setlocal

set root_dir=%userprofile%\bin\scalikejdbc-cli
if not exist "%root_dir%" ( mkdir "%root_dir%" )
set config_props=%root_dir%\config.properties
set dbconsole_command=%root_dir%\dbconsole.bat
set build_sbt=%root_dir%\build.sbt
set self_path=%~f0

pushd "%root_dir%"
  if exist "sbt-launch.jar*" ( del /f /q "sbt-launch.jar*" )
  call cscript "%self_path%" //E:JScript //Nologo https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/1.9.9/sbt-launch-1.9.9.jar
popd

set db_dir=%root_dir%\db
if not exist "%db_dir%" ( mkdir "%db_dir%" )
pushd "%db_dir%"
  if exist "sandbox.h2.db" ( del /f /q "sandbox.h2.db" )
  call cscript "%self_path%" //E:JScript //Nologo https://raw.github.com/scalikejdbc/scalikejdbc/master/scalikejdbc-cli/db/sandbox.h2.db sandbox.h2.db
popd

if not exist "%config_props%" (
  >>"%config_props%" echo sandbox.jdbc.url=jdbc:h2:file:./db/sandbox
  >>"%config_props%" echo sandbox.jdbc.username=
  >>"%config_props%" echo sandbox.jdbc.password=
  >>"%config_props%" echo #mysql.jdbc.url=jdbc:mysql://localhost:3306/dbname
  >>"%config_props%" echo #postgres.jdbc.url=jdbc:postgresql://localhost:5432/dbname
  >>"%config_props%" echo #oracle.jdbc.url=jdbc:oracle:thin:@localhost:1521:dbname
)

if exist "%dbconsole_command%" ( del /f /q "%dbconsole_command%" )
>>"%dbconsole_command%" echo @echo off
>>"%dbconsole_command%" echo setlocal
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo pushd "%%~dp0"
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo if "%%1" == "-e" (
>>"%dbconsole_command%" echo   call :edit_config
>>"%dbconsole_command%" echo   exit /b 0
>>"%dbconsole_command%" echo )
>>"%dbconsole_command%" echo if "%%1" == "--edit" (
>>"%dbconsole_command%" echo   call :edit_config
>>"%dbconsole_command%" echo   exit /b 0
>>"%dbconsole_command%" echo )
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo if "%%1" == "-c" (
>>"%dbconsole_command%" echo   call :run_sbt clean
>>"%dbconsole_command%" echo   exit /b 0
>>"%dbconsole_command%" echo )
>>"%dbconsole_command%" echo if "%%1" == "--clean" (
>>"%dbconsole_command%" echo   call :run_sbt clean
>>"%dbconsole_command%" echo   exit /b 0
>>"%dbconsole_command%" echo )
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo if "%%1" == "-h" (
>>"%dbconsole_command%" echo   call :show_help
>>"%dbconsole_command%" echo   exit /b 0
>>"%dbconsole_command%" echo )
>>"%dbconsole_command%" echo if "%%1" == "--help" (
>>"%dbconsole_command%" echo   call :show_help
>>"%dbconsole_command%" echo   exit /b 0
>>"%dbconsole_command%" echo )
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo set _arg=%%1
>>"%dbconsole_command%" echo if "%%_arg:~0,1%%" == "-" (
>>"%dbconsole_command%" echo   echo.
>>"%dbconsole_command%" echo   echo ERROR: Unknown option ^^(%%_arg%%^^)
>>"%dbconsole_command%" echo   call :show_help
>>"%dbconsole_command%" echo   exit /b 1
>>"%dbconsole_command%" echo )
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo set _profile=%%1
>>"%dbconsole_command%" echo :read_profile_start
>>"%dbconsole_command%" echo   if not "%%_profile%%" == "" ( goto :read_profile_end )
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo   set /p _profile="Select a profile>"
>>"%dbconsole_command%" echo   echo.
>>"%dbconsole_command%" echo   goto :read_profile_start
>>"%dbconsole_command%" echo :read_profile_end
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo echo.
>>"%dbconsole_command%" echo echo Starting sbt console for %%_profile%% ...
>>"%dbconsole_command%" echo echo.
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo call :run_sbt console
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo exit /b 0
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo :edit_config
>>"%dbconsole_command%" echo   call notepad %%userprofile%%\bin\scalikejdbc-cli\config.properties
>>"%dbconsole_command%" echo exit /b 0
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo :show_help
>>"%dbconsole_command%" echo   echo.
>>"%dbconsole_command%" echo   echo dbconsole is an extended sbt console to connect database easily.
>>"%dbconsole_command%" echo   echo.
>>"%dbconsole_command%" echo   echo Usage:
>>"%dbconsole_command%" echo   echo   dbconsole [OPTION]... [PROFILE]
>>"%dbconsole_command%" echo   echo.
>>"%dbconsole_command%" echo   echo General options:
>>"%dbconsole_command%" echo   echo   -e, --edit    edit configuration, then exit
>>"%dbconsole_command%" echo   echo   -c, --clean   clean sbt environment, then exit
>>"%dbconsole_command%" echo   echo   -h, --help    show this help, then exit
>>"%dbconsole_command%" echo   echo.
>>"%dbconsole_command%" echo exit /b 0
>>"%dbconsole_command%" echo.
>>"%dbconsole_command%" echo :run_sbt
>>"%dbconsole_command%" echo   call java -Xms256M -Xmx512M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxMetaspaceSize=386M ^^
>>"%dbconsole_command%" echo     -jar "%%~dp0\sbt-launch.jar" ^^
>>"%dbconsole_command%" echo     -Dscalikejdbc-cli.config.profile=%%_profile%% ^^
>>"%dbconsole_command%" echo     %%1
>>"%dbconsole_command%" echo exit /b 0
>>"%dbconsole_command%" echo.

if exist "%build_sbt%" ( del /f /q "%build_sbt%" )
>>"%build_sbt%" echo.
>>"%build_sbt%" echo scalaVersion := "2.12.19"
>>"%build_sbt%" echo.
>>"%build_sbt%" echo libraryDependencies ++= Seq(
>>"%build_sbt%" echo   "org.scalikejdbc"    %%%% "scalikejdbc"        %% "3.4.0",
>>"%build_sbt%" echo   "org.slf4j"          %% "slf4j-simple"         %% "1.7.29",
>>"%build_sbt%" echo   "com.h2database"     %% "h2"                   %% "1.4.200",
>>"%build_sbt%" echo   "org.apache.derby"   %% "derby"                %% "10.14.2.0",
>>"%build_sbt%" echo   "org.xerial"         %% "sqlite-jdbc"          %% "3.34.0",
>>"%build_sbt%" echo   "org.hsqldb"         %% "hsqldb"               %% "2.5.2",
>>"%build_sbt%" echo   "com.mysql"          %% "mysql-connector-j"    %% "8.3.0",
>>"%build_sbt%" echo   "org.postgresql"     %% "postgresql"           %% "42.2.22"
>>"%build_sbt%" echo )
>>"%build_sbt%" echo.
>>"%build_sbt%" echo initialCommands := """import scalikejdbc._
>>"%build_sbt%" echo import scalikejdbc.StringSQLRunner._
>>"%build_sbt%" echo def initialize() = {
>>"%build_sbt%" echo   val props = new java.util.Properties
>>"%build_sbt%" echo   using(new java.io.FileInputStream("config.properties")) { is =^> props.load(is) }
>>"%build_sbt%" echo   val profile = Option(System.getProperty("scalikejdbc-cli.config.profile")).getOrElse("default")
>>"%build_sbt%" echo   Option(props.get(profile + ".jdbc.url")).map { obj =^>
>>"%build_sbt%" echo     val url = obj.toString
>>"%build_sbt%" echo     if (url.startsWith("jdbc:postgresql")) { Class.forName("org.postgresql.Driver")
>>"%build_sbt%" echo     } else if (url.startsWith("jdbc:mysql")) { Class.forName("com.mysql.cj.jdbc.Driver")
>>"%build_sbt%" echo     } else if (url.startsWith("jdbc:h2")) { Class.forName("org.h2.Driver")
>>"%build_sbt%" echo     } else if (url.startsWith("jdbc:hsqldb")) { Class.forName("org.hsqldb.jdbc.JDBCDriver")
>>"%build_sbt%" echo     } else if (url.startsWith("jdbc:derby")) { Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
>>"%build_sbt%" echo     } else if (url.startsWith("jdbc:sqlite")) { Class.forName("org.sqlite.JDBC")
>>"%build_sbt%" echo     } else { throw new IllegalStateException("Driver is not found for " + url)
>>"%build_sbt%" echo     }
>>"%build_sbt%" echo     val user = Option(props.get(profile + ".jdbc.username")).map(_.toString).orNull[String]
>>"%build_sbt%" echo     val password = Option(props.get(profile + ".jdbc.password")).map(_.toString).orNull[String]
>>"%build_sbt%" echo     ConnectionPool.singleton(url, user, password)
>>"%build_sbt%" echo   }.getOrElse { 
>>"%build_sbt%" echo     throw new IllegalStateException("JDBC settings for \"" + profile + "\" is not found. Try \"dbconsole --edit\".")
>>"%build_sbt%" echo   }
>>"%build_sbt%" echo }
>>"%build_sbt%" echo initialize()
>>"%build_sbt%" echo def describe(table: String) = println(DB.describe(table))
>>"%build_sbt%" echo def tables = println(DB.showTables())
>>"%build_sbt%" echo implicit val session: DBSession = AutoSession
>>"%build_sbt%" echo """

echo Command installed to %dbconsole_command%
echo.
echo Please add the following path to the 'Path' Environment Variable,
echo   %root_dir%
echo and then, execute 'dbconsole -h' command.
echo.

pause
exit /b 0
*/ @end

function echo( s ) {
  WScript.Echo( s || "" );
}

function wget( url, filename ) {
  var client = WScript.CreateObject( "Msxml2.XMLHTTP" );
  var outStream = WScript.CreateObject( "Adodb.Stream" );
  var typeBinary = 1;
  var createOverWrite = 2;
  
  try {
    client.open( "GET", url, false );
    client.send();
  } catch (e) {
    echo( e.message );
    return 1;
  }
  
  outStream.type = typeBinary;
  outStream.Open();
  outStream.Write( client.responseBody );
  outStream.SaveToFile( filename, createOverWrite );
  
  return 0;
}

var args = WScript.Arguments;
var url = args(0);
var filename = args(1);

echo( "Downloading " + filename + " ..." );
echo( "  from: " + url );
var rc = wget( url, filename );
if ( rc == 0 ) {
  echo( "Download success!\n" );
} else {
  echo( "Download failed...\n" );
}
WScript.Quit( rc );
