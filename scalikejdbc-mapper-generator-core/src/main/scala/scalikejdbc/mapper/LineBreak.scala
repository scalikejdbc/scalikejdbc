package scalikejdbc.mapper

object LineBreak {
  def value(name: String) = name match {
    case "CR" => "\r"
    case "LF" => "\n"
    case "CRLF" => "\r\n"
    case _ => "\n"
  }
}

case class LineBreak(name: String) {
  def value = LineBreak.value(name)
}
