import sbt._
import xsbt.FileUtilities.write

trait Boilerplate {
  self: DefaultProject =>

  def srcManagedScala = "src_managed" / "main" / "scala"

  lazy val generateTupleW = {
    val cleanSrcManaged = cleanTask(srcManagedScala) named ("clean src_managed")
    task {
      val arities = 2 to 22

      def writeFileScalazPackage(fileName: String, source: String): Unit = {
        val file = (srcManagedScala / "scalaz" / fileName).asFile
        write(file, source)
      }

      for (arity: Int <- arities) {
        val tupleWSource: String = {
          val tparams = (0 until arity).map(n => ('A' + n).toChar).mkString(", ")
          val params = (1 to arity).map("_" + _).mkString(", ")
          """|
          |trait Tuple%dW[%s] extends PimpedType[Tuple%d[%s]] {
          |  def fold[Z](f: => (%s) => Z): Z = {import value._; f(%s)}
          |}
          |
          |trait Tuple%dWs {
          |  implicit def ToTuple%dW[%s](t: (%s)): Tuple%dW[%s] = new { val value = t } with Tuple%dW[%s]
          |}
          |""".stripMargin.format(arity, tparams, arity, tparams, tparams, params, arity, arity,
            tparams, tparams, arity, tparams, arity, tparams)
        }

        val source = "package scalaz\n\n" + tupleWSource
        writeFileScalazPackage("Tuple%dWs.scala".format(arity), source)
      }

      val source = "package scalaz\n\n" + "trait TupleWs extends " + arities.map(n => "Tuple%dWs".format(n)).mkString("\n     with ")
      writeFileScalazPackage("TupleW.scala", source)
      None
    } dependsOn (cleanSrcManaged)
  }
}