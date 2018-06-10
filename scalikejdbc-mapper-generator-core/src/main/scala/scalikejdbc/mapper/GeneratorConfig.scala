package scalikejdbc.mapper

import java.util.Locale.ENGLISH

case class GeneratorConfig(
  srcDir: String = "src/main/scala",
  testDir: String = "src/test/scala",
  packageName: String = "models",
  template: GeneratorTemplate = GeneratorTemplate.queryDsl,
  testTemplate: GeneratorTestTemplate = GeneratorTestTemplate(""),
  lineBreak: LineBreak = LineBreak("\n"),
  @deprecated("will be removed", "3.3.0") caseClassOnly: Boolean = false,
  encoding: String = "UTF-8",
  autoConstruct: Boolean = false,
  defaultAutoSession: Boolean = true,
  dateTimeClass: DateTimeClass = DateTimeClass.ZonedDateTime,
  tableNameToClassName: String => String = GeneratorConfig.toCamelCase,
  columnNameToFieldName: String => String = GeneratorConfig.columnNameToFieldNameBasic andThen GeneratorConfig.addSuffixIfConflict("Column"),
  returnCollectionType: ReturnCollectionType = ReturnCollectionType.List,
  view: Boolean = false,
  tableNamesToSkip: collection.Seq[String] = List(),
  tableNameToBaseTypes: String => Seq[String] = _ => Nil,
  tableNameToCompanionBaseTypes: String => Seq[String] = _ => Nil,
  tableNameToSyntaxName: String => String = GeneratorConfig.tableNameToSyntaxNameDefault,
  tableNameToSyntaxVariableName: String => String = GeneratorConfig.tableNameToSyntaxNameDefault)

object GeneratorConfig {
  private def toProperCase(s: String): String = {
    if (s == null || s.trim.isEmpty) ""
    else s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1).toLowerCase(ENGLISH)
  }

  private val toCamelCase: String => String = _.split("_").foldLeft("") {
    (camelCaseString, part) =>
      camelCaseString + toProperCase(part)
  }

  val reservedWords: Set[String] = Set(
    "abstract", "case", "catch", "class", "def",
    "do", "else", "extends", "false", "final",
    "finally", "for", "forSome", "if", "implicit",
    "import", "lazy", "match", "new", "null", "macro",
    "object", "override", "package", "private", "protected",
    "return", "sealed", "super", "then", "this", "throw",
    "trait", "try", "true", "type", "val",
    "var", "while", "with", "yield")

  val quoteReservedWord: String => String = {
    name =>
      if (reservedWords(name)) "`" + name + "`"
      else name
  }

  val conflictMethods: Set[String] = Set(
    "toString", "hashCode", "wait", "getClass", "notify", "notifyAll",
    "productArity", "productIterator", "productPrefix", "copy")

  def addSuffixIfConflict(suffix: String): String => String = {
    name =>
      if (conflictMethods(name)) name + suffix
      else name
  }

  val lowerCamelCase: String => String =
    GeneratorConfig.toCamelCase.andThen {
      camelCase => camelCase.head.toLower + camelCase.tail
    }

  val columnNameToFieldNameBasic: String => String = {
    GeneratorConfig.lowerCamelCase andThen GeneratorConfig.quoteReservedWord
  }

  private val tableNameToSyntaxNameDefault: String => String = { tableName =>
    val name = "[A-Z]".r.findAllIn(toCamelCase(tableName)).mkString.toLowerCase(ENGLISH)
    if (name == "rs" || name.isEmpty) "r" else name
  }
}
