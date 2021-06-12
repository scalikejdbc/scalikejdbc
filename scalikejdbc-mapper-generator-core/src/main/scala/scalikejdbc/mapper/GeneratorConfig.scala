package scalikejdbc.mapper

import java.util.Locale.ENGLISH

case class GeneratorConfig(
  srcDir: String = "src/main/scala",
  testDir: String = "src/test/scala",
  packageName: String = "models",
  template: GeneratorTemplate = GeneratorTemplate.queryDsl,
  testTemplate: GeneratorTestTemplate = GeneratorTestTemplate(""),
  lineBreak: LineBreak = LineBreak("\n"),
  encoding: String = "UTF-8",
  autoConstruct: Boolean = false,
  defaultAutoSession: Boolean = true,
  dateTimeClass: DateTimeClass = DateTimeClass.ZonedDateTime,
  tableNameToClassName: String => String = GeneratorConfig.toCamelCaseCaps,
  columnNameToFieldName: String => String =
    GeneratorConfig.columnNameToFieldNameBasic andThen GeneratorConfig
      .addSuffixIfConflict("Column"),
  returnCollectionType: ReturnCollectionType = ReturnCollectionType.List,
  view: Boolean = false,
  tableNamesToSkip: collection.Seq[String] = Nil,
  tableNameToBaseTypes: String => Seq[String] = _ => Nil,
  tableNameToCompanionBaseTypes: String => Seq[String] = _ => Nil,
  tableNameToSyntaxName: String => String =
    GeneratorConfig.tableNameToSyntaxNameDefault,
  tableNameToSyntaxVariableName: String => String =
    GeneratorConfig.tableNameToSyntaxNameDefault
)

object GeneratorConfig {
  private def toProperCase(s: String): String = {
    if (s == null || s.trim.isEmpty) ""
    else
      s.substring(0, 1).toUpperCase(ENGLISH) + s
        .substring(1)
        .toLowerCase(ENGLISH)
  }

  def toCamelCaseCaps: String => String = _.split("_").foldLeft("") {
    (camelCaseString, part) =>
      val newPart =
        if (part.nonEmpty && Character.isDigit(part.charAt(0))) s"_$part"
        else part
      camelCaseString + toProperCase(newPart)
  }

  val reservedWords: Set[String] = Set(
    "abstract",
    "case",
    "catch",
    "class",
    "def",
    "do",
    "else",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "forSome",
    "if",
    "implicit",
    "import",
    "lazy",
    "match",
    "new",
    "null",
    "macro",
    "object",
    "override",
    "package",
    "private",
    "protected",
    "return",
    "sealed",
    "super",
    "then",
    "this",
    "throw",
    "trait",
    "try",
    "true",
    "type",
    "val",
    "var",
    "while",
    "with",
    "yield"
  )

  val quoteReservedWord: String => String = { name =>
    if (reservedWords(name)) "`" + name + "`"
    else name
  }

  val conflictMethods: Set[String] = Set(
    "toString",
    "hashCode",
    "wait",
    "getClass",
    "notify",
    "notifyAll",
    "productArity",
    "productElementName",
    "productElementNames",
    "productIterator",
    "productPrefix",
    "copy"
  )

  def addSuffixIfConflict(suffix: String): String => String = { name =>
    if (conflictMethods(name)) name + suffix
    else name
  }

  val lowerCamelCase: String => String =
    GeneratorConfig.toCamelCaseCaps.andThen { camelCase =>
      s"${camelCase.head.toLower}${camelCase.tail}"
    }

  val columnNameToFieldNameBasic: String => String = {
    GeneratorConfig.lowerCamelCase andThen GeneratorConfig.quoteReservedWord
  }

  private val tableNameToSyntaxNameDefault: String => String = { tableName =>
    val name = "[A-Z]".r
      .findAllIn(toCamelCaseCaps(tableName))
      .mkString
      .toLowerCase(ENGLISH)
    if (name == "rs" || name.isEmpty) "r" else name
  }
}
