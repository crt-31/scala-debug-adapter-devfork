package ch.epfl.scala.debugadapter

import ch.epfl.scala.debugadapter.testfmk.TestingDebuggee
import scala.tools.nsc.ExpressionCompilerBridge

import java.nio.file.Files
import scala.collection.mutable.Buffer
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
 * This class is used to enter the expression compiler with a debugger
 *  It is not meant to be run in the CI
 */
class ExpressionCompilerDebug extends munit.FunSuite {
  val scalaVersion = ScalaVersion.`2.12`
  val compiler = new ExpressionCompilerBridge

  override def munitTimeout: Duration = 1.hour

  test("debug test".ignore) {
    val source =
      """|package example
         |
         |object Main {
         |  def main(args: Array[String]): Unit = {
         |    val list = List(1)
         |    for {
         |      x <- list
         |      y <- list
         |      z = x + y
         |    } yield x
         |    for {
         |      x <- list
         |      if x == 1
         |    } yield x
         |    for (x <- list) yield x
         |    for (x <- list) println(x)
         |  }
         |}
         |""".stripMargin
    implicit val debuggee: TestingDebuggee = TestingDebuggee.mainClass(source, "example.Main", scalaVersion)
    evaluate(13, "x", Set("x"))
  }

  private def evaluate(line: Int, expression: String, localVariables: Set[String] = Set.empty)(implicit
      debuggee: TestingDebuggee
  ): Unit = {
    val out = debuggee.tempDir.resolve("expr-classes")
    if (Files.notExists(out)) Files.createDirectory(out)
    val errors = Buffer.empty[String]
    compiler.run(
      out,
      "Expression",
      debuggee.classPathString,
      debuggee.mainModule.scalacOptions.toArray,
      debuggee.mainSource,
      line,
      expression,
      localVariables.asJava,
      "example",
      error => {
        println(Console.RED + error + Console.RESET)
        errors += error
      },
      testMode = true
    )
    if (errors.nonEmpty) throw new Exception("Evaluation failed:\n" + errors.mkString("\n"))
  }
}
